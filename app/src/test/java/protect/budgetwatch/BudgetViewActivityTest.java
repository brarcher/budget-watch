package protect.budgetwatch;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 17)
public class BudgetViewActivityTest
{
    @Before
    public void setUp()
    {
        // Output logs emitted during tests so they may be accessed
        ShadowLog.stream = System.out;
    }

    enum ViewMode
    {
        ADD_CARD,
        VIEW_CARD,
        UPDATE_CARD,
        ;
    }

    @Test
    public void clickBackFinishes()
    {
        final Activity activity = Robolectric.setupActivity(BudgetViewActivity.class);

        shadowOf(activity).clickMenuItem(android.R.id.home);
        assertTrue(shadowOf(activity).isFinishing());
    }

    private ActivityController setupActivity(final String budget, int value,
                                             boolean launchAsView, boolean launchAsUpdate)
    {
        Intent intent = new Intent();
        final Bundle bundle = new Bundle();

        if(budget != null)
        {
            bundle.putString("id", budget);
        }

        if(launchAsView)
        {
            bundle.putBoolean("view", true);
        }

        if(launchAsUpdate)
        {
            bundle.putBoolean("update", true);
        }

        intent.putExtras(bundle);

        ActivityController activityController = Robolectric.buildActivity(BudgetViewActivity.class, intent).create();

        Activity activity = (Activity)activityController.get();
        DBHelper db = new DBHelper(activity);

        if(budget != null)
        {
            boolean result = db.insertBudget(budget, value);
            assertTrue(result);
        }
        db.close();

        activityController.start();
        activityController.visible();
        activityController.resume();

        return activityController;
    }

    private void checkAllFields(final Activity activity, ViewMode mode,
                                final String budget, final String value)
    {
        int viewVisibility = (mode == ViewMode.VIEW_CARD) ? View.VISIBLE : View.GONE;
        int editVisibility = (mode != ViewMode.VIEW_CARD) ? View.VISIBLE : View.GONE;

        checkFieldProperties(activity, R.id.budgetNameEdit, editVisibility, mode == ViewMode.VIEW_CARD ? "" : budget);
        checkFieldProperties(activity, R.id.budgetNameView, viewVisibility, mode == ViewMode.VIEW_CARD ? budget : "");
        checkFieldProperties(activity, R.id.valueEdit, editVisibility, mode == ViewMode.VIEW_CARD ? "" : value);
        checkFieldProperties(activity, R.id.valueView, viewVisibility, mode == ViewMode.VIEW_CARD ? value : "");
    }

    private void checkFieldProperties(final Activity activity, final int id, final int visibility,
                                      final String contents)
    {
        final View view = activity.findViewById(id);
        assertNotNull(view);
        assertEquals(visibility, view.getVisibility());
        if(contents != null)
        {
            TextView textView = (TextView)view;
            assertEquals(contents, textView.getText().toString());
        }
    }

    @Test
    public void startAsAddCheckFieldsAvailable()
    {
        ActivityController activityController = setupActivity(null, 0, false, false);

        Activity activity = (Activity)activityController.get();

        checkAllFields(activity, ViewMode.ADD_CARD, "", "");
    }

    @Test
    public void startAsAddCannotCreateBudget()
    {
        ActivityController activityController = setupActivity(null, 0, false, false);

        Activity activity = (Activity)activityController.get();
        DBHelper db = new DBHelper(activity);
        assertEquals(0, db.getBudgetCount());

        for(String[] test : Arrays.asList(
                new String[]{"", ""},
                new String[]{"", "100"},
                new String[]{"budget", ""},
                new String[]{"budget", "NotANumber"}
        ))
        {
            String budget = test[0];
            String value = test[1];

            final EditText budgetNameView = (EditText) activity.findViewById(R.id.budgetNameEdit);
            budgetNameView.setText(budget);

            final EditText valueField = (EditText) activity.findViewById(R.id.valueEdit);
            valueField.setText(value);

            // Perform the actual test, no transaction should be created
            shadowOf(activity).clickMenuItem(R.id.action_save);
            assertEquals(0, db.getBudgetCount());
        }

        db.close();
    }

    /**
     * Save a budget and check that the database contains the
     * expected value
     */
    private void saveBudgetWithArguments(final Activity activity,
                                          final String budgetName, final int value,
                                          boolean creatingNewBudget)
    {
        DBHelper db = new DBHelper(activity);
        if(creatingNewBudget)
        {
            assertEquals(0, db.getBudgetCount());
        }
        else
        {
            assertEquals(1, db.getBudgetCount());
        }
        db.close();

        final EditText budgetField = (EditText) activity.findViewById(R.id.budgetNameEdit);
        final EditText valueField = (EditText) activity.findViewById(R.id.valueEdit);

        budgetField.setText(budgetName);
        valueField.setText(Integer.toString(value));

        assertEquals(false, activity.isFinishing());
        shadowOf(activity).clickMenuItem(R.id.action_save);
        assertEquals(true, activity.isFinishing());

        assertEquals(1, db.getBudgetCount());

        List<Budget> budgets = db.getBudgets(0, 0);
        assertEquals(1, budgets.size());
        Budget budget = budgets.get(0);

        assertEquals(budgetName, budget.name);
        assertEquals(value, budget.max);
        assertEquals(0, budget.current);
    }

