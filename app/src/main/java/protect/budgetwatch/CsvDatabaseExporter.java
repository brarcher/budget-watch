package protect.budgetwatch;

import android.content.Context;
import android.database.Cursor;

import com.google.common.base.Charsets;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

/**
 * Class for exporting the database into CSV (Comma Separate Values)
 * format.
 */
public class CsvDatabaseExporter implements DatabaseExporter
{
    public void exportData(Context context, DBHelper db, OutputStream outStream) throws IOException, InterruptedException
    {
        OutputStreamWriter output = new OutputStreamWriter(outStream, Charsets.UTF_8);
        CSVPrinter printer = new CSVPrinter(output, CSVFormat.RFC4180);

        try
        {

            // Print the header
            printer.printRecord(DBHelper.TransactionDbIds.NAME,
                    DBHelper.TransactionDbIds.TYPE,
                    DBHelper.TransactionDbIds.DESCRIPTION,
                    DBHelper.TransactionDbIds.ACCOUNT,
                    DBHelper.TransactionDbIds.BUDGET,
                    DBHelper.TransactionDbIds.VALUE,
                    DBHelper.TransactionDbIds.NOTE,
                    DBHelper.TransactionDbIds.DATE,
                    DBHelper.TransactionDbIds.RECEIPT);

            for (Cursor cursor : new Cursor[]{db.getExpenses(), db.getRevenues()})
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

                    printer.printRecord(transaction.id,
                            transaction.type == DBHelper.TransactionDbIds.EXPENSE ?
                                    "EXPENSE" : "REVENUE",
                            transaction.description,
                            transaction.account,
                            transaction.budget,
                            transaction.value,
                            transaction.note,
                            transaction.dateMs,
                            receiptFilename);

                    if (Thread.currentThread().isInterrupted())
                    {
                        throw new InterruptedException();
                    }
                }

                cursor.close();
            }


            for (String budgetName : db.getBudgetNames())
            {
                Budget budget = db.getBudgetStoredOnly(budgetName);

                printer.printRecord(budget.name,
                        "BUDGET",
                        "", // blank description
                        "", // blank account
                        "", // blank budget (handled in id field)
                        budget.max,
                        "", // blank note
                        "", // blank date
                        ""); // blank receipt

                if (Thread.currentThread().isInterrupted())
                {
                    throw new InterruptedException();
                }
            }
        }
        finally
        {
            printer.close();
        }
    }
}
