package protect.budgetwatch;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class BudgetViewActivity extends AppCompatActivity
{
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
    }

    @Override
    public void onResume()
    {
        super.onResume();

        final Bundle b = getIntent().getExtras();
        final String budgetName = b != null ? b.getString("id") : null;
        final boolean updateBudget = b != null && b.getBoolean("update", false);
        final boolean viewBudget = b != null && b.getBoolean("view", false);

        if(updateBudget || viewBudget)
        {
            EditText budgetNameField = (EditText) findViewById(R.id.budgetName);
            budgetNameField.setText(budgetName);

            EditText valueField = (EditText) findViewById(R.id.value);
            DBHelper db = new DBHelper(this);
            Budget existingBudget = db.getBudgetStoredOnly(budgetName);
            valueField.setText(String.format("%d", existingBudget.max));

            if(updateBudget)
            {
                budgetNameField.setEnabled(false);
                setTitle(R.string.editBudgetTitle);
            }
            else
            {
                budgetNameField.setEnabled(false);
                valueField.setEnabled(false);

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
        }

        Button saveButton = (Button)findViewById(R.id.saveButton);
        saveButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(final View v)
            {
                EditText budgetNameField = (EditText) findViewById(R.id.budgetName);
                String budgetName = budgetNameField.getText().toString();
                EditText valueField = (EditText) findViewById(R.id.value);
                String valueStr = valueField.getText().toString();

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
                    DBHelper db = new DBHelper(BudgetViewActivity.this);

                    if(updateBudget == false)
                    {
                        db.insertBudget(budgetName, value);
                    }
                    else
                    {
                        db.updateBudget(budgetName, value);
                    }

                    Toast.makeText(BudgetViewActivity.this, "Budget account: " + budgetName,
                            Toast.LENGTH_SHORT).show();

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
                Intent i = new Intent(getApplicationContext(), BudgetViewActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString("id", budgetName);
                bundle.putBoolean("update", true);
                i.putExtras(bundle);
                startActivity(i);
                onResume();
                return true;

            case R.id.action_delete:
                DBHelper db = new DBHelper(this);
                db.deleteBudget(budgetName);
                finish();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
