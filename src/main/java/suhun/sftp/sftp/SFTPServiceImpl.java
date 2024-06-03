package suhun.sftp.sftp;

import suhun.sftp.util.CalendarUtil;
import suhun.sftp.util.PropertiesUtil;

import com.jcraft.jsch.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SFTPServiceImpl implements SFTPService {
    private final JSch jSch = new JSch();
    private final CalendarUtil calendarUtil = new CalendarUtil();
    private final Properties properties = PropertiesUtil.getProperties();
    private Session session;
    private ChannelSftp channelSftp;
    private static final Logger log = LoggerFactory.getLogger(SFTPServiceImpl.class);
    private ScheduledExecutorService scheduler;

    public SFTPServiceImpl() {
        scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(this::getFilesFromBank, 0, Integer.parseInt(properties.getProperty("SFTP.DOWNLOAD.INTERVAL")), TimeUnit.SECONDS);
    }

    @Override
    public void setConnect() {
        long startTime = System.currentTimeMillis();
        try {
            jSch.addIdentity(properties.getProperty("SFTP.PRIVATE_KEY"));
            session = jSch.getSession(properties.getProperty("SFTP.USERNAME"), properties.getProperty("SFTP.HOST"), Integer.parseInt(properties.getProperty("SFTP.PORT")));
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();

            channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();

            long endTime = System.currentTimeMillis() - startTime;
            log.info("SFTP CONNECTED ({}sec)", endTime * 0.001);
        } catch (JSchException e) {
            log.info("COULD NOT CONNECT TO SFTP SERVER: {}", e.getMessage());
        }
    }

    @Override
    public void disconnect() {
        channelSftp.disconnect();
        session.disconnect();
        log.info("SFTP DISCONNECTED");
    }

    @Override
    public String createFile(String fileName, String fileContent) {
        long startTime = System.currentTimeMillis();
        try {
            File file = new File(properties.getProperty("SFTP.LOCAL.UPLOAD.DIR") + File.separator + fileName);

            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write(fileContent);
            fileWriter.close();

            long endTime = System.currentTimeMillis() - startTime;
            log.debug("[FILE CREATED] [{}] [{}] ({}sec)", file.getName(), file.getPath(), endTime * 0.001);

            return "S";
        } catch (IOException e) {
            log.error("CREATE FILE FAILED: {}", e.getMessage());
            return "F";
        }
    }

    @Override
    public void getFilesFromBank() {
        long startTime = System.currentTimeMillis();
        setConnect();
        try {
            Vector<ChannelSftp.LsEntry> fileList = channelSftp.ls(properties.getProperty("SFTP.REMOTE.DOWNLOAD.DIR"));

            for (ChannelSftp.LsEntry entry : fileList) {
                if (!entry.getAttrs().isDir()) {
                    String remoteFile = properties.getProperty("SFTP.REMOTE.DOWNLOAD.DIR") + "/" + entry.getFilename();
                    String localFile = properties.getProperty("SFTP.LOCAL.DOWNLOAD.DIR") + File.separator + entry.getFilename();
                    channelSftp.get(remoteFile, localFile);
                    long endTime = System.currentTimeMillis() - startTime;
                    log.info("[DOWNLOAD] BANK -> DEMON [{}] ({}sec)\r\n", entry.getFilename(), endTime * 0.001);
                }
            }
        } catch (SftpException e) {
            log.error("[DOWNLOAD] [{}] \r\n", e.getMessage());
        } finally {
            disconnect();
        }
    }
}
