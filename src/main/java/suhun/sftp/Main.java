package suhun.sftp;

import suhun.sftp.jco.JCoServerConfig;
import suhun.sftp.sftp.FileTransfer;
import suhun.sftp.sftp.SFTPServiceImpl;
import suhun.sftp.sftp.FileTransferToSAP;

public class Main {
    public static void main(String[] args) {
        new JCoServerConfig();
        new SFTPServiceImpl();

        Thread thread = new FileTransferToSAP();
        thread.start();
        Thread thread1 = new FileTransfer();
        thread1.start();
    }
}
