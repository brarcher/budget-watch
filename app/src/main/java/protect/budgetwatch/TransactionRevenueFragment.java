package protect.budgetwatch;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

public class TransactionRevenueFragment extends Fragment
{
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View layout = inflater.inflate(R.layout.list_layout, container, false);

        ListView listView = (ListView) layout.findViewById(R.id.list);
        final TextView helpText = (TextView) layout.findViewById(R.id.helpText);
        DBHelper dbhelper = new DBHelper(getContext());

        if(dbhelper.getTransactionCount(DBHelper.TransactionDbIds.REVENUE) > 0)
        {
            listView.setVisibility(View.VISIBLE);
            helpText.setVisibility(View.GONE);
        }
        else
        {
            listView.setVisibility(View.GONE);
            helpText.setVisibility(View.VISIBLE);
            helpText.setText(R.string.noRevenues);
        }

        Cursor expenseCursor = dbhelper.getRevenues();
        final TransactionCursorAdapter adapter = new TransactionCursorAdapter(getContext(), expenseCursor);
        listView.setAdapter(adapter);

        registerForContextMenu(listView);

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

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo)
    {
        super.onCreateContextMenu(menu, v, menuInfo);
        if (v.getId()==R.id.list)
        {
            MenuInflater inflater = getActivity().getMenuInflater();
            inflater.inflate(R.menu.edit_menu, menu);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item)
    {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        ListView listView = (ListView) getActivity().findViewById(R.id.list);
        Cursor selected = (Cursor)listView.getItemAtPosition(info.position);

        if(selected != null)
        {
            Transaction transaction = Transaction.toTransaction(selected);

            if(item.getItemId() == R.id.action_edit)
            {
                Intent i = new Intent(getActivity(), TransactionViewActivity.class);
                final Bundle b = new Bundle();
                b.putInt("id", transaction.id);
                b.putInt("type", DBHelper.TransactionDbIds.REVENUE);
                b.putBoolean("update", true);
                i.putExtras(b);
                startActivity(i);

                return true;
            }
        }

        return super.onContextItemSelected(item);
    }
}
