package protect.budgetwatch;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

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
