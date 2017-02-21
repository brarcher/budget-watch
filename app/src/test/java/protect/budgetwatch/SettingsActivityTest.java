package protect.budgetwatch;

import android.app.Activity;
import android.preference.ListPreference;
import android.support.v7.widget.AppCompatTextView;
import android.widget.LinearLayout;
import android.widget.ListView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;
import org.robolectric.util.ActivityController;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import edu.emory.mathcs.backport.java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 17)
public class SettingsActivityTest
{
    @Before
    public void setUp()
    {
        // Output logs emitted during tests so they may be accessed
        ShadowLog.stream = System.out;
    }

    @Test
    public void clickBackFinishes()
    {
        final Activity activity = Robolectric.setupActivity(SettingsActivity.class);

        shadowOf(activity).clickMenuItem(android.R.id.home);
        assertTrue(shadowOf(activity).isFinishing());
    }

    @Test
    public void testAvailableSettings()
    {
        ActivityController activityController = Robolectric.buildActivity(SettingsActivity.class).create();
        Activity activity = (Activity)activityController.get();

        activityController.start();
        activityController.resume();
        activityController.visible();

        ListView list = (ListView)activity.findViewById(android.R.id.list);
        shadowOf(list).populateItems();

        List<String> settingTitles = new LinkedList<>
            (Arrays.asList(
                "Receipt Quality"
            ));

        assertEquals(settingTitles.size(), list.getCount());

        for(int index = 0; index < list.getCount(); index++)
        {
            ListPreference preference = (ListPreference)list.getItemAtPosition(0);
            String title = preference.getTitle().toString();

            assertTrue(settingTitles.remove(title));
        }

        assertTrue(settingTitles.isEmpty());
    }
}
