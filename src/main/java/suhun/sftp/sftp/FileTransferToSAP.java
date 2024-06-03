package suhun.sftp.sftp;

import suhun.sftp.util.CalendarUtil;
import suhun.sftp.util.PropertiesUtil;
import com.sap.conn.jco.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.*;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

public class FileTransferToSAP extends Thread {
    private static final Logger log = LoggerFactory.getLogger(FileTransferToSAP.class);
    Properties properties = PropertiesUtil.getProperties();
    CalendarUtil calendarUtil = new CalendarUtil();

    private Set<String> processedFiles = new HashSet<>();

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

                    if (kind == StandardWatchEventKinds.ENTRY_CREATE){
                        String fileName = context.toString();
                        String filePath = properties.getProperty("SFTP.LOCAL.DOWNLOAD.DIR") + File.separator + fileName;

                        if (!processedFiles.contains(fileName)) {
                            processFile(fileName, filePath);
                            processedFiles.add(fileName);
                        } else {
                            log.info("File {} has already been processed, skipping...", fileName);
                        }

                    }
                }
                key.reset();
            }
        } catch (IOException | InterruptedException e) {
            log.error("Watch Service error: {}", e.getMessage());
        }
    }

    private void processFile(String fileName, String filePath) {
        try {
            StringBuilder fileContent = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    fileContent.append(line).append("\n");
                }
            }

            JCoDestination jCoDestination = JCoDestinationManager.getDestination(properties.getProperty("JCO.SERVER.REPOSITORY_DESTINATION"));
            JCoFunction jCoFunction = jCoDestination.getRepository().getFunction(properties.getProperty("JCO.FUNCTION"));

            JCoParameterList importParameterList = jCoFunction.getImportParameterList();
            JCoParameterList exportParameterList = jCoFunction.getExportParameterList();

            importParameterList.setValue(properties.getProperty("JCO.PARAM.IMPORT0"), fileName);
            importParameterList.setValue(properties.getProperty("JCO.PARAM.IMPORT1"), fileContent.toString());
            jCoFunction.execute(jCoDestination);

            String exportParameter = exportParameterList.getValue(properties.getProperty("JCO.PARAM.EXPORT")).toString();
            log.info("DEMON -> SAP [{}] [RESULT: {}]", fileName, exportParameter);

            String calendar = calendarUtil.setDownloadCalendar();
            Files.move(Paths.get(filePath), Paths.get(calendar + File.separator + fileName), StandardCopyOption.ATOMIC_MOVE);
            log.info("[FILE MOVE] [PATH: {}] -> [PATH: {}]\r\n", filePath, calendar + File.separator + fileName);


        } catch (IOException e) {
            log.error("IO Exception while reading file: {}", e.getMessage());
        } catch (JCoException e) {
            log.error("{}", e.getMessage());
        }
    }
}