package protect.budgetwatch;

import android.app.Activity;
import android.database.Cursor;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 17)
public class DatabaseTest
{
    private DBHelper db;
    private long nowMs;

    @Before
    public void setUp()
    {
        Activity activity = Robolectric.setupActivity(BudgetViewActivity.class);
        db = new DBHelper(activity);
        nowMs = System.currentTimeMillis();
    }

    @Test
    public void addRemoveOneBudget()
    {
        assertEquals(db.getBudgetCount(), 0);
        boolean result = db.insertBudget("budget", 100);
        assertTrue(result);
        assertEquals(db.getBudgetCount(), 1);

        Budget budget = db.getBudgetStoredOnly("budget");
        assertNotNull(budget);
        assertEquals("budget", budget.name);
        assertEquals(100, budget.max);
        assertEquals(0, budget.current);

        List<Budget> budgets = db.getBudgets(0, nowMs);
        assertEquals(1, budgets.size());
        assertEquals("budget", budgets.get(0).name);
        assertEquals(100, budgets.get(0).max);
        assertEquals(0, budgets.get(0).current);

        result = db.deleteBudget("budget");
        assertTrue(result);
        assertEquals(db.getBudgetNames().size(), 0);
        assertNull(db.getBudgetStoredOnly("budget"));
    }

    @Test
    public void checkTransactionsForBudget()
    {
        boolean result = db.insertBudget("budget", 100);
        assertTrue(result);

        final int NUM_EXPENSES = 1000;
        int expectedCurrent = 0;

        for(int index = 0; index < NUM_EXPENSES; index++)
        {
            result = db.insertTransaction(DBHelper.TransactionDbIds.EXPENSE, "", "", "budget", index, "", nowMs);
            assertTrue(result);
            expectedCurrent += index;
        }

        Cursor expenses = db.getExpenses();
        assertEquals(NUM_EXPENSES, expenses.getCount());
        expenses.close();

        Budget budget = db.getBudgetStoredOnly("budget");
        assertEquals(0, budget.current);

        // Budget current value should be positive, as there are only
        // expenses

        List<Budget> budgets = db.getBudgets(0, nowMs);
        assertEquals(1, budgets.size());
        assertEquals("budget", budgets.get(0).name);
        assertEquals(100, budgets.get(0).max);
        assertEquals(expectedCurrent, budgets.get(0).current);

        final int NUM_REVENUES = 2000;

        for(int index = 0; index < NUM_REVENUES; index++)
        {
            result = db.insertTransaction(DBHelper.TransactionDbIds.REVENUE, "", "", "budget", index, "", nowMs);
            assertTrue(result);
            expectedCurrent -= index;
        }

        Cursor revenues = db.getRevenues();
        assertEquals(NUM_REVENUES, revenues.getCount());
        revenues.close();

        budget = db.getBudgetStoredOnly("budget");
        assertEquals(0, budget.current);

        // Budget current value should be negative, as there is more
        // revenue than expenses

        budgets = db.getBudgets(0, nowMs);
        assertEquals(1, budgets.size());
        assertEquals("budget", budgets.get(0).name);
        assertEquals(100, budgets.get(0).max);
        assertEquals(expectedCurrent, budgets.get(0).current);

        result = db.deleteBudget("budget");
        assertTrue(result);
        assertEquals(db.getBudgetNames().size(), 0);
        assertNull(db.getBudgetStoredOnly("budget"));

        // Deleting the budget does not delete the transactions
        expenses = db.getExpenses();
        assertEquals(NUM_EXPENSES, expenses.getCount());
        expenses.close();

        revenues = db.getRevenues();
        assertEquals(NUM_REVENUES, revenues.getCount());
        revenues.close();
    }

    @Test
    public void multipleBudgets()
    {
        final int NUM_BUDGETS = 1000;

        // Add in reverse order to test sorting later
        for(int index = NUM_BUDGETS; index > 0; index--)
        {
            String name = String.format("budget%4d", index);
            boolean result = db.insertBudget(name, index);
            assertTrue(result);
        }

        assertEquals(NUM_BUDGETS, db.getBudgetCount());

        List<Budget> budgets = db.getBudgets(0, nowMs);
        int index = 1;
        for(Budget budget : budgets)
        {
            assertEquals(budget.current, 0);
            assertEquals(budget.max, index);
            index++;
        }

        List<String> names = db.getBudgetNames();
        index = 1;
        for(String name : names)
        {
            String expectedName = String.format("budget%4d", index);
            assertEquals(expectedName, name);
            index++;
        }
    }

    @Test
    public void updateBudget()
    {
        boolean result = db.insertBudget("budget", 100);
        assertTrue(result);

        for(int index = 0; index < 1000; index++)
        {
            result = db.updateBudget("budget", index);
            assertTrue(result);
            Budget budget = db.getBudgetStoredOnly("budget");
            assertEquals(index, budget.max);
        }
    }

    @Test
    public void updateMissingBudget()
    {
        boolean result = db.updateBudget("budget", 0);
        assertEquals(false, result);

    }

    @Test
    public void emptyBudgetValues()
    {
        boolean result = db.insertBudget("", 0);
        assertTrue(result);
        assertEquals(1, db.getBudgetCount());

        Budget budget = db.getBudgetStoredOnly("");
        assertEquals("", budget.name);
        assertEquals(0, budget.max);
    }

    private void checkTransaction(final Cursor cursor, final int type, final String description,
                                  final String account, final String budget, final double value,
                                  final String note, final long dateInMs)
    {
        Transaction transaction = Transaction.toTransaction(cursor);
        checkTransaction(transaction, type, description, account, budget, value, note, dateInMs);
    }

