package protect.budgetwatch;

/**
 * Exception thrown when something unexpected is
 * encountered with the format of data being
 * imported or exported.
 */
class FormatException extends Exception
{
    public FormatException(String message)
    {
        super(message);
    }

    public FormatException(String message, Exception rootCause)
    {
        super(message, rootCause);
    }
}
