package protect.budgetwatch;

public enum DataFormat
{
    CSV("text/csv"),
    JSON("application/json"),
    ZIP("application/zip"),
    ;

    private final String mimetype;

    DataFormat(String mimetype)
    {
        this.mimetype = mimetype;
    }

    /**
     * @return the file extension name for this data format.
     */
    public String extension()
    {
        return this.name().toLowerCase();
    }

    /**
     * @return the mime type for this data format.
     */
    public String mimetype()
    {
        return mimetype;
    }
}
