package protect.budgetwatch;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class BudgetViewActivity extends AppCompatActivity
{
    private static final String TAG = "BudgetWatch";
    private DBHelper _db;

    private EditText _budgetNameEdit;
    private TextView _budgetNameView;
    private EditText _valueEdit;
    private TextView _valueView;

    private String _budgetName;
    private boolean _updateBudget;
    private boolean _viewBudget;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.budget_view_activity);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null)
        {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        _db = new DBHelper(this);

        _budgetNameEdit = (EditText) findViewById(R.id.budgetNameEdit);
        _budgetNameView = (TextView) findViewById(R.id.budgetNameView);
        _valueEdit = (EditText) findViewById(R.id.valueEdit);
        _valueView = (TextView) findViewById(R.id.valueView);

        final Bundle b = getIntent().getExtras();
        _budgetName = b != null ? b.getString("id") : null;
        _updateBudget = b != null && b.getBoolean("update", false);
        _viewBudget = b != null && b.getBoolean("view", false);
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void onResume()
    {
        super.onResume();

        if(_updateBudget || _viewBudget)
        {
            (_updateBudget ? _budgetNameEdit : _budgetNameView).setText(_budgetName);

            Budget existingBudget = _db.getBudgetStoredOnly(_budgetName);
            (_updateBudget ? _valueEdit : _valueView).setText(String.format("%d", existingBudget.max));

            if(_updateBudget)
            {
                setTitle(R.string.editBudgetTitle);

                _budgetNameView.setVisibility(View.GONE);
                _valueView.setVisibility(View.GONE);
            }
            else
            {
                _budgetNameEdit.setVisibility(View.GONE);
                _valueEdit.setVisibility(View.GONE);
                setTitle(R.string.viewBudgetTitle);
            }
        }
        else
        {
            setTitle(R.string.addBudgetTitle);

            _budgetNameView.setVisibility(View.GONE);
            _valueView.setVisibility(View.GONE);
        }
    }

    private void doSave()
    {
        String budgetName = _budgetNameEdit.getText().toString();
        String valueStr = _valueEdit.getText().toString();

        int value;

        try
        {
            value = Integer.parseInt(valueStr);
        }
        catch (NumberFormatException e)
        {
            value = Integer.MIN_VALUE;
        }

        if(value < 0)
        {
            Snackbar.make(_valueEdit, R.string.budgetValueMissing, Snackbar.LENGTH_LONG).show();
            return;
        }

        if(budgetName.length() == 0)
        {
            Snackbar.make(_valueEdit, R.string.budgetTypeMissing, Snackbar.LENGTH_LONG).show();
            return;
        }

        if(_updateBudget == false)
        {
            _db.insertBudget(budgetName, value);
        }
        else
        {
            _db.updateBudget(budgetName, value);
        }

        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        final Bundle b = getIntent().getExtras();
        final boolean viewBudget = b != null && b.getBoolean("view", false);
        final boolean editBudget = b != null && b.getBoolean("update", false);

        if(viewBudget)
        {
            getMenuInflater().inflate(R.menu.view_menu, menu);
        }
        else if(editBudget)
        {
            getMenuInflater().inflate(R.menu.edit_menu, menu);
        }
        else
        {
            getMenuInflater().inflate(R.menu.add_menu, menu);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();

        final Bundle b = getIntent().getExtras();
        final String budgetName = b != null ? b.getString("id") : null;

        if(id == R.id.action_edit)
        {
            finish();

            Intent i = new Intent(getApplicationContext(), BudgetViewActivity.class);
            Bundle bundle = new Bundle();
            bundle.putString("id", budgetName);
            bundle.putBoolean("update", true);
            i.putExtras(bundle);
            startActivity(i);
            return true;
        }

        if(id == R.id.action_delete)
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.deleteBudgetTitle);
            builder.setMessage(R.string.deleteBudgetConfirmation);
            builder.setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    Log.e(TAG, "Deleting budget: " + budgetName);

                    _db.deleteBudget(budgetName);

                    finish();
                    dialog.dismiss();
                }
            });
            builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    dialog.dismiss();
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();

            return true;
        }

        if(id == R.id.action_save)
        {
            doSave();
            return true;
        }

        if(id == android.R.id.home)
        {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy()
    {
        _db.close();
        super.onDestroy();
    }
}
