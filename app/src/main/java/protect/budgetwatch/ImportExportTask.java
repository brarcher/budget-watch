package protect.budgetwatch;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

class ImportExportTask extends AsyncTask<Void, Void, Boolean>
{
    private static final String TAG = "BudgetWatch";

    private final Activity activity;
    private final boolean doImport;
    private final DataFormat format;
    private final File target;
    private final InputStream inputStream;
    private final TaskCompleteListener listener;

    // Start and end times for exporting transactions
    private final Long startTimeMs;
    private final Long endTimeMs;

    private ProgressDialog progress;

    /**
     * Constructor which will setup a task for exporting to the given file
     */
    ImportExportTask(Activity activity, DataFormat format,
                            File target, TaskCompleteListener listener,
                            Long startTimeMs, Long endTimeMs)
    {
        super();
        this.activity = activity;
        this.doImport = false;
        this.format = format;
        this.target = target;
        this.inputStream = null;
        this.listener = listener;
        this.startTimeMs = startTimeMs;
        this.endTimeMs = endTimeMs;
    }

    /**
     * Constructor which will setup a task for importing from the given InputStream.
     */
    ImportExportTask(Activity activity, DataFormat format,
                     InputStream input, TaskCompleteListener listener)
    {
        super();
        this.activity = activity;
        this.doImport = true;
        this.format = format;
        this.target = null;
        this.inputStream = input;
        this.listener = listener;
        this.startTimeMs = null;
        this.endTimeMs = null;
    }

    private boolean performImport(InputStream inputStream, DBHelper db)
    {
        final String BASE_MESSAGE = ImportExportTask.this.activity.getResources().getString(R.string.importProgressFormat);
        ImportExportProgressUpdater updater = new ImportExportProgressUpdater(activity, progress, BASE_MESSAGE);

        boolean result = MultiFormatImporter.importData(activity, db, inputStream, format, updater);

        Log.i(TAG, "Import result: " + result);
        return result;
    }

    private boolean performExport(File exportFile, DBHelper db, Long startTimeMs, Long endTimeMs)
    {
        boolean result = false;

        final String BASE_MESSAGE = ImportExportTask.this.activity.getResources().getString(R.string.exportProgressFormat);

        ImportExportProgressUpdater updater = new ImportExportProgressUpdater(activity, progress, BASE_MESSAGE);

        try
        {
            FileOutputStream outStream = new FileOutputStream(exportFile);
            result = MultiFormatExporter.exportData(this.activity, db, startTimeMs, endTimeMs, outStream, format, updater);
            outStream.close();
        }
        catch (IOException e)
        {
            Log.e(TAG, "Unable to export file", e);
        }

        Log.i(TAG, "Export of '" + exportFile.getAbsolutePath() + "' result: " + result);

        return result;
    }

    protected void onPreExecute()
    {
        progress = new ProgressDialog(activity);
        progress.setTitle(doImport ? R.string.importing : R.string.exporting);

        progress.setOnDismissListener(new DialogInterface.OnDismissListener()
        {
            @Override
            public void onDismiss(DialogInterface dialog)
            {
                ImportExportTask.this.cancel(true);
            }
        });

        progress.show();
    }

    protected Boolean doInBackground(Void... nothing)
    {
        boolean result;
        final DBHelper db = new DBHelper(activity);

        if(doImport)
        {
            result = performImport(inputStream, db);
        }
        else
        {
            result = performExport(target, db, startTimeMs, endTimeMs);
        }

        db.close();

        return result;
    }

    protected void onPostExecute(Boolean result)
    {
        listener.onTaskComplete(result);

        progress.dismiss();
        Log.i(TAG, (doImport ? "Import" : "Export") + " Complete");
    }

    protected void onCancelled()
    {
        progress.dismiss();
        Log.i(TAG, (doImport ? "Import" : "Export") + " Cancelled");
    }

    interface TaskCompleteListener
    {
        void onTaskComplete(boolean success);
    }
}
