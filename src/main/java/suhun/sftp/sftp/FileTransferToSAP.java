package suhun.sftp.sftp;

import suhun.sftp.util.CalendarUtil;
import suhun.sftp.util.PropertiesUtil;
import com.sap.conn.jco.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.Properties;

public class FileTransferToSAP extends Thread {
    private static final Logger log = LoggerFactory.getLogger(FileTransferToSAP.class);
    Properties properties = PropertiesUtil.getProperties();
    CalendarUtil calendarUtil = new CalendarUtil();

    @Override
    public void run() {
        try {
            WatchService watchService = FileSystems.getDefault().newWatchService();

            Path path = Paths.get(properties.getProperty("SFTP.LOCAL.DOWNLOAD.DIR"));

            path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);

            while (true) {
                WatchKey key = watchService.take();

                List<WatchEvent<?>> list = key.pollEvents();

                for (WatchEvent<?> event : list) {
                    WatchEvent.Kind<?> kind = event.kind();
                    Path context = (Path) event.context();

                    if (kind.equals(StandardWatchEventKinds.ENTRY_CREATE)) {
                        String fileName = context.toString();
                        String filePath = properties.getProperty("SFTP.LOCAL.DOWNLOAD.DIR") + File.separator + fileName;

                        while (true) {
                            try {
                                String fileContent = new String(Files.readAllBytes(Paths.get(filePath)));

                                JCoDestination jCoDestination = JCoDestinationManager.getDestination(properties.getProperty("JCO.SERVER.REPOSITORY_DESTINATION"));
                                JCoFunction jCoFunction = jCoDestination.getRepository().getFunction(properties.getProperty("JCO.FUNCTION"));

                                JCoParameterList importParameterList = jCoFunction.getImportParameterList();
                                JCoParameterList exportParameterList = jCoFunction.getExportParameterList();

                                importParameterList.setValue(properties.getProperty("JCO.PARAM.IMPORT0"), fileName);
                                importParameterList.setValue(properties.getProperty("JCO.PARAM.IMPORT1"), fileContent);
                                jCoFunction.execute(jCoDestination);

                                String exportParameter = exportParameterList.getValue(properties.getProperty("JCO.PARAM.EXPORT")).toString();
                                log.info("DEMON -> SAP [{}] [RESULT: {}]", fileName, exportParameter);

                                String calendar = calendarUtil.setDownloadCalendar();
                                Files.move(Paths.get(filePath), Paths.get(calendar + File.separator + fileName), StandardCopyOption.ATOMIC_MOVE);
                                log.info("[FILE MOVE] [PATH: {}] -> [PATH: {}]\r\n", filePath, calendar + File.separator + fileName);
                            } catch (IOException | JCoException e) {
                                log.error("DEMON -> SAP: [{}] \r\n", e.getMessage());
                            }
                        }
                    }
                }
                if (!key.reset()) break;
            }

            watchService.close();
        } catch (IOException | InterruptedException e) {
            log.error("Watch Service: [{}] \r\n", e.getMessage());
        }
    }
}