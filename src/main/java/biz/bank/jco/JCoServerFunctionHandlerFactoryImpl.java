package biz.bank.jco;

import com.sap.conn.jco.server.JCoServerContext;
import com.sap.conn.jco.server.JCoServerFunctionHandler;
import com.sap.conn.jco.server.JCoServerFunctionHandlerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JCoServerFunctionHandlerFactoryImpl implements JCoServerFunctionHandlerFactory {
    private static final Logger log = LoggerFactory.getLogger(JCoServerFunctionHandlerFactoryImpl.class);

    @Override
    public JCoServerFunctionHandler getCallHandler(JCoServerContext jCoServerContext, String s) {
        return new JCoServerFunctionHandlerImpl();
    }

    @Override
    public void sessionClosed(JCoServerContext jCoServerContext, String s, boolean b) {
        log.info(s);
    }
}
