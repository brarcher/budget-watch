package protect.budgetwatch;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class TransactionViewActivity extends AppCompatActivity
{
    private static final String TAG = "BudgetWatch";
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int PERMISSIONS_REQUEST_CAMERA = 2;

    private String capturedUncommittedReceipt = null;
    private DBHelper _db;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.transaction_view_activity);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null)
        {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        _db = new DBHelper(this);
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void onResume()
    {
        super.onResume();

        final Bundle b = getIntent().getExtras();
        final int transactionId = b.getInt("id");
        final int type = b.getInt("type");
        final boolean updateTransaction = b.getBoolean("update", false);
        final boolean viewTransaction = b.getBoolean("view", false);

        if(type == DBHelper.TransactionDbIds.EXPENSE)
        {
            if (updateTransaction)
            {
                setTitle(R.string.editExpenseTransactionTitle);
            }
            else if (viewTransaction)
            {
                setTitle(R.string.viewExpenseTransactionTitle);
            }
            else
            {
                setTitle(R.string.addExpenseTransactionTitle);
            }
        }
        else if(type == DBHelper.TransactionDbIds.REVENUE)
        {
            if(updateTransaction)
            {
                setTitle(R.string.editRevenueTransactionTitle);
            }
            else if(viewTransaction)
            {
                setTitle(R.string.viewRevenueTransactionTitle);
            }
            else
            {
                setTitle(R.string.addRevenueTransactionTitle);
            }
        }

        final Calendar date = new GregorianCalendar();
        final DateFormat dateFormatter = SimpleDateFormat.getDateInstance();
        final EditText dateEdit = (EditText) findViewById(R.id.dateEdit);
        dateEdit.setText(dateFormatter.format(date.getTime()));

        final DatePickerDialog.OnDateSetListener dateSetListener = new DatePickerDialog.OnDateSetListener()
        {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int day)
            {
                date.set(year, month, day);
                dateEdit.setText(dateFormatter.format(date.getTime()));
            }
        };

        dateEdit.setOnFocusChangeListener(new View.OnFocusChangeListener()
        {
            @Override
            public void onFocusChange(View v, boolean hasFocus)
            {
                if (hasFocus)
                {
                    int year = date.get(Calendar.YEAR);
                    int month = date.get(Calendar.MONTH);
                    int day = date.get(Calendar.DATE);
                    DatePickerDialog datePicker = new DatePickerDialog(TransactionViewActivity.this,
                            dateSetListener, year, month, day);
                    datePicker.show();
                }
            }
        });

        final Spinner budgetSpinner = (Spinner) findViewById(R.id.budgetSpinner);
        List<String> budgetNames = _db.getBudgetNames();

        // Add budget items to spinner if it has not been initialized yet
        if(budgetSpinner.getCount() == 0)
        {
            ArrayAdapter<String> budgets = new ArrayAdapter<>(this, R.layout.spinner_textview, budgetNames);
            budgetSpinner.setAdapter(budgets);
        }

        final EditText nameEdit = (EditText) findViewById(R.id.nameEdit);
        final TextView nameView = (TextView) findViewById(R.id.nameView);
        final EditText accountEdit = (EditText) findViewById(R.id.accountEdit);
        final TextView accountView = (TextView) findViewById(R.id.accountView);
        final EditText valueEdit = (EditText) findViewById(R.id.valueEdit);
        final TextView valueView = (TextView) findViewById(R.id.valueView);
        final EditText noteEdit = (EditText) findViewById(R.id.noteEdit);
        final TextView noteView = (TextView) findViewById(R.id.noteView);
        final TextView budgetView = (TextView) findViewById(R.id.budgetView);
        final TextView dateView = (TextView) findViewById(R.id.dateView);
        final Button cancelButton = (Button)findViewById(R.id.cancelButton);
        final Button saveButton = (Button)findViewById(R.id.saveButton);
        final Button captureButton = (Button)findViewById(R.id.captureButton);
        final Button viewButton = (Button)findViewById(R.id.viewButton);
        final Button updateButton = (Button)findViewById(R.id.updateButton);
        final View receiptLayout = findViewById(R.id.receiptLayout);
        final View endingDivider = findViewById(R.id.endingDivider);
        final TextView receiptLocationField = (TextView) findViewById(R.id.receiptLocation);
        final View noReceiptButtonLayout = findViewById(R.id.noReceiptButtonLayout);
        final View hasReceiptButtonLayout = findViewById(R.id.hasReceiptButtonLayout);

        if(updateTransaction || viewTransaction)
        {
            Transaction transaction = _db.getTransaction(transactionId);
            (updateTransaction ? nameEdit : nameView).setText(transaction.description);
            (updateTransaction ? accountEdit : accountView).setText(transaction.account);

            int budgetIndex = budgetNames.indexOf(transaction.budget);
            if(budgetIndex >= 0)
            {
                budgetSpinner.setSelection(budgetIndex);
            }
            budgetView.setText(viewTransaction ? transaction.budget : "");

            (updateTransaction ? valueEdit : valueView).setText(String.format(Locale.US, "%.2f", transaction.value));
            (updateTransaction ? noteEdit : noteView).setText(transaction.note);
            (updateTransaction ? dateEdit : dateView).setText(dateFormatter.format(new Date(transaction.dateMs)));
            receiptLocationField.setText(transaction.receipt);

            if(viewTransaction)
            {
                budgetSpinner.setVisibility(View.GONE);
                nameEdit.setVisibility(View.GONE);
                accountEdit.setVisibility(View.GONE);
                valueEdit.setVisibility(View.GONE);
                noteEdit.setVisibility(View.GONE);
                dateEdit.setVisibility(View.GONE);

                cancelButton.setVisibility(Button.GONE);
                saveButton.setVisibility(Button.GONE);

                // The no receipt layout need never be displayed
                // when only viewing a transaction, as one should
                // not be able to capture a receipt
                noReceiptButtonLayout.setVisibility(View.GONE);

                // If viewing a transaction, only display the receipt
                // field if a receipt is captured
                if(transaction.receipt.isEmpty() == false)
                {
                    receiptLayout.setVisibility(View.VISIBLE);
                    endingDivider.setVisibility(View.VISIBLE);
                    hasReceiptButtonLayout.setVisibility(View.VISIBLE);
                }
                else
                {
                    receiptLayout.setVisibility(View.GONE);
                    endingDivider.setVisibility(View.GONE);
                }
            }
            else
            {
                budgetView.setVisibility(View.GONE);
                nameView.setVisibility(View.GONE);
                accountView.setVisibility(View.GONE);
                valueView.setVisibility(View.GONE);
                noteView.setVisibility(View.GONE);
                dateView.setVisibility(View.GONE);

                // If editing a transaction, always list the receipt field
                receiptLayout.setVisibility(View.VISIBLE);
                endingDivider.setVisibility(View.VISIBLE);
                if(transaction.receipt.isEmpty() && capturedUncommittedReceipt == null)
                {
                    noReceiptButtonLayout.setVisibility(View.VISIBLE);
                    hasReceiptButtonLayout.setVisibility(View.GONE);
                }
                else
                {
                    noReceiptButtonLayout.setVisibility(View.GONE);
                    hasReceiptButtonLayout.setVisibility(View.VISIBLE);
                    updateButton.setVisibility(View.VISIBLE);
                }
            }
        }
        else
        {
            budgetView.setVisibility(View.GONE);
            nameView.setVisibility(View.GONE);
            accountView.setVisibility(View.GONE);
            valueView.setVisibility(View.GONE);
            noteView.setVisibility(View.GONE);
            dateView.setVisibility(View.GONE);

            // If adding a transaction, always list the receipt field
            receiptLayout.setVisibility(View.VISIBLE);
            endingDivider.setVisibility(View.VISIBLE);
            if(capturedUncommittedReceipt == null)
            {
                noReceiptButtonLayout.setVisibility(View.VISIBLE);
                hasReceiptButtonLayout.setVisibility(View.GONE);
            }
            else
            {
                noReceiptButtonLayout.setVisibility(View.GONE);
                hasReceiptButtonLayout.setVisibility(View.VISIBLE);
                updateButton.setVisibility(View.VISIBLE);
            }
        }

        View.OnClickListener captureCallback = new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (ContextCompat.checkSelfPermission(TransactionViewActivity.this,
                        Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
                {
                    captureReceipt();
                }
                else
                {
                    ActivityCompat.requestPermissions(TransactionViewActivity.this,
                            new String[]{Manifest.permission.CAMERA},
                            PERMISSIONS_REQUEST_CAMERA);
                }
            }
        };

        captureButton.setOnClickListener(captureCallback);
        updateButton.setOnClickListener(captureCallback);

        viewButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent i = new Intent(v.getContext(), ReceiptViewActivity.class);
                final Bundle b = new Bundle();

                final TextView receiptField = (TextView) findViewById(R.id.receiptLocation);

                String receipt = receiptField.getText().toString();
                if(capturedUncommittedReceipt != null)
                {
                    receipt = capturedUncommittedReceipt;
                }

                b.putString("receipt", receipt);
                i.putExtras(b);
                startActivity(i);
            }
        });

        saveButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(final View v)
            {
                final String name = nameEdit.getText().toString();
                // name field is optional, so it is OK if it is empty

                final String budget = (String)budgetSpinner.getSelectedItem();
                if (budget == null)
                {
                    Snackbar.make(v, R.string.budgetMissing, Snackbar.LENGTH_LONG).show();
                    return;
                }

                final String account = accountEdit.getText().toString();
                // The account field is optional, so it is OK if it is empty

                final String valueStr = valueEdit.getText().toString();
                if (valueStr.isEmpty())
                {
                    Snackbar.make(v, R.string.valueMissing, Snackbar.LENGTH_LONG).show();
                    return;
                }

                double value;
                try
                {
                    value = Double.parseDouble(valueStr);
                }
                catch (NumberFormatException e)
                {
                    Snackbar.make(v, R.string.valueInvalid, Snackbar.LENGTH_LONG).show();
                    return;
                }

                final String note = noteEdit.getText().toString();
                // The note field is optional, so it is OK if it is empty

                final String dateStr = dateEdit.getText().toString();
                final DateFormat dateFormatter = SimpleDateFormat.getDateInstance();
                long dateMs;
                try
                {
                    dateMs = dateFormatter.parse(dateStr).getTime();
                }
                catch (ParseException e)
                {
                    Snackbar.make(v, R.string.dateInvalid, Snackbar.LENGTH_LONG).show();
                    return;
                }

                String receipt = receiptLocationField.getText().toString();
                if(capturedUncommittedReceipt != null)
                {
                    // Delete the old receipt, it is no longer needed
                    File oldReceipt = new File(receipt);
                    if(oldReceipt.delete() == false)
                    {
                        Log.e(TAG, "Unable to delete old receipt file: " + capturedUncommittedReceipt);
                    }

                    // Remember the new receipt to save
                    receipt = capturedUncommittedReceipt;
                    capturedUncommittedReceipt = null;
                }

                if(updateTransaction)
                {
                    _db.updateTransaction(transactionId, type, name, account,
                            budget, value, note, dateMs, receipt);

                }
                else
                {
                    _db.insertTransaction(type, name, account, budget,
                            value, note, dateMs, receipt);
                }

                finish();
            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                finish();
            }
        });
    }

    private void captureReceipt()
    {
        if(capturedUncommittedReceipt != null)
        {
            Log.i(TAG, "Deleting unsaved image: " + capturedUncommittedReceipt);
            File unneededReceipt = new File(capturedUncommittedReceipt);
            if(unneededReceipt.delete() == false)
            {
                Log.e(TAG, "Unable to delete unnecessary file: " + capturedUncommittedReceipt);
            }
            capturedUncommittedReceipt = null;
        }

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        PackageManager packageManager = getPackageManager();
        if(packageManager == null)
        {
            Log.e(TAG, "Failed to get package manager, cannot take picture");
            Toast.makeText(getApplicationContext(), R.string.pictureCaptureError,
                    Toast.LENGTH_LONG).show();
            return;
        }

        if(takePictureIntent.resolveActivity(packageManager) == null)
        {
            Log.e(TAG, "Could not find an activity to take a picture");
            Toast.makeText(getApplicationContext(), R.string.pictureCaptureError, Toast.LENGTH_LONG).show();
            return;
        }

        File imageLocation = getNewImageLocation();
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(imageLocation));
        capturedUncommittedReceipt = (imageLocation != null ? imageLocation.getAbsolutePath() : null);
        startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
    }

    @Override
    protected void onDestroy()
    {
        if(capturedUncommittedReceipt != null)
        {
            // The receipt was captured but never used
            Log.i(TAG, "Deleting unsaved image: " + capturedUncommittedReceipt);
            File unneededReceipt = new File(capturedUncommittedReceipt);
            if(unneededReceipt.delete() == false)
            {
                Log.e(TAG, "Unable to delete unnecessary file: " + capturedUncommittedReceipt);
            }
            capturedUncommittedReceipt = null;
        }

        _db.close();

        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        final Bundle b = getIntent().getExtras();
        final boolean viewBudget = b != null && b.getBoolean("view", false);
        final boolean editBudget = b != null && b.getBoolean("update", false);

        if(viewBudget)
        {
            getMenuInflater().inflate(R.menu.edit_menu, menu);
        }

        if(editBudget)
        {
            getMenuInflater().inflate(R.menu.delete_menu, menu);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();

        final Bundle b = getIntent().getExtras();
        final int transactionId = b.getInt("id");
        final int type = b.getInt("type");

        switch(id)
        {
            case R.id.action_edit:
                finish();

                Intent i = new Intent(getApplicationContext(), TransactionViewActivity.class);
                Bundle bundle = new Bundle();
                bundle.putInt("id", transactionId);
                bundle.putInt("type", type);
                bundle.putBoolean("update", true);
                i.putExtras(bundle);
                startActivity(i);
                return true;

            case R.id.action_delete:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.deleteTransactionTitle);
                builder.setMessage(R.string.deleteTransactionConfirmation);
                builder.setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        Log.e(TAG, "Deleting transaction: " + transactionId);

                        _db.deleteTransaction(transactionId);
                        finish();

                        dialog.dismiss();
                    }
                });
                builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        dialog.dismiss();
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();

                return true;

            case android.R.id.home:
                finish();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private File getNewImageLocation()
    {
        File imageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        if(imageDir == null)
        {
            Log.e(TAG, "Failed to locate directory for pictures");
            Toast.makeText(this, R.string.pictureCaptureError, Toast.LENGTH_LONG).show();
            return null;
        }

        if(imageDir.exists() == false)
        {
            if(imageDir.mkdirs() == false)
            {
                Log.e(TAG, "Failed to create receipts image directory");
                Toast.makeText(this, R.string.pictureCaptureError, Toast.LENGTH_LONG).show();
                return null;
            }
        }

        UUID imageFilename = UUID.randomUUID();
        File receiptFile = new File(imageDir, imageFilename.toString() + ".jpg");

        return receiptFile;
    }

    private boolean reencodeImageWithQuality(String original, int quality)
    {
        File destFile = new File(original);
        File tmpLocation = getNewImageLocation();

        try
        {
            if (tmpLocation == null)
            {
                throw new IOException("Could not create location for tmp file");
            }

            boolean created = tmpLocation.createNewFile();
            if (created == false)
            {
                throw new IOException("Could not create tmp file");
            }

            Bitmap bitmap = BitmapFactory.decodeFile(original);
            FileOutputStream fOut = new FileOutputStream(tmpLocation);
            boolean fileWritten = bitmap.compress(Bitmap.CompressFormat.JPEG, quality, fOut);
            fOut.flush();
            fOut.close();

            if (fileWritten == false)
            {
                throw new IOException("Could not down compress file");
            }

            boolean renamed = tmpLocation.renameTo(destFile);
            if (renamed == false)
            {
                throw new IOException("Could not move converted file");
            }

            Log.i(TAG, "Image file " + original + " saved at quality " + quality);

            return true;
        }
        catch(IOException e)
        {
            Log.e(TAG, "Failed to encode image", e);

            for(File item : new File[]{tmpLocation, destFile})
            {
                if(item != null)
                {
                    boolean result = item.delete();
                    if(result == false)
                    {
                        Log.w(TAG, "Failed to delete image file: " + item.getAbsolutePath());
                    }
                }
            }
        }

        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        Log.d(TAG, "Received image from camera");

        if(requestCode == REQUEST_IMAGE_CAPTURE)
        {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

            String jpegQualityLevelStr = prefs.getString("jpegQuality", "");
            int jpegQualityLevel = 40; // default value

            try
            {
                jpegQualityLevel = Integer.parseInt(jpegQualityLevelStr);
            }
            catch(NumberFormatException e)
            {
                // If the setting has no value or is otherwise invalid, fall back
                // on a default value
            }

            final int JPEG_QUALITY_LEVEL = jpegQualityLevel;

            if (resultCode != RESULT_OK || JPEG_QUALITY_LEVEL == 100)
            {
                if(resultCode != RESULT_OK)
                {
                    Log.e(TAG, "Failed to create receipt image: " + resultCode);
                    // No image was actually created, simply forget the patch
                    capturedUncommittedReceipt = null;
                }
                else
                {
                    Log.i(TAG, "Image file saved: " + capturedUncommittedReceipt);
                }

                onResume();
            }
            else
            {
                Log.i(TAG, "Re-encoding image in background");

                AsyncTask<Void, Void, Boolean> imageConverter = new AsyncTask<Void, Void, Boolean>()
                {
                    ProgressDialog dialog;

                    @Override
                    protected void onPreExecute()
                    {
                        dialog = new ProgressDialog(TransactionViewActivity.this);
                        dialog.setMessage(TransactionViewActivity.this.getResources().getString(R.string.encodingReceipt));
                        dialog.setCancelable(false);
                        dialog.setCanceledOnTouchOutside(false);
                        dialog.show();
                    }

                    @Override
                    protected Boolean doInBackground(Void... params)
                    {
                        return reencodeImageWithQuality(capturedUncommittedReceipt, JPEG_QUALITY_LEVEL);
                    }

                    @Override
                    protected void onPostExecute(Boolean result)
                    {
                        if(result != null && result)
                        {
                            Log.i(TAG, "Image file re-encoded: " + capturedUncommittedReceipt);
                        }
                        else
                        {
                            Log.e(TAG, "Failed to re-encode image");
                            // No image was actually created, simply forget the patch
                            capturedUncommittedReceipt = null;
                        }

                        dialog.hide();
                        TransactionViewActivity.this.onResume();
                    }
                };

                imageConverter.execute();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults)
    {
        if(requestCode == PERMISSIONS_REQUEST_CAMERA)
        {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                // permission was granted.
                captureReceipt();
            }
            else
            {
                // Camera permission rejected, inform user that
                // no receipt can be taken.
                Toast.makeText(getApplicationContext(), R.string.noCameraPermissionError,
                        Toast.LENGTH_LONG).show();
            }

        }
    }
}
