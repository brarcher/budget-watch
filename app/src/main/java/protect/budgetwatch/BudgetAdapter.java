package protect.budgetwatch;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.List;

class BudgetAdapter extends ArrayAdapter<Budget>
{
    public BudgetAdapter(Context context, List<Budget> items)
    {
        super(context, 0, items);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        // Get the data item for this position
        Budget item = getItem(position);

        // Check if an existing view is being reused, otherwise inflate the view

        if (convertView == null)
        {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.budget_layout,
                    parent, false);
        }

        TextView budgetName = (TextView) convertView.findViewById(R.id.budgetName);
        ProgressBar budgetBar = (ProgressBar) convertView.findViewById(R.id.budgetBar);
        TextView budgetValue = (TextView) convertView.findViewById(R.id.budgetValue);

        budgetName.setText(item.name);

        budgetBar.setMax(item.max);
        budgetBar.setProgress(item.current);

        String fractionFormat = getContext().getResources().getString(R.string.fraction);
        String fraction = String.format(fractionFormat, item.current, item.max);

        budgetValue.setText(fraction);

        return convertView;
    }
}
