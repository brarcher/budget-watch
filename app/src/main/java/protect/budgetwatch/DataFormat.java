package protect.budgetwatch;

public enum DataFormat
{
    CSV,
    JSON,
    ZIP,
    ;

    /**
     * @return the file extension name for this data format.
     */
    public String extension()
    {
        return this.name().toLowerCase();
    }
}
