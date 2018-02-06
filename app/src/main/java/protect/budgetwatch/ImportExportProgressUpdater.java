package protect.budgetwatch;

import android.app.Activity;
import android.app.ProgressDialog;

/**
 * An interface for communicating the progress
 * of exporting data to a file.
 */
class ImportExportProgressUpdater
{
    private static final long UPDATE_INTERVAL = 250;

    private final Activity activity;
    private final ProgressDialog dialog;
    private final String baseMessage;

    private Integer totalEntries;
    private int entriesMoved = 0;
    private long lastUpdateTimeMs = 0;

    ImportExportProgressUpdater(Activity activity, ProgressDialog dialog, String baseMessage)
    {
        this.activity = activity;
        this.dialog = dialog;
        this.baseMessage = baseMessage;
        this.totalEntries = null;
    }

    public void setTotal(int totalEntries)
    {
        this.totalEntries = totalEntries;
    }

    /**
     * When a single record is exported or imported, either a budget or a
     * transaction, this should be invoked to update the UI.
     */
    public void update()
    {
        entriesMoved += 1;

        // So we do not spend all of our time updating the message,
        // only post an update periodically.
        long currentTime = System.currentTimeMillis();
        if( (currentTime - lastUpdateTimeMs) >= UPDATE_INTERVAL ||
                (totalEntries != null && entriesMoved == totalEntries) )
        {
            lastUpdateTimeMs = currentTime;

            activity.runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    String formatted;

                    if(totalEntries != null)
                    {
                        formatted = String.format(baseMessage, entriesMoved, totalEntries);
                    }
                    else
                    {
                        formatted = String.format(baseMessage, entriesMoved);
                    }

                    dialog.setMessage(formatted);
                }
            });
        }
    }
}
