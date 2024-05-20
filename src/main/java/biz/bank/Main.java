package biz.bank;

import biz.bank.jco.JCoServerConfig;
import biz.bank.sftp.SFTPWatchService;

public class Main {
    public static void main(String[] args) {
        new JCoServerConfig();

        SFTPWatchService sftpWatchService = new SFTPWatchService();
        sftpWatchService.start();
    }
}
