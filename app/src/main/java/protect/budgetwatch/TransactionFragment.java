package protect.budgetwatch;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.TextView;

public class TransactionFragment extends Fragment
{
    private int _transactionType;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        Bundle arguments = getArguments();
        if(arguments == null || arguments.getInt("type", -1) == -1)
        {
            throw new IllegalStateException("Required argument 'type' is missing");
        }

        _transactionType = arguments.getInt("type");

        // If a budget has been passed then only transactions from that budget
        // will be displayed. Otherwise, all transactions wil be displayed.
        final Bundle b = getActivity().getIntent().getExtras();
        final String budgetToDisplay = b != null ? b.getString("budget", null) : null;

        View layout = inflater.inflate(R.layout.list_layout, container, false);
        ListView listView = (ListView) layout.findViewById(R.id.list);
        final TextView helpText = (TextView) layout.findViewById(R.id.helpText);
        DBHelper dbhelper = new DBHelper(getContext());

        Cursor cursor;
        if(_transactionType == DBHelper.TransactionDbIds.EXPENSE)
        {
            if(budgetToDisplay == null)
            {
                cursor = dbhelper.getExpenses();
            }
            else
            {
                cursor = dbhelper.getExpensesForBudget(budgetToDisplay);
            }
        }
        else
        {
            if(budgetToDisplay == null)
            {
                cursor = dbhelper.getRevenues();
            }
            else
            {
                cursor = dbhelper.getRevenuesForBudget(budgetToDisplay);
            }
        }

        if(cursor.getCount() > 0)
        {
            listView.setVisibility(View.VISIBLE);
            helpText.setVisibility(View.GONE);
        }
        else
        {
            listView.setVisibility(View.GONE);
            helpText.setVisibility(View.VISIBLE);

            String message;

            if(budgetToDisplay == null)
            {
                int stringId = (_transactionType == DBHelper.TransactionDbIds.EXPENSE) ?
                        R.string.noExpenses : R.string.noRevenues;
                message = getResources().getString(stringId);
            }
            else
            {
                int stringId = (_transactionType == DBHelper.TransactionDbIds.EXPENSE) ?
                        R.string.noExpensesForBudget : R.string.noRevenuesForBudget;
                String base = getResources().getString(stringId);
                message = String.format(base, budgetToDisplay);
            }

            helpText.setText(message);
        }

        final TransactionCursorAdapter adapter = new TransactionCursorAdapter(getContext(), cursor);
        listView.setAdapter(adapter);

        registerForContextMenu(listView);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                Cursor selected = (Cursor)parent.getItemAtPosition(position);
                Transaction transaction = Transaction.toTransaction(selected);

                Intent i = new Intent(view.getContext(), TransactionViewActivity.class);
                final Bundle b = new Bundle();
                b.putInt("id", transaction.id);
                b.putInt("type", _transactionType);
                b.putBoolean("view", true);
                i.putExtras(b);
                startActivity(i);
            }
        });

        return layout;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
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
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
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
                b.putInt("type", _transactionType);
                b.putBoolean("update", true);
                i.putExtras(b);
                startActivity(i);

                return true;
            }
        }

        return super.onContextItemSelected(item);
    }
}