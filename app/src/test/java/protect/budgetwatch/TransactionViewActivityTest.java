package protect.budgetwatch;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.common.collect.ImmutableList;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowLog;
import org.robolectric.shadows.ShadowPackageManager;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 23)
public class TransactionViewActivityTest
{
    private static final int ORIGINAL_JPEG_QUALITY = 100;
    private static final int LOWER_JPEG_QUALITY = 0;

    private long nowMs;
    private String nowString;

    @Before
    public void setUp() throws ParseException
    {
        // Output logs emitted during tests so they may be accessed
        ShadowLog.stream = System.out;

        final DateFormat dateFormatter = SimpleDateFormat.getDateInstance();
        nowString = dateFormatter.format(System.currentTimeMillis());
        nowMs = dateFormatter.parse(nowString).getTime();
    }

    /**
     * Register a handler in the package manager for a image capture intent
     */
    private void registerMediaStoreIntentHandler()
    {
        // Add something that will 'handle' the media capture intent
        ShadowPackageManager shadowPackageManager = shadowOf(RuntimeEnvironment.application.getPackageManager());

        ResolveInfo info = new ResolveInfo();
        info.isDefault = true;

        ApplicationInfo applicationInfo = new ApplicationInfo();
        applicationInfo.packageName = "does.not.matter";
        info.activityInfo = new ActivityInfo();
        info.activityInfo.applicationInfo = applicationInfo;
        info.activityInfo.name = "DoesNotMatter";

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        shadowPackageManager.addResolveInfoForIntent(intent, info);
    }

    /**
     * Save an expense and check that the database contains the
     * expected value
     */
    private void saveExpenseWithArguments(final Activity activity,
                                          final String name, final String account, final String budget,
                                          final double value, final String note, final String dateStr,
                                          final long dateMs, final String expectedReceipt,
                                          boolean creatingNewExpense)
    {
        DBHelper db = new DBHelper(activity);
        if(creatingNewExpense)
        {
            assertEquals(0, db.getTransactionCount(DBHelper.TransactionDbIds.EXPENSE));
        }
        else
        {
            assertEquals(1, db.getTransactionCount(DBHelper.TransactionDbIds.EXPENSE));
        }
        db.close();

        final EditText nameField = (EditText) activity.findViewById(R.id.nameEdit);
        final EditText accountField = (EditText) activity.findViewById(R.id.accountEdit);
        final EditText valueField = (EditText) activity.findViewById(R.id.valueEdit);
        final EditText noteField = (EditText) activity.findViewById(R.id.noteEdit);
        final EditText dateField = (EditText) activity.findViewById(R.id.dateEdit);
        final Spinner budgetSpinner = (Spinner) activity.findViewById(R.id.budgetSpinner);

        nameField.setText(name);
        accountField.setText(account);
        valueField.setText(Double.toString(value));
        noteField.setText(note);

        dateField.setText(dateStr);

        // Select the correct budget from the spinner, as there is a blank
        // item in the first position.
        for(int index = 0; index < budgetSpinner.getCount(); index++)
        {
            String item = budgetSpinner.getItemAtPosition(index).toString();
            if(item.equals(budget))
            {
                budgetSpinner.setSelection(index);
                break;
            }
        }

        assertEquals(budget, budgetSpinner.getSelectedItem().toString());

        assertEquals(false, activity.isFinishing());
        shadowOf(activity).clickMenuItem(R.id.action_save);
        assertEquals(true, activity.isFinishing());

        assertEquals(1, db.getTransactionCount(DBHelper.TransactionDbIds.EXPENSE));

        Transaction transaction = db.getTransaction(1);

        assertEquals(DBHelper.TransactionDbIds.EXPENSE, transaction.type);
        assertEquals(name, transaction.description);
        assertEquals(account, transaction.account);
        assertEquals(budget, transaction.budget);
        assertEquals(0, Double.compare(value, transaction.value));
        assertEquals(note, transaction.note);
        assertEquals(dateMs, transaction.dateMs);
        assertEquals(expectedReceipt, transaction.receipt);
    }

