package protect.budgetwatch;

public enum DataFormat
{
    CSV,
    ZIP,
    ;

    /**
     * Returns the file extension name for this data format.
     * @return
     */
    public String extension()
    {
        return this.name().toLowerCase();
    }
}
