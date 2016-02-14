package protect.budgetwatch;

import android.database.Cursor;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.IOException;
import java.io.OutputStreamWriter;

/**
 * Class for exporting the database into CSV (Comma Separate Values)
 * format.
 */
public class CsvDatabaseExporter implements DatabaseExporter
{
    public void exportData(DBHelper db, OutputStreamWriter output) throws IOException, InterruptedException
    {
        CSVPrinter printer = new CSVPrinter(output, CSVFormat.RFC4180);

        // Print the header
        printer.printRecord(DBHelper.TransactionDbIds.NAME,
                DBHelper.TransactionDbIds.TYPE,
                DBHelper.TransactionDbIds.DESCRIPTION,
                DBHelper.TransactionDbIds.ACCOUNT,
                DBHelper.TransactionDbIds.BUDGET,
                DBHelper.TransactionDbIds.VALUE,
                DBHelper.TransactionDbIds.NOTE,
                DBHelper.TransactionDbIds.DATE);

        for(Cursor cursor : new Cursor[]{db.getExpenses(), db.getRevenues()})
        {
            while(cursor.moveToNext())
            {
                Transaction transaction = Transaction.toTransaction(cursor);

                printer.printRecord(transaction.id,
                        transaction.type == DBHelper.TransactionDbIds.EXPENSE ?
                                "EXPENSE" : "REVENUE",
                        transaction.description,
                        transaction.account,
                        transaction.budget,
                        transaction.value,
                        transaction.note,
                        transaction.dateMs);

                if(Thread.currentThread().isInterrupted())
                {
                    throw new InterruptedException();
                }
            }

            cursor.close();
        }


        for(String budgetName : db.getBudgetNames())
        {
            Budget budget = db.getBudgetStoredOnly(budgetName);

            printer.printRecord(budget.name,
                    "BUDGET",
                    "", // blank description
                    "", // blank account
                    "", // blank budget (handled in id field)
                    budget.max,
                    "", // blank note
                    ""); // blank date

            if(Thread.currentThread().isInterrupted())
            {
                throw new InterruptedException();
            }
        }

        printer.close();
    }
}
