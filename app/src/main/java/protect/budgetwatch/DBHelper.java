package protect.budgetwatch;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.LinkedList;
import java.util.List;

public class DBHelper extends SQLiteOpenHelper
{
    public static final String DATABASE_NAME = "BudgetWatch.db";
    public static final int DATABASE_VERSION = 1;

    class BudgetDbIds
    {
        public static final String TABLE = "budgets";
        public static final String NAME = "_id";
        public static final String MAX = "max";
    }

    public DBHelper(Context context)
    {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db)
    {
        // create table for budgets
        db.execSQL(
            "create table  " + BudgetDbIds.TABLE + "(" +
                BudgetDbIds.NAME + " text primary key," +
                BudgetDbIds.MAX + " INTEGER not null)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
        // Do not support versioning yet
        db.execSQL("DROP TABLE IF EXISTS " + BudgetDbIds.TABLE);
        onCreate(db);
    }

    public boolean insertBudget(final String name, final int max)
    {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(BudgetDbIds.NAME, name);
        contentValues.put(BudgetDbIds.MAX, max);
        final long newId = db.insert(BudgetDbIds.TABLE, null, contentValues);
        return (newId != 1);
    }

    public boolean updateBudget(final String name, final int max)
    {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(BudgetDbIds.MAX, max);
        int rowsUpdated = db.update(BudgetDbIds.TABLE, contentValues, BudgetDbIds.NAME + "=?",
                new String[]{name});
        return (rowsUpdated == 1);
    }

    public boolean deleteBudget (final String name)
    {
        SQLiteDatabase db = getWritableDatabase();
        int rowsDeleted =  db.delete(BudgetDbIds.TABLE,
                BudgetDbIds.NAME + " = ? ",
                new String[]{name});
        return (rowsDeleted == 1);
    }


    public List<Budget> getBudgets(long startDateMs, long endDateMs)
    {
        SQLiteDatabase db = getReadableDatabase();
        Cursor data = db.rawQuery("select * from " + BudgetDbIds.TABLE, null);

        LinkedList<Budget> budgets = new LinkedList<>();

        if(data.moveToFirst())
        {
            do
            {
                String name = data.getString(data.getColumnIndexOrThrow(BudgetDbIds.NAME));
                int max = data.getInt(data.getColumnIndexOrThrow(BudgetDbIds.MAX));

                // STOPSHIP: Get actual value
                int current = 0;

                budgets.add(new Budget(name, max, current));
            } while(data.moveToNext());
        }

        data.close();

        return budgets;
    }

    public List<String> getBudgetNames()
    {
        SQLiteDatabase db = getReadableDatabase();
        Cursor data = db.rawQuery("select " + BudgetDbIds.NAME + " from " + BudgetDbIds.TABLE, null);

        LinkedList<String> budgetNames = new LinkedList<>();

        if(data.moveToFirst())
        {
            do
            {
                String name = data.getString(data.getColumnIndexOrThrow(BudgetDbIds.NAME));

                budgetNames.add(name);
            } while(data.moveToNext());
        }

        data.close();

        return budgetNames;
    }
}
