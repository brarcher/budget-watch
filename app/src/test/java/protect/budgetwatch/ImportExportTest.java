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
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 17)
public class ImportExportTest
{
    private Activity activity;
    private DBHelper db;

    class TestTaskCompleteListener implements ImportExportTask.TaskCompleteListener
    {
        Boolean success;
        File file;

        public void onTaskComplete(boolean success, File file)
        {
            this.success = success;
            this.file = file;
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

            // Export data to CSV format
            boolean result = MultiFormatExporter.exportData(activity, db, outData, format);
            assertTrue(result);

            DatabaseTestHelper.clearDatabase(db, activity);

            ByteArrayInputStream inData = new ByteArrayInputStream(outData.toByteArray());

            // Import the CSV data
            result = MultiFormatImporter.importData(activity, db, inData, format);
            assertTrue(result);

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

            // Export into CSV data
            boolean result = MultiFormatExporter.exportData(activity, db, outData, format);
            assertTrue(result);

            ByteArrayInputStream inData = new ByteArrayInputStream(outData.toByteArray());

            // Import the CSV data on top of the existing database
            result = MultiFormatImporter.importData(activity, db, inData, format);
            assertTrue(result);

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

            // Export data to CSV format
            boolean result = MultiFormatExporter.exportData(activity, db, outData, format);
            assertTrue(result);

            DatabaseTestHelper.clearDatabase(db, activity);

            ByteArrayInputStream inData = new ByteArrayInputStream(outData.toByteArray());
            TransactionDatabaseChangedReceiver dbChanged = new TransactionDatabaseChangedReceiver();
            activity.registerReceiver(dbChanged, new IntentFilter(TransactionDatabaseChangedReceiver.ACTION_DATABASE_CHANGED));
            assertFalse(dbChanged.hasChanged());

            // Import the CSV data
            result = MultiFormatImporter.importData(activity, db, inData, format);
            assertTrue(result);

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

            // Export data to CSV format
            boolean result = MultiFormatExporter.exportData(activity, db, outData, format);
            assertTrue(result);

            // Do not clear database

            ByteArrayInputStream inData = new ByteArrayInputStream(outData.toByteArray());

            TransactionDatabaseChangedReceiver dbChanged = new TransactionDatabaseChangedReceiver();
            activity.registerReceiver(dbChanged, new IntentFilter(TransactionDatabaseChangedReceiver.ACTION_DATABASE_CHANGED));
            assertFalse(dbChanged.hasChanged());

            // Import the CSV data on top of the existing database
            result = MultiFormatImporter.importData(activity, db, inData, format);
            assertTrue(result);

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

            // Export data to CSV format
            boolean result = MultiFormatExporter.exportData(activity, db, outData, format);
            assertTrue(result);

            DatabaseTestHelper.clearDatabase(db, activity);

            ByteArrayInputStream inData = new ByteArrayInputStream(outData.toByteArray());

            // Import the CSV data
            result = MultiFormatImporter.importData(activity, db, inData, format);
            assertTrue(result);

            DatabaseTestHelper.checkBudgets(db, NUM_ITEMS);
            DatabaseTestHelper.checkTransactions(db, activity, NUM_ITEMS, format == DataFormat.ZIP);

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

            // Export data to CSV format
            boolean result = MultiFormatExporter.exportData(activity, db, outData, format);
            assertTrue(result);

            DatabaseTestHelper.clearDatabase(db, activity);

            String corruptEntry = "ThisStringIsLikelyNotPartOfAnyFormat";

            ByteArrayInputStream inData = new ByteArrayInputStream((outData.toString() + corruptEntry).getBytes());

            // Attempt to import the CSV data
            result = MultiFormatImporter.importData(activity, db, inData, format);
            assertEquals(false, result);

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
            ImportExportTask task = new ImportExportTask(activity, false, format, exportFile, listener);
            task.execute();

            // Actually run the task to completion
            Robolectric.flushBackgroundThreadScheduler();

            assertNotNull(listener.success);
            assertEquals(true, listener.success);
            assertNotNull(listener.file);
            assertEquals(exportFile, listener.file);

            DatabaseTestHelper.clearDatabase(db, activity);

            // Import everything back from the default location
            listener = new TestTaskCompleteListener();
            task = new ImportExportTask(activity, true, format, exportFile, listener);
            task.execute();

            // Actually run the task to completion
            Robolectric.flushBackgroundThreadScheduler();

            assertNotNull(listener.success);
            assertEquals(true, listener.success);
            assertNotNull(listener.file);
            assertEquals(exportFile, listener.file);

            DatabaseTestHelper.checkBudgets(db, NUM_ITEMS);
            DatabaseTestHelper.checkTransactions(db, activity, NUM_ITEMS, format == DataFormat.ZIP);

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