    /**
     * Initiate and complete an image capture, returning the
     * location of the resulting file if the capture was
     * a success.
     *
     * @param success
     *      true if the image capture is a success, and a
     *      file is to be created at the requested location,
     *      false otherwise.
     * @param buttonId
     *      id of the button to press to initiate the capture
     * @return The URI pointing to the image file location,
     * regardless if the operation was successful or not.
     */
    private Uri captureImageWithResult(final Activity activity, final int buttonId,
                                       final boolean success, final int jpegQuality) throws IOException
    {
        // Ensure that the application has permissions to ask to use the camera
        shadowOf(activity).grantPermissions(Manifest.permission.CAMERA);

        // Start image capture
        final Button captureButton = (Button) activity.findViewById(buttonId);
        captureButton.performClick();

        ShadowActivity.IntentForResult intentForResult = shadowOf(activity).getNextStartedActivityForResult();
        assertNotNull(intentForResult);

        Intent intent = intentForResult.intent;
        assertNotNull(intent);

        String action = intent.getAction();
        assertNotNull(action);
        assertEquals(MediaStore.ACTION_IMAGE_CAPTURE, action);

        Bundle bundle = intent.getExtras();
        assertNotNull(bundle);

        assertEquals(false, bundle.isEmpty());
        Uri argument = bundle.getParcelable(MediaStore.EXTRA_OUTPUT);
        assertNotNull(argument);
        assertTrue(argument.toString().length() > 0);

        // Set the JPEG quality setting, which will determine if the fake jpeg
        // file is converted to a lower quality
        final SharedPreferences.Editor preferencesEditor = PreferenceManager.getDefaultSharedPreferences(activity).edit();
        preferencesEditor.putString("jpegQuality", Integer.toString(jpegQuality));
        preferencesEditor.apply();

        // Respond to image capture, success
        shadowOf(activity).receiveResult(
                intent,
                success ? Activity.RESULT_OK : Activity.RESULT_CANCELED,
                null);

        // If any async tasks were created, run them now
        Robolectric.flushBackgroundThreadScheduler();

        if(success)
        {
            File imageFile = new File(argument.getPath());

            // No camera is actually invoked in the unit test to create a
            // file. If the original JPEG quality is used, nothing creates
            // a file. However, if a lower quality is used, an async task
            // will generate a file, even if the original file was empty.
            if(jpegQuality == ORIGINAL_JPEG_QUALITY)
            {
                assertEquals(false, imageFile.exists());
                boolean result = imageFile.createNewFile();
                assertTrue(result);
            }
            else
            {
                assertEquals(true, imageFile.exists());
            }
        }

        return argument;
    }

    private void checkFieldProperties(final Activity activity, final int id, final int visibility,
                                      final String contents)
    {
        final View view = activity.findViewById(id);
        assertNotNull(view);
        assertEquals(visibility, view.getVisibility());
        if(contents != null)
        {
            if(view instanceof TextView)
            {
                TextView textView = (TextView)view;
                assertEquals(contents, textView.getText().toString());
            }

            if(view instanceof Spinner)
            {
                Spinner spinner = (Spinner)view;
                String selection = (String)spinner.getSelectedItem();
                assertNotNull(selection);
                assertEquals(contents, selection);
            }
        }
    }

    private void checkSpinnerValues(final Activity activity, final int id,
                              List<String> expectedValues)
    {
        final View view = activity.findViewById(id);
        assertNotNull(view);
        assertTrue(view instanceof Spinner);

        Spinner spinner = (Spinner)view;

        Set<String> expectedSet = new HashSet<>();
        expectedSet.addAll(expectedValues);

        LinkedList<String> actualValues = new LinkedList<>();
        for(int index = 0; index < spinner.getCount(); index++)
        {
            actualValues.add(spinner.getItemAtPosition(index).toString());
        }

        Set<String> actualSet = new HashSet<>(actualValues);

        assertEquals(expectedSet, actualSet);
    }

