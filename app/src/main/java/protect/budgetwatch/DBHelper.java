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

    class TransactionDbIds
    {
        public static final String TABLE = "transactions";
        public static final String NAME = "_id";
        public static final String TYPE = "type";
        public static final String DESCRIPTION = "description";
        public static final String ACCOUNT = "account";
        public static final String BUDGET = "budget";
        public static final String VALUE = "value";
        public static final String NOTE = "note";
        public static final String DATE = "date";

        public static final int EXPENSE = 1;
        public static final int REVENUE = 2;
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
       // create table for transactions
        db.execSQL("create table " + TransactionDbIds.TABLE + "(" +
                TransactionDbIds.NAME + " INTEGER primary key autoincrement," +
                TransactionDbIds.TYPE + " INTEGER not null," +
                TransactionDbIds.DESCRIPTION + " TEXT not null," +
                TransactionDbIds.ACCOUNT + " TEXT," +
                TransactionDbIds.BUDGET + " TEXT," +
                TransactionDbIds.VALUE + " REAL not null," +
                TransactionDbIds.NOTE + " TEXT," +
                TransactionDbIds.DATE + " INTEGER not null)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
        // Do not support versioning yet
        db.execSQL("DROP TABLE IF EXISTS " + BudgetDbIds.TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + TransactionDbIds.TABLE);
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

    public Budget getBudget(final String name)
    {
        SQLiteDatabase db = getReadableDatabase();
        Cursor data = db.rawQuery("select * from " + BudgetDbIds.TABLE +
                " where " + BudgetDbIds.NAME + "=?", new String[]{name});

        Budget budget = null;

        if(data.getCount() == 1)
        {
            data.moveToFirst();
            String goalName = data.getString(data.getColumnIndexOrThrow(BudgetDbIds.NAME));
            int goalMax = data.getInt(data.getColumnIndexOrThrow(BudgetDbIds.MAX));

            budget = new Budget(goalName, goalMax, 0);
        }

        data.close();

        return budget;
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
                int current = getTotalForBudget(name, startDateMs, endDateMs);

                budgets.add(new Budget(name, max, current));
            } while(data.moveToNext());
        }

        data.close();

        return budgets;
    }

    public int getTotalForBudget(String budget, long startDateMs, long endDateMs)
    {
        int expense = getTotalForTransactionType(TransactionDbIds.EXPENSE, budget, startDateMs, endDateMs);
        int revenue = getTotalForTransactionType(TransactionDbIds.REVENUE, budget, startDateMs, endDateMs);

        return expense - revenue;
    }

    public int getTotalForTransactionType(int type, String budget, long startDateMs, long endDateMs)
    {
        SQLiteDatabase db = getReadableDatabase();
        Cursor data = db.rawQuery("select total(" + TransactionDbIds.VALUE + ") " +
                        "from " + TransactionDbIds.TABLE + " " +
                        "WHERE " + TransactionDbIds.BUDGET + " = ? AND " +
                        TransactionDbIds.TYPE + " = ? AND " +
                        TransactionDbIds.DATE + " >= ? AND " +
                        TransactionDbIds.DATE + " <= ?",
                new String[]{budget, Integer.toString(type),
                        Long.toString(startDateMs), Long.toString(endDateMs)});
        data.moveToFirst();
        int value = data.getInt(0);
        data.close();

        return value;
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

    public boolean insertTransaction(final int type, final String description, final String account, final String budget,
                                 final double value, final String note, final long dateInMs)
    {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(TransactionDbIds.TYPE, type);
        contentValues.put(TransactionDbIds.DESCRIPTION, description);
        contentValues.put(TransactionDbIds.ACCOUNT, account);
        contentValues.put(TransactionDbIds.BUDGET, budget);
        contentValues.put(TransactionDbIds.VALUE, value);
        contentValues.put(TransactionDbIds.NOTE, note);
        contentValues.put(TransactionDbIds.DATE, dateInMs);

        long newId = db.insert(TransactionDbIds.TABLE, null, contentValues);
        return (newId != 1);
    }

    public boolean updateTransaction(final int id, final int type, final String description,
                                     final String account, final String budget, final double value,
                                     final String note, final long dateInMs)
    {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(TransactionDbIds.TYPE, type);
        contentValues.put(TransactionDbIds.DESCRIPTION, description);
        contentValues.put(TransactionDbIds.ACCOUNT, account);
        contentValues.put(TransactionDbIds.BUDGET, budget);
        contentValues.put(TransactionDbIds.VALUE, value);
        contentValues.put(TransactionDbIds.NOTE, note);
        contentValues.put(TransactionDbIds.DATE, dateInMs);
        int rowsUpdated = db.update(TransactionDbIds.TABLE, contentValues, TransactionDbIds.NAME + "=?",
                new String[]{Integer.toString(id)});
        return (rowsUpdated == 1);
    }

    public Transaction getTransaction(final int id)
    {
        SQLiteDatabase db = getReadableDatabase();
        Cursor data = db.rawQuery("select * from " + TransactionDbIds.TABLE +
                " where " + TransactionDbIds.NAME + "=?", new String[]{String.format("%d", id)});

        Transaction transaction = null;

        if(data.getCount() == 1)
        {
            data.moveToFirst();
            transaction = Transaction.toTransaction(data);
        }

        data.close();

        return transaction;
    }

    public boolean deleteTransaction(final int id)
    {
        SQLiteDatabase db = getWritableDatabase();
        int rowsDeleted =  db.delete(TransactionDbIds.TABLE,
                TransactionDbIds.NAME + " = ? ",
                new String[]{Integer.valueOf(id).toString()});
        return (rowsDeleted == 1);
    }

    public Cursor getExpenses()
    {
        SQLiteDatabase db = getReadableDatabase();
        Cursor res =  db.rawQuery("select * from " + TransactionDbIds.TABLE + " where " +
                TransactionDbIds.TYPE + "=" + TransactionDbIds.EXPENSE +
                " ORDER BY " + TransactionDbIds.DATE + " DESC", null);
        return res;
    }

    public Cursor getRevenues()
    {
        SQLiteDatabase db = getReadableDatabase();
        Cursor res =  db.rawQuery("select * from " + TransactionDbIds.TABLE + " where " +
                TransactionDbIds.TYPE + "=" + TransactionDbIds.REVENUE +
                " ORDER BY " + TransactionDbIds.DATE + " DESC", null);
        return res;
    }
}
