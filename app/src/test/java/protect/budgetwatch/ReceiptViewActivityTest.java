package protect.budgetwatch;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.webkit.WebView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;
import org.robolectric.shadows.ShadowWebView;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 25)
public class ReceiptViewActivityTest
{
    private ActivityController activityController;

    @Before
    public void setUp()
    {
        // Output logs emitted during tests so they may be accessed
        ShadowLog.stream = System.out;

        Intent intent = new Intent();
        final Bundle bundle = new Bundle();
        bundle.putString("receipt", "receipt");
        intent.putExtras(bundle);

        activityController = Robolectric.buildActivity(
                ReceiptViewActivity.class, intent).create();
    }

    @Test
    public void LoadReceipt()
    {
        activityController.start();
        activityController.resume();

        Activity activity = (Activity)activityController.get();
        WebView receiptView = (WebView)activity.findViewById(R.id.imageView);

        ShadowWebView.LoadDataWithBaseURL loadedData = shadowOf(receiptView).getLastLoadDataWithBaseURL();
        assertEquals("", loadedData.baseUrl);
        assertEquals("text/html", loadedData.mimeType);
        assertEquals("utf-8", loadedData.encoding);
        assertNull(loadedData.historyUrl);
        assertTrue(loadedData.data.contains("src=\"file://receipt\""));
    }
}
