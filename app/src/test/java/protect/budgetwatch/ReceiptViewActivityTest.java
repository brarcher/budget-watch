package protect.budgetwatch;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.webkit.WebView;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowWebView;
import org.robolectric.util.ActivityController;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 17)
public class ReceiptViewActivityTest
{
    @Test
    public void LoadReceipt()
    {
        Intent intent = new Intent();
        final Bundle bundle = new Bundle();
        bundle.putString("receipt", "receipt");
        intent.putExtras(bundle);

        ActivityController activityController =  Robolectric.buildActivity(
            ReceiptViewActivity.class).withIntent(intent).create();
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
