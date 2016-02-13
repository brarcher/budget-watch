package protect.budgetwatch;

import android.app.Activity;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Calendar;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 17)
public class ImportExportTest
{
    private Activity activity;
    private DBHelper db;
    private long nowMs;
    private long lastYearMs;
    private int MONTHS_PER_YEAR = 12;

    @Before
    public void setUp()
    {
        activity = Robolectric.setupActivity(BudgetViewActivity.class);
        db = new DBHelper(activity);
        nowMs = System.currentTimeMillis();

        Calendar lastYear = Calendar.getInstance();
        lastYear.set(Calendar.YEAR, lastYear.get(Calendar.YEAR)-1);
        lastYearMs = lastYear.getTimeInMillis();
    }

    /**
     * Add the given number of budgets, each with
     * an index in the name.
     * @param budgetsToAdd
     */
    private void addBudgets(int budgetsToAdd)
    {
        // Add in reverse order to test sorting
        for(int index = budgetsToAdd; index > 0; index--)
        {
            String name = String.format("budget, \"%4d", index);
            boolean result = db.insertBudget(name, index);
            assertTrue(result);
        }

        assertEquals(budgetsToAdd, db.getBudgetCount());
    }

    /**
     * Check that all of the budgets follow the pattern
     * specified in addBudgets(), and are in sequential order
     * where the smallest budget value is 1
     */
    private void checkBudgets()
    {
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
            String expectedName = String.format("budget, \"%4d", index);
            assertEquals(expectedName, name);
            index++;
        }
    }

    /**
     * Delete the contents of the budgets and transactions databases
     */
    private void clearDatabase()
    {
        SQLiteDatabase database = db.getWritableDatabase();
        database.execSQL("delete from " + DBHelper.BudgetDbIds.TABLE);
        database.execSQL("delete from " + DBHelper.TransactionDbIds.TABLE);
        database.close();

        assertEquals(0, db.getBudgetCount());
        assertEquals(0, db.getTransactionCount(DBHelper.TransactionDbIds.EXPENSE));
        assertEquals(0, db.getTransactionCount(DBHelper.TransactionDbIds.REVENUE));
    }

    /**
     * Add the given number of revenue and expense transactions.
     * All string fields will be in the format
     *     name id
     * where "name" is the name of the field, and "id"
     * is the index for the entry. All numberical fields will
     * be assigned to the index.
     *
     * @param transactionsToAdd
     *   Number of transaction to add.
     */
    private void addTransactions(int transactionsToAdd)
    {
        // Add in increasing order to test sorting later
        for(int type : new Integer[]{DBHelper.TransactionDbIds.REVENUE, DBHelper.TransactionDbIds.EXPENSE})
        {
            for(int index = 1; index <= transactionsToAdd; index++)
            {
                boolean result = db.insertTransaction(type,
                        String.format(DBHelper.TransactionDbIds.DESCRIPTION + ", \"%4d", index),
                        String.format(DBHelper.TransactionDbIds.ACCOUNT + "%4d", index),
                        String.format(DBHelper.TransactionDbIds.BUDGET + "%4d", index),
                        index,
                        String.format(DBHelper.TransactionDbIds.NOTE + "%4d", index),
                        index);
                assertTrue(result);
            }
        }
    }

    /**
     * Check that all of the transactions follow the pattern
     * specified in addTransactions(), and are in sequential order
     * from the most recent to the oldest
     */
    private void checkTransactions()
    {
        boolean isExpense = true;

        for(Cursor cursor : new Cursor[]{db.getExpenses(), db.getRevenues()})
        {
            int index = cursor.getCount();
            while(cursor.moveToNext())
            {
                Transaction transaction = Transaction.toTransaction(cursor);
                assertEquals(isExpense ? DBHelper.TransactionDbIds.EXPENSE : DBHelper.TransactionDbIds.REVENUE, transaction.type);
                assertEquals(String.format(DBHelper.TransactionDbIds.DESCRIPTION + ", \"%4d", index), transaction.description);
                assertEquals(String.format(DBHelper.TransactionDbIds.ACCOUNT + "%4d", index), transaction.account);
                assertEquals(String.format(DBHelper.TransactionDbIds.BUDGET + "%4d", index), transaction.budget);
                assertEquals(index, (int)transaction.value);
                assertEquals(index, (int) transaction.value);
                assertEquals(String.format(DBHelper.TransactionDbIds.NOTE + "%4d", index), transaction.note);
                assertEquals(index, transaction.dateMs);

                index--;
            }
            cursor.close();

            isExpense = false;
        }
    }

    @Test
    public void multipleBudgetsExportImport() throws IOException
    {
        final int NUM_BUDGETS = 1000;

        for(DataFormat format : DataFormat.values())
        {
            addBudgets(NUM_BUDGETS);

            ByteArrayOutputStream outData = new ByteArrayOutputStream();
            OutputStreamWriter outStream = new OutputStreamWriter(outData);

            // Export data to CSV format
            boolean result = MultiFormatExporter.exportData(db, outStream, format);
            assertTrue(result);
            outStream.close();

            clearDatabase();

            ByteArrayInputStream inData = new ByteArrayInputStream(outData.toByteArray());
            InputStreamReader inStream = new InputStreamReader(inData);

            // Import the CSV data
            result = MultiFormatImporter.importData(db, inStream, DataFormat.CSV);
            assertTrue(result);

            assertEquals(NUM_BUDGETS, db.getBudgetCount());

            checkBudgets();

            // Clear the database for the next format under test
            clearDatabase();
        }
    }

    @Test
    public void importExistingBudgetsNotReplace() throws IOException
    {
        final int NUM_BUDGETS = 1000;

        for(DataFormat format : DataFormat.values())
        {
            addBudgets(NUM_BUDGETS);

            ByteArrayOutputStream outData = new ByteArrayOutputStream();
            OutputStreamWriter outStream = new OutputStreamWriter(outData);

            // Export into CSV data
            boolean result = MultiFormatExporter.exportData(db, outStream, format);
            assertTrue(result);
            outStream.close();

            ByteArrayInputStream inData = new ByteArrayInputStream(outData.toByteArray());
            InputStreamReader inStream = new InputStreamReader(inData);

            // Import the CSV data on top of the existing database
            result = MultiFormatImporter.importData(db, inStream, DataFormat.CSV);
            assertTrue(result);

            assertEquals(NUM_BUDGETS, db.getBudgetCount());

            checkBudgets();

            // Clear the database for the next format under test
            clearDatabase();
        }
    }

    @Test
    public void multipleTransactionsExportImport() throws IOException
    {
        final int NUM_TRANSACTIONS = 1000;

        for(DataFormat format : DataFormat.values())
        {
            addTransactions(NUM_TRANSACTIONS);

            ByteArrayOutputStream outData = new ByteArrayOutputStream();
            OutputStreamWriter outStream = new OutputStreamWriter(outData);

            // Export data to CSV format
            boolean result = MultiFormatExporter.exportData(db, outStream, format);
            assertTrue(result);
            outStream.close();

            clearDatabase();

            ByteArrayInputStream inData = new ByteArrayInputStream(outData.toByteArray());
            InputStreamReader inStream = new InputStreamReader(inData);

            // Import the CSV data
            result = MultiFormatImporter.importData(db, inStream, DataFormat.CSV);
            assertTrue(result);

            assertEquals(NUM_TRANSACTIONS,
                    db.getTransactionCount(DBHelper.TransactionDbIds.EXPENSE));
            assertEquals(NUM_TRANSACTIONS, db.getTransactionCount(DBHelper.TransactionDbIds.REVENUE));

            checkTransactions();

            // Clear the database for the next format under test
            clearDatabase();
        }
    }

    @Test
    public void importExistingTransactionsNotReplace() throws IOException
    {
        final int NUM_TRANSACTIONS = 1000;

        for(DataFormat format : DataFormat.values())
        {
            addTransactions(NUM_TRANSACTIONS);

            ByteArrayOutputStream outData = new ByteArrayOutputStream();
            OutputStreamWriter outStream = new OutputStreamWriter(outData);

            // Export data to CSV format
            boolean result = MultiFormatExporter.exportData(db, outStream, format);
            assertTrue(result);
            outStream.close();

            // Do not clear database

            ByteArrayInputStream inData = new ByteArrayInputStream(outData.toByteArray());
            InputStreamReader inStream = new InputStreamReader(inData);

            // Import the CSV data on top of the existing database
            result = MultiFormatImporter.importData(db, inStream, DataFormat.CSV);
            assertTrue(result);

            assertEquals(NUM_TRANSACTIONS,
                    db.getTransactionCount(DBHelper.TransactionDbIds.EXPENSE));
            assertEquals(NUM_TRANSACTIONS, db.getTransactionCount(DBHelper.TransactionDbIds.REVENUE));

            checkTransactions();

            // Clear the database for the next format under test
            clearDatabase();
        }
    }

    @Test
    public void multipleEverythingExportImport() throws IOException
    {
        final int NUM_ITEMS = 1000;

        for(DataFormat format : DataFormat.values())
        {
            addBudgets(NUM_ITEMS);
            addTransactions(NUM_ITEMS);

            ByteArrayOutputStream outData = new ByteArrayOutputStream();
            OutputStreamWriter outStream = new OutputStreamWriter(outData);

            // Export data to CSV format
            boolean result = MultiFormatExporter.exportData(db, outStream, format);
            assertTrue(result);
            outStream.close();

            clearDatabase();

            ByteArrayInputStream inData = new ByteArrayInputStream(outData.toByteArray());
            InputStreamReader inStream = new InputStreamReader(inData);

            // Import the CSV data
            result = MultiFormatImporter.importData(db, inStream, DataFormat.CSV);
            assertTrue(result);

            assertEquals(NUM_ITEMS, db.getBudgetCount());
            assertEquals(NUM_ITEMS,
                    db.getTransactionCount(DBHelper.TransactionDbIds.EXPENSE));
            assertEquals(NUM_ITEMS, db.getTransactionCount(DBHelper.TransactionDbIds.REVENUE));

            checkBudgets();
            checkTransactions();

            // Clear the database for the next format under test
            clearDatabase();
        }
    }

    @Test
    public void corruptedImportNothingSaved() throws IOException
    {
        final int NUM_ITEMS = 1000;

        for(DataFormat format : DataFormat.values())
        {
            addBudgets(NUM_ITEMS);
            addTransactions(NUM_ITEMS);

            ByteArrayOutputStream outData = new ByteArrayOutputStream();
            OutputStreamWriter outStream = new OutputStreamWriter(outData);

            // Export data to CSV format
            boolean result = MultiFormatExporter.exportData(db, outStream, format);
            assertTrue(result);

            clearDatabase();

            String corruptEntry = "ThisStringIsLikelyNotPartOfAnyFormat";

            ByteArrayInputStream inData = new ByteArrayInputStream((outData.toString() + corruptEntry).getBytes());
            InputStreamReader inStream = new InputStreamReader(inData);

            // Attempt to import the CSV data
            result = MultiFormatImporter.importData(db, inStream, DataFormat.CSV);
            assertEquals(false, result);

            assertEquals(0, db.getBudgetCount());
            assertEquals(0,
                    db.getTransactionCount(DBHelper.TransactionDbIds.EXPENSE));
            assertEquals(0, db.getTransactionCount(DBHelper.TransactionDbIds.REVENUE));
        }
    }

    @Test
    public void useImportExportTask()
    {
        final int NUM_ITEMS = 10;

        for(DataFormat format : DataFormat.values())
        {
            addBudgets(NUM_ITEMS);
            addTransactions(NUM_ITEMS);

            // Export to whatever the default location is
            ImportExportTask task = new ImportExportTask(activity, false, format);
            task.execute();

            // Actually run the task to completion
            Robolectric.flushBackgroundThreadScheduler();

            clearDatabase();

            // Import everything back from the default location

            task = new ImportExportTask(activity, true, format);
            task.execute();

            // Actually run the task to completion
            Robolectric.flushBackgroundThreadScheduler();

            assertEquals(NUM_ITEMS, db.getBudgetCount());
            assertEquals(NUM_ITEMS,
                    db.getTransactionCount(DBHelper.TransactionDbIds.EXPENSE));
            assertEquals(NUM_ITEMS, db.getTransactionCount(DBHelper.TransactionDbIds.REVENUE));

            checkBudgets();
            checkTransactions(true);

            // Clear the database for the next format under test
            clearDatabase();
        }
    }
}
