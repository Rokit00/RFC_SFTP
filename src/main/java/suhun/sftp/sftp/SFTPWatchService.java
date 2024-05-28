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

public class SFTPWatchService {
    private static final Logger log = LoggerFactory.getLogger(SFTPWatchService.class);
    Properties properties = PropertiesUtil.getProperties();
    CalendarUtil calendarUtil = new CalendarUtil();

    public void start() {
        try {
            WatchService watchService = FileSystems.getDefault().newWatchService();

            Path path = Paths.get(properties.getProperty("SFTP.LOCAL.DOWNLOAD.DIR"));

            path.register(watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE,
                    StandardWatchEventKinds.ENTRY_MODIFY);

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
                            download(filePath, fileName);
                        }
                    }
                }

                if (!key.reset()) break;
            }

            watchService.close();
        } catch (IOException | InterruptedException e) {
            log.error("Watch Service IO OR Interrupted ERROR {} \r\n", e.getMessage());
        }
    }

    private void download(String filePath, String fileName) {
        try {
            String fileContent = new String(Files.readAllBytes(Paths.get(filePath)));

            JCoDestination jCoDestination = JCoDestinationManager.getDestination(properties.getProperty("jco.server.repository_destination"));
            JCoFunction jCoFunction = jCoDestination.getRepository().getFunction(properties.getProperty("jco.function"));

            JCoParameterList importParameterList = jCoFunction.getImportParameterList();
            JCoParameterList exportParameterList = jCoFunction.getExportParameterList();

            importParameterList.setValue(properties.getProperty("jco.param.import0"), fileName);
            importParameterList.setValue(properties.getProperty("jco.param.import1"), fileContent);
            log.info("From Bank File Name {}", fileName);

            jCoFunction.execute(jCoDestination);

            String result = exportParameterList.getValue(properties.getProperty("jco.param.export")).toString();
            log.info("IFRESULT [{}]", result);
            log.info("To SAP SUCCESS");

            String calendar = calendarUtil.setDownloadCalendar();
            Files.move(Paths.get(filePath), Paths.get(calendar + File.separator + fileName), StandardCopyOption.ATOMIC_MOVE);
            log.info("[FILE MOVE] [PATH] {} -> [PATH] {} \r\n", filePath, calendar + File.separator + fileName);
        } catch (IOException e) {
            log.error("File Access Failed: {} \r\n", e.getMessage());
        } catch (JCoException e) {
            log.error("Watch Service JCo ERROR {} \r\n", e.getMessage());
        }
    }
}