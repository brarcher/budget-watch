package protect.budgetwatch;

import android.database.Cursor;

public class Transaction
{
    public final int id;
    public final int type;
    public final String description;
    public final String account;
    public final String budget;
    public final double value;
    public final String note;
    public final long dateMs;
    public final String receipt;

    private Transaction(final int id, final int type, final String description, final String account,
                       final String budget, final double value, final String note, final long dateMs,
                       final String receipt)
    {
        this.id = id;
        this.type = type;
        this.description = description;
        this.account = account;
        this.budget = budget;
        this.value = value;
        this.note = note;
        this.dateMs = dateMs;
        this.receipt = receipt;
    }

    private static String toBlankIfNull(final String string)
    {
        if(string != null)
        {
            return string;
        }
        else
        {
            return "";
        }
    }

    public static Transaction toTransaction(Cursor cursor)
    {
        int id = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.TransactionDbIds.NAME));
        int type = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.TransactionDbIds.TYPE));
        String description = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.TransactionDbIds.DESCRIPTION));
        String account = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.TransactionDbIds.ACCOUNT));
        String budget = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.TransactionDbIds.BUDGET));
        double value = cursor.getDouble(cursor.getColumnIndexOrThrow(DBHelper.TransactionDbIds.VALUE));
        String note = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.TransactionDbIds.NOTE));
        long dateMs = cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.TransactionDbIds.DATE));
        String receipt = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.TransactionDbIds.RECEIPT));

        return new Transaction(id, type, toBlankIfNull(description), toBlankIfNull(account),
                toBlankIfNull(budget), value, toBlankIfNull(note), dateMs,
                toBlankIfNull(receipt));
    }
}