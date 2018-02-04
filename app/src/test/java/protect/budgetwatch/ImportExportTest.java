package protect.budgetwatch;

import android.app.Activity;
import android.content.IntentFilter;
import android.os.Environment;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 25)
public class ImportExportTest
{
    private Activity activity;
    private DBHelper db;

    class TestTaskCompleteListener implements ImportExportTask.TaskCompleteListener
    {
        Boolean success;

        public void onTaskComplete(boolean success)
        {
            this.success = success;
        }
    }

    class TestProgressUpdater extends ImportExportProgressUpdater
    {
        private int numUpdates = 0;
        private Integer totalEntries;

        TestProgressUpdater()
        {
            super(null, null, null);
        }

        @Override
        public void update()
        {
            numUpdates++;
        }

        @Override
        public void setTotal(int totalEntries)
        {
            this.totalEntries = totalEntries;
        }

        public int getNumUpdates()
        {
            return numUpdates;
        }
        public Integer getTotal()
        {
            return totalEntries;
        }
    }

    @Before
    public void setUp()
    {
        // Output logs emitted during tests so they may be accessed
        ShadowLog.stream = System.out;

        activity = Robolectric.setupActivity(BudgetViewActivity.class);
        db = new DBHelper(activity);
    }

    @After
    public void tearDown()
    {
        db.close();
    }

    @Test
    public void multipleBudgetsExportImport() throws IOException
    {
        final int NUM_BUDGETS = 10;

        for(DataFormat format : DataFormat.values())
        {
            DatabaseTestHelper.addBudgets(db, NUM_BUDGETS);

            ByteArrayOutputStream outData = new ByteArrayOutputStream();

            TestProgressUpdater updater = new TestProgressUpdater();

            // Export data to CSV format
            boolean result = MultiFormatExporter.exportData(activity, db, null, null, outData, format, updater);
            assertTrue(result);

            assertEquals(NUM_BUDGETS, updater.getNumUpdates());

            DatabaseTestHelper.clearDatabase(db, activity);

            ByteArrayInputStream inData = new ByteArrayInputStream(outData.toByteArray());

            updater = new TestProgressUpdater();

            // Import the CSV data
            result = MultiFormatImporter.importData(activity, db, inData, format, updater);
            assertTrue(result);

            assertEquals(null, updater.getTotal());
            assertEquals(NUM_BUDGETS, updater.getNumUpdates());

            DatabaseTestHelper.checkBudgets(db, NUM_BUDGETS);

            // Clear the database for the next format under test
            DatabaseTestHelper.clearDatabase(db, activity);
        }
    }

    @Test
    public void importExistingBudgetsNotReplace() throws IOException
    {
        final int NUM_BUDGETS = 10;

        for(DataFormat format : DataFormat.values())
        {
            DatabaseTestHelper.addBudgets(db, NUM_BUDGETS);

            ByteArrayOutputStream outData = new ByteArrayOutputStream();

            TestProgressUpdater updater = new TestProgressUpdater();

            // Export into CSV data
            boolean result = MultiFormatExporter.exportData(activity, db, null, null, outData, format, updater);
            assertTrue(result);

            assertEquals(NUM_BUDGETS, updater.getTotal().intValue());
            assertEquals(NUM_BUDGETS, updater.getNumUpdates());

            ByteArrayInputStream inData = new ByteArrayInputStream(outData.toByteArray());

            updater = new TestProgressUpdater();

            // Import the CSV data on top of the existing database
            result = MultiFormatImporter.importData(activity, db, inData, format, updater);
            assertTrue(result);

            assertEquals(null, updater.getTotal());
            assertEquals(NUM_BUDGETS, updater.getNumUpdates());

            DatabaseTestHelper.checkBudgets(db, NUM_BUDGETS);

            // Clear the database for the next format under test
            DatabaseTestHelper.clearDatabase(db, activity);
        }
    }

