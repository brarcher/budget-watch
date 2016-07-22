package protect.budgetwatch;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.DatePicker;

public class TransactionActivity extends AppCompatActivity
{
    private TransactionDatabaseChangedReceiver _dbChanged;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.transaction_activity);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null)
        {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        _dbChanged = new TransactionDatabaseChangedReceiver();
        this.registerReceiver(_dbChanged, new IntentFilter(TransactionDatabaseChangedReceiver.ACTION_DATABASE_CHANGED));

        resetView();
    }

    private void resetView()
    {
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabLayout);
        tabLayout.removeAllTabs();
        tabLayout.addTab(tabLayout.newTab().setText(R.string.expensesTitle));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.revenuesTitle));
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

        final ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
        final PagerAdapter adapter = new TransactionPagerAdapter
                (getSupportFragmentManager(), tabLayout.getTabCount());
        viewPager.setAdapter(adapter);

        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener()
        {
            @Override
            public void onTabSelected(TabLayout.Tab tab)
            {
                viewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab)
            {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab)
            {

            }
        });
    }

    @Override
    public void onResume()
    {
        super.onResume();

        if(_dbChanged.hasChanged())
        {
            resetView();
            _dbChanged.reset();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.transaction_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();

        if (id == R.id.action_add)
        {
            Intent i = new Intent(getApplicationContext(), TransactionViewActivity.class);
            final Bundle b = new Bundle();
            b.putInt("type", getCurrentTabType());
            i.putExtras(b);
            startActivity(i);
            return true;
        }

        if(id == R.id.action_purge_receipts)
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.cleanupHelp);

            final View view = getLayoutInflater().inflate(R.layout.cleanup_layout, null, false);

            builder.setView(view);
            builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    dialog.cancel();
                }
            });
            builder.setPositiveButton(R.string.clean, new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    DatePicker endDatePicker = (DatePicker) view.findViewById(R.id.endDate);

                    long endOfBudgetMs = CalendarUtil.getEndOfDayMs(endDatePicker.getYear(),
                            endDatePicker.getMonth(), endDatePicker.getDayOfMonth());

                    DatabaseCleanupTask task = new DatabaseCleanupTask(TransactionActivity.this,
                            endOfBudgetMs);
                    task.execute();
                }
            });

            builder.show();
            return true;
        }

        if(id == android.R.id.home)
        {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private int getCurrentTabType()
    {
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabLayout);

        if(tabLayout.getSelectedTabPosition() == 0)
        {
            return DBHelper.TransactionDbIds.EXPENSE;
        }
        else
        {
            return DBHelper.TransactionDbIds.REVENUE;
        }
    }

    @Override
    public void onDestroy()
    {
        this.unregisterReceiver(_dbChanged);
        super.onDestroy();
    }
}