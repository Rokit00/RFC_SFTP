package suhun.sftp.sftp;

import com.jcraft.jsch.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import suhun.sftp.util.CalendarUtil;
import suhun.sftp.util.PropertiesUtil;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.Properties;

public class FileTransfer extends Thread {

    private static final Logger log = LoggerFactory.getLogger(FileTransfer.class);
    private Session session;
    private ChannelSftp channelSftp;
    private JSch jSch = new JSch();
    private final CalendarUtil calendarUtil = new CalendarUtil();
    private static final Properties properties = PropertiesUtil.getProperties();


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

                        uploadFile(fileName, filePath);
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
            byte[] fileContent = Files.readAllBytes(Paths.get(filePath));

            jSch.addIdentity(properties.getProperty("SFTP.PRIVATE_KEY"));
            session = jSch.getSession(properties.getProperty("SFTP.USERNAME"), properties.getProperty("SFTP.HOST"), Integer.parseInt(properties.getProperty("SFTP.PORT")));
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();

            channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();

            log.info("SFTP CONNECTED ({}sec)", (System.currentTimeMillis() - startTime) * 0.001);

            channelSftp.put(new ByteArrayInputStream(fileContent), properties.getProperty("SFTP.REMOTE.UPLOAD.DIR") + "/" + fileName);
            log.info("DEMON -> BANK [{}] [{}]", fileName, properties.getProperty("SFTP.REMOTE.UPLOAD.DIR") + "/" + fileName);

            String calendar = calendarUtil.setUploadCalendar();
            Files.move(Paths.get(filePath), Paths.get(calendar + File.separator + fileName), StandardCopyOption.ATOMIC_MOVE);
            log.debug("[FILE MOVE] [{}] -> [{}]", filePath, calendar + File.separator + fileName);
        } catch (IOException e) {
            log.error("FileTransfer IO: {}", e.getMessage());
        } catch (JSchException e) {
            log.info("COULD NOT CONNECT TO SFTP SERVER: {}", e.getMessage());
        } catch (SftpException e) {
            log.error("FileTransfer SFTP: {}", e.getMessage());
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
