package protect.budgetwatch;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;

import com.google.common.base.Charsets;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Class for importing a database from CSV (Comma Separate Values)
 * formatted data.
 *
 * The database's transactions and budgets tables are both expected to
 * appear in the CSV data, with the transactions first. A header is expected
 * for each table showing the names of the columns. A newline separates
 * the transactions and budgets databases.
 */
public class CsvDatabaseImporter implements DatabaseImporter
{
    public void importData(Context context, DBHelper db, InputStream input, ImportExportProgressUpdater updater) throws IOException, FormatException, InterruptedException
    {
        InputStreamReader reader = new InputStreamReader(input, Charsets.UTF_8);
        final CSVParser parser = new CSVParser(reader, CSVFormat.RFC4180.withHeader());

        SQLiteDatabase database = db.getWritableDatabase();
        database.beginTransaction();

        try
        {
            for (CSVRecord record : parser)
            {
                String type = record.get(DBHelper.TransactionDbIds.TYPE);
                if(type.equals("BUDGET"))
                {
                    importBudget(database, db, record);
                }
                else
                {
                    importTransaction(context, database, db, record);
                }

                updater.update();

                if(Thread.currentThread().isInterrupted())
                {
                    throw new InterruptedException();
                }
            }

            // Do not close the parser, as it will close the input stream;
            // Closing the input stream is the responsibility of the caller.
            database.setTransactionSuccessful();
        }
        catch(IllegalArgumentException|IllegalStateException e)
        {
            throw new FormatException("Issue parsing CSV data", e);
        }
        finally
        {
            database.endTransaction();
            database.close();
        }
    }

    /**
     * Extract a string from the items array. The index into the array
     * is determined by looking up the index in the fields map using the
     * "key" as the key. If no such key exists, defaultValue is returned
     * if it is not null. Otherwise, a FormatException is thrown.
     */
    private String extractString(String key, CSVRecord record, String defaultValue)
            throws FormatException
    {
        String toReturn = defaultValue;

        if(record.isMapped(key))
        {
            toReturn = record.get(key);
        }
        else
        {
            if(defaultValue == null)
            {
                throw new FormatException("Field not used but expected: " + key);
            }
        }

        return toReturn;
    }

    /**
     * Extract an integer from the items array. The index into the array
     * is determined by looking up the index in the fields map using the
     * "key" as the key. If no such key exists, or the data is not a valid
     * int, a FormatException is thrown.
     */
    private int extractInt(String key, CSVRecord record)
            throws FormatException
    {
        if(record.isMapped(key) == false)
        {
            throw new FormatException("Field not used but expected: " + key);
        }

        try
        {
            return Integer.parseInt(record.get(key));
        }
        catch(NumberFormatException e)
        {
            throw new FormatException("Failed to parse field: " + key, e);
        }
    }

    /**
     * Extract an double from the items array. The index into the array
     * is determined by looking up the index in the fields map using the
     * "key" as the key. If no such key exists, or the data is not a valid
     * double, a FormatException is thrown.
     */
    private double extractDouble(String key, CSVRecord record)
            throws FormatException
    {
        if(record.isMapped(key) == false)
        {
            throw new FormatException("Field not used but expected: " + key);
        }

        try
        {
            return Double.parseDouble(record.get(key));
        }
        catch(NumberFormatException e)
        {
            throw new FormatException("Failed to parse field: " + key, e);
        }
    }

    /**
     * Extract an long from the items array. The index into the array
     * is determined by looking up the index in the fields map using the
     * "key" as the key. If no such key exists, or the data is not a valid
     * long, a FormatException is thrown.
     */
    private long extractLong(String key, CSVRecord record)
            throws FormatException
    {
        if(record.isMapped(key) == false)
        {
            throw new FormatException("Field not used but expected: " + key);
        }

        try
        {
            return Long.parseLong(record.get(key));
        }
        catch(NumberFormatException e)
        {
            throw new FormatException("Failed to parse field: " + key, e);
        }
    }

    /**
     * Import a single transaction into the database using the given
     * session.
     */
    private void importTransaction(Context context, SQLiteDatabase database, DBHelper helper, CSVRecord record)
            throws FormatException
    {
        int id = extractInt(DBHelper.TransactionDbIds.NAME, record);

        int type;
        String typeStr = extractString(DBHelper.TransactionDbIds.TYPE, record, "");
        switch (typeStr)
        {
            case "EXPENSE":
                type = DBHelper.TransactionDbIds.EXPENSE;
                break;
            case "REVENUE":
                type = DBHelper.TransactionDbIds.REVENUE;
                break;
            default:
                throw new FormatException("Unrecognized type: " + typeStr);
        }

        String description = extractString(DBHelper.TransactionDbIds.DESCRIPTION, record, "");
        String account = extractString(DBHelper.TransactionDbIds.ACCOUNT, record, "");
        String budget = extractString(DBHelper.TransactionDbIds.BUDGET, record, "");
        double value = extractDouble(DBHelper.TransactionDbIds.VALUE, record);
        String note = extractString(DBHelper.TransactionDbIds.NOTE, record, "");
        long dateMs = extractLong(DBHelper.TransactionDbIds.DATE, record);

        String receipt = "";
        String potentialReceipt = extractString(DBHelper.TransactionDbIds.RECEIPT, record, "");
        if(potentialReceipt.length() > 0)
        {
            // There is a receipt here. If the file actually exists, go with it
            File dir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            if(dir != null)
            {
                File imageFile = new File(dir, potentialReceipt);
                if(imageFile.isFile())
                {
                    receipt = imageFile.getAbsolutePath();
                }
            }
        }

        helper.insertTransaction(database, id, type, description, account, budget, value, note, dateMs, receipt);
    }

    /**
     * Import a single budget into the database using the given
     * session.
     */
    private void importBudget(SQLiteDatabase database, DBHelper helper, CSVRecord record)
            throws FormatException
    {
        String name = extractString(DBHelper.BudgetDbIds.NAME, record, null);

        // The transaction field for value is used to indicate the budget value
        int budget = extractInt(DBHelper.TransactionDbIds.VALUE, record);

        helper.insertBudget(database, name, budget);
    }
}
