package protect.budgetwatch;


import android.app.Application;
import android.os.Build;
import android.os.StrictMode;

public class BudgetWatchApplication extends Application
{
    @Override
    public void onCreate()
    {
        super.onCreate();
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2)
        {
            // In SDK 24+ if a file:// Uri leaves the activity then an
            // exception is thrown. Instead, content:// Uri's should be used.
            // This affects the capturing of receipt images from the camera.
            // This should be resolved by using a FileProvider, however in
            // doing so there was difficulty in getting Robolectric to
            // successfully test the result. Until this is figured out,
            // downgrading this issue to a log message.
            builder.detectAll().penaltyLog();
        }

        StrictMode.setVmPolicy(builder.build());
    }
}
