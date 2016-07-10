package protect.budgetwatch;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.RemoteViews;

public class TransactionExpenseWidget extends AppWidgetProvider
{
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds)
    {
        ComponentName widget = new ComponentName(context, TransactionExpenseWidget.class);
        RemoteViews remoteView = new RemoteViews(context.getPackageName(), R.layout.widget_layout);

        Intent intent = new Intent(context, TransactionViewActivity.class);
        Bundle extras = new Bundle();
        extras.putInt("type", DBHelper.TransactionDbIds.EXPENSE);
        intent.putExtras(extras);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
        remoteView.setOnClickPendingIntent(R.id.addTransaction, pendingIntent);

        appWidgetManager.updateAppWidget(widget, remoteView);
    }
}
