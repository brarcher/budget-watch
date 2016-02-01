package protect.budgetwatch;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.view.Menu;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.util.ActivityController;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 17)
public class BudgetActivityTest
{
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
        assertEquals(menu.size(), 1);

        assertEquals("Add", menu.findItem(R.id.action_add).getTitle().toString());
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
}