    @Test
    public void multipleTransactionsExportImport() throws IOException
    {
        final int NUM_TRANSACTIONS = 10;

        for(DataFormat format : DataFormat.values())
        {
            DatabaseTestHelper.addTransactions(db, activity, NUM_TRANSACTIONS);

            ByteArrayOutputStream outData = new ByteArrayOutputStream();

            TestProgressUpdater updater = new TestProgressUpdater();

            // Export data to CSV format
            boolean result = MultiFormatExporter.exportData(activity, db, null, null, outData, format, updater);
            assertTrue(result);

            assertEquals(NUM_TRANSACTIONS * 2, updater.getTotal().intValue());
            assertEquals(NUM_TRANSACTIONS * 2, updater.getNumUpdates());

            DatabaseTestHelper.clearDatabase(db, activity);

            ByteArrayInputStream inData = new ByteArrayInputStream(outData.toByteArray());
            TransactionDatabaseChangedReceiver dbChanged = new TransactionDatabaseChangedReceiver();
            activity.registerReceiver(dbChanged, new IntentFilter(TransactionDatabaseChangedReceiver.ACTION_DATABASE_CHANGED));
            assertFalse(dbChanged.hasChanged());

            updater = new TestProgressUpdater();

            // Import the CSV data
            result = MultiFormatImporter.importData(activity, db, inData, format, updater);
            assertTrue(result);

            assertEquals(null, updater.getTotal());
            assertEquals(NUM_TRANSACTIONS * 2, updater.getNumUpdates());

            // The contents of the database should have changed
            assertTrue(dbChanged.hasChanged());
            activity.unregisterReceiver(dbChanged);

            DatabaseTestHelper.checkTransactions(db, activity, NUM_TRANSACTIONS, format == DataFormat.ZIP);

            // Clear the database for the next format under test
            DatabaseTestHelper.clearDatabase(db, activity);
        }
    }

    @Test
    public void importExistingTransactionsNotReplace() throws IOException
    {
        final int NUM_TRANSACTIONS = 10;

        for(DataFormat format : DataFormat.values())
        {
            DatabaseTestHelper.addTransactions(db, activity, NUM_TRANSACTIONS);

            ByteArrayOutputStream outData = new ByteArrayOutputStream();

            TestProgressUpdater updater = new TestProgressUpdater();

            // Export data to CSV format
            boolean result = MultiFormatExporter.exportData(activity, db, null, null, outData, format, updater);
            assertTrue(result);

            assertEquals(NUM_TRANSACTIONS * 2, updater.getTotal().intValue());
            assertEquals(NUM_TRANSACTIONS * 2, updater.getNumUpdates());

            // Do not clear database

            ByteArrayInputStream inData = new ByteArrayInputStream(outData.toByteArray());

            TransactionDatabaseChangedReceiver dbChanged = new TransactionDatabaseChangedReceiver();
            activity.registerReceiver(dbChanged, new IntentFilter(TransactionDatabaseChangedReceiver.ACTION_DATABASE_CHANGED));
            assertFalse(dbChanged.hasChanged());

            updater = new TestProgressUpdater();

            // Import the CSV data on top of the existing database
            result = MultiFormatImporter.importData(activity, db, inData, format, updater);
            assertTrue(result);

            assertEquals(null, updater.getTotal());
            assertEquals(NUM_TRANSACTIONS * 2, updater.getNumUpdates());

            // The contents of the database should not have changed
            assertFalse(dbChanged.hasChanged());
            activity.unregisterReceiver(dbChanged);

            // Because the database is in tact, it should still have receipt data
            DatabaseTestHelper.checkTransactions(db, activity, NUM_TRANSACTIONS, true);

            // Clear the database for the next format under test
            DatabaseTestHelper.clearDatabase(db, activity);
        }
    }

