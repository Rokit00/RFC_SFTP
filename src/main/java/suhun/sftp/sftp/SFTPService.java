package suhun.sftp.sftp;

public interface SFTPService {
    void setConnect();
    void disconnect();
    String createFile(String fileName, String importParam);
    void getFilesFromBank();
}
