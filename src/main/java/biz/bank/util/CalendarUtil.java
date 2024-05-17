package biz.bank.util;

import java.io.File;
import java.util.Calendar;
import java.util.Properties;

public class CalendarUtil {
    Properties properties = PropertiesUtil.getProperties();

    public String setUploadCalendar() {
        Calendar calendar = Calendar.getInstance();
        String dateString = String.format("%04d_%02d", calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1);

        File file = new File(properties.getProperty("SFTP.LOCAL.UPLOAD.DIR") + File.separator + "backup" + File.separator + dateString);
        file.mkdir();

        return file.getPath();
    }

    public String setDownloadCalendar() {
        Calendar calendar = Calendar.getInstance();
        String dateString = String.format("%04d_%02d", calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1);

        File file = new File(properties.getProperty("SFTP.LOCAL.DOWNLOAD.DIR") + File.separator + "backup" + File.separator + dateString);
        file.mkdir();

        return file.getPath();
    }
}
