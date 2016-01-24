package protect.budgetwatch;


import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

public class BudgetActivity extends AppCompatActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.budget_activity);
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

        final ListView budgetList = (ListView) findViewById(R.id.list);
        DBHelper db = new DBHelper(this);

        final Calendar date = new GregorianCalendar();
        final long dateNowMs = date.getTimeInMillis();
        int year = date.get(Calendar.YEAR);
        int month = date.get(Calendar.MONTH);

        // Set to beginning of the month
        date.set(year, month, 1);
        final long dateMonthStartMs = date.getTimeInMillis();

        final List<Budget> budgets = db.getBudgets(dateMonthStartMs, dateNowMs);
        final BudgetAdapter budgetListAdapter = new BudgetAdapter(this, budgets);
        budgetList.setAdapter(budgetListAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.add_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();

        if (id == R.id.action_add)
        {
        }

        return super.onOptionsItemSelected(item);
    }
}

class BudgetAdapter extends ArrayAdapter<Budget>
{
    public BudgetAdapter(Context context, List<Budget> items)
    {
        super(context, 0, items);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        // Get the data item for this position
        Budget item = getItem(position);

        // Check if an existing view is being reused, otherwise inflate the view

        if (convertView == null)
        {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.budget_layout,
                    parent, false);
        }

        TextView budgetName = (TextView) convertView.findViewById(R.id.budgetName);
        ProgressBar budgetBar = (ProgressBar) convertView.findViewById(R.id.budgetBar);
        TextView budgetValue = (TextView) convertView.findViewById(R.id.budgetValue);

        budgetName.setText(item.name);

        budgetBar.setMax(item.max);
        budgetBar.setProgress(item.current);

        String fractionFormat = getContext().getResources().getString(R.string.fraction);
        String fraction = String.format(fractionFormat, item.current, item.max);

        budgetValue.setText(fraction);

        return convertView;
    }
}