    private void checkAllFields(final Activity activity,
                                final String name, final String account, final String budget,
                                final String budgetSpinnerValue, final String value, final String note,
                                final String dateStr, final String comittedReceipt,
                                boolean hasUncommitedReceipt, final boolean isLaunchedAsView)
    {
        final boolean hasReceipt = (comittedReceipt.length() > 0) || hasUncommitedReceipt;
        final boolean canUpdateReceipt = hasReceipt && !isLaunchedAsView;
        final boolean canUpdateOrViewReceipt = hasReceipt || !isLaunchedAsView;

        final int hasReceiptVisibility = hasReceipt ? View.VISIBLE : View.GONE;
        final int noReceiptVisibility = (hasReceipt||isLaunchedAsView) ? View.GONE : View.VISIBLE;
        final int canUpdateReceiptVisibility = canUpdateReceipt ? View.VISIBLE : View.GONE;
        final int canUpdateOrViewReceiptVisibility = canUpdateOrViewReceipt ? View.VISIBLE : View.GONE;

        final int editVisibility = isLaunchedAsView ? View.GONE : View.VISIBLE;
        final int viewVisibility = isLaunchedAsView ? View.VISIBLE : View.GONE;

        checkFieldProperties(activity, R.id.nameEdit, editVisibility, isLaunchedAsView ? "" : name);
        checkFieldProperties(activity, R.id.nameView, viewVisibility, isLaunchedAsView ? name : "");
        checkFieldProperties(activity, R.id.accountEdit, editVisibility, isLaunchedAsView ? "" : account);
        checkFieldProperties(activity, R.id.accountView, viewVisibility, isLaunchedAsView ? account : "");
        checkFieldProperties(activity, R.id.budgetSpinner, editVisibility, budgetSpinnerValue);
        checkSpinnerValues(activity, R.id.budgetSpinner, Arrays.asList("", budget));
        checkFieldProperties(activity, R.id.budgetView, viewVisibility, isLaunchedAsView ? budget : "");
        checkFieldProperties(activity, R.id.valueEdit, editVisibility, isLaunchedAsView ? "" : value);
        checkFieldProperties(activity, R.id.valueView, viewVisibility, isLaunchedAsView ? value : "");
        checkFieldProperties(activity, R.id.noteEdit, editVisibility, isLaunchedAsView ? "" : note);
        checkFieldProperties(activity, R.id.noteView, viewVisibility, isLaunchedAsView ? note : "");
        checkFieldProperties(activity, R.id.dateEdit, editVisibility, dateStr);
        checkFieldProperties(activity, R.id.dateView, viewVisibility, isLaunchedAsView ? dateStr : "");
        checkFieldProperties(activity, R.id.receiptLocation, View.GONE, comittedReceipt);
        checkFieldProperties(activity, R.id.receiptLayout, canUpdateOrViewReceiptVisibility, null);
        checkFieldProperties(activity, R.id.endingDivider, canUpdateOrViewReceiptVisibility, null);
        checkFieldProperties(activity, R.id.hasReceiptButtonLayout, hasReceiptVisibility, null);
        checkFieldProperties(activity, R.id.noReceiptButtonLayout, noReceiptVisibility, null);
        checkFieldProperties(activity, R.id.captureButton, View.VISIBLE, null);
        checkFieldProperties(activity, R.id.updateButton, canUpdateReceiptVisibility, null);
        checkFieldProperties(activity, R.id.viewButton, View.VISIBLE, null);
    }

    private void addBudget(Activity activity, String budget)
    {
        DBHelper db = new DBHelper(activity);

        if(budget != null)
        {
            boolean result = db.insertBudget(budget, 0);
            assertTrue(result);
        }
        db.close();
    }

    private void addTransactionForBudget(Activity activity, String budget, String receipt)
    {
        DBHelper db = new DBHelper(activity);

        boolean result = db.insertTransaction(DBHelper.TransactionDbIds.EXPENSE, "description",
                "account", budget,
                100.10, "note", nowMs, receipt);
        assertTrue(result);
        db.close();
    }

    private ActivityController setupActivity(final String budget, final String receipt,
                                             boolean launchAsView, boolean launchAsUpdate)
    {
        Intent intent = new Intent();
        final Bundle bundle = new Bundle();
        bundle.putInt("type", DBHelper.TransactionDbIds.EXPENSE);

        if(receipt != null)
        {
            // Put the ID of the first transaction, which will be added shortly
            bundle.putInt("id", 1);
        }

        if(launchAsView)
        {
            bundle.putBoolean("view", true);
        }

        if(launchAsUpdate)
        {
            bundle.putBoolean("update", true);
        }

        intent.putExtras(bundle);

        ActivityController activityController = Robolectric.buildActivity(TransactionViewActivity.class, intent).create();

        Activity activity = (Activity)activityController.get();

        if(budget != null)
        {
            addBudget(activity, budget);

            if (receipt != null)
            {
                addTransactionForBudget(activity, budget, receipt);
            }
        }

        activityController.start();
        activityController.visible();
        activityController.resume();

        return activityController;
    }

