package protect.budgetwatch;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.javatuples.Triplet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 17)
public class DatabaseTest
{
    private Context context;
    private DBHelper db;
    private long nowMs;
    private long lastYearMs;
    private final int MONTHS_PER_YEAR = 12;

    @Before
    public void setUp()
    {
        // Output logs emitted during tests so they may be accessed
        ShadowLog.stream = System.out;

        Activity activity = Robolectric.setupActivity(BudgetViewActivity.class);
        context = activity;
        db = new DBHelper(activity);
        nowMs = System.currentTimeMillis();

        Calendar lastYear = Calendar.getInstance();
        lastYear.set(Calendar.YEAR, lastYear.get(Calendar.YEAR)-1);
        lastYearMs = lastYear.getTimeInMillis();
    }

    @After
    public void tearDown()
    {
        db.close();
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

        List<Budget> budgets = db.getBudgets(lastYearMs, nowMs);
        assertEquals(1, budgets.size());
        assertEquals("budget", budgets.get(0).name);
        assertEquals(100*(MONTHS_PER_YEAR+1), budgets.get(0).max);
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

        final int NUM_EXPENSES = 10;
        int expectedCurrent = 0;

        for(int index = 0; index < NUM_EXPENSES; index++)
        {
            result = db.insertTransaction(DBHelper.TransactionDbIds.EXPENSE, "", "", "budget", index, "", nowMs, "");
            assertTrue(result);
            // Transaction with empty budget
            result = db.insertTransaction(DBHelper.TransactionDbIds.EXPENSE, "", "", "", index, "", nowMs, "");
            assertTrue(result);
            expectedCurrent += index;
        }

        Cursor expenses = db.getExpenses();
        assertEquals(NUM_EXPENSES*2, expenses.getCount());
        expenses.close();

        Budget budget = db.getBudgetStoredOnly("budget");
        assertEquals(0, budget.current);

        // Budget current value should be positive, as there are only
        // expenses

        List<Budget> budgets = db.getBudgets(lastYearMs, nowMs);
        assertEquals(1, budgets.size());
        assertEquals("budget", budgets.get(0).name);
        assertEquals(100*(MONTHS_PER_YEAR+1), budgets.get(0).max);
        assertEquals(expectedCurrent, budgets.get(0).current);

        Budget blankBudget = db.getBlankBudget(lastYearMs, nowMs);
        assertEquals("", blankBudget.name);
        assertEquals(0, blankBudget.max);
        assertEquals(expectedCurrent, blankBudget.current);

        final int NUM_REVENUES = 20;

        for(int index = 0; index < NUM_REVENUES; index++)
        {
            result = db.insertTransaction(DBHelper.TransactionDbIds.REVENUE, "", "", "budget", index, "", nowMs, "");
            assertTrue(result);
            // Transaction with empty budget
            result = db.insertTransaction(DBHelper.TransactionDbIds.REVENUE, "", "", "", index, "", nowMs, "");
            assertTrue(result);
            expectedCurrent -= index;
        }

        Cursor revenues = db.getRevenues();
        assertEquals(NUM_REVENUES*2, revenues.getCount());
        revenues.close();

        budget = db.getBudgetStoredOnly("budget");
        assertEquals(0, budget.current);

        // Budget current value should be negative, as there is more
        // revenue than expenses

        budgets = db.getBudgets(lastYearMs, nowMs);
        assertEquals(1, budgets.size());
        assertEquals("budget", budgets.get(0).name);
        assertEquals(100*(MONTHS_PER_YEAR+1), budgets.get(0).max);
        assertEquals(expectedCurrent, budgets.get(0).current);

        blankBudget = db.getBlankBudget(lastYearMs, nowMs);
        assertEquals("", blankBudget.name);
        assertEquals(0, blankBudget.max);
        assertEquals(expectedCurrent, blankBudget.current);

        result = db.deleteBudget("budget");
        assertTrue(result);
        assertEquals(db.getBudgetNames().size(), 0);
        assertNull(db.getBudgetStoredOnly("budget"));

        // Deleting the budget does not delete the transactions
        expenses = db.getExpenses();
        assertEquals(NUM_EXPENSES*2, expenses.getCount());
        expenses.close();

        revenues = db.getRevenues();
        assertEquals(NUM_REVENUES*2, revenues.getCount());
        revenues.close();
    }

