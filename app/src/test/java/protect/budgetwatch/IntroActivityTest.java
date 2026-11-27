package protect.budgetwatch;

import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager.widget.ViewPager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

import protect.budgetwatch.intro.IntroActivity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class IntroActivityTest
{
    @Before
    public void setUp()
    {
        ShadowLog.stream = System.out;
    }

    @Test
    public void createsAllSevenSlides()
    {
        ActivityController<IntroActivity> controller = Robolectric.buildActivity(IntroActivity.class)
                .create().start().resume().visible();
        IntroActivity activity = controller.get();

        ViewPager pager = activity.findViewById(com.github.appintro.R.id.view_pager);
        assertNotNull(pager);
        assertNotNull(pager.getAdapter());
        assertEquals(7, pager.getAdapter().getCount());
    }

    @Test
    public void skipButtonFinishesActivity()
    {
        ActivityController<IntroActivity> controller = Robolectric.buildActivity(IntroActivity.class)
                .create().start().resume().visible();
        IntroActivity activity = controller.get();

        assertFalse(activity.isFinishing());
        View skipButton = activity.findViewById(com.github.appintro.R.id.skip);
        assertNotNull(skipButton);
        skipButton.performClick();
        assertTrue(activity.isFinishing());
    }

    @Test
    public void doneButtonOnLastSlideFinishesActivity()
    {
        ActivityController<IntroActivity> controller = Robolectric.buildActivity(IntroActivity.class)
                .create().start().resume().visible();
        IntroActivity activity = controller.get();

        ViewPager pager = activity.findViewById(com.github.appintro.R.id.view_pager);
        assertNotNull(pager);
        pager.setCurrentItem(6, false);

        assertFalse(activity.isFinishing());
        View doneButton = activity.findViewById(com.github.appintro.R.id.done);
        assertNotNull(doneButton);
        doneButton.performClick();
        assertTrue(activity.isFinishing());
    }

    @Test
    public void appliesSystemBarInsetsToRootView()
    {
        ActivityController<IntroActivity> controller = Robolectric.buildActivity(IntroActivity.class)
                .create().start().resume().visible();
        IntroActivity activity = controller.get();

        View root = activity.findViewById(com.github.appintro.R.id.background);
        assertNotNull(root);

        WindowInsetsCompat insets = new WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.systemBars(), Insets.of(0, 64, 0, 128))
                .build();
        ViewCompat.dispatchApplyWindowInsets(root, insets);

        assertEquals(0, root.getPaddingLeft());
        assertEquals(64, root.getPaddingTop());
        assertEquals(0, root.getPaddingRight());
        assertEquals(128, root.getPaddingBottom());
    }
}
