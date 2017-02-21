package protect.budgetwatch;

import android.app.Activity;
import android.database.Cursor;
import android.os.Environment;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 17)
public class DatabaseCleanupTaskTest
{
    private Activity activity;
    private DBHelper db;
    private File imageDir;
    private File missingReceipt;
    private File orphanReceipt;

    private static final int NUM_TRANSACTIONS = 10;
    private static final String WITH_RECEIPT_NAME = "receipt_exists";
    private static final String WITHOUT_RECEIPT_NAME = "receipt_missing";

    @Before
    public void setUp() throws IOException
    {
        // Output logs emitted during tests so they may be accessed
        ShadowLog.stream = System.out;

        activity = Robolectric.setupActivity(ImportExportActivity.class);
        db = new DBHelper(activity);

        imageDir = activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        assertNotNull(imageDir);

        boolean result;
        if(imageDir.exists() == false)
        {
            result = imageDir.mkdirs();
            assertTrue(result);
        }

        missingReceipt = new File(imageDir, "missing");

        orphanReceipt = new File(imageDir, "orphan");
        result = orphanReceipt.createNewFile();
        assertTrue(result);
    }

    @After
    public void tearDown()
    {
        db.close();
    }

    /**
     * Add 10 transactions each of the following:
     *  - expense
     *    * receipt exists
     *    * receipt missing
     *  - revenue
     *    * receipt exists
     *    * receipt missing
     */
    private void addTransactions() throws IOException
    {
        for(int type : new Integer[]{DBHelper.TransactionDbIds.REVENUE, DBHelper.TransactionDbIds.EXPENSE})
        {
            for(int index = 1; index <= NUM_TRANSACTIONS; index++)
            {
                File existingReceipt = new File(imageDir, "exists-" + type + "-" + index);
                boolean result = existingReceipt.createNewFile();
                assertTrue(result);

                result = db.insertTransaction(type,
                        WITH_RECEIPT_NAME,
                        DBHelper.TransactionDbIds.ACCOUNT,
                        DBHelper.TransactionDbIds.BUDGET,
                        0,
                        DBHelper.TransactionDbIds.NOTE,
                        index,
                        existingReceipt.getAbsolutePath());
                assertTrue(result);

                result = db.insertTransaction(type,
                        WITHOUT_RECEIPT_NAME,
                        DBHelper.TransactionDbIds.ACCOUNT,
                        DBHelper.TransactionDbIds.BUDGET,
                        0,
                        DBHelper.TransactionDbIds.NOTE,
                        index,
                        missingReceipt.getAbsolutePath());
                assertTrue(result);
            }
        }
    }

    @Test
    public void testCleanupOnly() throws IOException
    {
        addTransactions();

        DatabaseCleanupTask task = new DatabaseCleanupTask(activity);
        task.execute();

        // Actually run the task to completion
        Robolectric.flushBackgroundThreadScheduler();

        // Check that the orphaned image is now deleted
        assertEquals(false, orphanReceipt.exists());

        // Check that the database only reports transactions with
        // existing receipts
        Cursor cursor = db.getTransactionsWithReceipts(null);

        // There should be NUM_TRANSACTIONS transactions for each of
        // REVENUE and EXPENSE
        assertEquals(NUM_TRANSACTIONS*2, cursor.getCount());

        while(cursor.moveToNext())
        {
            Transaction transaction = Transaction.toTransaction(cursor);
            assertEquals(WITH_RECEIPT_NAME, transaction.description);

            // Check that the image still exists
            File receipt = new File(transaction.receipt);
            assertEquals(true, receipt.exists());
            assertEquals(true, receipt.isFile());
        }

        cursor.close();
    }

    @Test
    public void testCleanupAndPurge() throws IOException
    {
        addTransactions();

        final Long DATE_CUTOFF = (long)NUM_TRANSACTIONS/2;

        DatabaseCleanupTask task = new DatabaseCleanupTask(activity, DATE_CUTOFF);
        task.execute();

        // Actually run the task to completion
        Robolectric.flushBackgroundThreadScheduler();

        // Check that the orphaned image is not deleted
        assertEquals(false, orphanReceipt.exists());

        // Check that the database only reports transactions with
        // existing receipts
        Cursor cursor = db.getTransactionsWithReceipts(null);

        // Before the purge there were NUM_TRANSACTIONS transactions for each of
        // REVENUE and EXPENSE. The purge will reduce the count by half.
        assertEquals(NUM_TRANSACTIONS, cursor.getCount());

        while(cursor.moveToNext())
        {
            Transaction transaction = Transaction.toTransaction(cursor);
            assertEquals(WITH_RECEIPT_NAME, transaction.description);

            // Check that the image still exists
            File receipt = new File(transaction.receipt);
            assertEquals(true, receipt.exists());
            assertEquals(true, receipt.isFile());

            assertTrue(transaction.dateMs > DATE_CUTOFF);
        }

        cursor.close();
    }
}
