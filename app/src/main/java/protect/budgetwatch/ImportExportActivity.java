package protect.budgetwatch;

import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class ImportExportActivity extends AppCompatActivity
{
    private static final String TAG = "BudgetWatch";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.import_export_activity);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null)
        {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        final File sdcardDir = Environment.getExternalStorageDirectory();
        final File importExportFile = new File(sdcardDir, "BudgetWatch.csv");
        final DBHelper db = new DBHelper(this);

        Button importButton = (Button)findViewById(R.id.importButton);
        importButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if(importExportFile.exists() == false)
                {
                    toastWithArg(R.string.fileMissing, importExportFile.getAbsolutePath());
                    return;
                }

                try
                {
                    FileInputStream fileReader = new FileInputStream(importExportFile);
                    InputStreamReader reader = new InputStreamReader(fileReader);
                    boolean result = MultiFormatImporter.importData(db, reader, DataFormat.CSV);
                    reader.close();

                    int messageId = result ? R.string.importedFrom : R.string.importFailed;
                    toastWithArg(messageId, importExportFile.getAbsolutePath());
                }
                catch(IOException e)
                {
                    Log.e(TAG, "Unable to import file", e);
                }
            }
        });

        Button exportButton = (Button)findViewById(R.id.exportButton);
        exportButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                try
                {
                    FileOutputStream fileWriter = new FileOutputStream(importExportFile);
                    OutputStreamWriter writer = new OutputStreamWriter(fileWriter);
                    boolean result = MultiFormatExporter.exportData(db, writer, DataFormat.CSV);
                    writer.close();

                    int messageId = result ? R.string.exportedTo : R.string.exportFailed;
                    toastWithArg(messageId, importExportFile.getAbsolutePath());
                }
                catch(IOException e)
                {
                    Log.e(TAG, "Unable to export file", e);
                }
            }
        });
    }

    private void toastWithArg(int stringId, String argument)
    {
        String template = getResources().getString(stringId);
        String message = String.format(template, argument);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}
