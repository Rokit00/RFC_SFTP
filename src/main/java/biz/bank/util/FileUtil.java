package biz.bank.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class FileUtil {
    private static final Logger log = LoggerFactory.getLogger(FileUtil.class);

    public void checkFileList() {
        File filePath = new File("C:\\Users\\admin\\Desktop\\bizSFTP\\upload");
        File[] files = filePath.listFiles(File::isFile);

        for (File file : files) {
            log.info(file.getName());
        }
    }

    public void deleteFiles(String fileName) {
        File file = new File("C:\\Users\\admin\\Desktop\\bizSFTP\\upload\\" + fileName);
        if (file.delete()) {
            log.info("[File is Deleted] {}", file.getName());
        } else {
            log.info("Can Not Delete File");
        }
    }
}

