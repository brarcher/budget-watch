package protect.budgetwatch;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import java.io.File;

class DatabaseCleanupTask  extends AsyncTask<Void, Void, Void>
{
    private static final String TAG = "BudgetWatch";

    private final Activity activity;
    private final Long receiptPurgeCutoff;

    private ProgressDialog progress;

    public DatabaseCleanupTask(Activity activity)
    {
        super();
        this.activity = activity;
        receiptPurgeCutoff = null;
    }

    public DatabaseCleanupTask(Activity activity, long receiptPurgeCutoff)
    {
        super();
        this.activity = activity;
        this.receiptPurgeCutoff = receiptPurgeCutoff;
    }

    protected void onPreExecute()
    {
        progress = new ProgressDialog(activity);
        progress.setTitle(R.string.cleaning);

        progress.setOnDismissListener(new DialogInterface.OnDismissListener()
        {
            @Override
            public void onDismiss(DialogInterface dialog)
            {
                DatabaseCleanupTask.this.cancel(true);
            }
        });

        progress.show();
    }

    private void removeOldReceiptsFromTransactions(DBHelper db)
    {
        Cursor cursor = db.getTransactionsWithReceipts(receiptPurgeCutoff);

        while(cursor.moveToNext())
        {
            Transaction transaction = Transaction.toTransaction(cursor);
            File receipt = new File(transaction.receipt);
            boolean result = receipt.delete();
            if(result == false)
            {
                Log.i(TAG, "Failed to delete old receipt from transaction: " + transaction.id);
            }

            db.updateTransaction(transaction.id, transaction.type, transaction.description,
                    transaction.account, transaction.budget, transaction.value, transaction.note,
                    transaction.dateMs, /* no receipt */ "");
        }
        cursor.close();
    }

    private void correctTransactionsWithMissingReceipts(DBHelper db)
    {
        Cursor cursor = db.getTransactionsWithReceipts(null);

        while(cursor.moveToNext())
        {
            Transaction transaction = Transaction.toTransaction(cursor);
            if(transaction.receipt.isEmpty() == false)
            {
                File receipt = new File(transaction.receipt);
                if(receipt.isFile() == false)
                {
                    // This entry's receipt is missing. Cannot recover
                    // the receipt image, but can update database to remove
                    // the receipt from the transaction
                    db.updateTransaction(transaction.id, transaction.type, transaction.description,
                            transaction.account, transaction.budget, transaction.value, transaction.note,
                            transaction.dateMs, /* no receipt */ "");
                    Log.i(TAG, "Transaction " + transaction.id + " listed a receipt but it is missing, " +
                            "removing receipt");
                }
            }
        }
        cursor.close();
    }

    private void deleteOrphanedReceipts(DBHelper db)
    {
        File imageDir = activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if(imageDir == null || imageDir.exists() == false)
        {
            // There are no images to cleanup
            return;
        }

        Cursor cursor = db.getTransactionsWithReceipts(null);

        File [] files = imageDir.listFiles();
        if(files == null)
        {
            // If the directory could not be queried, fill in with
            // an empty list so nothing is processed.
            files = new File[0];
        }

        for(File receipt : files)
        {
            // Search for this receipt attached to a transaction
            boolean found = false;
            cursor.moveToPosition(-1);
            while(cursor.moveToNext())
            {
                Transaction transaction = Transaction.toTransaction(cursor);
                File transactionReceipt = new File(transaction.receipt);
                if(transactionReceipt.equals(receipt))
                {
                    // Found the receipt used in the database, ok to move on
                    found = true;
                    break;
                }
            }

            if(found == false)
            {
                Log.i(TAG, "Deleting orphaned receipt: " + receipt.getAbsolutePath());
                boolean result = receipt.delete();
                if(result == false)
                {
                    Log.w(TAG, "Failed to delete orphaned receipt: " + receipt.getAbsolutePath());
                }
            }
        }

        cursor.close();
    }

    protected Void doInBackground(Void... nothing)
    {
        DBHelper db = new DBHelper(activity);

        if(receiptPurgeCutoff != null)
        {
            removeOldReceiptsFromTransactions(db);
        }

        correctTransactionsWithMissingReceipts(db);
        deleteOrphanedReceipts(db);

        db.close();

        return null;
    }

    protected void onPostExecute(Void result)
    {
        progress.dismiss();
        Log.i(TAG, "Cleanup Complete");
    }

    protected void onCancelled()
    {
        progress.dismiss();
        Log.i(TAG, "Cleanup Cancelled");
    }
}