    private ActivityController setupActivity(final int actionType, final String budget, final String receipt)
    {
        Intent intent = new Intent();

        if(actionType == DBHelper.TransactionDbIds.EXPENSE)
        {
            intent.setAction(TransactionViewActivity.ACTION_NEW_EXPENSE);
        }
        else
        {
            intent.setAction(TransactionViewActivity.ACTION_NEW_REVENUE);
        }

        ActivityController activityController = Robolectric.buildActivity(TransactionViewActivity.class, intent).create();

        Activity activity = (Activity)activityController.get();

        if(budget != null)
        {
            addBudget(activity, budget);

            if (receipt != null)
            {
                addTransactionForBudget(activity, budget, receipt);
            }
        }

        activityController.start();
        activityController.visible();
        activityController.resume();

        return activityController;
    }

    @Test
    public void startAsAddCheckFieldsAvailable()
    {
        ActivityController activityController = setupActivity("budget", null, false, false);

        Activity activity = (Activity)activityController.get();

        checkAllFields(activity, "", "", "budget","", "", "", nowString, "", false, false);
        DBHelper db = new DBHelper(activity);
        DatabaseTestHelper.clearDatabase(db, activity);
        db.close();

        for(int type : new int[]{DBHelper.TransactionDbIds.EXPENSE, DBHelper.TransactionDbIds.REVENUE})
        {
            activityController = setupActivity(type, "budget", null);
            activity = (Activity)activityController.get();
            checkAllFields(activity, "", "", "budget","", "", "", nowString, "", false, false);

            db = new DBHelper(activity);
            DatabaseTestHelper.clearDatabase(db, activity);
            db.close();
        }

    }

    @Test
    public void startAsAddCannotCreateExpense()
    {
        ActivityController activityController = setupActivity(null, null, false, false);

        Activity activity = (Activity)activityController.get();
        DBHelper db = new DBHelper(activity);
        assertEquals(0, db.getTransactionCount(DBHelper.TransactionDbIds.EXPENSE));

        for(String[] test : Arrays.asList(
                new String[]{null},
                new String[]{"NotANumber"}
        ))
        {
            String value = test[0];
            String budget = "budget";

            boolean result;

            final Spinner budgetSpinner = (Spinner) activity.findViewById(R.id.budgetSpinner);

            // Add a budget and reload, so the budget spinner has an item
            result = db.insertBudget(budget, 100);
            assertTrue(result);
            assertEquals(1, db.getBudgetCount());
            activityController.resume();
            // Note, a "" is the first budget option. The test will need to select
            // the most recently added option.
            budgetSpinner.setSelection(budgetSpinner.getCount()-1);

            final EditText valueField = (EditText) activity.findViewById(R.id.valueEdit);
            if(value != null)
            {
                valueField.setText(value);
            }
            else
            {
                valueField.setText("");
            }

            // Perform the actual test, no transaction should be created
            shadowOf(activity).clickMenuItem(R.id.action_save);
            assertEquals(0, db.getTransactionCount(DBHelper.TransactionDbIds.EXPENSE));

            // Remove a budget and reload, so the budget spinner will have no item
            result = db.deleteBudget(budget);
            assertTrue(result);
            assertEquals(0, db.getBudgetCount());
            activityController.resume();
            budgetSpinner.setSelection(0);
        }

        db.close();
    }

    @Test
    public void noBudgetSelectedBlankBudgetUsed()
    {
        ActivityController activityController = setupActivity(null, null, false, false);

        Activity activity = (Activity)activityController.get();
        DBHelper db = new DBHelper(activity);
        assertEquals(0, db.getTransactionCount(DBHelper.TransactionDbIds.EXPENSE));

        final String valueStr = "0";
        final int valueInt = Integer.parseInt(valueStr);

        assertEquals(0, db.getBudgetCount());
        final Spinner budgetSpinner = (Spinner) activity.findViewById(R.id.budgetSpinner);
        assertEquals(1, budgetSpinner.getCount());
        assertEquals("", budgetSpinner.getSelectedItem().toString());

        final EditText valueField = (EditText) activity.findViewById(R.id.valueEdit);
        valueField.setText(valueStr);

        // Perform the actual test, no transaction should be created
        shadowOf(activity).clickMenuItem(R.id.action_save);
        assertEquals(1, db.getTransactionCount(DBHelper.TransactionDbIds.EXPENSE));

        Transaction transaction = db.getTransaction(DatabaseTestHelper.FIRST_ID);
        assertNotNull(transaction);

        assertEquals("", transaction.budget);
        assertTrue(valueInt == (int)transaction.value);

        db.close();
    }

