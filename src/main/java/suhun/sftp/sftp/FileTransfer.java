package suhun.sftp.sftp;

import com.jcraft.jsch.*;
import com.sap.conn.jco.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import suhun.sftp.util.CalendarUtil;
import suhun.sftp.util.PropertiesUtil;

import java.io.*;
import java.nio.file.*;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

public class FileTransfer extends Thread {

    private static final Logger log = LoggerFactory.getLogger(FileTransfer.class);
    private Session session;
    private ChannelSftp channelSftp;
    private JSch jSch = new JSch();
    private final CalendarUtil calendarUtil = new CalendarUtil();
    private static final Properties properties = PropertiesUtil.getProperties();

    private Set<String> processedFiles = new HashSet<>();

    @Override
    public void run() {
        try {
            Path directory = Paths.get(properties.getProperty("SFTP.LOCAL.UPLOAD.DIR"));
            WatchService watchService = FileSystems.getDefault().newWatchService();
            directory.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);

            while (true) {
                WatchKey key = watchService.take();
                List<WatchEvent<?>> list = key.pollEvents();

                for (WatchEvent<?> event : list) {
                    WatchEvent.Kind<?> kind = event.kind();
                    Path context = (Path) event.context();

                    if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                        String fileName = context.toString();
                        String filePath = properties.getProperty("SFTP.LOCAL.UPLOAD.DIR") + File.separator + fileName;

                        if (!processedFiles.contains(fileName)) {
                            uploadFile(fileName, filePath);
                            processedFiles.add(fileName);
                        } else {
                            log.info("File {} has already been processed, skipping...", fileName);
                        }
                    }
                }
                key.reset();
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void uploadFile(String fileName, String filePath) {
        long startTime = System.currentTimeMillis();

        try {
            StringBuilder fileContent = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    fileContent.append(line).append("\n");
                }
            }
            jSch.addIdentity(properties.getProperty("SFTP.PRIVATE_KEY"));
            session = jSch.getSession(properties.getProperty("SFTP.USERNAME"), properties.getProperty("SFTP.HOST"), Integer.parseInt(properties.getProperty("SFTP.PORT")));
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();

            channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();

            log.debug("SFTP CONNECTED ({}sec)", (System.currentTimeMillis() - startTime) * 0.001);

            channelSftp.put(fileContent.toString(), properties.getProperty("SFTP.REMOTE.UPLOAD.DIR") + "/" + fileName);

            String calendar = calendarUtil.setUploadCalendar();
            Files.move(Paths.get(filePath), Paths.get(calendar + File.separator + fileName), StandardCopyOption.ATOMIC_MOVE);
            log.info("DEMON -> BANK [{}] [{}]", fileName, properties.getProperty("SFTP.REMOTE.UPLOAD.DIR") + "/" + fileName);

            File resultFile = new File(properties.getProperty("SFTP.LOCAL.DOWNLOAD.DIR") + File.separator + fileName + ".OK");
            try {
                FileWriter fileWriter = new FileWriter(resultFile);
                fileWriter.write("resultFile");
                fileWriter.close();
            } catch (IOException ex) {
                log.error(ex.getMessage());
            }

            JCoDestination jCoDestination = JCoDestinationManager.getDestination(properties.getProperty("JCO.SERVER.REPOSITORY_DESTINATION"));
            JCoFunction jCoFunction = jCoDestination.getRepository().getFunction(properties.getProperty("JCO.FUNCTION"));

            JCoParameterList importParameterList = jCoFunction.getImportParameterList();

            importParameterList.setValue(properties.getProperty("JCO.PARAM.IMPORT0"), fileName + ".OK");
            jCoFunction.execute(jCoDestination);
            log.info("DEMON -> SAP [FILE RESULT: {}]", fileName + ".OK");
        } catch (IOException e) {
            log.error("IO ERROR {}", e.getMessage());
        } catch (JSchException e) {
            log.error("JSch ERROR {}", e.getMessage());
        } catch (SftpException e) {
            File resultFile = new File(properties.getProperty("SFTP.LOCAL.DOWNLOAD.DIR") + File.separator + fileName + ".ERROR");
            try {
                FileWriter fileWriter = new FileWriter(resultFile);
                fileWriter.write("resultFile");
                fileWriter.close();
            } catch (IOException ex) {
                log.error(ex.getMessage());
            }

            JCoDestination jCoDestination = null;
            try {
                jCoDestination = JCoDestinationManager.getDestination(properties.getProperty("JCO.SERVER.REPOSITORY_DESTINATION"));
                JCoFunction jCoFunction = jCoDestination.getRepository().getFunction(properties.getProperty("JCO.FUNCTION"));

                JCoParameterList importParameterList = jCoFunction.getImportParameterList();

                importParameterList.setValue(properties.getProperty("JCO.PARAM.IMPORT0"), fileName + ".ERROR");
                jCoFunction.execute(jCoDestination);
                log.error("SFTP ERROR {}", e.getMessage());
            } catch (JCoException ex) {
                log.error(ex.getMessage());
            }
        } catch (JCoException e) {
            log.error("JCO ERROR {}", e.getMessage());
        } finally {
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
            if (channelSftp != null && channelSftp.isConnected()) {
                channelSftp.disconnect();
            }
        }
    }
}
