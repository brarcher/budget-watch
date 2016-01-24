package protect.budgetwatch;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

class TransactionCursorAdapter extends CursorAdapter
{
    public TransactionCursorAdapter(Context context, Cursor cursor)
    {
        super(context, cursor, 0);
    }

    // The newView method is used to inflate a new view and return it,
    // you don't bind any data to the view at this point.
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent)
    {
        return LayoutInflater.from(context).inflate(R.layout.transaction_layout, parent, false);
    }

    // The bindView method is used to bind all data to a given view
    // such as setting the text on a TextView.
    @Override
    public void bindView(View view, Context context, Cursor cursor)
    {
        // Find fields to populate in inflated template
        TextView nameField = (TextView) view.findViewById(R.id.name);
        TextView valueField = (TextView) view.findViewById(R.id.value);
        TextView dateField = (TextView) view.findViewById(R.id.date);
        TextView budgetField = (TextView) view.findViewById(R.id.budget);

        // Extract properties from cursor
        Transaction transaction = Transaction.toTransaction(cursor);

        // Populate fields with extracted properties
        nameField.setText(transaction.description);
        valueField.setText(String.format("%.2f", transaction.value));
        budgetField.setText(transaction.budget);

        final DateFormat dateFormatter = SimpleDateFormat.getDateInstance();
        dateField.setText(dateFormatter.format(transaction.dateMs));
    }
}
