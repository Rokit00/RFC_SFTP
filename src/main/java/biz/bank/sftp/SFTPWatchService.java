package biz.bank.sftp;

import biz.bank.util.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.Properties;

public class SFTPWatchService {
    private static final Logger log = LoggerFactory.getLogger(SFTPWatchService.class);
    Properties properties = PropertiesUtil.getProperties();

    public void start() {
        try {
            WatchService watchService = FileSystems.getDefault().newWatchService();

            Path path = Paths.get(properties.getProperty("SFTP.REMOTE.DOWNLOAD.DIR"));

            path.register(watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE,
                    StandardWatchEventKinds.ENTRY_MODIFY);

            while (true) {
                WatchKey key = watchService.take();

                List<WatchEvent<?>> list = key.pollEvents();

                for (WatchEvent<?> event : list) {
                    WatchEvent.Kind<?> kind = event.kind();
                    Path context = (Path) event.context();

                    if (kind.equals(StandardWatchEventKinds.ENTRY_CREATE)) {
                    }
                }

                if (!key.reset()) break;
            }

            watchService.close();
        } catch (IOException | InterruptedException e) {
            log.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
