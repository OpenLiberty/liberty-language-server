package io.openliberty.tools.langserver;

import java.util.logging.Logger;

import io.openliberty.tools.common.CommonLoggerI;

public class CommonLogger implements CommonLoggerI {
    
    private static CommonLogger logger = null;
    private Logger loggerImpl;

    public static CommonLogger getInstance(Logger log) {
        if (logger == null) {
            logger = new CommonLogger(log);
        } else {
            logger.setLogger(log);
        }

        return logger;
    }

    private CommonLogger(Logger logger) {
        loggerImpl = logger;
    }

    private void setLogger(Logger logger) {
        loggerImpl = logger;
    }

    public Logger getLog() {
        return this.loggerImpl;
    }

    @Override
    public void debug(String msg) {
        if (isDebugEnabled()) {
            getLog().warning(msg);
        }
    }

    @Override
    public void debug(String msg, Throwable e) {
        if (isDebugEnabled()) {
            getLog().warning(msg);
        }
    }

    @Override
    public void debug(Throwable e) {
        if (isDebugEnabled()) {
            getLog().warning(e.getMessage());
        }
    }

    @Override
    public void warn(String msg) {
        getLog().warning(msg);
    }

    @Override
    public void info(String msg) {
        getLog().info(msg);
    }

    @Override
    public void error(String msg) {
        getLog().warning(msg);
    }

    @Override
    public boolean isDebugEnabled() {
        return false;
    }

}
