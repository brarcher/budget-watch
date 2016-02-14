package protect.budgetwatch;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
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
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.UUID;

public class TransactionViewActivity extends AppCompatActivity
{
    private static final String TAG = "BudgetWatch";
    private static final int REQUEST_IMAGE_CAPTURE = 1;

    private String capturedUncommittedReceipt = null;

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
    }

    @Override
    public void onResume()
    {
        super.onResume();

        final Bundle b = getIntent().getExtras();
        final int transactionId = b.getInt("id");
        final int type = b.getInt("type");
        final boolean updateTransaction = b.getBoolean("update", false);
        final boolean viewTransaction = b.getBoolean("view", false);

        switch(type)
        {
            case DBHelper.TransactionDbIds.EXPENSE:
                if(updateTransaction)
                {
                    setTitle(R.string.editExpenseTransactionTitle);
                }
                else if(viewTransaction)
                {
                    setTitle(R.string.viewExpenseTransactionTitle);
                }
                else
                {
                    setTitle(R.string.addExpenseTransactionTitle);
                }

                break;

            case DBHelper.TransactionDbIds.REVENUE:
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

                break;
        }

        final Calendar date = new GregorianCalendar();
        final DateFormat dateFormatter = SimpleDateFormat.getDateInstance();
        final EditText dateField = (EditText) findViewById(R.id.date);
        dateField.setText(dateFormatter.format(date.getTime()));

        final DatePickerDialog.OnDateSetListener dateSetListener = new DatePickerDialog.OnDateSetListener()
        {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int day)
            {
                date.set(year, month, day);
                dateField.setText(dateFormatter.format(date.getTime()));
            }
        };

        dateField.setOnFocusChangeListener(new View.OnFocusChangeListener()
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
        DBHelper db = new DBHelper(TransactionViewActivity.this);
        List<String> budgetNames = db.getBudgetNames();
        ArrayAdapter<String> budgets = new ArrayAdapter<>(this, R.layout.spinner_textview, budgetNames);
        budgetSpinner.setAdapter(budgets);

        final EditText nameField = (EditText) findViewById(R.id.name);
        final EditText accountField = (EditText) findViewById(R.id.account);
        final EditText valueField = (EditText) findViewById(R.id.value);
        final EditText noteField = (EditText) findViewById(R.id.note);
        final Button cancelButton = (Button)findViewById(R.id.cancelButton);
        final Button saveButton = (Button)findViewById(R.id.saveButton);
        final Button captureButton = (Button)findViewById(R.id.captureButton);
        final Button viewButton = (Button)findViewById(R.id.viewButton);
        final Button updateButton = (Button)findViewById(R.id.updateButton);
        final View receiptLayout = findViewById(R.id.receiptLayout);
        final TextView receiptLocationField = (TextView) findViewById(R.id.receiptLocation);
        final View noReceiptButtonLayout = findViewById(R.id.noReceiptButtonLayout);
        final View hasReceiptButtonLayout = findViewById(R.id.hasReceiptButtonLayout);

        if(updateTransaction || viewTransaction)
        {
            Transaction transaction = db.getTransaction(transactionId);
            nameField.setText(transaction.description);
            accountField.setText(transaction.account);

            int budgetIndex = budgetNames.indexOf(transaction.budget);
            if(budgetIndex >= 0)
            {
                budgetSpinner.setSelection(budgetIndex);
            }

            valueField.setText(String.format("%.2f", transaction.value));
            noteField.setText(transaction.note);
            dateField.setText(dateFormatter.format(new Date(transaction.dateMs)));
            receiptLocationField.setText(transaction.receipt);

            if(viewTransaction)
            {
                budgetSpinner.setEnabled(false);
                nameField.setEnabled(false);
                accountField.setEnabled(false);
                valueField.setEnabled(false);
                noteField.setEnabled(false);
                dateField.setEnabled(false);
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
                    hasReceiptButtonLayout.setVisibility(View.VISIBLE);
                }
                else
                {
                    receiptLayout.setVisibility(View.GONE);
                }
            }
            else
            {
                // If editing a transaction, always list the receipt field
                receiptLayout.setVisibility(View.VISIBLE);
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
            // If adding a transaction, always list the receipt field
            receiptLayout.setVisibility(View.VISIBLE);
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
                capturedUncommittedReceipt = imageLocation.getAbsolutePath();
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
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
                final String name = nameField.getText().toString();
                if (name.isEmpty())
                {
                    Snackbar.make(v, "Name needed", Snackbar.LENGTH_LONG).show();
                    return;
                }

                final String budget = (String)budgetSpinner.getSelectedItem();
                if (budget == null)
                {
                    Snackbar.make(v, "No budget selected", Snackbar.LENGTH_LONG).show();
                    return;
                }

                final String account = accountField.getText().toString();
                // The account field is optional, so it is OK if it is empty

                final String valueStr = valueField.getText().toString();
                if (valueStr.isEmpty())
                {
                    Snackbar.make(v, "Value needed", Snackbar.LENGTH_LONG).show();
                    return;
                }

                double value;
                try
                {
                    value = Double.parseDouble(valueStr);
                }
                catch (NumberFormatException e)
                {
                    Snackbar.make(v, "Value invalid", Snackbar.LENGTH_LONG).show();
                    return;
                }

                final String note = noteField.getText().toString();
                // The note field is optional, so it is OK if it is empty

                EditText dateField = (EditText) findViewById(R.id.date);
                final String dateStr = dateField.getText().toString();
                final DateFormat dateFormatter = SimpleDateFormat.getDateInstance();
                long dateMs;
                try
                {
                    dateMs = dateFormatter.parse(dateStr).getTime();
                }
                catch (ParseException e)
                {
                    Snackbar.make(v, "Date invalid", Snackbar.LENGTH_LONG).show();
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


                DBHelper db = new DBHelper(TransactionViewActivity.this);

                if(updateTransaction)
                {
                    db.updateTransaction(transactionId, type, name, account,
                            budget, value, note, dateMs, receipt);

                }
                else
                {
                    db.insertTransaction(type, name, account, budget,
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
                DBHelper db = new DBHelper(this);
                db.deleteTransaction(transactionId);
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
        File receiptFile = new File(imageDir, imageFilename.toString() + ".png");

        return receiptFile;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if(requestCode == REQUEST_IMAGE_CAPTURE)
        {
            if (resultCode == RESULT_OK)
            {
                Log.i(TAG, "Image file saved: " + capturedUncommittedReceipt);
            }
            else
            {
                Log.e(TAG, "Failed to create receipt image: " + resultCode);
                // No image was actually created, simply forget the patch
                capturedUncommittedReceipt = null;
            }

            onResume();
        }
    }
}
