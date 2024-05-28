package suhun.sftp.sftp;

public interface SFTPService {
    void setConnect();
    void disconnect();
    String  upload(String fileName, String importParam);
    void download();
}