    @Test
    public void startAsAddCreateExpenseNoReceipt()
    {
        ActivityController activityController = setupActivity("budget", null, false, false);

        Activity activity = (Activity)activityController.get();

        saveExpenseWithArguments(activity, "name", "account", "budget", 0, "note", nowString, nowMs,
                "", true);
    }


    @Test
    public void startAsAddCaptureReceiptCreateExpenseImageUnedited() throws IOException
    {
        ActivityController activityController = setupActivity("budget", null, false, false);

        // Add something that will 'handle' the media capture intent
        registerMediaStoreIntentHandler();

        Activity activity = (Activity)activityController.get();

        checkAllFields(activity, "", "", "budget", "", "", "", nowString, "", false, false);

        // Complete image capture successfully
        Uri imageLocation = captureImageWithResult(activity, R.id.captureButton, true, ORIGINAL_JPEG_QUALITY);

        checkAllFields(activity, "", "", "budget","", "", "", nowString, "", true, false);

        // Save and check the expense
        saveExpenseWithArguments(activity, "name", "account", "budget", 100, "note",
                nowString, nowMs, imageLocation.getPath(), true);

        // Ensure that the file still exists
        File imageFile = new File(imageLocation.getPath());
        assertTrue(imageFile.isFile());

        // Delete the file to cleanup
        boolean result = imageFile.delete();
        assertTrue(result);
    }

    @Test
    public void startAsAddCaptureReceiptCreateExpenseImageReencoded() throws IOException
    {
        ActivityController activityController = setupActivity("budget", null, false, false);

        // Add something that will 'handle' the media capture intent
        registerMediaStoreIntentHandler();

        Activity activity = (Activity)activityController.get();

        checkAllFields(activity, "", "", "budget", "", "", "", nowString, "", false, false);

        // Complete image capture successfully
        Uri imageLocation = captureImageWithResult(activity, R.id.captureButton, true, LOWER_JPEG_QUALITY);

        checkAllFields(activity, "", "", "budget","", "", "", nowString, "", true, false);

        // Save and check the expense
        saveExpenseWithArguments(activity, "name", "account", "budget", 100, "note",
                nowString, nowMs, imageLocation.getPath(), true);

        // Ensure that the file still exists
        File imageFile = new File(imageLocation.getPath());
        assertTrue(imageFile.isFile());

        // Delete the file to cleanup
        boolean result = imageFile.delete();
        assertTrue(result);
    }

    @Test
    public void startAsAddCaptureReceiptFailureCreateExpense() throws IOException
    {
        ActivityController activityController = setupActivity("budget", null, false, false);

        // Add something that will 'handle' the media capture intent
        registerMediaStoreIntentHandler();

        Activity activity = (Activity)activityController.get();

        checkAllFields(activity, "", "", "budget","", "", "", nowString, "", false, false);

        // Complete image capture in failure
        Uri imageLocation = captureImageWithResult(activity, R.id.captureButton, false, ORIGINAL_JPEG_QUALITY);

        checkAllFields(activity, "", "", "budget", "", "", "", nowString, "", false, false);

        // Save and check the gift card
        saveExpenseWithArguments(activity, "name", "account", "budget", 100, "note",
                nowString, nowMs, "", true);

        // Check that no file was created
        File imageFile = new File(imageLocation.getPath());
        assertEquals(false, imageFile.exists());
    }

    @Test
    public void startAsAddCaptureReceiptCancel() throws IOException
    {
        ActivityController activityController = setupActivity("budget", null, false, false);

        // Add something that will 'handle' the media capture intent
        registerMediaStoreIntentHandler();

        Activity activity = (Activity)activityController.get();

        checkAllFields(activity, "", "", "budget", "", "", "", nowString, "", false, false);

        // Complete image capture successfully
        Uri imageLocation = captureImageWithResult(activity, R.id.captureButton, true, ORIGINAL_JPEG_QUALITY);

        checkAllFields(activity, "", "", "budget", "", "", "", nowString, "", true, false);

        // Ensure that the file still exists
        File imageFile = new File(imageLocation.getPath());
        assertTrue(imageFile.isFile());

        // Cancel the expense creation
        assertEquals(false, activity.isFinishing());
        shadowOf(activity).clickMenuItem(android.R.id.home);
        assertEquals(true, activity.isFinishing());
        activityController.destroy();

        // Ensure the image has been deleted
        assertEquals(false, imageFile.exists());
    }

