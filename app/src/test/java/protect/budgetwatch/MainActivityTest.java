package protect.budgetwatch;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ListView;

import org.apache.tools.ant.Main;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowListView;
import org.robolectric.shadows.ShadowLog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 25)
public class MainActivityTest
{
    private SharedPreferences prefs;

    @Before
    public void setUp()
    {
        // Output logs emitted during tests so they may be accessed
        ShadowLog.stream = System.out;

        prefs = RuntimeEnvironment.application.getSharedPreferences("protect.budgetwatch", Context.MODE_PRIVATE);
        // Assume that this is not the first launch
        prefs.edit().putBoolean("firstrun", false).commit();
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

    @Test
    public void testFirstRunStartsIntro()
    {
        prefs.edit().remove("firstrun").commit();

        ActivityController controller = Robolectric.buildActivity(MainActivity.class).create();
        Activity activity = (Activity)controller.get();

        assertTrue(activity.isFinishing() == false);

        Intent next = shadowOf(activity).getNextStartedActivity();

        ComponentName componentName = next.getComponent();
        String name = componentName.flattenToShortString();
        assertEquals("protect.budgetwatch/.intro.IntroActivity", name);

        Bundle extras = next.getExtras();
        assertNull(extras);

        assertEquals(false, prefs.getBoolean("firstrun", true));
    }
}

