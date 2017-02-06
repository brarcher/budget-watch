package protect.budgetwatch;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.util.JsonReader;
import android.util.JsonToken;

import com.google.common.base.Charsets;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Class for importing a database from JSON formatted data.
 */
public class JsonDatabaseImporter implements DatabaseImporter
{
    public void importData(Context context, DBHelper db, InputStream input) throws IOException, FormatException, InterruptedException
    {
        InputStreamReader reader = new InputStreamReader(input, Charsets.UTF_8);
        JsonReader parser = new JsonReader(reader);

        SQLiteDatabase database = db.getWritableDatabase();
        database.beginTransaction();

        try
        {
            parser.beginArray();

            while(parser.hasNext())
            {
                parser.beginObject();

                Integer id = null;
                String name = null;
                String type = null;
                String description = null;
                String account = null;
                String budget = null;
                Double value = null;
                String note = null;
                Long dateMs = null;
                String receiptFilename = null;

                while(parser.hasNext())
                {
                    String itemName = parser.nextName();
                    if(itemName.equals(DBHelper.TransactionDbIds.NAME))
                    {
                        name = parser.nextString();
                    }
                    else if(itemName.equals("ID"))
                    {
                        id = parser.nextInt();
                    }
                    else if(itemName.equals(DBHelper.TransactionDbIds.TYPE))
                    {
                        type = parser.nextString();
                    }
                    else if(itemName.equals(DBHelper.TransactionDbIds.DESCRIPTION))
                    {
                        description = parser.nextString();
                    }
                    else if(itemName.equals(DBHelper.TransactionDbIds.ACCOUNT))
                    {
                        account = parser.nextString();
                    }
                    else if(itemName.equals(DBHelper.TransactionDbIds.BUDGET))
                    {
                        budget = parser.nextString();
                    }
                    else if(itemName.equals(DBHelper.TransactionDbIds.VALUE))
                    {
                        value = parser.nextDouble();
                    }
                    else if(itemName.equals(DBHelper.TransactionDbIds.NOTE))
                    {
                        note = parser.nextString();
                    }
                    else if(itemName.equals(DBHelper.TransactionDbIds.DATE))
                    {
                        dateMs = parser.nextLong();
                    }
                    else if(itemName.equals(DBHelper.TransactionDbIds.RECEIPT))
                    {
                        receiptFilename = parser.nextString();
                    }
                    else
                    {
                        throw new FormatException("Issue parsing JSON data, unknown field: " + itemName);
                    }
                }

                if(type == null)
                {
                    throw new FormatException("Issue parsing JSON data, missing type");
                }

                if(type.equals("BUDGET"))
                {
                    importBudget(database, db, name, value);
                }
                else if(type.equals("EXPENSE") || type.equals("REVENUE"))
                {
                    importTransaction(context, database, db, id, type, description, account,
                            budget, value, note, dateMs, receiptFilename);
                }
                else
                {
                    throw new FormatException("Issue parsing JSON data, unexpected type: " + type);
                }

                parser.endObject();

                if(Thread.currentThread().isInterrupted())
                {
                    throw new InterruptedException();
                }
            }

            parser.endArray();

            if(parser.peek() != JsonToken.END_DOCUMENT)
            {
                throw new FormatException("Issue parsing JSON data, no more data expected but found some");
            }

            parser.close();

            // Do not close the parser, as it will close the input stream;
            // Closing the input stream is the responsibility of the caller.
            database.setTransactionSuccessful();
        }
        catch(IllegalArgumentException e)
        {
            throw new FormatException("Issue parsing JSON data", e);
        }
        finally
        {
            database.endTransaction();
            database.close();
        }
    }

    /**
     * Import a single transaction into the database using the given
     * session.
     */
    private void importTransaction(Context context, SQLiteDatabase database, DBHelper helper,
                                   Integer id, String typeStr, String description, String account,
                                   String budget, Double value, String note, Long dateMs,
                                   String receiptFilename)
            throws FormatException
    {
        int type;
        if(typeStr.equals("EXPENSE"))
        {
            type = DBHelper.TransactionDbIds.EXPENSE;
        }
        else if(typeStr.equals("REVENUE"))
        {
            type = DBHelper.TransactionDbIds.REVENUE;
        }
        else
        {
            throw new FormatException("Unrecognized type: " + typeStr);
        }

        // Ensure that the required data exists
        if(id == null || budget == null || value == null || dateMs == null)
        {
            throw new FormatException("Missing required data in JSON record");
        }

        // All the other fields can be blank strings if they are missing.

        description = (description != null ? description : "");
        account = (account != null ? account : "");
        note = (note != null ? note : "");
        receiptFilename = (receiptFilename != null ? receiptFilename : "");

        String receipt = "";
        if(receiptFilename.length() > 0)
        {
            // There is a receipt here. If the file actually exists, go with it
            File dir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            if(dir != null)
            {
                File imageFile = new File(dir, receiptFilename);
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
    private void importBudget(SQLiteDatabase database, DBHelper helper, String name, Double value)
            throws FormatException
    {
        // Check that both fields exist
        // Ensure that the required data exists
        if(name == null || value == null)
        {
            throw new FormatException("Missing required data in JSON record");
        }

        helper.insertBudget(database, name, value.intValue());
    }
}
