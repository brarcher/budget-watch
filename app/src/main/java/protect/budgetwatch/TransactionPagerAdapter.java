package protect.budgetwatch;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

class TransactionPagerAdapter extends FragmentStatePagerAdapter
{
    final int numTabs;

    public TransactionPagerAdapter(FragmentManager fm, int numTabs)
    {
        super(fm);
        this.numTabs = numTabs;
    }

    @Override
    public Fragment getItem(int position)
    {
        switch (position)
        {
            case 0:
                return new TransactionExpenseFragment();
            case 1:
                return new TransactionRevenueFragment();
            default:
                return null;
        }
    }

    @Override
    public int getCount()
    {
        return numTabs;
    }
}
