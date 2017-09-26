package protect.budgetwatch;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowListView;
import org.robolectric.shadows.ShadowLog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 17)
public class BudgetActivityTest
{
    @Before
    public void setUp()
    {
        // Output logs emitted during tests so they may be accessed
        ShadowLog.stream = System.out;
    }

    @Test
    public void initiallyNoBudgets() throws Exception
    {
        ActivityController activityController = Robolectric.buildActivity(BudgetActivity.class).create();

        Activity activity = (Activity)activityController.get();
        activityController.start();
        activityController.resume();

        TextView helpText = (TextView)activity.findViewById(R.id.helpText);
        assertEquals(View.VISIBLE, helpText.getVisibility());

        ListView list = (ListView)activity.findViewById(R.id.list);
        assertEquals(View.GONE, list.getVisibility());
    }

    @Test
    public void onCreateShouldInflateMenu() throws Exception
    {
        final Activity activity = Robolectric.setupActivity(BudgetActivity.class);

        final Menu menu = shadowOf(activity).getOptionsMenu();
        assertTrue(menu != null);

        // The settings and add button should be present
        assertEquals(menu.size(), 2);

        assertEquals("Add", menu.findItem(R.id.action_add).getTitle().toString());
        assertEquals("Select Dates", menu.findItem(R.id.action_calendar).getTitle().toString());
    }

    @Test
    public void clickAddLaunchesBudgetViewActivity()
    {
        final Activity activity = Robolectric.setupActivity(BudgetActivity.class);

        shadowOf(activity).clickMenuItem(R.id.action_add);

        Intent intent = shadowOf(activity).peekNextStartedActivityForResult().intent;

        assertEquals(new ComponentName(activity, BudgetViewActivity.class), intent.getComponent());
        assertNull(intent.getExtras());
    }

    @Test
    public void clickBackFinishes()
    {
        final Activity activity = Robolectric.setupActivity(BudgetActivity.class);

        shadowOf(activity).clickMenuItem(android.R.id.home);
        assertTrue(shadowOf(activity).isFinishing());
    }

    @Test
    public void testClickAdd()
    {
        ActivityController activityController = Robolectric.buildActivity(BudgetActivity.class).create();
        Activity activity = (Activity)activityController.get();

        activityController.start();
        activityController.resume();
        activityController.visible();

        shadowOf(activity).clickMenuItem(R.id.action_add);

        ShadowActivity shadowActivity = shadowOf(activity);
        Intent startedIntent = shadowActivity.getNextStartedActivity();

        ComponentName name = startedIntent.getComponent();
        assertNotNull(name);
        assertEquals("protect.budgetwatch/.BudgetViewActivity", name.flattenToShortString());
        Bundle bundle = startedIntent.getExtras();
        assertNull(bundle);
    }

    @Test
    public void addOneBudget()
    {
        ActivityController activityController = Robolectric.buildActivity(BudgetActivity.class).create();

        Activity mainActivity = (Activity)activityController.get();
        activityController.start();
        activityController.resume();

        TextView helpText = (TextView)mainActivity.findViewById(R.id.helpText);
        ListView list = (ListView)mainActivity.findViewById(R.id.list);

        assertEquals(0, list.getCount());

        assertEquals(View.VISIBLE, helpText.getVisibility());
        assertEquals(View.GONE, list.getVisibility());

        DBHelper db = new DBHelper(mainActivity);
        db.insertBudget("name", 100);
        db.close();

        activityController.pause();
        activityController.resume();

        assertEquals(View.GONE, helpText.getVisibility());
        assertEquals(View.VISIBLE, list.getVisibility());

        assertEquals(1, list.getAdapter().getCount());
        Budget budget = (Budget)list.getAdapter().getItem(0);
        assertNotNull(budget);
        assertEquals("name", budget.name);
        assertEquals(100, budget.max);
        assertEquals(0, budget.current);
    }

    private void checkTotalItem(final Activity activity, final int current, final int max)
    {
        final TextView budgetName = (TextView)activity.findViewById(R.id.budgetName);
        final TextView budgetValue = (TextView)activity.findViewById(R.id.budgetValue);
        final ProgressBar budgetBar = (ProgressBar)activity.findViewById(R.id.budgetBar);

        final String totalBudgetTitle = activity.getResources().getString(R.string.totalBudgetTitle);
        final String fractionFormat = activity.getResources().getString(R.string.fraction);

        String fraction = String.format(fractionFormat, current, max);
        assertEquals(budgetName.getText().toString(), totalBudgetTitle);
        assertEquals(budgetValue.getText().toString(), fraction);
        assertEquals(budgetBar.getProgress(), current);
        assertEquals(budgetBar.getMax(), max);
    }

    @Test
    public void addBudgetTotal()
    {
        ActivityController activityController = Robolectric.buildActivity(BudgetActivity.class).create();

        Activity mainActivity = (Activity)activityController.get();
        activityController.start();
        activityController.resume();

        final long nowMs = System.currentTimeMillis();

        int current = 0;
        int max = 0;

        checkTotalItem(mainActivity, current, max);

        DBHelper db = new DBHelper(mainActivity);

        final int budgetValue = 1234;
        final int expenseValue = 123;
        final int revenueValue = 12;

        for(int index = 0; index < 10; index++)
        {
            final String budgetName = "budget" + index;
            db.insertBudget("budget" + index, budgetValue);
            max += budgetValue;

            db.insertTransaction(DBHelper.TransactionDbIds.EXPENSE, "description", "account", budgetName, expenseValue, "note", nowMs, "receipt");
            db.insertTransaction(DBHelper.TransactionDbIds.REVENUE, "description", "account", budgetName, revenueValue, "note", nowMs, "receipt");

            current = current + expenseValue - revenueValue;
        }

        activityController.pause();
        activityController.resume();

        checkTotalItem(mainActivity, current, max);

        // Add a few blank budget transactions

        for(int index = 0; index < 10; index++)
        {
            db.insertTransaction(DBHelper.TransactionDbIds.EXPENSE, "description", "account", "", expenseValue, "note", nowMs, "receipt");
            db.insertTransaction(DBHelper.TransactionDbIds.REVENUE, "description", "account", "", revenueValue, "note", nowMs, "receipt");

            current = current + expenseValue - revenueValue;
        }

        activityController.pause();
        activityController.resume();

        checkTotalItem(mainActivity, current, max);

        db.close();
    }

    @Test
    public void clickOnBudget()
    {
        ActivityController activityController = Robolectric.buildActivity(BudgetActivity.class).create();
        Activity activity = (Activity)activityController.get();

        DBHelper db = new DBHelper(activity);
        db.insertBudget("name", 100);
        db.close();

        activityController.start();
        activityController.resume();

        ListView list = (ListView)activity.findViewById(R.id.list);

        ShadowListView shadowList = shadowOf(list);
        shadowList.populateItems();
        shadowList.performItemClick(0);

        ShadowActivity shadowActivity = shadowOf(activity);
        Intent startedIntent = shadowActivity.getNextStartedActivity();

        ComponentName name = startedIntent.getComponent();
        assertEquals("protect.budgetwatch/.TransactionActivity", name.flattenToShortString());
        Bundle bundle = startedIntent.getExtras();
        String budget = bundle.getString("budget");
        assertEquals("name", budget);
    }
}
