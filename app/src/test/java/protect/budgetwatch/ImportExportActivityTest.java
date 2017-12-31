package protect.budgetwatch;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.Spinner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowContentResolver;
import org.robolectric.shadows.ShadowLog;
import org.robolectric.shadows.ShadowPackageManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Set;

import static org.junit.Assert.assertNotNull;
import static org.robolectric.Shadows.shadowOf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 25)
public class ImportExportActivityTest
{
    private Activity activity;
    private DBHelper db;
    private File sdcardDir;

    @Before
    public void setUp()
    {
        // Output logs emitted during tests so they may be accessed
        ShadowLog.stream = System.out;

        activity = Robolectric.setupActivity(ImportExportActivity.class);
        db = new DBHelper(activity);
        sdcardDir = Environment.getExternalStorageDirectory();
    }

    private void registerIntentHandler(String handler)
    {
        // Add something that will 'handle' the given intent type
        ShadowPackageManager shadowPackageManager = shadowOf(RuntimeEnvironment.application.getPackageManager());

        ResolveInfo info = new ResolveInfo();
        info.isDefault = true;

        ApplicationInfo applicationInfo = new ApplicationInfo();
        applicationInfo.packageName = "does.not.matter";
        info.activityInfo = new ActivityInfo();
        info.activityInfo.applicationInfo = applicationInfo;
        info.activityInfo.name = "DoesNotMatter";
        info.activityInfo.exported = true;

        Intent intent = new Intent(handler);

        if(handler.equals(Intent.ACTION_GET_CONTENT))
        {
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
        }

        shadowPackageManager.addResolveInfoForIntent(intent, info);
    }

    private void checkVisibility(Activity activity, Integer state, Integer divider, Integer title,
                                 Integer message, Integer label, Integer spinner, Integer button)
    {
        View dividerView = activity.findViewById(divider);
        assertEquals(state.intValue(), dividerView.getVisibility());

        View titleView = activity.findViewById(title);
        assertEquals(state.intValue(), titleView.getVisibility());

        View messageView = activity.findViewById(message);
        assertEquals(state.intValue(), messageView.getVisibility());

        if(label != null)
        {
            View labelView = activity.findViewById(label);
            assertEquals(state.intValue(), labelView.getVisibility());
        }

        if(spinner != null)
        {
            View spinnerView = activity.findViewById(spinner);
            assertEquals(state.intValue(), spinnerView.getVisibility());
        }

        View buttonView = activity.findViewById(button);
        assertEquals(state.intValue(), buttonView.getVisibility());
    }

    @Test
    public void testImportFilesystemOption()
    {
        for(boolean isInstalled : new Boolean[]{false, true})
        {
            int visibility = isInstalled ? View.VISIBLE : View.GONE;

            if(isInstalled)
            {
                registerIntentHandler(Intent.ACTION_PICK);
            }

            Activity activity = Robolectric.setupActivity(ImportExportActivity.class);

            checkVisibility(activity, visibility, R.id.dividerImportFilesystem,
                    R.id.importOptionFilesystemTitle, R.id.importOptionFilesystemExplanation,
                    null, null, R.id.importOptionFilesystemButton);

            // Should always be gone, as its provider is never installed
            checkVisibility(activity, View.GONE, R.id.dividerImportApplication,
                    R.id.importOptionApplicationTitle, R.id.importOptionApplicationExplanation,
                    null, null, R.id.importOptionApplicationButton);

            // Import from file system should always be present

            checkVisibility(activity, View.VISIBLE, R.id.dividerImportFixed,
                    R.id.importOptionFixedTitle, R.id.importOptionFixedExplanation,
                    R.id.importOptionFixedFileFormatSpinnerLabel, R.id.importFileFormatSpinner,
                    R.id.importOptionFixedButton);
        }
    }

