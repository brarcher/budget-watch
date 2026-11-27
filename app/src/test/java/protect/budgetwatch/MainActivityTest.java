package protect.budgetwatch;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ListView;

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
@Config(sdk = 25)
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
    @Config(sdk = 34)
    public void testPreApi35ThemeKeepsDarkNavigationBarIconsOff()
    {
        ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class)
                .create().start().resume().visible();
        MainActivity activity = controller.get();

        android.content.res.TypedArray attrs = activity.getTheme().obtainStyledAttributes(
                new int[] { android.R.attr.windowLightNavigationBar, android.R.attr.statusBarColor });
        try
        {
            assertEquals(false, attrs.getBoolean(0, false));
            assertEquals(androidx.core.content.ContextCompat.getColor(activity, R.color.colorPrimaryDark),
                    attrs.getColor(1, 0));
        }
        finally
        {
            attrs.recycle();
        }
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

    @Test
    public void testAboutDialogDestroysWebViewOnDismiss()
    {
        ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class)
                .create().start().resume().visible();
        MainActivity activity = controller.get();

        shadowOf(activity).clickMenuItem(R.id.action_about);

        android.app.Dialog dialog = org.robolectric.shadows.ShadowDialog.getLatestDialog();
        assertNotNull(dialog);
        assertTrue(dialog.isShowing());

        android.webkit.WebView webView = findWebView(dialog.getWindow().getDecorView());
        assertNotNull(webView);
        assertEquals(false, shadowOf(webView).wasDestroyCalled());

        dialog.dismiss();
        shadowOf(android.os.Looper.getMainLooper()).idle();

        assertTrue(shadowOf(webView).wasDestroyCalled());
    }

    @Test
    public void testAboutDialogDestroysWebViewOnActivityDestroy()
    {
        ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class)
                .create().start().resume().visible();
        MainActivity activity = controller.get();

        shadowOf(activity).clickMenuItem(R.id.action_about);

        android.app.Dialog dialog = org.robolectric.shadows.ShadowDialog.getLatestDialog();
        assertNotNull(dialog);
        assertTrue(dialog.isShowing());

        android.webkit.WebView webView = findWebView(dialog.getWindow().getDecorView());
        assertNotNull(webView);
        assertEquals(false, shadowOf(webView).wasDestroyCalled());

        controller.pause().stop().destroy();
        shadowOf(android.os.Looper.getMainLooper()).idle();

        assertEquals(false, dialog.isShowing());
        assertTrue(shadowOf(webView).wasDestroyCalled());
    }

    @Test
    @Config(sdk = 26)
    public void testAboutDialogHandlesRenderProcessGone()
    {
        ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class)
                .create().start().resume().visible();
        MainActivity activity = controller.get();

        shadowOf(activity).clickMenuItem(R.id.action_about);

        android.app.Dialog dialog = org.robolectric.shadows.ShadowDialog.getLatestDialog();
        assertNotNull(dialog);
        assertTrue(dialog.isShowing());

        android.webkit.WebView webView = findWebView(dialog.getWindow().getDecorView());
        assertNotNull(webView);
        android.webkit.WebViewClient client = shadowOf(webView).getWebViewClient();
        assertNotNull(client);

        boolean handled = client.onRenderProcessGone(webView, null);
        shadowOf(android.os.Looper.getMainLooper()).idle();

        assertTrue(handled);
        assertEquals(false, dialog.isShowing());
        assertTrue(shadowOf(webView).wasDestroyCalled());
    }

    private android.webkit.WebView findWebView(android.view.View root)
    {
        if (root instanceof android.webkit.WebView)
        {
            return (android.webkit.WebView) root;
        }
        if (root instanceof android.view.ViewGroup)
        {
            android.view.ViewGroup vg = (android.view.ViewGroup) root;
            for (int i = 0; i < vg.getChildCount(); i++)
            {
                android.webkit.WebView hit = findWebView(vg.getChildAt(i));
                if (hit != null)
                {
                    return hit;
                }
            }
        }
        return null;
    }
}

