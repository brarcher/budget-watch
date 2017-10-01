package protect.budgetwatch;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;

class MultiFormatImporter
{
    private static final String TAG = "BudgetWatch";

    /**
     * Attempts to import data from the input stream of the
     * given format into the database.
     *
     * The input stream is not closed, and doing so is the
     * responsibility of the caller.
     *
     * @return true if the database was successfully imported,
     * false otherwise. If false, no data was written to
     * the database.
     */
    public static boolean importData(Context context, DBHelper db, InputStream input, DataFormat format, ImportExportProgressUpdater updater)
    {
        DatabaseImporter importer = null;

        switch(format)
        {
            case CSV:
                importer = new CsvDatabaseImporter();
                break;
            case JSON:
                importer = new JsonDatabaseImporter();
                break;
            case ZIP:
                importer = new ZipDatabaseImporter();
                break;
        }

        if(importer != null)
        {
            try
            {
                importer.importData(context, db, input, updater);
                return true;
            }
            catch(IOException | FormatException | InterruptedException e)
            {
                Log.e(TAG, "Failed to input data", e);
            }

            return false;
        }
        else
        {
            Log.e(TAG, "Unsupported data format imported: " + format.name());
            return false;
        }
    }
}
