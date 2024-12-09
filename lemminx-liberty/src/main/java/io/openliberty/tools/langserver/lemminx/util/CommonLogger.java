/**
 * (C) Copyright IBM Corporation 2024.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.openliberty.tools.langserver.lemminx.util;

import io.openliberty.tools.common.CommonLoggerI;

import java.util.logging.Level;
import java.util.logging.Logger;

public class CommonLogger implements CommonLoggerI {

    private Logger loggerImpl;

    public CommonLogger(Logger mojoLogger) {
        loggerImpl = mojoLogger;
    }

    public Logger getLog() {
        if (this.loggerImpl == null) {
            this.loggerImpl = Logger.getLogger(CommonLogger.class.getName());
        }
        return this.loggerImpl;
    }

    @Override
    public void debug(String msg) {
        if (isDebugEnabled()) {
            getLog().info(msg);
        }
    }

    @Override
    public void debug(String msg, Throwable e) {
        if (isDebugEnabled()) {
            getLog().log(Level.INFO,msg, e);
        }
    }

    @Override
    public void debug(Throwable e) {
        if (isDebugEnabled()) {
            getLog().log(Level.INFO,"", e);
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
        getLog().severe(msg);
    }

    @Override
    public boolean isDebugEnabled() {
        return false;
    }
}