    @Test
    public void multipleEverythingExportImport() throws IOException
    {
        final int NUM_ITEMS = 10;

        for(DataFormat format : DataFormat.values())
        {
            DatabaseTestHelper.addBudgets(db, NUM_ITEMS);
            DatabaseTestHelper.addTransactions(db, activity, NUM_ITEMS);

            ByteArrayOutputStream outData = new ByteArrayOutputStream();

            TestProgressUpdater updater = new TestProgressUpdater();

            // Export data to CSV format
            boolean result = MultiFormatExporter.exportData(activity, db, null, null, outData, format, updater);
            assertTrue(result);

            assertEquals(NUM_ITEMS * 3, updater.getTotal().intValue());
            assertEquals(NUM_ITEMS * 3, updater.getNumUpdates());

            DatabaseTestHelper.clearDatabase(db, activity);

            ByteArrayInputStream inData = new ByteArrayInputStream(outData.toByteArray());

            updater = new TestProgressUpdater();

            // Import the CSV data
            result = MultiFormatImporter.importData(activity, db, inData, format, updater);
            assertTrue(result);

            assertEquals(null, updater.getTotal());
            assertEquals(NUM_ITEMS * 3, updater.getNumUpdates());

            DatabaseTestHelper.checkBudgets(db, NUM_ITEMS);
            DatabaseTestHelper.checkTransactions(db, activity, NUM_ITEMS, format == DataFormat.ZIP);

            // Clear the database for the next format under test
            DatabaseTestHelper.clearDatabase(db, activity);
        }
    }

    @Test
    public void partialDateRangeExported() throws IOException
    {
        final int NUM_ITEMS = 10;

        for(DataFormat format : DataFormat.values())
        {
            DatabaseTestHelper.addBudgets(db, NUM_ITEMS);
            DatabaseTestHelper.addTransactions(db, activity, NUM_ITEMS);

            ByteArrayOutputStream outData = new ByteArrayOutputStream();

            TestProgressUpdater updater = new TestProgressUpdater();

            final long START_DATE_MS = 3;
            final long END_DATE_MS = 7;

            // Export data to the given format
            boolean result = MultiFormatExporter.exportData(activity, db, START_DATE_MS, END_DATE_MS, outData, format, updater);
            assertTrue(result);

            final int EXPECTED_TRANSACTIONS = (int)(END_DATE_MS - START_DATE_MS + 1);

            final int EXPECTED_ITEMS = /*Budgets*/ NUM_ITEMS + /*transactions per type*/(EXPECTED_TRANSACTIONS)*2;

            assertEquals(EXPECTED_ITEMS, updater.getTotal().intValue());
            assertEquals(EXPECTED_ITEMS, updater.getNumUpdates());

            // Clear the database for the next format under test
            DatabaseTestHelper.clearDatabase(db, activity);
        }
    }

    @Test
    public void corruptedImportNothingSaved() throws IOException
    {
        final int NUM_ITEMS = 10;

        for(DataFormat format : DataFormat.values())
        {
            DatabaseTestHelper.addBudgets(db, NUM_ITEMS);
            DatabaseTestHelper.addTransactions(db, activity, NUM_ITEMS);

            ByteArrayOutputStream outData = new ByteArrayOutputStream();

            TestProgressUpdater updater = new TestProgressUpdater();

            // Export data to CSV format
            boolean result = MultiFormatExporter.exportData(activity, db, null, null, outData, format, updater);
            assertTrue(result);

            assertEquals(NUM_ITEMS * 3, updater.getTotal().intValue());
            assertEquals(NUM_ITEMS * 3, updater.getNumUpdates());

            DatabaseTestHelper.clearDatabase(db, activity);

            // commons-csv would throw a RuntimeException if an entry was quotes but had
            // content after. For example:
            //   abc,def,""abc,abc
            //             ^ after the quote there should only be a , \n or EOF
            String corruptEntry = "ThisStringIsLikelyNotPartOfAnyFormat,\"\"a";

            ByteArrayInputStream inData = new ByteArrayInputStream((outData.toString() + corruptEntry).getBytes());

            updater = new TestProgressUpdater();

            // Attempt to import the CSV data
            result = MultiFormatImporter.importData(activity, db, inData, format, updater);
            assertEquals(false, result);

            assertEquals(null, updater.getTotal());
            if(format == DataFormat.ZIP)
            {
                // If there is corrupted data at the end of a zip file,
                // the file will fail an integrity check and nothing
                // will be imported
                assertEquals(0, updater.getNumUpdates());
            }
            else
            {
                assertEquals(NUM_ITEMS * 3, updater.getNumUpdates());
            }


            assertEquals(0, db.getBudgetCount());
            assertEquals(0,
                    db.getTransactionCount(DBHelper.TransactionDbIds.EXPENSE));
            assertEquals(0, db.getTransactionCount(DBHelper.TransactionDbIds.REVENUE));
        }
    }

