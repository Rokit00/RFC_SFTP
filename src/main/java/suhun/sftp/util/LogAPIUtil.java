package suhun.sftp.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Properties;

public class LogAPIUtil {
    private static final Logger log = LoggerFactory.getLogger(LogAPIUtil.class);
    Properties propertiesUtil = PropertiesUtil.getProperties();

    public void send(String state) {
        long startTime = System.currentTimeMillis();
        try {
            String encodedUrl = propertiesUtil.getProperty("API.URL").trim() + "?" +
                    "&sysname=" + propertiesUtil.getProperty("API.NAME") +
                    "&type3=" + state;
            log.debug(encodedUrl);

            sendHttp(encodedUrl);
            long result = System.currentTimeMillis() - startTime;
            log.debug("[SUCCESS] Log API SEND ({}sec)", result * 0.001);
        } catch (IOException e) {
            log.error("[LOG API SEND ERROR] {}", e.getMessage());
        }
    }

    private void sendHttp(String encodedUrl) throws IOException {
        URL url = new URL(encodedUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.connect();
        log.debug("Log API RESPONSE: [{}]", connection.getResponseMessage());
    }
}