    @Test
    public void multipleBudgets()
    {
        final int NUM_BUDGETS = 10;

        // Add in reverse order to test sorting later
        for(int index = NUM_BUDGETS; index > 0; index--)
        {
            String name = String.format("budget%4d", index);
            boolean result = db.insertBudget(name, index);
            assertTrue(result);
        }

        assertEquals(NUM_BUDGETS, db.getBudgetCount());

        List<Budget> budgets = db.getBudgets(lastYearMs, nowMs);
        int index = 1;
        for(Budget budget : budgets)
        {
            assertEquals(budget.current, 0);
            assertEquals(budget.max, index*(MONTHS_PER_YEAR+1));
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

        for(int index = 0; index < 10; index++)
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
                                  final String note, final long dateInMs, final String receipt)
    {
        Transaction transaction = Transaction.toTransaction(cursor);
        checkTransaction(transaction, type, description, account, budget, value, note, dateInMs,
                receipt);
    }

    private void checkTransaction(final Transaction transaction, final int type, final String description,
                                  final String account, final String budget, final double value,
                                  final String note, final long dateInMs, final String receipt)
    {
        assertEquals(transaction.type, type);
        assertEquals(transaction.description, description);
        assertEquals(transaction.account, account);
        assertEquals(transaction.budget, budget);
        assertEquals(0, Double.compare(transaction.value, value));
        assertEquals(transaction.note, note);
        assertEquals(transaction.dateMs, dateInMs);
        assertEquals(transaction.receipt, receipt);
    }

    @Test
    public void addRemoveOneTransaction()
    {
        TransactionDatabaseChangedReceiver dbChanged = new TransactionDatabaseChangedReceiver();
        context.registerReceiver(dbChanged, new IntentFilter(TransactionDatabaseChangedReceiver.ACTION_DATABASE_CHANGED));
        assertFalse(dbChanged.hasChanged());

        assertEquals(0, db.getTransactionCount(DBHelper.TransactionDbIds.EXPENSE));
        assertEquals(0, db.getTransactionCount(DBHelper.TransactionDbIds.REVENUE));

        assertFalse(dbChanged.hasChanged());

        db.insertTransaction(DBHelper.TransactionDbIds.EXPENSE, "description", "account", "budget",
                100.50, "note", nowMs, "receipt");

        assertTrue(dbChanged.hasChanged());
        dbChanged.reset();

        assertEquals(1, db.getTransactionCount(DBHelper.TransactionDbIds.EXPENSE));
        assertEquals(0, db.getTransactionCount(DBHelper.TransactionDbIds.REVENUE));

        Cursor expenses = db.getExpenses();

        expenses.moveToFirst();
        int expenseId = expenses.getInt(
                expenses.getColumnIndexOrThrow(DBHelper.TransactionDbIds.NAME));
        checkTransaction(expenses, DBHelper.TransactionDbIds.EXPENSE, "description", "account",
                "budget", 100.50, "note", nowMs, "receipt");

        expenses.close();

        Transaction expenseTransaction = db.getTransaction(expenseId);
        checkTransaction(expenseTransaction, DBHelper.TransactionDbIds.EXPENSE, "description", "account",
                "budget", 100.50, "note", nowMs, "receipt");

        assertFalse(dbChanged.hasChanged());

        db.deleteTransaction(expenseId);

        assertTrue(dbChanged.hasChanged());
        dbChanged.reset();

        assertEquals(0, db.getTransactionCount(DBHelper.TransactionDbIds.EXPENSE));
        assertEquals(0, db.getTransactionCount(DBHelper.TransactionDbIds.REVENUE));

        assertFalse(dbChanged.hasChanged());

        db.insertTransaction(DBHelper.TransactionDbIds.REVENUE, "description2", "account2",
                "budget2",
                100.25, "note2", nowMs + 1, "receipt2");

        assertTrue(dbChanged.hasChanged());
        dbChanged.reset();

        assertEquals(0, db.getTransactionCount(DBHelper.TransactionDbIds.EXPENSE));
        assertEquals(1, db.getTransactionCount(DBHelper.TransactionDbIds.REVENUE));

        Cursor revenues = db.getRevenues();
        revenues.moveToFirst();
        int revenueId = revenues.getInt(
                revenues.getColumnIndexOrThrow(DBHelper.TransactionDbIds.NAME));
        checkTransaction(revenues, DBHelper.TransactionDbIds.REVENUE, "description2", "account2",
                "budget2", 100.25, "note2", nowMs+1, "receipt2");

        revenues.close();

        Transaction revenueTransaction = db.getTransaction(revenueId);
        checkTransaction(revenueTransaction, DBHelper.TransactionDbIds.REVENUE, "description2", "account2",
                "budget2", 100.25, "note2", nowMs+1, "receipt2");

        assertFalse(dbChanged.hasChanged());

        db.deleteTransaction(revenueId);

        assertTrue(dbChanged.hasChanged());
        dbChanged.reset();

        assertEquals(0, db.getTransactionCount(DBHelper.TransactionDbIds.EXPENSE));
        assertEquals(0, db.getTransactionCount(DBHelper.TransactionDbIds.REVENUE));

        assertFalse(dbChanged.hasChanged());

        context.unregisterReceiver(dbChanged);
    }

    @Test
    public void multipleTransactions()
    {
        final int NUM_TRANSACTIONS = 10;
        boolean result;

        for(int type : new Integer[]{DBHelper.TransactionDbIds.REVENUE, DBHelper.TransactionDbIds.EXPENSE})
        {
            // Add in increasing order to test sorting later
            for(int index = 1; index <= NUM_TRANSACTIONS; index++)
            {
                result = db.insertTransaction(type, "", "", "", 0, "", index, "");
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
        TransactionDatabaseChangedReceiver dbChanged = new TransactionDatabaseChangedReceiver();
        context.registerReceiver(dbChanged, new IntentFilter(TransactionDatabaseChangedReceiver.ACTION_DATABASE_CHANGED));
        assertFalse(dbChanged.hasChanged());

        boolean result = db.insertTransaction(DBHelper.TransactionDbIds.EXPENSE, "description",
                "account", "budget", 100.50, "note", nowMs, "receipt");
        assertTrue(result);

        assertTrue(dbChanged.hasChanged());
        dbChanged.reset();

        Transaction transaction = db.getTransaction(1);
        checkTransaction(transaction, DBHelper.TransactionDbIds.EXPENSE, "description",
                "account", "budget", 100.50, "note", nowMs, "receipt");

        assertFalse(dbChanged.hasChanged());

        result = db.updateTransaction(1, DBHelper.TransactionDbIds.EXPENSE, "description2",
                "account2", "budget2", 25, "note2", nowMs + 1, "receipt2");
        assertTrue(result);

        assertTrue(dbChanged.hasChanged());
        dbChanged.reset();

        transaction = db.getTransaction(1);
        checkTransaction(transaction, DBHelper.TransactionDbIds.EXPENSE, "description2",
                "account2", "budget2", 25, "note2", nowMs + 1, "receipt2");

        assertFalse(dbChanged.hasChanged());

        context.unregisterReceiver(dbChanged);
    }

    @Test
    public void updateMissingTransaction()
    {
        TransactionDatabaseChangedReceiver dbChanged = new TransactionDatabaseChangedReceiver();
        context.registerReceiver(dbChanged, new IntentFilter(TransactionDatabaseChangedReceiver.ACTION_DATABASE_CHANGED));
        assertFalse(dbChanged.hasChanged());

        boolean result = db.updateTransaction(1, DBHelper.TransactionDbIds.EXPENSE, "", "", "", 0,
                "", 0, "");
        assertEquals(false, result);

        assertFalse(dbChanged.hasChanged());

        context.unregisterReceiver(dbChanged);
    }

    private void setupDatabaseVersion1(SQLiteDatabase database)
    {
        // Delete the tables as they exist now
        database.execSQL("drop table " + DBHelper.BudgetDbIds.TABLE);
        database.execSQL("drop table " + DBHelper.TransactionDbIds.TABLE);

        // Create the table as it existed in revision 1
        database.execSQL(
                "create table  " + DBHelper.BudgetDbIds.TABLE + "(" +
                        DBHelper.BudgetDbIds.NAME + " text primary key," +
                        DBHelper.BudgetDbIds.MAX + " INTEGER not null)");
        database.execSQL("create table " + DBHelper.TransactionDbIds.TABLE + "(" +
                DBHelper.TransactionDbIds.NAME + " INTEGER primary key autoincrement," +
                DBHelper.TransactionDbIds.TYPE + " INTEGER not null," +
                DBHelper.TransactionDbIds.DESCRIPTION + " TEXT not null," +
                DBHelper.TransactionDbIds.ACCOUNT + " TEXT," +
                DBHelper.TransactionDbIds.BUDGET + " TEXT," +
                DBHelper.TransactionDbIds.VALUE + " REAL not null," +
                DBHelper.TransactionDbIds.NOTE + " TEXT," +
                DBHelper.TransactionDbIds.DATE + " INTEGER not null)");
    }

    private void insertBudgetAndTransactionVersion1(SQLiteDatabase database,
                                                    final String budgetName, final int budgetMax,
                                                    final int type, final String description,
                                                    final String account, final double value,
                                                    final String note, final long dateInMs)
    {
        ContentValues contentValues = new ContentValues();
        contentValues.put(DBHelper.BudgetDbIds.NAME, budgetName);
        contentValues.put(DBHelper.BudgetDbIds.MAX, budgetMax);
        long newId = database.insert(DBHelper.BudgetDbIds.TABLE, null, contentValues);
        assertTrue(newId != -1);

        contentValues = new ContentValues();
        contentValues.put(DBHelper.TransactionDbIds.TYPE, type);
        contentValues.put(DBHelper.TransactionDbIds.DESCRIPTION, description);
        contentValues.put(DBHelper.TransactionDbIds.ACCOUNT, account);
        contentValues.put(DBHelper.TransactionDbIds.BUDGET, budgetName);
        contentValues.put(DBHelper.TransactionDbIds.VALUE, value);
        contentValues.put(DBHelper.TransactionDbIds.NOTE, note);
        contentValues.put(DBHelper.TransactionDbIds.DATE, dateInMs);
        newId = database.insert(DBHelper.TransactionDbIds.TABLE, null, contentValues);
        assertTrue(newId != -1);
    }

    @Test
    public void databaseUpgradeFromVersion1()
    {
        SQLiteDatabase database = db.getWritableDatabase();

        // Setup the database as it appeared in revision 1
        setupDatabaseVersion1(database);

        // Insert a budget and transaction
        insertBudgetAndTransactionVersion1(database, "budget", 100,DBHelper.TransactionDbIds.REVENUE,
                "description", "account", 1, "note", 200);

        // Upgrade database
        db.onUpgrade(database, DBHelper.ORIGINAL_DATABASE_VERSION, DBHelper.DATABASE_VERSION);

        // Determine that the entries are queryable and the fields are correct
        Budget budget = db.getBudgetStoredOnly("budget");
        assertEquals("budget", budget.name);
        assertEquals(100, budget.max);

        Transaction transaction = db.getTransaction(1);
        assertEquals(DBHelper.TransactionDbIds.REVENUE, transaction.type);
        assertEquals("description", transaction.description);
        assertEquals("account", transaction.account);
        assertEquals("budget", transaction.budget);
        assertEquals(0, Double.compare(1, transaction.value));
        assertEquals("note", transaction.note);
        assertEquals(200, transaction.dateMs);
        assertEquals("", transaction.receipt);

        database.close();
    }

    @Test
    public void queryTransactionsWithReceipts()
    {
        final int NUM_TRANSACTIONS_PER_TYPE = 100;

        for(int index = 0; index < NUM_TRANSACTIONS_PER_TYPE; index++)
        {
            for(int type : new int[]{DBHelper.TransactionDbIds.EXPENSE, DBHelper.TransactionDbIds.REVENUE})
            {
                for(boolean hasReceipt : new boolean[]{true, false})
                {
                    String receipt = hasReceipt ? "receipt" : "";
                    db.insertTransaction(type, "description", "account", "budget", 0, "note", index, receipt);
                }
            }
        }

        assertEquals(NUM_TRANSACTIONS_PER_TYPE * 2,
                db.getTransactionCount(DBHelper.TransactionDbIds.EXPENSE));
        assertEquals(NUM_TRANSACTIONS_PER_TYPE * 2,
                db.getTransactionCount(DBHelper.TransactionDbIds.REVENUE));

        // There are 100 * 2 * 2 transactions, half of which have a receipt.
        // Check that only those with receipts are queried
        final Long dateCutoffValue = (long)25;
        Cursor receiptTransactions = db.getTransactionsWithReceipts(dateCutoffValue);

        // There are 200 transactions with a receipt. A cutoff of 25 will return
        // the first quarter + 2 for the half way point.
        assertEquals(NUM_TRANSACTIONS_PER_TYPE / 2 + 2, receiptTransactions.getCount());

        while(receiptTransactions.moveToNext())
        {
            Transaction transaction = Transaction.toTransaction(receiptTransactions);
            assertEquals("receipt", transaction.receipt);
            assertTrue(transaction.dateMs <= dateCutoffValue);
        }

        receiptTransactions.close();

        // Now ensure that all receipt transactions will be found if no cutoff
        // date is provided

        receiptTransactions = db.getTransactionsWithReceipts(null);

        // There are 2000 transactions with a receipt.
        assertEquals(NUM_TRANSACTIONS_PER_TYPE*2, receiptTransactions.getCount());

        while(receiptTransactions.moveToNext())
        {
            Transaction transaction = Transaction.toTransaction(receiptTransactions);
            assertEquals("receipt", transaction.receipt);
        }

        receiptTransactions.close();
    }

    @Test
    public void filterTransactionsByBudget()
    {
        boolean result;

        final String BUDGET_1 = "budget1";
        final String BUDGET_2 = "budget2";

        for(String budget : new String[]{BUDGET_1, BUDGET_2})
        {
            result = db.insertBudget(budget, 100);
            assertTrue(result);
        }

        final int NUM_TRANSACTIONS_BUDGET_1 = 10;
        for(int index = 0; index < NUM_TRANSACTIONS_BUDGET_1; index++)
        {
            for(int type : new int[]{DBHelper.TransactionDbIds.EXPENSE, DBHelper.TransactionDbIds.REVENUE})
            {
                result = db.insertTransaction(type, "description", "account", BUDGET_1, 0, "note", index, "");
                assertTrue(result);
            }
        }

        final int NUM_TRANSACTIONS_BUDGET_2 = 20;
        for(int index = 0; index < NUM_TRANSACTIONS_BUDGET_2; index++)
        {
            for(int type : new int[]{DBHelper.TransactionDbIds.EXPENSE, DBHelper.TransactionDbIds.REVENUE})
            {
                result = db.insertTransaction(type, "description", "account", BUDGET_2, 0, "note", index, "");
                assertTrue(result);
            }
        }

        Cursor cursor = db.getTransactions(DBHelper.TransactionDbIds.EXPENSE, BUDGET_1, null, null, null);
        assertEquals(NUM_TRANSACTIONS_BUDGET_1, cursor.getCount());
        cursor.close();

        cursor = db.getTransactions(DBHelper.TransactionDbIds.REVENUE, BUDGET_1, null, null, null);
        assertEquals(NUM_TRANSACTIONS_BUDGET_1, cursor.getCount());
        cursor.close();

        cursor = db.getTransactions(DBHelper.TransactionDbIds.EXPENSE, BUDGET_2, null, null, null);
        assertEquals(NUM_TRANSACTIONS_BUDGET_2, cursor.getCount());
        cursor.close();

        cursor = db.getTransactions(DBHelper.TransactionDbIds.REVENUE, BUDGET_2, null, null, null);
        assertEquals(NUM_TRANSACTIONS_BUDGET_2, cursor.getCount());
        cursor.close();
    }

    private void printTransactions(Cursor cursor)
    {
        System.out.println("Contents: " + cursor.getCount());
        if(cursor.moveToFirst())
        {
            do
            {
                Transaction t = Transaction.toTransaction(cursor);
                System.out.println("   '" + t.description + "' '" + t.account + "' '" + t.budget + "' '" + t.value + "' '" + t.note);
            }while(cursor.moveToNext());
        }
    }

    @Test
    public void filterTransactions()
    {
        boolean result;

        final String BUDGET_1 = "budget1";
        final String BUDGET_2 = "budget2";

        for(String budget : new String[]{BUDGET_1, BUDGET_2})
        {
            result = db.insertBudget(budget, 100);
            assertTrue(result);
        }

        for(int type : new int[]{DBHelper.TransactionDbIds.EXPENSE, DBHelper.TransactionDbIds.REVENUE})
        {
            // First entry
            result = db.insertTransaction(type, "description", "account", BUDGET_1, 100, "note", 0, "");
            assertTrue(result);

            // Second entry
            result = db.insertTransaction(type, "destination", "actions", BUDGET_2, 10, "notation", 1, "receipt.jpg");
            assertTrue(result);

            // Third entry
            result = db.insertTransaction(type, "nowhere", "abc", BUDGET_1, 222, "words", 2, "description");
            assertTrue(result);
        }

        for(int type : new int[]{DBHelper.TransactionDbIds.EXPENSE, DBHelper.TransactionDbIds.REVENUE})
        {
            {
                // Should only pick up First
                for(String search : new String[]{"description", "account", "100", "note"})
                {
                    Cursor cursor = db.getTransactions(type, null, search, null, null);
                    printTransactions(cursor);
                    assertEquals(1, cursor.getCount());
                    cursor.moveToFirst();
                    Transaction transaction = Transaction.toTransaction(cursor);
                    assertEquals(transaction.description, "description");
                    cursor.close();
                }
            }

            {
                // Should pick up First, Second
                for(String search : new String[]{"des", "ac", "10", "not"})
                {
                    Cursor cursor = db.getTransactions(type, null, search, null, null);
                    printTransactions(cursor);
                    assertEquals(2, cursor.getCount());

                    boolean transactionAFound = false;
                    boolean transactionBFound = false;

                    cursor.moveToFirst();
                    do
                    {
                        Transaction transaction = Transaction.toTransaction(cursor);

                        switch (transaction.description)
                        {
                            case "description":
                                // This should be the first time seeing First
                                assertFalse(transactionAFound);
                                transactionAFound = true;
                                break;
                            case "destination":
                                // This should be the first time seeing Second
                                assertFalse(transactionBFound);
                                transactionBFound = true;
                                break;
                            default:
                                fail("Unexpected transaction: " + transaction.description);
                                break;
                        }
                    }while(cursor.moveToNext());

                    assertTrue(transactionAFound);
                    assertTrue(transactionBFound);

                    cursor.close();
                }
            }

            {
                // Should only pick up First
                for(String search : new String[]{"des", "ac", "10", "not"})
                {
                    Cursor cursor = db.getTransactions(type, BUDGET_1, search, null, null);
                    printTransactions(cursor);
                    assertEquals(1, cursor.getCount());
                    cursor.moveToFirst();
                    Transaction transaction = Transaction.toTransaction(cursor);
                    assertEquals(transaction.description, "description");
                    cursor.close();
                }
            }

            {
                LinkedList<Triplet<Long, Long, Integer>> tests = new LinkedList<>();
                tests.add(new Triplet<>(0L, 0L, 1));
                tests.add(new Triplet<>(1L, 1L, 1));
                tests.add(new Triplet<>(2L, 2L, 1));
                tests.add(new Triplet<>(3L, 3L, 0));
                tests.add(new Triplet<>(0L, 10000L, 3));
                tests.add(new Triplet<>(1L, 10000L, 2));
                for(Triplet<Long, Long, Integer> test : tests)
                {
                    Long startTimeMs = test.getValue0();
                    Long endTimeMs = test.getValue1();
                    int expectedEntries = test.getValue2();

                    Cursor cursor = db.getTransactions(type, null, null, startTimeMs, endTimeMs);

                    assertEquals(expectedEntries, cursor.getCount());

                    if(expectedEntries > 0)
                    {
                        cursor.moveToFirst();
                        do
                        {
                            Transaction transaction = Transaction.toTransaction(cursor);
                            assertTrue(transaction.dateMs >= startTimeMs);
                            assertTrue(transaction.dateMs <= endTimeMs);
                        }while(cursor.moveToNext());
                    }
                }
            }
        }
    }

    @Test
    public void testFetchingTransactionsWithBlankBudget()
    {
        final int budgetValue = 1234;
        final int expenseValue = 123;
        final int revenueValue = 12;

        for(int index = 0; index < 10; index++)
        {
            final String budgetName = "budget" + index;
            db.insertBudget("budget" + index, budgetValue);

            db.insertTransaction(DBHelper.TransactionDbIds.EXPENSE, "description", "account", budgetName, expenseValue, "note", nowMs, "receipt");
            db.insertTransaction(DBHelper.TransactionDbIds.REVENUE, "description", "account", budgetName, revenueValue, "note", nowMs, "receipt");
        }

        // Add a few blank budget transactions
        int value = 0;

        for(int index = 0; index < 3; index++)
        {
            db.insertTransaction(DBHelper.TransactionDbIds.EXPENSE, "description", "account", "", expenseValue, "note", nowMs, "receipt");
            db.insertTransaction(DBHelper.TransactionDbIds.REVENUE, "description", "account", "", revenueValue, "note", nowMs, "receipt");

            value = value + expenseValue - revenueValue;
        }

        Budget blankBudget = db.getBlankBudget(nowMs, nowMs);
        assertEquals("", blankBudget.name);
        assertEquals(0, blankBudget.max);
        assertEquals(value, blankBudget.current);
    }
}