    @Test
    public void useImportExportTask() throws IOException
    {
        final int NUM_ITEMS = 10;
        final File sdcardDir = Environment.getExternalStorageDirectory();
        final File exportFile = new File(sdcardDir, "file.csv");

        for(DataFormat format : DataFormat.values())
        {
            DatabaseTestHelper.addBudgets(db, NUM_ITEMS);
            DatabaseTestHelper.addTransactions(db, activity, NUM_ITEMS);

            // Export to whatever the default location is
            TestTaskCompleteListener listener = new TestTaskCompleteListener();
            ImportExportTask task = new ImportExportTask(activity, format, exportFile, listener, null, null);
            task.execute();

            // Actually run the task to completion
            Robolectric.flushBackgroundThreadScheduler();

            assertNotNull(listener.success);
            assertEquals(true, listener.success);

            DatabaseTestHelper.clearDatabase(db, activity);

            // Import everything back from the default location
            listener = new TestTaskCompleteListener();
            FileInputStream fileStream = new FileInputStream(exportFile);
            task = new ImportExportTask(activity, format, fileStream, listener);
            task.execute();

            // Actually run the task to completion
            Robolectric.flushBackgroundThreadScheduler();

            assertNotNull(listener.success);
            assertEquals(true, listener.success);

            DatabaseTestHelper.checkBudgets(db, NUM_ITEMS);
            DatabaseTestHelper.checkTransactions(db, activity, NUM_ITEMS, format == DataFormat.ZIP);

            // Clear the database for the next format under test
            DatabaseTestHelper.clearDatabase(db, activity);
        }
    }

    @Test
    public void useImportExportTaskDateRange() throws IOException
    {
        final int NUM_ITEMS = 10;
        final File sdcardDir = Environment.getExternalStorageDirectory();
        final File exportFile = new File(sdcardDir, "file.csv");

        for(DataFormat format : DataFormat.values())
        {
            DatabaseTestHelper.addBudgets(db, NUM_ITEMS);
            DatabaseTestHelper.addTransactions(db, activity, NUM_ITEMS);

            final long START_DATE_MS = 1;
            final long END_DATE_MS = 5;

            // Export to whatever the default location is
            TestTaskCompleteListener listener = new TestTaskCompleteListener();
            ImportExportTask task = new ImportExportTask(activity, format, exportFile, listener, START_DATE_MS, END_DATE_MS);
            task.execute();

            // Actually run the task to completion
            Robolectric.flushBackgroundThreadScheduler();

            assertNotNull(listener.success);
            assertEquals(true, listener.success);

            DatabaseTestHelper.clearDatabase(db, activity);

            // Import everything back from the default location
            listener = new TestTaskCompleteListener();
            FileInputStream fileStream = new FileInputStream(exportFile);
            task = new ImportExportTask(activity, format, fileStream, listener);
            task.execute();

            // Actually run the task to completion
            Robolectric.flushBackgroundThreadScheduler();

            assertNotNull(listener.success);
            assertEquals(true, listener.success);

            DatabaseTestHelper.checkBudgets(db, NUM_ITEMS);
            DatabaseTestHelper.checkTransactions(db, activity, (int)END_DATE_MS, format == DataFormat.ZIP);

            // Clear the database for the next format under test
            DatabaseTestHelper.clearDatabase(db, activity);
        }
    }

    @Test
    public void clickBackFinishes()
    {
        shadowOf(activity).clickMenuItem(android.R.id.home);
        assertTrue(shadowOf(activity).isFinishing());
    }
}
