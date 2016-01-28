package protect.budgetwatch;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

public class TransactionRevenueFragment extends Fragment
{
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View layout = inflater.inflate(R.layout.list_layout, container, false);

        ListView listView = (ListView) layout.findViewById(R.id.list);

        DBHelper dbhelper = new DBHelper(getContext());
        Cursor expenseCursor = dbhelper.getRevenues();
        final TransactionCursorAdapter adapter = new TransactionCursorAdapter(getContext(), expenseCursor);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                Cursor selected = (Cursor) parent.getItemAtPosition(position);
                Transaction transaction = Transaction.toTransaction(selected);

                Intent i = new Intent(view.getContext(), TransactionViewActivity.class);
                final Bundle b = new Bundle();
                b.putInt("id", transaction.id);
                b.putInt("type", DBHelper.TransactionDbIds.REVENUE);
                b.putBoolean("view", true);
                i.putExtras(b);
                startActivity(i);
            }
        });

        return layout;
    }
}