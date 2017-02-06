package protect.budgetwatch;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

class ImportExportTask extends AsyncTask<Void, Void, Boolean>
{
    private static final String TAG = "BudgetWatch";

    private final Activity activity;
    private final boolean doImport;
    private final DataFormat format;
    private final File target;
    private final TaskCompleteListener listener;

    private ProgressDialog progress;

    public ImportExportTask(Activity activity, boolean doImport, DataFormat format,
                            File target, TaskCompleteListener listener)
    {
        super();
        this.activity = activity;
        this.doImport = doImport;
        this.format = format;
        this.target = target;
        this.listener = listener;
    }

    private boolean performImport(File importFile, DBHelper db)
    {
        boolean result = false;

        try
        {
            FileInputStream fileReader = new FileInputStream(importFile);
            result = MultiFormatImporter.importData(activity, db, fileReader, format);
            fileReader.close();
        }
        catch(IOException e)
        {
            Log.e(TAG, "Unable to import file", e);
        }

        Log.i(TAG, "Import of '" + importFile.getAbsolutePath() + "' result: " + result);
        return result;
    }

    private boolean performExport(File exportFile, DBHelper db)
    {
        boolean result = false;

        try
        {
            FileOutputStream outStream = new FileOutputStream(exportFile);
            result = MultiFormatExporter.exportData(this.activity, db, outStream, format);
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
            result = performImport(target, db);
        }
        else
        {
            result = performExport(target, db);
        }

        db.close();

        return result;
    }

    protected void onPostExecute(Boolean result)
    {
        listener.onTaskComplete(result, target);

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
        void onTaskComplete(boolean success, File file);
    }
}
