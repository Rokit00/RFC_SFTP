import biz.bank.sftp.SFTPService;
import biz.bank.sftp.SFTPServiceImpl;
import org.junit.jupiter.api.Test;

public class SFTPTest {

    @Test
    void sftp() {
        SFTPService sftpService = new SFTPServiceImpl();
        sftpService.upload("a", "b");
    }
}
