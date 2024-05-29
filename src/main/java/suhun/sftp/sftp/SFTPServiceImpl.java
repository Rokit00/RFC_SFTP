package suhun.sftp.sftp;

import suhun.sftp.util.CalendarUtil;
import suhun.sftp.util.PropertiesUtil;

import com.jcraft.jsch.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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
        scheduler.scheduleAtFixedRate(this::download, 0, 1, TimeUnit.MINUTES);
    }

    @Override
    public void setConnect() {
        try {
            jSch.addIdentity(properties.getProperty("SFTP.PRIVATE_KEY"));
            session = jSch.getSession(properties.getProperty("SFTP.USERNAME"), properties.getProperty("SFTP.HOST"), Integer.parseInt(properties.getProperty("SFTP.PORT")));
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();

            channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();

            log.info("SFTP CONNECTED");
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
    public String upload(String fileName, String importParam) {
        long startTime = System.currentTimeMillis();
        setConnect();
        try {
            log.info("SAP -> DEMON: [{}] [{}]", fileName, importParam);
            File file = new File(properties.getProperty("SFTP.LOCAL.UPLOAD.DIR") + File.separator + fileName);

            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write(importParam);
            fileWriter.close();
            log.info("[FILE CREATED] [{}/{}]", file.getPath(), file.getName());

            String calendar = calendarUtil.setUploadCalendar();

            FileInputStream fileInputStream = new FileInputStream(file);
            channelSftp.put(fileInputStream, properties.getProperty("SFTP.REMOTE.UPLOAD.DIR") + "/" + fileName);
            fileInputStream.close();
            log.info("[UPLOAD] DEMON -> BANK [{}/{}]", file.getName(), properties.getProperty("SFTP.REMOTE.UPLOAD.DIR") + "/" + fileName);

            Files.move(Paths.get(properties.getProperty("SFTP.LOCAL.UPLOAD.DIR") + File.separator + fileName), Paths.get(calendar + File.separator + fileName), StandardCopyOption.ATOMIC_MOVE);
            log.info("[FILE MOVE] [{}] -> [{}]", file.getPath(), calendar + File.separator + fileName);

            return "S";
        } catch (IOException | SftpException e) {
            log.error("FILE UPLOAD FAILED: {}", e.getMessage());
            return "F";
        } finally {
            disconnect();
            long endTime = System.currentTimeMillis() - startTime;
            log.info("[SUCCESS] UPLOAD ({}sec)", endTime * 0.001);
        }
    }

    @Override
    public void download() {
        long startTime = System.currentTimeMillis();
        setConnect();
        try {
            Vector<ChannelSftp.LsEntry> fileList = channelSftp.ls(properties.getProperty("SFTP.REMOTE.DOWNLOAD.DIR"));

            for (ChannelSftp.LsEntry entry : fileList) {
                if (!entry.getAttrs().isDir()) {
                    String remoteFile = properties.getProperty("SFTP.REMOTE.DOWNLOAD.DIR") + "/" + entry.getFilename();
                    String localFile = properties.getProperty("SFTP.LOCAL.DOWNLOAD.DIR") + File.separator + entry.getFilename();
                    channelSftp.get(remoteFile, localFile);
                    log.info("[DOWNLOAD] BANK -> DEMON {}", entry.getFilename());
                }
            }
        } catch (SftpException e) {
            log.info("CAN NOT DOWNLOADED FILES: {} \r\n", e.getMessage());
        } finally {
            disconnect();
            long endTime = System.currentTimeMillis() - startTime;
            log.info("[SUCCESS DOWNLOAD] BANK -> DEMON ({}sec)\r\n", endTime * 0.001);
        }
    }
}