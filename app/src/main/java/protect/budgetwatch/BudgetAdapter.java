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
    private final String FRACTION_FORMAT;

    public BudgetAdapter(Context context, List<Budget> items)
    {
        super(context, 0, items);

        FRACTION_FORMAT = context.getResources().getString(R.string.fraction);
    }

    static class ViewHolder
    {
        TextView budgetName;
        ProgressBar budgetBar;
        TextView budgetValue;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        // Get the data item for this position
        Budget item = getItem(position);

        ViewHolder holder;

        // Check if an existing view is being reused, otherwise inflate the view

        if (convertView == null)
        {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.budget_layout,
                    parent, false);

            holder = new ViewHolder();
            holder.budgetName = (TextView) convertView.findViewById(R.id.budgetName);
            holder.budgetBar = (ProgressBar) convertView.findViewById(R.id.budgetBar);
            holder.budgetValue = (TextView) convertView.findViewById(R.id.budgetValue);
            convertView.setTag(holder);
        }
        else
        {
            holder = (ViewHolder)convertView.getTag();
        }

        holder.budgetName.setText(item.name);

        holder.budgetBar.setMax(item.max);
        holder.budgetBar.setProgress(item.current);

        String fraction = String.format(FRACTION_FORMAT, item.current, item.max);

        holder.budgetValue.setText(fraction);

        return convertView;
    }
}