    @Test
    public void testImportApplicationOption()
    {
        for(boolean isInstalled : new Boolean[]{false, true})
        {
            int visibility = isInstalled ? View.VISIBLE : View.GONE;

            if(isInstalled)
            {
                registerIntentHandler(Intent.ACTION_GET_CONTENT);
            }

            Activity activity = Robolectric.setupActivity(ImportExportActivity.class);

            checkVisibility(activity, visibility, R.id.dividerImportApplication,
                    R.id.importOptionApplicationTitle, R.id.importOptionApplicationExplanation,
                    null, null, R.id.importOptionApplicationButton);

            // Should always be gone, as its provider is never installed
            checkVisibility(activity, View.GONE, R.id.dividerImportFilesystem,
                    R.id.importOptionFilesystemTitle, R.id.importOptionFilesystemExplanation,
                    null, null, R.id.importOptionFilesystemButton);

            // Import from file system should always be present

            checkVisibility(activity, View.VISIBLE, R.id.dividerImportFixed,
                    R.id.importOptionFixedTitle, R.id.importOptionFixedExplanation,
                    R.id.importOptionFixedFileFormatSpinnerLabel, R.id.importFileFormatSpinner,
                    R.id.importOptionFixedButton);
        }
    }

    @Test
    public void testAllOptionsAvailable()
    {
        registerIntentHandler(Intent.ACTION_PICK);
        registerIntentHandler(Intent.ACTION_GET_CONTENT);

        Activity activity = Robolectric.setupActivity(ImportExportActivity.class);

        checkVisibility(activity, View.VISIBLE, R.id.dividerImportApplication,
                R.id.importOptionApplicationTitle, R.id.importOptionApplicationExplanation,
                null, null, R.id.importOptionApplicationButton);

        checkVisibility(activity, View.VISIBLE, R.id.dividerImportFilesystem,
                R.id.importOptionFilesystemTitle, R.id.importOptionFilesystemExplanation,
                null, null, R.id.importOptionFilesystemButton);

        checkVisibility(activity, View.VISIBLE, R.id.dividerImportFixed,
                R.id.importOptionFixedTitle, R.id.importOptionFixedExplanation,
                R.id.importOptionFixedFileFormatSpinnerLabel, R.id.importFileFormatSpinner,
                R.id.importOptionFixedButton);
    }

