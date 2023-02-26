package protect.budgetwatch;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

class TransactionPagerAdapter extends FragmentStatePagerAdapter
{
    private final int numTabs;
    private final String search;

    public TransactionPagerAdapter(FragmentManager fm, String search, int numTabs)
    {
        super(fm);
        this.search = search;
        this.numTabs = numTabs;
    }

    @Override
    public Fragment getItem(int position)
    {
        Fragment fragment = new TransactionFragment();
        Bundle arguments = new Bundle();
        int transactionType = (position == 0) ?
                DBHelper.TransactionDbIds.EXPENSE : DBHelper.TransactionDbIds.REVENUE;
        arguments.putInt("type", transactionType);
        if(search != null)
        {
            arguments.putString("search", search);
        }

        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public int getCount()
    {
        return numTabs;
    }
}
