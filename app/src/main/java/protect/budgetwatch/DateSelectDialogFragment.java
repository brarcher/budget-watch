package protect.budgetwatch;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.DatePicker;
import android.widget.Toast;

public class DateSelectDialogFragment extends DialogFragment {

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        final View view = LayoutInflater.from(getActivity()).inflate(R.layout.budget_date_picker_layout, null, false);

        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.budgetDateRangeHelp)
                .setView(view)
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        dialog.cancel();
                    }
                })
                .setPositiveButton(R.string.set, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        DatePicker startDatePicker = view.findViewById(R.id.startDate);
                        DatePicker endDatePicker = view.findViewById(R.id.endDate);

                        long startOfBudgetMs = CalendarUtil.getStartOfDayMs(startDatePicker.getYear(),
                                startDatePicker.getMonth(), startDatePicker.getDayOfMonth());
                        long endOfBudgetMs = CalendarUtil.getEndOfDayMs(endDatePicker.getYear(),
                                endDatePicker.getMonth(), endDatePicker.getDayOfMonth());

                        if (startOfBudgetMs > endOfBudgetMs)
                        {
                            Toast.makeText(getActivity(), R.string.startDateAfterEndDate, Toast.LENGTH_LONG).show();
                            return;
                        }

                        Intent intent = new Intent(getActivity(), BudgetActivity.class);
                        intent.setFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_TASK_ON_HOME);

                        Bundle bundle = new Bundle();
                        bundle.putLong("budgetStart", startOfBudgetMs);
                        bundle.putLong("budgetEnd", endOfBudgetMs);
                        intent.putExtras(bundle);
                        startActivity(intent);

                        getActivity().finish();
                    }
                })
                .create();
    }
}
