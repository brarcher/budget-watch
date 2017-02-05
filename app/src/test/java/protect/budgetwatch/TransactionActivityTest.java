package protect.budgetwatch;

import android.app.Activity;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;
import org.robolectric.util.ActivityController;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 17)
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
    public void clickBackFinishes()
    {
        final Activity activity = Robolectric.setupActivity(TransactionActivity.class);

        shadowOf(activity).clickMenuItem(android.R.id.home);
        assertTrue(shadowOf(activity).isFinishing());
    }
}
