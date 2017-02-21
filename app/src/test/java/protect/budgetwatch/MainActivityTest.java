package protect.budgetwatch;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ListView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowListView;
import org.robolectric.shadows.ShadowLog;
import org.robolectric.util.ActivityController;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21)
public class MainActivityTest
{
    @Before
    public void setUp()
    {
        // Output logs emitted during tests so they may be accessed
        ShadowLog.stream = System.out;
    }

    private void testNextStartedActivity(Activity activity, String nextActivity)
    {
        ShadowActivity shadowActivity = shadowOf(activity);
        Intent startedIntent = shadowActivity.getNextStartedActivity();

        ComponentName name = startedIntent.getComponent();
        assertNotNull(name);
        assertEquals(nextActivity, name.flattenToShortString());
        Bundle bundle = startedIntent.getExtras();
        assertNull(bundle);
    }

    private void testItemClickLaunchesActivity(int index, String expectedActivity)
    {
        ActivityController activityController = Robolectric.buildActivity(MainActivity.class).create();
        Activity activity = (Activity)activityController.get();

        activityController.start();
        activityController.resume();

        ListView list = (ListView)activity.findViewById(R.id.list);

        ShadowListView shadowList = shadowOf(list);
        shadowList.populateItems();

        // First item should be the Budgets, second should be Transactions
        shadowList.performItemClick(index);

        testNextStartedActivity(activity, expectedActivity);
    }

    @Test
    public void clickOnBudgets()
    {
        testItemClickLaunchesActivity(0, "protect.budgetwatch/.BudgetActivity");
    }

    @Test
    public void clickOnTransactions()
    {
        testItemClickLaunchesActivity(1, "protect.budgetwatch/.TransactionActivity");
    }

    @Test
    public void testClickSettings()
    {
        ActivityController activityController = Robolectric.buildActivity(MainActivity.class).create();
        Activity activity = (Activity)activityController.get();

        activityController.start();
        activityController.resume();
        activityController.visible();

        shadowOf(activity).clickMenuItem(R.id.action_settings);
        testNextStartedActivity(activity, "protect.budgetwatch/.SettingsActivity");
    }

    @Test
    public void testClickImportExport()
    {
        ActivityController activityController = Robolectric.buildActivity(MainActivity.class).create();
        Activity activity = (Activity)activityController.get();

        activityController.start();
        activityController.resume();
        activityController.visible();

        shadowOf(activity).clickMenuItem(R.id.action_import_export);
        testNextStartedActivity(activity, "protect.budgetwatch/.ImportExportActivity");
    }
}

