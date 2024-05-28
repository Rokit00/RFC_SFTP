package suhun.sftp.jco;

import suhun.sftp.sftp.SFTPService;
import suhun.sftp.sftp.SFTPServiceImpl;
import suhun.sftp.util.PropertiesUtil;
import com.sap.conn.jco.*;
import com.sap.conn.jco.server.JCoServerContext;
import com.sap.conn.jco.server.JCoServerFunctionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class JCoServerFunctionHandlerImpl implements JCoServerFunctionHandler {
    private final Properties properties = PropertiesUtil.getProperties();
    private static final Logger log = LoggerFactory.getLogger(JCoServerFunctionHandlerImpl.class);
    private final SFTPService sftpService = new SFTPServiceImpl();

    @Override
    public void handleRequest(JCoServerContext jCoServerContext, JCoFunction jCoFunction) {
        log.info("[START] RFC HANDLE REQUEST");
        long startTime = System.currentTimeMillis();

        JCoParameterList importParameterList = jCoFunction.getImportParameterList();
        JCoParameterList exportParameterList = jCoFunction.getExportParameterList();
        JCoTable tableParameterList = jCoFunction.getTableParameterList().getTable(properties.getProperty("JCO.REQUEST.TABLE"));

        String fileName = importParameterList.getString(properties.getProperty("JCO.REQUEST.PARAM.IMPORT0"));

        StringBuilder fileContent = new StringBuilder();
        do {
            String tableColumn = tableParameterList.getString(properties.getProperty("JCO.REQUEST.TABLE.COL"));
            fileContent.append(tableColumn).append("\r\n");
        } while (tableParameterList.nextRow());

        String upload = sftpService.upload(fileName, fileContent.toString());

        exportParameterList.setValue(properties.getProperty("JCO.REQUEST.PARAM.EXPORT"), upload);

        long result = System.currentTimeMillis() - startTime;
        log.info("[END] RFC HANDLE REQUEST ({}sec) \r\n", result * 0.001);
    }
}
