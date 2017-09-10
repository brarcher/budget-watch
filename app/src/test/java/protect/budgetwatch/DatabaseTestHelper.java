package protect.budgetwatch;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;

import com.google.common.io.ByteStreams;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.Calendar;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Collection of methods to help testing data in the database
 */
class DatabaseTestHelper
{
    public static final int FIRST_ID = 1;

    private static final int MONTHS_PER_YEAR = 12;
    private static final long lastYearMs;

    static
    {
        Calendar lastYear = Calendar.getInstance();
        lastYear.set(Calendar.YEAR, lastYear.get(Calendar.YEAR)-1);
        lastYearMs = lastYear.getTimeInMillis();
    }


    /**
     * Add the given number of budgets, each with
     * an index in the name.
     */
    public static void addBudgets(DBHelper db, int budgetsToAdd)
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
     * Check that the expected number of budgets exist and that
     * all of the budgets follow the pattern specified in addBudgets(),
     * and are in sequential order where the smallest budget value is 1.
     */
    public static void checkBudgets(DBHelper db, int expectedCount)
    {
        long nowMs = System.currentTimeMillis();

        assertEquals(expectedCount, db.getBudgetCount());

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
    public static void clearDatabase(DBHelper db, Context context)
    {
        SQLiteDatabase database = db.getWritableDatabase();
        database.execSQL("delete from " + DBHelper.BudgetDbIds.TABLE);
        database.execSQL("delete from " + DBHelper.TransactionDbIds.TABLE);
        database.close();

        assertEquals(0, db.getBudgetCount());
        assertEquals(0, db.getTransactionCount(DBHelper.TransactionDbIds.EXPENSE));
        assertEquals(0, db.getTransactionCount(DBHelper.TransactionDbIds.REVENUE));

        // Also delete all the receipt images, in case the exporter should have saved them
        File receiptFolder = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        assertNotNull(receiptFolder);
        File [] files = receiptFolder.listFiles();
        assertNotNull(files);
        for(File file : files)
        {
            boolean result = file.delete();
            assertTrue(result);
            assertTrue(file.exists() == false);
        }
    }

    /**
     * Add the given number of revenue and expense transactions.
     * All string fields will be in the format
     *     name id
     * where "name" is the name of the field, and "id"
     * is the index for the entry. All numerical fields will
     * be assigned to the index.
     *
     * @param transactionsToAdd
     *   Number of transaction to add.
     */
    public static void addTransactions(DBHelper db, Context context, int transactionsToAdd) throws IOException
    {
        File receiptFolder = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        assertNotNull(receiptFolder);
        if(receiptFolder.exists() == false)
        {
            boolean result = receiptFolder.mkdir();
            assertTrue(result);
        }

        // Add in increasing order to test sorting later
        for(int type : new Integer[]{DBHelper.TransactionDbIds.REVENUE, DBHelper.TransactionDbIds.EXPENSE})
        {
            for(int index = 1; index <= transactionsToAdd; index++)
            {
                String receiptString = String.format(DBHelper.TransactionDbIds.RECEIPT + "%4d", index);
                File receiptFile = new File(receiptFolder, receiptString);

                OutputStreamWriter output = new OutputStreamWriter(new FileOutputStream(receiptFile), Charset.forName("UTF-8"));
                output.write(receiptString);
                output.close();

                boolean result = db.insertTransaction(type,
                        String.format(DBHelper.TransactionDbIds.DESCRIPTION + ", \"%4d", index),
                        String.format(DBHelper.TransactionDbIds.ACCOUNT + "%4d", index),
                        String.format(DBHelper.TransactionDbIds.BUDGET + "%4d", index),
                        index,
                        String.format(DBHelper.TransactionDbIds.NOTE + "%4d", index),
                        index,
                        receiptFile.getAbsolutePath());
                assertTrue(result);
            }
        }
    }

    /**
     * Check that the expected number of transactions exist and that
     * all of the transactions follow the pattern specified in addTransactions(),
     * and are in sequential order from the most recent to the oldest.
     */
    public static void checkTransactions(DBHelper db, Context context, int expectedCount, boolean shouldHaveReceipts) throws IOException
    {
        assertEquals(expectedCount,
                db.getTransactionCount(DBHelper.TransactionDbIds.EXPENSE));
        assertEquals(expectedCount, db.getTransactionCount(DBHelper.TransactionDbIds.REVENUE));

        boolean isExpense = true;

        File receiptDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);

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
                assertEquals(String.format(DBHelper.TransactionDbIds.NOTE + "%4d", index), transaction.note);
                assertEquals(index, transaction.dateMs);

                if(shouldHaveReceipts)
                {
                    String receiptName = String.format(DBHelper.TransactionDbIds.RECEIPT + "%4d", index);
                    File receiptFile = new File(receiptDir, receiptName);
                    assertEquals(receiptFile.getAbsolutePath(), transaction.receipt);
                    assertTrue(receiptFile.isFile());

                    BufferedInputStream stream = new BufferedInputStream(new FileInputStream(receiptFile));
                    String contents = new String(ByteStreams.toByteArray(stream));
                    stream.close();
                    assertEquals(receiptName, contents);
                }
                else
                {
                    assertEquals(0, transaction.receipt.length());
                }

                index--;
            }
            cursor.close();

            isExpense = false;
        }
    }
}
