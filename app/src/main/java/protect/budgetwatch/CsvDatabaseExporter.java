package protect.budgetwatch;

import android.content.Context;
import android.database.Cursor;

import com.google.common.base.Charsets;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Class for exporting the database into CSV (Comma Separate Values)
 * format.
 */
public class CsvDatabaseExporter implements DatabaseExporter
{
    private static final String DATE_FORMATTED_FIELD = "date_formatted";

    public void exportData(Context context, DBHelper db, Long startTimeMs, Long endTimeMs, OutputStream outStream, ImportExportProgressUpdater updater) throws IOException, InterruptedException
    {
        OutputStreamWriter stream = new OutputStreamWriter(outStream, Charsets.UTF_8);
        BufferedWriter output = new BufferedWriter(stream);
        CSVPrinter printer = new CSVPrinter(output, CSVFormat.RFC4180);

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
            // Print the header
            printer.printRecord(DBHelper.TransactionDbIds.NAME,
                    DBHelper.TransactionDbIds.TYPE,
                    DBHelper.TransactionDbIds.DESCRIPTION,
                    DBHelper.TransactionDbIds.ACCOUNT,
                    DBHelper.TransactionDbIds.BUDGET,
                    DBHelper.TransactionDbIds.VALUE,
                    DBHelper.TransactionDbIds.NOTE,
                    DBHelper.TransactionDbIds.DATE,
                    DATE_FORMATTED_FIELD,
                    DBHelper.TransactionDbIds.RECEIPT);

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

                    Locale currentLocale = Locale.getDefault();
                    DateFormat dateFormat = DateFormat.getDateTimeInstance(
                            DateFormat.DEFAULT, DateFormat.DEFAULT, currentLocale);

                    String dateFormatted = dateFormat.format(new Date(transaction.dateMs));

                    printer.printRecord(transaction.id,
                            transaction.type == DBHelper.TransactionDbIds.EXPENSE ?
                                    "EXPENSE" : "REVENUE",
                            transaction.description,
                            transaction.account,
                            transaction.budget,
                            transaction.value,
                            transaction.note,
                            transaction.dateMs,
                            dateFormatted,
                            receiptFilename);

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

                printer.printRecord(budget.name,
                        "BUDGET",
                        "", // blank description
                        "", // blank account
                        "", // blank budget (handled in id field)
                        budget.max,
                        "", // blank note
                        "", // blank date
                        "", // blank formatted date
                        ""); // blank receipt

                updater.update();

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