    @Test
    public void startAsEditNoReceiptCheckDisplay() throws IOException
    {
        ActivityController activityController = setupActivity("budget", "", false, true);
        Activity activity = (Activity)activityController.get();
        checkAllFields(activity, "description", "account", "budget", "budget", "100.10", "note", nowString,
                "", false, false);
    }

    @Test
    public void startAsEditNoReceiptCheckValueWithLocale() throws IOException
    {
        ActivityController activityController = setupActivity("budget", "", false, true);
        Activity activity = (Activity)activityController.get();

        for(String locale : ImmutableList.of(
                "en",  // 100.10
                "nl")) // 100,10
        {
            System.out.println("Using locale: " + locale);
            Locale.setDefault(new Locale(locale));

            activityController.pause();
            activityController.restart();
            activityController.resume();

            EditText value = (EditText)activity.findViewById(R.id.valueEdit);

            assertEquals("100.10", value.getText().toString());
        }
    }

    @Test
    public void startAsEditWithReceiptCheckDisplay() throws IOException
    {
        ActivityController activityController = setupActivity("budget", "receipt", false, true);

        Activity activity = (Activity)activityController.get();

        checkAllFields(activity, "description", "account", "budget", "budget", "100.10", "note", nowString, "receipt", false, false);
    }

    @Test
    public void startAsEditWithExpensedWithReceiptUpdateReceipt() throws IOException
    {
        ActivityController activityController = setupActivity("budget", "receipt", false, true);
        Activity activity = (Activity)activityController.get();

        checkAllFields(activity, "description", "account", "budget", "budget", "100.10", "note", nowString,
                "receipt", false, false);

        // Add something that will 'handle' the media capture intent
        registerMediaStoreIntentHandler();

        // Complete image capture successfully
        Uri imageLocation = captureImageWithResult(activity, R.id.updateButton, true, ORIGINAL_JPEG_QUALITY);

        checkAllFields(activity, "description", "account", "budget", "budget", "100.10", "note", nowString,
                "receipt", true, false);

        // Save and check the expense
        saveExpenseWithArguments(activity, "name", "account", "budget", 100, "note",
                nowString, nowMs, imageLocation.getPath(), false);

        // Ensure that the file still exists
        File imageFile = new File(imageLocation.getPath());
        assertTrue(imageFile.isFile());

        // Delete the file to cleanup
        boolean result = imageFile.delete();
        assertTrue(result);
    }

    @Test
    public void startAsEditWithExpenseWithReceiptUpdateReceiptCancel() throws IOException
    {
        ActivityController activityController = setupActivity("budget", "receipt", false, true);
        Activity activity = (Activity)activityController.get();

        checkAllFields(activity, "description", "account", "budget", "budget", "100.10", "note", nowString,
                "receipt", false, false);

        // Add something that will 'handle' the media capture intent
        registerMediaStoreIntentHandler();

        // Complete image capture successfully
        Uri imageLocation = captureImageWithResult(activity, R.id.updateButton, true, ORIGINAL_JPEG_QUALITY);

        checkAllFields(activity, "description", "account", "budget", "budget", "100.10", "note", nowString,
                "receipt", true, false);

        // Cancel the expense update
        assertEquals(false, activity.isFinishing());
        shadowOf(activity).clickMenuItem(android.R.id.home);
        assertEquals(true, activity.isFinishing());
        activityController.destroy();

        // Ensure the image has been deleted
        File imageFile = new File(imageLocation.getPath());
        assertEquals(false, imageFile.exists());
    }

    @Test
    public void startAsViewNoReceiptCheckDisplay() throws IOException
    {
        ActivityController activityController = setupActivity("budget", "", true, false);

        Activity activity = (Activity)activityController.get();

        checkAllFields(activity, "description", "account", "budget", "budget", "100.10", "note", nowString, "", false, true);
    }

    @Test
    public void startAsViewWithReceiptCheckDisplay() throws IOException
    {
        ActivityController activityController = setupActivity("budget", "receipt", true, false);

        Activity activity = (Activity)activityController.get();

        checkAllFields(activity, "description", "account", "budget", "budget", "100.10", "note", nowString, "receipt", false, true);
    }

