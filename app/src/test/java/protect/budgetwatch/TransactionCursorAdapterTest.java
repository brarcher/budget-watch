package protect.budgetwatch;

import android.app.Activity;
import android.database.Cursor;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 25)
public class TransactionCursorAdapterTest
{
    private Activity activity;
    private DBHelper db;

    @Before
    public void setUp()
    {
        // Output logs emitted during tests so they may be accessed
        ShadowLog.stream = System.out;

        activity = Robolectric.setupActivity(BudgetViewActivity.class);
        db = new DBHelper(activity);
    }

    @After
    public void tearDown()
    {
        db.close();
    }

    @Test
    public void checkTransaction()
    {
        final String DESCRIPTION = "description";
        final String ACCOUNT = "account";
        final String BUDGET = "budget";
        final double VALUE = 100.50;
        final String NOTE = "note";
        final long DATE = 0;
        final String RECEIPT = "receipt";

        for(boolean hasReceipt : new boolean [] {false, true})
        {
            for(boolean hasNote : new boolean [] {false, true})
            {
                db.insertTransaction(DBHelper.TransactionDbIds.EXPENSE, DESCRIPTION, ACCOUNT, BUDGET,
                        VALUE, hasNote ? NOTE : "", DATE, hasReceipt ? RECEIPT : "");
                Cursor cursor = db.getExpenses();
                cursor.moveToFirst();
                int transactionId = cursor.getInt(
                        cursor.getColumnIndexOrThrow(DBHelper.TransactionDbIds.NAME));

                TransactionCursorAdapter adapter = new TransactionCursorAdapter(activity, cursor);
                View view = adapter.newView(activity, cursor, null);
                adapter.bindView(view, activity, cursor);
                cursor.close();

                db.deleteTransaction(transactionId);

                TextView nameField = (TextView) view.findViewById(R.id.name);
                TextView valueField = (TextView) view.findViewById(R.id.value);
                TextView dateField = (TextView) view.findViewById(R.id.date);
                TextView budgetField = (TextView) view.findViewById(R.id.budget);
                ImageView receiptIcon = (ImageView) view.findViewById(R.id.receiptIcon);
                View noteLayout = view.findViewById(R.id.noteLayout);
                TextView noteField = (TextView) view.findViewById(R.id.note);

                assertEquals(hasReceipt ? View.VISIBLE : View.GONE, receiptIcon.getVisibility());
                assertEquals(DESCRIPTION, nameField.getText().toString());
                assertEquals(BUDGET, budgetField.getText().toString());

                String expectedValue = String.format(Locale.US, "%.2f", VALUE);
                assertEquals(expectedValue, valueField.getText().toString());

                // As the date field may be converted using the current locale,
                // simply check that there is something there.
                assertTrue(dateField.getText().length() > 0);

                assertEquals(hasNote ? View.VISIBLE : View.GONE, noteLayout.getVisibility());
                assertEquals(hasNote ? NOTE : "", noteField.getText());
            }
        }
    }
}