    @Test
    public void startAsAddCreateBudget()
    {
        ActivityController activityController = setupActivity(null, 0, false, false);

        Activity activity = (Activity)activityController.get();

        saveBudgetWithArguments(activity, "budgetName", 100, true);
    }

    @Test
    public void startAsEditCheckDisplay() throws IOException
    {
        ActivityController activityController = setupActivity("budgetName", 100, false, true);

        Activity activity = (Activity)activityController.get();

        checkAllFields(activity, ViewMode.UPDATE_CARD, "budgetName", "100");
    }

    @Test
    public void startAsEditUpdateValue() throws IOException
    {
        ActivityController activityController = setupActivity("budgetName", 100, false, true);
        Activity activity = (Activity)activityController.get();

        checkAllFields(activity, ViewMode.UPDATE_CARD, "budgetName", "100");

        saveBudgetWithArguments(activity, "budgetName", 1234, false);
    }

    @Test
    public void startAsViewCheckDisplay() throws IOException
    {
        ActivityController activityController = setupActivity("budgetName", 100, true, false);

        Activity activity = (Activity)activityController.get();

        checkAllFields(activity, ViewMode.VIEW_CARD, "budgetName", "100");
    }

    @Test
    public void startAsAddCheckActionBar() throws Exception
    {
        ActivityController activityController = setupActivity(null, 0, false, false);
        Activity activity = (Activity)activityController.get();

        final Menu menu = shadowOf(activity).getOptionsMenu();
        assertTrue(menu != null);

        assertEquals(menu.size(), 1);

        MenuItem item = menu.findItem(R.id.action_save);
        assertNotNull(item);
        assertEquals("Save", item.getTitle().toString());
    }

    @Test
    public void startAsUpdateCheckActionBar() throws Exception
    {
        ActivityController activityController = setupActivity("budgetName", 100, false, true);
        Activity activity = (Activity)activityController.get();

        final Menu menu = shadowOf(activity).getOptionsMenu();
        assertTrue(menu != null);

        assertEquals(menu.size(), 2);

        MenuItem item = menu.findItem(R.id.action_delete);
        assertNotNull(item);
        assertEquals("Delete", item.getTitle().toString());

        item = menu.findItem(R.id.action_save);
        assertNotNull(item);
        assertEquals("Save", item.getTitle().toString());
    }

    @Test
    public void clickDeleteRemovesBudget()
    {
        ActivityController activityController = setupActivity("budgetName", 100, false, true);
        Activity activity = (Activity)activityController.get();
        DBHelper db = new DBHelper(activity);

        assertEquals(1, db.getBudgetCount());
        shadowOf(activity).clickMenuItem(R.id.action_delete);

        // TODO: Finish this test once robolectric has shadows of the android.support AlertDialog class.
        // https://github.com/robolectric/robolectric/issues/1944

        /*
        // A dialog should be displayed now
        AlertDialog alert = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull(alert);
        ShadowAlertDialog sAlert = shadowOf(alert);
        assertEquals(sAlert.getTitle().toString(), activity.getString(R.string.deleteTransactionTitle));
        assertEquals(sAlert.getMessage().toString(), activity.getString(R.string.deleteTransactionConfirmation));

        alert.clickOnText(R.string.confirm);
        assertEquals(0, db.getBudgetCount());
        */

        db.close();
    }

    @Test
    public void startAsViewCheckActionBar() throws Exception
    {
        ActivityController activityController = setupActivity("budget", 100, true, false);
        Activity activity = (Activity)activityController.get();

        final Menu menu = shadowOf(activity).getOptionsMenu();
        assertTrue(menu != null);

        assertEquals(menu.size(), 1);

        MenuItem item = menu.findItem(R.id.action_edit);
        assertNotNull(item);
        assertEquals("Edit", item.getTitle().toString());
    }

    @Test
    public void clickEditLaunchesTransactionViewActivity()
    {
        ActivityController activityController = setupActivity("budgetName", 100, true, false);
        Activity activity = (Activity)activityController.get();

        shadowOf(activity).clickMenuItem(R.id.action_edit);

        Intent intent = shadowOf(activity).peekNextStartedActivityForResult().intent;

        assertEquals(new ComponentName(activity, BudgetViewActivity.class), intent.getComponent());
        Bundle bundle = intent.getExtras();
        assertNotNull(bundle);
        assertEquals("budgetName", bundle.getString("id", ""));
        assertEquals(true, bundle.getBoolean("update", false));
    }
}
