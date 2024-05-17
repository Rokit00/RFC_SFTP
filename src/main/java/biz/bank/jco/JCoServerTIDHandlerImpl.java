package biz.bank.jco;

import com.sap.conn.jco.server.JCoServerContext;
import com.sap.conn.jco.server.JCoServerTIDHandler;

public class JCoServerTIDHandlerImpl implements JCoServerTIDHandler {
    @Override
    public boolean checkTID(JCoServerContext jCoServerContext, String s) {
        return false;
    }

    @Override
    public void confirmTID(JCoServerContext jCoServerContext, String s) {

    }

    @Override
    public void commit(JCoServerContext jCoServerContext, String s) {

    }

    @Override
    public void rollback(JCoServerContext jCoServerContext, String s) {

    }
}

