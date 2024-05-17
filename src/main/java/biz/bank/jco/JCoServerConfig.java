package biz.bank.jco;

import biz.bank.util.PropertiesUtil;
import com.sap.conn.jco.JCoException;
import com.sap.conn.jco.ext.DestinationDataProvider;
import com.sap.conn.jco.ext.ServerDataProvider;
import com.sap.conn.jco.server.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Properties;

public class JCoServerConfig {
    Properties properties = PropertiesUtil.getProperties();
    private static final Logger log = LoggerFactory.getLogger(JCoServerConfig.class);

    public JCoServerConfig() {
        long startTime = System.currentTimeMillis();
        if (properties.getProperty("jco.type").equals("MESSAGE")) {
            log.info("MESSAGE SERVER");
            properties.setProperty(DestinationDataProvider.JCO_MSHOST, properties.getProperty("jco.client.mshost"));
            properties.setProperty(DestinationDataProvider.JCO_MSSERV, properties.getProperty("jco.client.msserv"));
            properties.setProperty(DestinationDataProvider.JCO_R3NAME, properties.getProperty("jco.client.r3name"));
            properties.setProperty(DestinationDataProvider.JCO_GROUP, properties.getProperty("jco.client.group"));

            properties.setProperty(DestinationDataProvider.JCO_ASHOST, properties.getProperty("jco.client.ashost"));
            properties.setProperty(DestinationDataProvider.JCO_SYSNR, properties.getProperty("jco.client.sysnr"));
            properties.setProperty(DestinationDataProvider.JCO_CLIENT, properties.getProperty("jco.client.client"));
            properties.setProperty(DestinationDataProvider.JCO_USER, properties.getProperty("jco.client.user"));
            properties.setProperty(DestinationDataProvider.JCO_PASSWD, properties.getProperty("jco.client.passwd"));
            properties.setProperty(DestinationDataProvider.JCO_LANG, properties.getProperty("jco.client.lang"));
            properties.setProperty(DestinationDataProvider.JCO_POOL_CAPACITY, properties.getProperty("jco.destination.pool_capacity"));
            properties.setProperty(DestinationDataProvider.JCO_PEAK_LIMIT, properties.getProperty("jco.destination.peak_limit"));

            properties.setProperty(ServerDataProvider.JCO_GWHOST, properties.getProperty("jco.server.gwhost"));
            properties.setProperty(ServerDataProvider.JCO_GWSERV, properties.getProperty("jco.server.gwserv"));
            properties.setProperty(ServerDataProvider.JCO_PROGID, properties.getProperty("jco.server.progid"));
            properties.setProperty(ServerDataProvider.JCO_CONNECTION_COUNT, properties.getProperty("jco.server.connection_count"));
            properties.setProperty(ServerDataProvider.JCO_REP_DEST, properties.getProperty("jco.server.repository_destination"));

            createDataFile(properties.getProperty("jco.server.progid"), properties, ".JcoServer");
            createDataFile(properties.getProperty("jco.server.repository_destination"), properties, ".JcoDestination");
        } else {
            properties.setProperty(DestinationDataProvider.JCO_ASHOST, properties.getProperty("jco.client.ashost"));
            properties.setProperty(DestinationDataProvider.JCO_SYSNR, properties.getProperty("jco.client.sysnr"));
            properties.setProperty(DestinationDataProvider.JCO_CLIENT, properties.getProperty("jco.client.client"));
            properties.setProperty(DestinationDataProvider.JCO_USER, properties.getProperty("jco.client.user"));
            properties.setProperty(DestinationDataProvider.JCO_PASSWD, properties.getProperty("jco.client.passwd"));
            properties.setProperty(DestinationDataProvider.JCO_LANG, properties.getProperty("jco.client.lang"));
            properties.setProperty(DestinationDataProvider.JCO_POOL_CAPACITY, properties.getProperty("jco.destination.pool_capacity"));
            properties.setProperty(DestinationDataProvider.JCO_PEAK_LIMIT, properties.getProperty("jco.destination.peak_limit"));

            properties.setProperty(ServerDataProvider.JCO_GWHOST, properties.getProperty("jco.server.gwhost"));
            properties.setProperty(ServerDataProvider.JCO_GWSERV, properties.getProperty("jco.server.gwserv"));
            properties.setProperty(ServerDataProvider.JCO_PROGID, properties.getProperty("jco.server.progid"));
            properties.setProperty(ServerDataProvider.JCO_CONNECTION_COUNT, properties.getProperty("jco.server.connection_count"));
            properties.setProperty(ServerDataProvider.JCO_REP_DEST, properties.getProperty("jco.server.repository_destination"));

            createDataFile(properties.getProperty("jco.server.progid"), properties, ".JcoServer");
            createDataFile(properties.getProperty("jco.server.repository_destination"), properties, ".JcoDestination");
        }

        try {
            JCoServer jCoServer = JCoServerFactory.getServer(properties.getProperty("jco.server.progid"));
            jCoServer.setCallHandlerFactory(new JCoServerFunctionHandlerFactoryImpl());
            jCoServer.addServerExceptionListener(new JCoServerListenerImpl());
            jCoServer.addServerErrorListener(new JCoServerListenerImpl());
            jCoServer.addServerStateChangedListener(new JCoServerStateChangedListenerImpl());
            jCoServer.setTIDHandler(new JCoServerTIDHandlerImpl());
            jCoServer.start();

            long result = System.currentTimeMillis() - startTime;
            log.info("[JCO CONFIG SUCCESS] {} ({}sec)\r\n", jCoServer.getProgramID(), result * 0.001);
        } catch (JCoException e) {
            log.info("JCO CONFIG ERROR: {}", e.getMessage());
        }
    }

    private void createDataFile(String destinationName, Properties properties, String string) {
        File file = new File(destinationName + string);
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file, false);
            properties.store(fileOutputStream, "For test purposes only");
            fileOutputStream.close();
        } catch (FileNotFoundException e) {
            log.info("NOT CREATED DATA FILES {}", e.getMessage());
        } catch (IOException e) {
            log.info("CREATE DATA FILE ERROR{}", e.getMessage());
        }
    }
}