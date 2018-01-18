package protect.budgetwatch;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.LinkedList;
import java.util.List;

public class ShortcutConfigure extends AppCompatActivity
{
    static final String TAG = "BudgetWatch";

    static class ShortcutOption
    {
        String name;
        Intent intent;
    }

    static class ShortcutAdapter extends ArrayAdapter<ShortcutOption>
    {
        ShortcutAdapter(Context context, List<ShortcutOption> items)
        {
            super(context, 0, items);
        }

        static class ViewHolder
        {
            TextView name;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            // Get the data item for this position
            ShortcutOption item = getItem(position);

            ShortcutAdapter.ViewHolder holder;

            // Check if an existing view is being reused, otherwise inflate the view

            if (convertView == null)
            {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.shortcut_option_layout,
                        parent, false);

                holder = new ShortcutAdapter.ViewHolder();
                holder.name = (TextView) convertView.findViewById(R.id.name);
                convertView.setTag(holder);
            }
            else
            {
                holder = (ShortcutAdapter.ViewHolder)convertView.getTag();
            }

            holder.name.setText(item.name);

            return convertView;
        }
    }

    private List<ShortcutOption> getPossibleShortcuts()
    {
        LinkedList<ShortcutOption> shortcuts = new LinkedList<>();

        for(int transactionType : new int[]{DBHelper.TransactionDbIds.EXPENSE, DBHelper.TransactionDbIds.REVENUE})
        {
            Intent shortcutIntent = new Intent(this, TransactionViewActivity.class);
            shortcutIntent.setAction(Intent.ACTION_MAIN);
            // Prevent instances of the view activity from piling up; if one exists let this
            // one replace it.
            shortcutIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            Bundle bundle = new Bundle();
            bundle.putInt("type", transactionType);
            shortcutIntent.putExtras(bundle);

            String title;
            if(transactionType == DBHelper.TransactionDbIds.EXPENSE)
            {
                title = getResources().getString(R.string.addExpenseTransactionShortcutTitle);
            }
            else
            {
                title = getResources().getString(R.string.addRevenueTransactionShortcutTitle);
            }

            ShortcutOption shortcutOption = new ShortcutOption();
            shortcutOption.name = title;
            shortcutOption.intent = shortcutIntent;

            shortcuts.add(shortcutOption);
        }

        return shortcuts;
    }

    @Override
    public void onCreate(Bundle bundle)
    {
        super.onCreate(bundle);

        // Set the result to CANCELED.  This will cause nothing to happen if the
        // aback button is pressed.
        setResult(RESULT_CANCELED);

        setContentView(R.layout.main_activity);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setVisibility(View.GONE);

        final ListView shortcutList = (ListView) findViewById(R.id.list);
        shortcutList.setVisibility(View.VISIBLE);

        final ShortcutAdapter adapter = new ShortcutAdapter(this, getPossibleShortcuts());
        shortcutList.setAdapter(adapter);

        shortcutList.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                ShortcutOption shortcut = (ShortcutOption)parent.getItemAtPosition(position);
                if(shortcut == null)
                {
                    Log.w(TAG, "Clicked shortcut at position " + position + " is null");
                    return;
                }

                Parcelable icon = Intent.ShortcutIconResource.fromContext(ShortcutConfigure.this, R.mipmap.ic_launcher);
                Intent intent = new Intent();
                intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcut.intent);
                intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, shortcut.name);
                intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, icon);
                setResult(RESULT_OK, intent);

                finish();
            }
        });
    }
}

