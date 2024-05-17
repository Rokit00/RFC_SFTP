package biz.bank.jco;

import biz.bank.sftp.SFTPService;
import biz.bank.sftp.SFTPServiceImpl;
import biz.bank.util.PropertiesUtil;
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
        long startTime = System.currentTimeMillis();

        JCoParameterList importParameterList = jCoFunction.getImportParameterList();
        JCoParameterList exportParameterList = jCoFunction.getExportParameterList();
        JCoTable tableParameterList = jCoFunction.getTableParameterList().getTable(properties.getProperty("jco.param.table"));

        String fileName = importParameterList.getString(properties.getProperty("jco.param.import"));

        StringBuilder fileContent = new StringBuilder();
        do {
            String tableColumn = tableParameterList.getString(properties.getProperty("jco.param.table"));
            fileContent.append(tableColumn).append("\r\n");
        } while (tableParameterList.nextRow());

        String upload = sftpService.upload(fileName, fileContent.toString());

        exportParameterList.setValue(properties.getProperty("jco.param.export"), upload);

        long result = System.currentTimeMillis() - startTime;
        log.info("RFC HANDLE REQUEST SUCCESS ({}sec)", result * 0.001);
    }
}