    @Test
    public void testExportAndImportButtons() throws IOException
    {
        final Spinner exportSpinner = (Spinner)activity.findViewById(R.id.exportFileFormatSpinner);

        int selection = 0;
        final int NUM_ITEMS = 10;

        for(DataFormat format : DataFormat.values())
        {
            for(int importButtonId : new int[]{R.id.importOptionFixedButton,
                    R.id.importOptionFilesystemButton, R.id.importOptionApplicationButton})
            {
                DatabaseTestHelper.addBudgets(db, NUM_ITEMS);
                DatabaseTestHelper.addTransactions(db, activity, NUM_ITEMS);

                // This assumes that the DataFormat entries are in the same order in the spinner
                // as they appear in the DataFormat class. This assumption is verified below.

                exportSpinner.setSelection(selection);
                String selectedName = (String)exportSpinner.getSelectedItem();
                assertEquals(format.name().toLowerCase(), selectedName.toLowerCase());

                final File exportFile = new File(sdcardDir, "BudgetWatch." + format.name().toLowerCase());
                exportFile.delete();
                assertEquals(false, exportFile.isFile());

                final Button exportButton = (Button) activity.findViewById(R.id.exportButton);

                try
                {
                    exportButton.performClick();
                }
                catch(RuntimeException e)
                {
                    // Clicking on the button should finish the export. However, showing the AlertDialog
                    // will fail due to the following which bubbles up as a RuntimeException:
                    /*
                    Caused by: java.lang.IllegalStateException: You need to use a Theme.AppCompat theme (or descendant) with this activity.
                        at android.support.v7.app.AppCompatDelegateImplV7.createSubDecor(AppCompatDelegateImplV7.java:310)
                        at android.support.v7.app.AppCompatDelegateImplV7.ensureSubDecor(AppCompatDelegateImplV7.java:279)
                        at android.support.v7.app.AppCompatDelegateImplV7.setContentView(AppCompatDelegateImplV7.java:253)
                        at android.support.v7.app.AppCompatDialog.setContentView(AppCompatDialog.java:76)
                        at android.support.v7.app.AlertController.installContent(AlertController.java:213)
                        at android.support.v7.app.AlertDialog.onCreate(AlertDialog.java:240)
                        at android.app.Dialog.dispatchOnCreate(Dialog.java:355)
                        at android.app.Dialog.show(Dialog.java:260)
                        at org.robolectric.shadows.ShadowDialog.show(ShadowDialog.java:68)
                        at android.app.Dialog.show(Dialog.java)
                        at protect.budgetwatch.ImportExportActivity.onExportComplete(ImportExportActivity.java:363)
                    ...
                    */
                    // As the AlertDialog is not the important part of the test, ignoring the issue.
                }

                assertEquals(true, exportFile.isFile());

                // Clear the database, so the import can be checked
                DatabaseTestHelper.clearDatabase(db, activity);

                // Set the export spinner to its start position, so we can check that it does not
                // affect the import
                exportSpinner.setSelection(0);

                if(importButtonId == R.id.importOptionFixedButton)
                {
                    // Select the data format to import
                    final Spinner importSpinner = (Spinner)activity.findViewById(R.id.importFileFormatSpinner);
                    importSpinner.setSelection(selection);
                }

                final Button importButton = (Button) activity.findViewById(importButtonId);

                try
                {
                    importButton.performClick();
                }
                catch(RuntimeException e)
                {
                    // Same issue as before, problems with displaying an AlertDialog at the end
                    // of the import
                }

                if(importButtonId == R.id.importOptionFilesystemButton ||
                        importButtonId == R.id.importOptionApplicationButton)
                {
                    // The import button should have started another activity.

                    ShadowActivity.IntentForResult intentForResult = shadowOf(activity).getNextStartedActivityForResult();
                    assertNotNull(intentForResult);

                    Intent intent = intentForResult.intent;
                    assertNotNull(intent);

                    String action = intent.getAction();
                    assertNotNull(action);

                    Bundle bundle = intent.getExtras();
                    assertNull(bundle);

                    if(importButtonId == R.id.importOptionFilesystemButton)
                    {
                        assertEquals(Intent.ACTION_PICK, action);
                    }

                    if(importButtonId == R.id.importOptionApplicationButton)
                    {
                        assertEquals(Intent.ACTION_GET_CONTENT, action);
                        Set<String> categories = intent.getCategories();
                        assertNotNull(categories);
                        assertTrue(categories.contains(Intent.CATEGORY_OPENABLE));
                    }

                    // This will return the file location from the activity, starting the import
                    Intent result = new Intent();
                    Uri uri = Uri.fromFile(exportFile);
                    result.setData(uri);

                    FileInputStream inputStream = new FileInputStream(exportFile);
                    // Register this stream so it can be retrieved by the code later
                    ContentResolver contentResolver = activity.getContentResolver();
                    ShadowContentResolver shadowContentResolver = Shadow.extract(contentResolver);
                    shadowContentResolver.registerInputStream(uri, inputStream);

                    try
                    {
                        shadowOf(activity).receiveResult(intent,
                                //success ? Activity.RESULT_OK : Activity.RESULT_CANCELED,
                                Activity.RESULT_OK, result);
                    }
                    catch(RuntimeException e)
                    {
                        // Same issue as before, problems with displaying an AlertDialog at the end
                        // of the import
                    }
                }

                // Check that the import worked
                DatabaseTestHelper.checkBudgets(db, NUM_ITEMS);
                DatabaseTestHelper.checkTransactions(db, activity, NUM_ITEMS, format == DataFormat.ZIP);

                DatabaseTestHelper.clearDatabase(db, activity);
            }

            selection++;
        }
    }
}
