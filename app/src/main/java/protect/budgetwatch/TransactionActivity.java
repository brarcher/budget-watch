package protect.budgetwatch;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

public class TransactionActivity extends AppCompatActivity
{
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
    }

    @Override
    public void onResume()
    {
        super.onResume();

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
            Intent i = new Intent(getApplicationContext(), TransactionViewActivity.class);
            final Bundle b = new Bundle();
            b.putInt("type", getCurrentTabType());
            i.putExtras(b);
            startActivity(i);
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
}