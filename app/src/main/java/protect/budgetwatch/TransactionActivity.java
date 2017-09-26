package protect.budgetwatch;

import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.Context;
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
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.DatePicker;

public class TransactionActivity extends AppCompatActivity
{
    private TransactionDatabaseChangedReceiver _dbChanged;
    private static final String TAG = "BudgetWatch";

    private boolean _currentlySearching = false;

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

        String search = getIntent().getStringExtra(SearchManager.QUERY);
        resetView(search);
    }

    private void resetView(String search)
    {
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabLayout);
        tabLayout.removeAllTabs();
        tabLayout.addTab(tabLayout.newTab().setText(R.string.expensesTitle));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.revenuesTitle));
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

        final ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
        final PagerAdapter adapter = new TransactionPagerAdapter
                (getSupportFragmentManager(), search, tabLayout.getTabCount());
        viewPager.setAdapter(adapter);

        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener()
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

        if(_dbChanged.hasChanged() || Intent.ACTION_SEARCH.equals(getIntent().getAction()))
        {
            String search = null;

            // Only use the search if the search view is open. When it is canceled
            // ignore the search.
            if(_currentlySearching)
            {
                search = getIntent().getStringExtra(SearchManager.QUERY);
            }

            resetView(search);
            _dbChanged.reset();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.transaction_menu, menu);

        // Associate searchable configuration with the SearchView
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

        searchView.setOnCloseListener(new SearchView.OnCloseListener()
        {
            @Override
            public boolean onClose()
            {
                _currentlySearching = false;

                // Re-populate the transactions
                onResume();

                // false: allow the default cleanup behavior on the search view on closing.
                return false;
            }
        });

        searchView.setOnSearchClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                _currentlySearching = true;
            }
        });

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
    protected void onNewIntent(Intent intent)
    {
        if (Intent.ACTION_SEARCH.equals(intent.getAction()))
        {
            String query = intent.getStringExtra(SearchManager.QUERY);
            Log.d(TAG, "Received search: " + query);

            setIntent(intent);
            // onResume() will be called right after this, so the search will be used
        }
    }

    @Override
    public void onDestroy()
    {
        this.unregisterReceiver(_dbChanged);
        super.onDestroy();
    }
}