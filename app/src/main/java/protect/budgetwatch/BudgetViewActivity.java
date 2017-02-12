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
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void onResume()
    {
        super.onResume();

        final Bundle b = getIntent().getExtras();
        final String budgetName = b != null ? b.getString("id") : null;
        final boolean updateBudget = b != null && b.getBoolean("update", false);
        final boolean viewBudget = b != null && b.getBoolean("view", false);

        final EditText budgetNameEdit = (EditText) findViewById(R.id.budgetNameEdit);
        final TextView budgetNameView = (TextView) findViewById(R.id.budgetNameView);
        final EditText valueEdit = (EditText) findViewById(R.id.valueEdit);
        final TextView valueView = (TextView) findViewById(R.id.valueView);

        if(updateBudget || viewBudget)
        {
            (updateBudget ? budgetNameEdit : budgetNameView).setText(budgetName);

            Budget existingBudget = _db.getBudgetStoredOnly(budgetName);
            (updateBudget ? valueEdit : valueView).setText(String.format("%d", existingBudget.max));

            if(updateBudget)
            {
                setTitle(R.string.editBudgetTitle);

                budgetNameView.setVisibility(View.GONE);
                valueView.setVisibility(View.GONE);
            }
            else
            {
                budgetNameEdit.setVisibility(View.GONE);
                valueEdit.setVisibility(View.GONE);

                Button saveButton = (Button) findViewById(R.id.saveButton);
                Button cancelButton = (Button) findViewById(R.id.cancelButton);
                saveButton.setVisibility(Button.GONE);
                cancelButton.setVisibility(Button.GONE);

                setTitle(R.string.viewBudgetTitle);
            }
        }
        else
        {
            setTitle(R.string.addBudgetTitle);

            budgetNameView.setVisibility(View.GONE);
            valueView.setVisibility(View.GONE);
        }

        Button saveButton = (Button)findViewById(R.id.saveButton);
        saveButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(final View v)
            {
                String budgetName = budgetNameEdit.getText().toString();
                String valueStr = valueEdit.getText().toString();

                int value;

                try
                {
                    value = Integer.parseInt(valueStr);
                }
                catch (NumberFormatException e)
                {
                    value = Integer.MIN_VALUE;
                }

                if (budgetName.length() > 0 && value >= 0)
                {
                    if(updateBudget == false)
                    {
                        _db.insertBudget(budgetName, value);
                    }
                    else
                    {
                        _db.updateBudget(budgetName, value);
                    }

                    finish();
                }
                else
                {
                    int messageId = R.string.budgetTypeMissing;
                    if (budgetName.isEmpty() == false)
                    {
                        messageId = R.string.budgetValueMissing;
                    }

                    Snackbar.make(v, messageId, Snackbar.LENGTH_LONG).show();
                }
            }
        });

        Button cancelButton = (Button)findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                finish();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        final Bundle b = getIntent().getExtras();
        final boolean viewBudget = b != null && b.getBoolean("view", false);
        final boolean editBudget = b != null && b.getBoolean("update", false);

        if(viewBudget)
        {
            getMenuInflater().inflate(R.menu.edit_menu, menu);
        }

        if(editBudget)
        {
            getMenuInflater().inflate(R.menu.delete_menu, menu);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();

        final Bundle b = getIntent().getExtras();
        final String budgetName = b != null ? b.getString("id") : null;

        switch(id)
        {
            case R.id.action_edit:
                finish();

                Intent i = new Intent(getApplicationContext(), BudgetViewActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString("id", budgetName);
                bundle.putBoolean("update", true);
                i.putExtras(bundle);
                startActivity(i);
                return true;

            case R.id.action_delete:
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

            case android.R.id.home:
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
