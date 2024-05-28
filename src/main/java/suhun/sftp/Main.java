package suhun.sftp;

import suhun.sftp.jco.JCoServerConfig;
import suhun.sftp.sftp.SFTPService;
import suhun.sftp.sftp.SFTPServiceImpl;
import suhun.sftp.sftp.SFTPWatchService;

public class Main {
    public static void main(String[] args) {
        new JCoServerConfig();

        SFTPService sftpService = new SFTPServiceImpl();

        SFTPWatchService sftpWatchService = new SFTPWatchService();
        sftpWatchService.start();
    }
}