    @Test
    public void startAsAddCheckActionBar() throws Exception
    {
        ActivityController activityController = setupActivity("budget", null, false, false);
        Activity activity = (Activity)activityController.get();

        final Menu menu = shadowOf(activity).getOptionsMenu();
        assertTrue(menu != null);

        assertEquals(menu.size(), 1);

        MenuItem item = menu.findItem(R.id.action_save);
        assertNotNull(item);
        assertEquals("Save", item.getTitle().toString());
    }

    @Test
    public void startAsUpdateCheckActionBar() throws Exception
    {
        ActivityController activityController = setupActivity("budget", "", false, true);
        Activity activity = (Activity)activityController.get();

        final Menu menu = shadowOf(activity).getOptionsMenu();
        assertTrue(menu != null);

        assertEquals(menu.size(), 2);

        MenuItem item = menu.findItem(R.id.action_delete);
        assertNotNull(item);
        assertEquals("Delete", item.getTitle().toString());

        item = menu.findItem(R.id.action_save);
        assertNotNull(item);
        assertEquals("Save", item.getTitle().toString());
    }

    @Test
    public void clickDeleteRemovesExpense()
    {
        ActivityController activityController = setupActivity("budget", "", false, true);
        Activity activity = (Activity)activityController.get();
        DBHelper db = new DBHelper(activity);

        assertEquals(1, db.getTransactionCount(DBHelper.TransactionDbIds.EXPENSE));
        shadowOf(activity).clickMenuItem(R.id.action_delete);

        // TODO: Finish this test once robolectric has shadows of the android.support AlertDialog class.
        // https://github.com/robolectric/robolectric/issues/1944

        /*
        // A dialog should be displayed now
        AlertDialog alert = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull(alert);
        ShadowAlertDialog sAlert = shadowOf(alert);
        assertEquals(sAlert.getTitle().toString(), activity.getString(R.string.deleteTransactionTitle));
        assertEquals(sAlert.getMessage().toString(), activity.getString(R.string.deleteTransactionConfirmation));

        sAlert.clickOnText(R.string.confirm);
        assertEquals(0, db.getTransactionCount(DBHelper.TransactionDbIds.EXPENSE));
        */

        db.close();
    }

    @Test
    public void clickBackFinishes()
    {
        ActivityController activityController = setupActivity("budget", "", false, true);
        Activity activity = (Activity)activityController.get();

        shadowOf(activity).clickMenuItem(android.R.id.home);
        assertTrue(shadowOf(activity).isFinishing());
    }

    @Test
    public void startAsViewCheckActionBar() throws Exception
    {
        ActivityController activityController = setupActivity("budget", "", true, false);
        Activity activity = (Activity)activityController.get();

        final Menu menu = shadowOf(activity).getOptionsMenu();
        assertTrue(menu != null);

        assertEquals(menu.size(), 1);

        MenuItem item = menu.findItem(R.id.action_edit);
        assertNotNull(item);
        assertEquals("Edit", item.getTitle().toString());
    }

    @Test
    public void startAsViewClickEdit() throws Exception
    {
        ActivityController activityController = setupActivity("budget", "", true, false);
        Activity activity = (Activity)activityController.get();

        shadowOf(activity).clickMenuItem(R.id.action_edit);

        ShadowActivity shadowActivity = shadowOf(activity);
        Intent startedIntent = shadowActivity.getNextStartedActivity();

        ComponentName name = startedIntent.getComponent();
        assertNotNull(name);
        assertEquals("protect.budgetwatch/.TransactionViewActivity", name.flattenToShortString());
        Bundle bundle = startedIntent.getExtras();
        assertNotNull(bundle);

        assertEquals(DBHelper.TransactionDbIds.EXPENSE, bundle.getInt("type", -1));
        assertEquals(1, bundle.getInt("id", -1));
        assertEquals(true, bundle.getBoolean("update", false));
        assertEquals(false, bundle.getBoolean("view", false));
    }

    @Test
    public void clickEditLaunchesTransactionViewActivity()
    {
        ActivityController activityController = setupActivity("budget", "", true, false);
        Activity activity = (Activity)activityController.get();

        shadowOf(activity).clickMenuItem(R.id.action_edit);

        Intent intent = shadowOf(activity).peekNextStartedActivityForResult().intent;

        assertEquals(new ComponentName(activity, TransactionViewActivity.class), intent.getComponent());
        Bundle bundle = intent.getExtras();
        assertNotNull(bundle);
        assertEquals(1, bundle.getInt("id", -1));
        assertEquals(true, bundle.getBoolean("update", false));
    }
}
