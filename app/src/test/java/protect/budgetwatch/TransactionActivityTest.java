package protect.budgetwatch;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowLog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 25)
public class TransactionActivityTest
{
    @Before
    public void setUp()
    {
        // Output logs emitted during tests so they may be accessed
        ShadowLog.stream = System.out;
    }

    @Test
    public void bothTabsExists() throws Exception
    {
        ActivityController activityController = Robolectric.buildActivity(TransactionActivity.class).create();

        Activity activity = (Activity)activityController.get();
        activityController.start();
        activityController.resume();

        TabLayout tabLayout = (TabLayout) activity.findViewById(R.id.tabLayout);
        assertNotNull(tabLayout);
        assertEquals(2, tabLayout.getTabCount());

        TabLayout.Tab expenseTab = tabLayout.getTabAt(0);
        assertNotNull(expenseTab);
        String expenseTabTitle = activity.getResources().getString(R.string.expensesTitle);
        assertEquals(expenseTabTitle, expenseTab.getText().toString());

        TabLayout.Tab revenueTab = tabLayout.getTabAt(1);
        assertNotNull(revenueTab);
        String revenueTabTitle = activity.getResources().getString(R.string.revenuesTitle);
        assertEquals(revenueTabTitle, revenueTab.getText().toString());
    }


    private void checkFragmentStatus(final Activity activity, final int adapterItemIndex)
    {
        final ViewPager viewPager = (ViewPager) activity.findViewById(R.id.pager);
        assertNotNull(viewPager);
        final FragmentStatePagerAdapter adapter = (FragmentStatePagerAdapter)viewPager.getAdapter();
        assertNotNull(adapter);

        Fragment fragment = adapter.getItem(adapterItemIndex);
        assertNotNull(fragment);

        Bundle arguments = fragment.getArguments();
        assertNotNull(arguments);
        final int expectedTransactionType = (adapterItemIndex == 0) ?
                DBHelper.TransactionDbIds.EXPENSE : DBHelper.TransactionDbIds.REVENUE;
        assertEquals(expectedTransactionType, arguments.getInt("type"));
    }

    @Test
    public void correctFragmentsInTabs()
    {
        ActivityController activityController = Robolectric.buildActivity(TransactionActivity.class).create();

        Activity activity = (Activity)activityController.get();
        activityController.start();
        activityController.resume();

        final ViewPager viewPager = (ViewPager) activity.findViewById(R.id.pager);
        assertNotNull(viewPager);
        final FragmentStatePagerAdapter adapter = (FragmentStatePagerAdapter)viewPager.getAdapter();
        assertNotNull(adapter);
        assertEquals(2, adapter.getCount());

        checkFragmentStatus(activity, 0);
        checkFragmentStatus(activity, 1);
    }

    @Test
    @Ignore
    public void onCreateShouldInflateMenu() throws Exception
    {
        final Activity activity = Robolectric.setupActivity(TransactionActivity.class);

        final Menu menu = shadowOf(activity).getOptionsMenu();
        assertTrue(menu != null);

        // The purge and add button should be present
        assertEquals(menu.size(), 3);

        assertEquals("Add", menu.findItem(R.id.action_add).getTitle().toString());
        assertEquals("Search", menu.findItem(R.id.action_search).getTitle().toString());
        assertEquals("Purge Old Receipts", menu.findItem(R.id.action_purge_receipts).getTitle().toString());
    }

    @Test
    @Ignore
    public void clickBackFinishes()
    {
        final Activity activity = Robolectric.setupActivity(TransactionActivity.class);

        shadowOf(activity).clickMenuItem(android.R.id.home);
        assertTrue(shadowOf(activity).isFinishing());
    }

    private void checkClickAddWhileOnTap(Integer tab, int expectedType)
    {
        ActivityController activityController = Robolectric.buildActivity(TransactionActivity.class).create();
        Activity activity = (Activity)activityController.get();

        activityController.start();
        activityController.resume();
        activityController.visible();

        if(tab != null)
        {
            final ViewPager viewPager = (ViewPager) activity.findViewById(R.id.pager);
            viewPager.setCurrentItem(tab);
        }

        shadowOf(activity).clickMenuItem(R.id.action_add);

        ShadowActivity shadowActivity = shadowOf(activity);
        Intent startedIntent = shadowActivity.getNextStartedActivity();

        ComponentName name = startedIntent.getComponent();
        assertNotNull(name);
        assertEquals("protect.budgetwatch/.TransactionViewActivity", name.flattenToShortString());
        Bundle bundle = startedIntent.getExtras();
        assertNotNull(bundle);

        // Fields which should not be present
        assertEquals(-1, bundle.getInt("id", -1));
        assertEquals(false, bundle.getBoolean("update", false));
        assertEquals(false, bundle.getBoolean("view", false));

        // Check the field which is expected
        assertEquals(expectedType, bundle.getInt("type", -1));
    }

    @Test
    @Ignore
    public void testClickAddDefaultTab()
    {
        checkClickAddWhileOnTap(null, DBHelper.TransactionDbIds.EXPENSE);
    }

    @Test
    @Ignore
    public void testClickAddExpense()
    {
        checkClickAddWhileOnTap(0, DBHelper.TransactionDbIds.EXPENSE);
    }

    @Test
    @Ignore
    public void testClickAddRevenue()
    {
        checkClickAddWhileOnTap(1, DBHelper.TransactionDbIds.REVENUE);
    }
}
