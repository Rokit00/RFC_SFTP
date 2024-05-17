package biz.bank.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

public class PropertiesUtil {
    private static final Properties properties = new Properties();
    private static final Logger log = LoggerFactory.getLogger(PropertiesUtil.class);

    static {
        try (InputStream inputStream = Files.newInputStream(Paths.get("config.properties"))) {
            properties.load(inputStream);
        } catch (IOException e) {
            log.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public static Properties getProperties() {
        return properties;
    }
}
