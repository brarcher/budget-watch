package protect.budgetwatch;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.collect.ImmutableMap;

import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import protect.budgetwatch.intro.IntroActivity;

public class MainActivity extends AppCompatActivity
{
    private final static String TAG = "BudgetWatch";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        List<MainMenuItem> menuItems = new LinkedList<>();
        menuItems.add(new MainMenuItem(R.drawable.purse, R.string.budgetsTitle,
                R.string.budgetDescription));
        menuItems.add(new MainMenuItem(R.drawable.transaction, R.string.transactionsTitle,
                R.string.transactionsDescription));

        final ListView buttonList = (ListView) findViewById(R.id.list);
        final MenuAdapter buttonListAdapter = new MenuAdapter(this, menuItems);
        buttonList.setAdapter(buttonListAdapter);
        buttonList.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                MainMenuItem item = (MainMenuItem)parent.getItemAtPosition(position);
                if(item == null)
                {
                    Log.w(TAG, "Clicked menu item at position " + position + " is null");
                    return;
                }

                Class goalClass = null;

                switch(item.menuTextId)
                {
                    case R.string.budgetsTitle:
                        goalClass = BudgetActivity.class;
                        break;
                    case R.string.transactionsTitle:
                        goalClass = TransactionActivity.class;
                        break;
                    default:
                        Log.w(TAG, "Unexpected menu text id: " + item.menuTextId);
                        break;
                }

                if(goalClass != null)
                {
                    Intent i = new Intent(getApplicationContext(), goalClass);
                    startActivity(i);
                }
            }
        });

        SharedPreferences prefs = getSharedPreferences("protect.budgetwatch", MODE_PRIVATE);
        if (prefs.getBoolean("firstrun", true)) {
            startIntro();
            prefs.edit().putBoolean("firstrun", false).commit();
        }
    }

    static class MainMenuItem
    {
        public final int iconId;
        public final int menuTextId;
        public final int menuDescId;

        public MainMenuItem(int iconId, int menuTextId, int menuDescId)
        {
            this.iconId = iconId;
            this.menuTextId = menuTextId;
            this.menuDescId = menuDescId;
        }
    }

    static class MenuAdapter extends ArrayAdapter<MainMenuItem>
    {
        public MenuAdapter(Context context, List<MainMenuItem> items)
        {
            super(context, 0, items);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            // Get the data item for this position
            MainMenuItem item = getItem(position);

            // Check if an existing view is being reused, otherwise inflate the view

            if (convertView == null)
            {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.main_button,
                        parent, false);
            }

            TextView menuText = (TextView) convertView.findViewById(R.id.menu);
            TextView menuDescText = (TextView) convertView.findViewById(R.id.menudesc);
            ImageView icon = (ImageView) convertView.findViewById(R.id.image);

            menuText.setText(item.menuTextId);
            menuDescText.setText(item.menuDescId);
            icon.setImageResource(item.iconId);

            return convertView;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();

        if(id == R.id.action_import_export)
        {
            Intent i = new Intent(getApplicationContext(), ImportExportActivity.class);
            startActivity(i);
            return true;
        }

        if(id == R.id.action_settings)
        {
            Intent i = new Intent(getApplicationContext(), SettingsActivity.class);
            startActivity(i);
            return true;
        }

        if(id == R.id.action_intro)
        {
            startIntro();
            return true;
        }

        if(id == R.id.action_about)
        {
            displayAboutDialog();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void displayAboutDialog()
    {
        final Map<String, String> USED_LIBRARIES = ImmutableMap.of
        (
            "Commons CSV", "https://commons.apache.org/proper/commons-csv/",
            "Guava", "https://github.com/google/guava",
            "AppIntro", "https://github.com/apl-devs/AppIntro"
        );

        final Map<String, String> USED_ASSETS = ImmutableMap.of
        (
            "Piggy Bank by Icons8", "https://thenounproject.com/term/piggy-bank/61478/",
            "Purse by Dima Lagunov", "https://thenounproject.com/term/purse/26896/",
            "Ticket Bill by naim", "https://thenounproject.com/term/ticket-bill/634398/",
            "Purchase Order by Icons8", "https://icons8.com/web-app/for/all/purchase-order",
            "Save by Bernar Novalyi", "https://thenounproject.com/term/save/716011"
        );

        StringBuilder libs = new StringBuilder().append("<ul>");
        for (Map.Entry<String, String> entry : USED_LIBRARIES.entrySet())
        {
            libs.append("<li><a href=\"").append(entry.getValue()).append("\">").append(entry.getKey()).append("</a></li>");
        }
        libs.append("</ul>");

        StringBuilder resources = new StringBuilder().append("<ul>");
        for (Map.Entry<String, String> entry : USED_ASSETS.entrySet())
        {
            resources.append("<li><a href=\"").append(entry.getValue()).append("\">").append(entry.getKey()).append("</a></li>");
        }
        resources.append("</ul>");

        String appName = getString(R.string.app_name);
        int year = Calendar.getInstance().get(Calendar.YEAR);

        String version = "?";
        try
        {
            PackageManager manager = getPackageManager();
            if(manager != null)
            {
                PackageInfo pi = manager.getPackageInfo(getPackageName(), 0);
                version = pi.versionName;
            }
            else
            {
                Log.w(TAG, "Package name not found, PackageManager unavailable");
            }
        }
        catch (PackageManager.NameNotFoundException e)
        {
            Log.w(TAG, "Package name not found", e);
        }

        WebView wv = new WebView(this);
        String html =
            "<meta http-equiv=\"content-type\" content=\"text/html; charset=utf-8\" />" +
            "<img src=\"file:///android_res/mipmap/ic_launcher.png\" alt=\"" + appName + "\"/>" +
            "<h1>" +
            String.format(getString(R.string.about_title_fmt),
                    "<a href=\"" + getString(R.string.app_webpage_url)) + "\">" +
            appName +
            "</a>" +
            "</h1><p>" +
            appName +
            " " +
            String.format(getString(R.string.debug_version_fmt), version) +
            "</p><p>" +
            String.format(getString(R.string.app_revision_fmt),
                    "<a href=\"" + getString(R.string.app_revision_url) + "\">" +
                            getString(R.string.app_revision_url) +
                            "</a>") +
            "</p><hr/><p>" +
            String.format(getString(R.string.app_copyright_fmt), year) +
            "</p><hr/><p>" +
            getString(R.string.app_license) +
            "</p><hr/><p>" +
            String.format(getString(R.string.app_libraries), appName, libs.toString()) +
            "</p><hr/><p>" +
            String.format(getString(R.string.app_resources), appName, resources.toString());


        wv.loadDataWithBaseURL("file:///android_res/drawable/", html, "text/html", "utf-8", null);
        new AlertDialog.Builder(this)
            .setView(wv)
            .setCancelable(true)
            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener()
            {
                public void onClick(DialogInterface dialog, int which)
                {
                    dialog.dismiss();
                }
            })
            .show();
    }

    private void startIntro()
    {
        Intent intent = new Intent(this, IntroActivity.class);
        startActivity(intent);
    }
}