    private void checkTransaction(final Transaction transaction, final int type, final String description,
                                  final String account, final String budget, final double value,
                                  final String note, final long dateInMs)
    {
        assertEquals(transaction.type, type);
        assertEquals(transaction.description, description);
        assertEquals(transaction.account, account);
        assertEquals(transaction.budget, budget);
        assertEquals(0, Double.compare(transaction.value, value));
        assertEquals(transaction.note, note);
        assertEquals(transaction.dateMs, dateInMs);
    }

    @Test
    public void addRemoveOneTransaction()
    {
        assertEquals(0, db.getTransactionCount(DBHelper.TransactionDbIds.EXPENSE));
        assertEquals(0, db.getTransactionCount(DBHelper.TransactionDbIds.REVENUE));

        db.insertTransaction(DBHelper.TransactionDbIds.EXPENSE, "description", "account", "budget",
                100.50, "note", nowMs);

        assertEquals(1, db.getTransactionCount(DBHelper.TransactionDbIds.EXPENSE));
        assertEquals(0, db.getTransactionCount(DBHelper.TransactionDbIds.REVENUE));

        Cursor expenses = db.getExpenses();

        expenses.moveToFirst();
        int expenseId = expenses.getInt(
                expenses.getColumnIndexOrThrow(DBHelper.TransactionDbIds.NAME));
        checkTransaction(expenses, DBHelper.TransactionDbIds.EXPENSE, "description", "account",
                "budget", 100.50, "note", nowMs);

        expenses.close();

        Transaction expenseTransaction = db.getTransaction(expenseId);
        checkTransaction(expenseTransaction, DBHelper.TransactionDbIds.EXPENSE, "description", "account",
                "budget", 100.50, "note", nowMs);

        db.deleteTransaction(expenseId);

        assertEquals(0, db.getTransactionCount(DBHelper.TransactionDbIds.EXPENSE));
        assertEquals(0, db.getTransactionCount(DBHelper.TransactionDbIds.REVENUE));

        db.insertTransaction(DBHelper.TransactionDbIds.REVENUE, "description2", "account2",
                "budget2",
                100.25, "note2", nowMs + 1);

        assertEquals(0, db.getTransactionCount(DBHelper.TransactionDbIds.EXPENSE));
        assertEquals(1, db.getTransactionCount(DBHelper.TransactionDbIds.REVENUE));

        Cursor revenues = db.getRevenues();
        revenues.moveToFirst();
        int revenueId = revenues.getInt(
                revenues.getColumnIndexOrThrow(DBHelper.TransactionDbIds.NAME));
        checkTransaction(revenues, DBHelper.TransactionDbIds.REVENUE, "description2", "account2",
                "budget2", 100.25, "note2", nowMs+1);

        revenues.close();

        Transaction revenueTransaction = db.getTransaction(revenueId);
        checkTransaction(revenueTransaction, DBHelper.TransactionDbIds.REVENUE, "description2", "account2",
                "budget2", 100.25, "note2", nowMs+1);

        db.deleteTransaction(revenueId);

        assertEquals(0, db.getTransactionCount(DBHelper.TransactionDbIds.EXPENSE));
        assertEquals(0, db.getTransactionCount(DBHelper.TransactionDbIds.REVENUE));
    }

    @Test
    public void multipleTransactions()
    {
        final int NUM_TRANSACTIONS = 1000;
        boolean result;

        for(int type : new Integer[]{DBHelper.TransactionDbIds.REVENUE, DBHelper.TransactionDbIds.EXPENSE})
        {
            // Add in increasing order to test sorting later
            for(int index = 1; index <= NUM_TRANSACTIONS; index++)
            {
                result = db.insertTransaction(type, "", "", "", 0, "", index);
                assertTrue(result);
            }
        }

        assertEquals(NUM_TRANSACTIONS, db.getTransactionCount(DBHelper.TransactionDbIds.EXPENSE));
        assertEquals(NUM_TRANSACTIONS, db.getTransactionCount(DBHelper.TransactionDbIds.REVENUE));

        for(Cursor cursor : new Cursor[]{db.getExpenses(), db.getRevenues()})
        {
            int index = NUM_TRANSACTIONS;
            while(cursor.moveToNext())
            {
                Transaction transaction = Transaction.toTransaction(cursor);
                assertEquals(index, transaction.dateMs);
                index--;
            }
            cursor.close();
        }
    }

    @Test
    public void updateTransaction()
    {
        boolean result = db.insertTransaction(DBHelper.TransactionDbIds.EXPENSE, "description",
                "account", "budget", 100.50, "note", nowMs);
        assertTrue(result);
        Transaction transaction = db.getTransaction(1);
        checkTransaction(transaction, DBHelper.TransactionDbIds.EXPENSE, "description",
                "account", "budget", 100.50, "note", nowMs);

        result = db.updateTransaction(1, DBHelper.TransactionDbIds.EXPENSE, "description2",
                "account2", "budget2", 25, "note2", nowMs + 1);
        assertTrue(result);
        transaction = db.getTransaction(1);
        checkTransaction(transaction, DBHelper.TransactionDbIds.EXPENSE, "description2",
                "account2", "budget2", 25, "note2", nowMs + 1);
    }

    @Test
    public void updateMissingTransaction()
    {
        boolean result = db.updateTransaction(1, DBHelper.TransactionDbIds.EXPENSE, "", "", "", 0, "", 0);
        assertEquals(false, result);
    }
}