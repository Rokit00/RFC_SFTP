package biz.bank;

import biz.bank.jco.JCoServerConfig;
import biz.bank.sftp.SFTPService;
import biz.bank.sftp.SFTPServiceImpl;

public class Main {
    public static void main(String[] args) {
        new JCoServerConfig();

        SFTPService sftpService = new SFTPServiceImpl();
        sftpService.batchDownload();
    }
}
