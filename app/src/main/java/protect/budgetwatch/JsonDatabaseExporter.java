package protect.budgetwatch;

import android.content.Context;
import android.database.Cursor;
import android.util.JsonWriter;

import com.google.common.base.Charsets;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.List;

/**
 * Class for exporting the database into JSON format.
 */
public class JsonDatabaseExporter implements DatabaseExporter
{
    public void exportData(Context context, DBHelper db, Long startTimeMs, Long endTimeMs, OutputStream outStream, ImportExportProgressUpdater updater) throws IOException, InterruptedException
    {
        OutputStreamWriter stream = new OutputStreamWriter(outStream, Charsets.UTF_8);
        BufferedWriter output = new BufferedWriter(stream);
        JsonWriter writer = new JsonWriter(output);

        int numEntries = 0;

        List<String> budgetNames = db.getBudgetNames();
        numEntries += budgetNames.size();

        Cursor expenseTransactions = db.getTransactions(DBHelper.TransactionDbIds.EXPENSE, null, null, startTimeMs, endTimeMs);
        numEntries += expenseTransactions.getCount();
        Cursor revenueTransactions = db.getTransactions(DBHelper.TransactionDbIds.REVENUE, null, null, startTimeMs, endTimeMs);
        numEntries += revenueTransactions.getCount();

        updater.setTotal(numEntries);

        try
        {
            writer.setIndent("   ");
            writer.beginArray();

            for (Cursor cursor : new Cursor[]{
                db.getTransactions(DBHelper.TransactionDbIds.EXPENSE, null, null, startTimeMs, endTimeMs),
                db.getTransactions(DBHelper.TransactionDbIds.REVENUE, null, null, startTimeMs, endTimeMs)
            })
            {
                while (cursor.moveToNext())
                {
                    Transaction transaction = Transaction.toTransaction(cursor);

                    String receiptFilename = "";
                    if(transaction.receipt.length() > 0)
                    {
                        File receiptFile = new File(transaction.receipt);
                        receiptFilename = receiptFile.getName();
                    }

                    writer.beginObject();

                    writer.name("ID").value(transaction.id);
                    writer.name(DBHelper.TransactionDbIds.TYPE).value(
                            transaction.type == DBHelper.TransactionDbIds.EXPENSE ?
                                "EXPENSE" : "REVENUE");
                    writer.name(DBHelper.TransactionDbIds.DESCRIPTION).value(transaction.description);
                    writer.name(DBHelper.TransactionDbIds.ACCOUNT).value(transaction.account);
                    writer.name(DBHelper.TransactionDbIds.BUDGET).value(transaction.budget);
                    writer.name(DBHelper.TransactionDbIds.VALUE).value(transaction.value);
                    writer.name(DBHelper.TransactionDbIds.NOTE).value(transaction.note);
                    writer.name(DBHelper.TransactionDbIds.DATE).value(transaction.dateMs);
                    writer.name(DBHelper.TransactionDbIds.RECEIPT).value(receiptFilename);

                    writer.endObject();

                    updater.update();

                    if (Thread.currentThread().isInterrupted())
                    {
                        throw new InterruptedException();
                    }
                }

                cursor.close();
            }


            for (String budgetName : budgetNames)
            {
                Budget budget = db.getBudgetStoredOnly(budgetName);

                writer.beginObject();

                writer.name(DBHelper.TransactionDbIds.NAME).value(budget.name);
                writer.name(DBHelper.TransactionDbIds.TYPE).value("BUDGET");
                writer.name(DBHelper.TransactionDbIds.VALUE).value((double)budget.max);

                writer.endObject();

                updater.update();

                if (Thread.currentThread().isInterrupted())
                {
                    throw new InterruptedException();
                }
            }

            writer.endArray();
        }
        finally
        {
            writer.close();
        }
    }
}
