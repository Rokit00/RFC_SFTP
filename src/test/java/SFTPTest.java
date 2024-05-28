import suhun.sftp.sftp.SFTPService;
import suhun.sftp.sftp.SFTPServiceImpl;
import org.junit.jupiter.api.Test;

public class SFTPTest {

    @Test
    void sftp() {
        SFTPService sftpService = new SFTPServiceImpl();
        sftpService.upload("a", "b");
    }
}
