package protect.budgetwatch;

import android.app.Activity;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.util.ActivityController;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 17)
public class TransactionActivityTest
{
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

        switch(adapterItemIndex)
        {
            case 0:
                assertTrue(fragment instanceof TransactionExpenseFragment);
                break;
            case 1:
                assertTrue(fragment instanceof TransactionRevenueFragment);
                break;
            default:
                fail("Unexpected adapter item index: " + adapterItemIndex);
        }
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
        assertEquals(menu.size(), 2);

        assertEquals("Add", menu.findItem(R.id.action_add).getTitle().toString());
        assertEquals("Purge Old Receipts", menu.findItem(R.id.action_purge_receipts).getTitle().toString());
    }
}
