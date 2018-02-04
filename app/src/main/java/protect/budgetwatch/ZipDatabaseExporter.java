package protect.budgetwatch;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Class for exporting the database and its receipt images into a ZIP file
 */
public class ZipDatabaseExporter implements DatabaseExporter
{
    public void exportData(Context context, DBHelper db, Long startTimeMs, Long endTimeMs, OutputStream outStream, ImportExportProgressUpdater updater) throws IOException, InterruptedException
    {
        ZipOutputStream out = new ZipOutputStream(outStream);

        File receiptFolder = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if(receiptFolder != null)
        {
            File [] receipts = receiptFolder.listFiles();
            if(receipts != null)
            {
                byte [] data = new byte[1024];
                for(File receipt : receipts)
                {
                    ZipEntry receiptEntry = new ZipEntry(receipt.getName());
                    out.putNextEntry(receiptEntry);

                    FileInputStream image = new FileInputStream(receipt);

                    try
                    {
                        int count;
                        while ((count = image.read(data, 0, data.length)) != -1)
                        {
                            out.write(data, 0, count);
                        }
                    }
                    finally
                    {
                        image.close();
                    }
                }
            }
        }

        // Write the database to the zip file as a CSV file
        ZipEntry databaseEntry = new ZipEntry("database.csv");
        out.putNextEntry(databaseEntry);
        MultiFormatExporter.exportData(context, db, startTimeMs, endTimeMs, out, DataFormat.CSV, updater);
    }
}
