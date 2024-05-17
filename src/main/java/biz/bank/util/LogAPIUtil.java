package biz.bank.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Properties;

public class LogAPIUtil {
    private static final Logger log = LoggerFactory.getLogger(LogAPIUtil.class);
    Properties propertiesUtil = PropertiesUtil.getProperties();

    public void send(String state) {
        try {
            String encodedUrl = propertiesUtil.getProperty("API.URL").trim() + "?" +
                    "&sysname=" + URLEncoder.encode(propertiesUtil.getProperty("API.NAME"), "UTF-8") +
                    "&type1=" + URLEncoder.encode(state, "UTF-8");

            sendHttp(encodedUrl);
        } catch (IOException e) {
            log.error("[LOG API SEND ERROR] {}", e.getMessage());
        }
        log.info("HTTP API SUCCESS");
    }

    private void sendHttp(String encodedUrl) throws IOException {
        URL url = new URL(encodedUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "application/xml,text/xml,application/xhtml+xml");
        connection.setRequestProperty("Accept-Charset", "UTF-8");
        connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");

        connection.connect();

        connection.disconnect();
    }
}
