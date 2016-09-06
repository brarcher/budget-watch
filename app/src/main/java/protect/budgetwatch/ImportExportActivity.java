package protect.budgetwatch;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import com.google.common.collect.ImmutableMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ImportExportActivity extends AppCompatActivity
{
    private static final String TAG = "BudgetWatch";

    ImportExportTask importExporter;
    private Map<String, DataFormat> _fileFormatMap;

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

        _fileFormatMap = ImmutableMap.<String, DataFormat>builder()
                .put(getResources().getString(R.string.csv), DataFormat.CSV)
                .put(getResources().getString(R.string.zip), DataFormat.ZIP)
                .build();

        final Spinner fileFormatSpinner = (Spinner) findViewById(R.id.fileFormatSpinner);
        List<String> names = new ArrayList<>(_fileFormatMap.keySet());
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, names);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        fileFormatSpinner.setAdapter(dataAdapter);

        updateHelpText();

        fileFormatSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                updateHelpText();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent)
            {
                // Nothing to do
            }
        });

        Button importButton = (Button)findViewById(R.id.importButton);
        importButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Object selected = fileFormatSpinner.getSelectedItem();
                DataFormat format = _fileFormatMap.get(selected);
                importExporter = new ImportExportTask(ImportExportActivity.this,
                        true, format);
                importExporter.execute();
            }
        });

        Button exportButton = (Button)findViewById(R.id.exportButton);
        exportButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Object selected = fileFormatSpinner.getSelectedItem();
                DataFormat format = _fileFormatMap.get(selected);
                importExporter = new ImportExportTask(ImportExportActivity.this,
                        false, format);
                importExporter.execute();
            }
        });
    }

    private void updateHelpText()
    {
        final Spinner fileFormatSpinner = (Spinner) findViewById(R.id.fileFormatSpinner);
        String selection = (String)fileFormatSpinner.getSelectedItem();
        selection = selection.toLowerCase();
        String text = String.format(getString(R.string.importExportHelp), selection);

        TextView helpText = (TextView)findViewById(R.id.helpText);
        helpText.setText(text);
    }

    @Override
    protected void onDestroy()
    {
        if(importExporter != null && importExporter.getStatus() != AsyncTask.Status.RUNNING)
        {
            importExporter.cancel(true);
        }
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();

        if(id == android.R.id.home)
        {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}