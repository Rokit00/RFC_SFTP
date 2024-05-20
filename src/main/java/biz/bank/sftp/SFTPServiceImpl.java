package biz.bank.sftp;

import biz.bank.util.CalendarUtil;
import biz.bank.util.PropertiesUtil;

import com.jcraft.jsch.*;
import com.sap.conn.jco.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class SFTPServiceImpl implements SFTPService {
    private final JSch jSch = new JSch();
    private final CalendarUtil calendarUtil = new CalendarUtil();
    private final Properties properties = PropertiesUtil.getProperties();
    private Session session;
    private ChannelSftp channelSftp;
    private static final Logger log = LoggerFactory.getLogger(SFTPServiceImpl.class);

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
            log.info("Could not connect to SFTP server", e);
        }
    }

    @Override
    public void disconnect() {
        channelSftp.disconnect();
        session.disconnect();
    }

    @Override
    public String upload(String fileName, String importParam) {
        long startTime = System.currentTimeMillis();
        setConnect();
        try {
            File file = new File(properties.getProperty("SFTP.LOCAL.UPLOAD.DIR") + File.separator + fileName);

            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write(importParam);
            fileWriter.close();
            log.info("[FILE CREATED] {} [PATH] {}", file.getName(), file.getPath());

            String calendar = calendarUtil.setUploadCalendar();

            FileInputStream fileInputStream = new FileInputStream(file);
            channelSftp.put(fileInputStream, properties.getProperty("SFTP.REMOTE.UPLOAD.DIR") + "/" + fileName);
            fileInputStream.close();
            log.info("[FILE UPLOADED] {} [PATH] {}", file.getName(), properties.getProperty("SFTP.REMOTE.UPLOAD.DIR") + "/" + fileName);

            Files.move(
                    Paths.get(properties.getProperty("SFTP.LOCAL.UPLOAD.DIR") + File.separator + fileName),
                    Paths.get(calendar + File.separator + fileName),
                    StandardCopyOption.ATOMIC_MOVE);
            log.info("[FILE MOVE] [PATH] {} -> [PATH] {}", file.getPath(), calendar + File.separator + fileName);

            long endTime = System.currentTimeMillis() - startTime;
            log.info("SFTP UPLOADED SUCCESS [{}sec]", endTime * 0.001);
            return "S";
        } catch (IOException | SftpException e) {
            log.error(e.getMessage());
            return "F";
        } finally {
            disconnect();
        }
    }

    @Override
    public void download() {
        long startTime = System.currentTimeMillis();
        setConnect();
        try {
            JCoDestination jCoDestination = JCoDestinationManager.getDestination(properties.getProperty("jco.server.repository_destination"));
            JCoFunction jCoFunction = jCoDestination.getRepository().getFunction(properties.getProperty("jco.function"));

            Vector<ChannelSftp.LsEntry> fileList = channelSftp.ls(properties.getProperty("SFTP.REMOTE.DOWNLOAD.DIR"));

            for (ChannelSftp.LsEntry entry : fileList) {
                if (!entry.getAttrs().isDir()) {
                    String remoteFile = properties.getProperty("SFTP.REMOTE.DOWNLOAD.DIR") + "/" + entry.getFilename();
                    String localFile = properties.getProperty("SFTP.LOCAL.DOWNLOAD.DIR") + File.separator + entry.getFilename();
                    channelSftp.get(remoteFile, localFile);
                    log.info("[FILE DOWNLOADED] {}", entry.getFilename());
                }
            }

            long endTime = System.currentTimeMillis() - startTime;
            log.info("SFTP DOWNLOAD SUCCESS [{}sec] \r\n", endTime * 0.001);
        } catch (SftpException e) {
            log.info("CAN NOT FILE DOWNLOADED: {} \r\n", e.getMessage());
        } catch (JCoException e) {
            log.info("SFTP DOWNLOAD JCO FUNCTION ERROR: {} \r\n", e.getMessage());
        } finally {
            disconnect();
        }
    }
}