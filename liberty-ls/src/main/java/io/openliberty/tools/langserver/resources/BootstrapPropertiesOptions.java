/*******************************************************************************
* Copyright (c) 2022 IBM Corporation and others.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v. 2.0 which is available at
* http://www.eclipse.org/legal/epl-2.0.
*
* SPDX-License-Identifier: EPL-2.0
*******************************************************************************/
package io.openliberty.tools.langserver.resources;

import java.util.ListResourceBundle;
import java.util.ResourceBundle;

public class BootstrapPropertiesOptions extends ListResourceBundle {
 public static ResourceBundle serverenvProps = ResourceBundle.getBundle("io.openliberty.tools.langserver.resources.ServerEnvOptions");
    
 public Object[][] getContents() {
 return resources;
    }

 private final static Object[][] resources = {
        { "command.port", "Custom property. Overrides the default behavior of the Liberty server command port." },
        { "server.start.wait.time", "Custom property. Indicated in seconds, defaults to 30 seconds, does not apply when server is started with server run command." },
        { "bootstrap.include", "Specifies arnother properties file to also be read during the bootstrap stage. For example, this `bootstrap.include` file can contain a common set of bootstrap properties for multiple servers to use. Set the `bootstrap.include` file to an absolute or relative path." },
        { "osgi.console", "Set the port for the OSGi console (OSGi framework diagnostics)." },
        { "org.osgi.framework.bootdelegation", "Set if this property is required by external monitoring tools. THe value is a comma-delimited list of packages. (OSGi framework extension)." },
        { "wlp.install.dir", "The directory where the Open Liberty runtime is installed." },
        { "wlp.server.name", "The name of the server." },
        { "wlp.user.dir", "The directory of the usr folder. The default is `${wlp.install.dir}/usr`." },
        { "shared.app.dir", "The directory of shared applications. The default is `${wlp.user.dir}/shared/apps`." },
        { "shared.config.dir", "The directory of shared configuration files. The default is `${wlp.user.dir}/shared/config`." },
        { "shared.resource.dir", "The directory of shared resource files. The default is `${wlp.user.dir}/shared/resources`." },
        { "server.config.dir", "The directory where the server configuration is stored. The default is `${wlp.user.dir}/servers/${wlp.server.name}`." },
        { "server.output.dir", "The directory where the server writes the workarea, logs, and other runtime-generated files. The default is `${server.config.dir}`." },
        { "com.ibm.ws.logging.hideMessage", "You can use this setting to configure the messages keys that you want to hide from the console.log and messages.log files. When the messages are hidden, they are redirected to the trace.log file." },
        { "com.ibm.ws.logging.json.field.mappings", serverenvProps.getString("WLP_LOGGING_JSON_FIELD_MAPPINGS") },
        { "com.ibm.ws.logging.log.directory", serverenvProps.getString("LOG_DIR") },
        { "com.ibm.ws.logging.console.format", serverenvProps.getString("WLP_LOGGING_CONSOLE_FORMAT") },
        { "com.ibm.ws.logging.console.log.level", serverenvProps.getString("WLP_LOGGING_CONSOLE_LOGLEVEL") },
        { "com.ibm.ws.logging.console.source", serverenvProps.getString("WLP_LOGGING_CONSOLE_SOURCE") },
        { "com.ibm.ws.logging.copy.system.streams", "If this setting is set to true, messages that are written to the `System.out` and `System.err` streams are copied to process stdout and stderr streams and so appear in the console.log file. If this setting is set to `false`, those messages are written to configured logs such as the `messages.log` file or `trace.log` file, but they are not copied to stdout and stderr and do not appear in console.log. The default value is `true`." },
        { "com.ibm.ws.logging.newLogsOnStart", "If this setting is set to true when Open Liberty starts, any existing `messages.log` or `trace.log` files are rolled over and logging writes to a new `messages.log` or `trace.log` file. If this setting is set to false, `messages.log` or `trace.log` files only refresh when they hit the size that is specified by the `maxFileSize` attribute. The default value is `true`. This setting cannot be provided using the logging element in the `server.xml` file because it is only processed during server bootstrap." },
        { "com.ibm.ws.logging.isoDateFormat", "This setting specifies whether to use ISO-8601 formatted dates in log files. The default value is `false`. If this setting is set to `true`, the ISO-8601 format is used in the messages.log file, the trace.log file, and the FFDC logs. The format is `yyyy-MM-dd'T'HH:mm:ss.SSSZ`. If you specify a value of `false`, the date and time are formatted according to the default locale set in the system. If the default locale is not found, the format is `dd/MMM/yyyy HH:mm:ss:SSS z`" },
        { "com.ibm.ws.logging.max.files", "This setting specifies how many of each of the logs files are kept. This setting also applies to the number of exception summary logs for FFDC. So if this number is 10, you might have 10 message logs, 10 trace logs, and 10 exception summaries in the ffdc directory. By default, the value is `2`. The console log does not roll so this setting does not apply to the console.log file." },
        { "com.ibm.ws.logging.max.file.size", "This setting specifies the maximum size (in MB) that a log file can reach before it is rolled. Setting the value to `0` disables log rolling. The default value is `20`. The console.log does not roll so this setting does not apply." },
        { "com.ibm.ws.logging.message.file.name", "This setting specifies the name of the message log file. The message log file has a default name of `messages.log`. This file always exists, and contains INFO and other (AUDIT, WARNING, ERROR, FAILURE) messages in addition to the System.out and System.err streams. This log also contains time stamps and the issuing thread ID. If the log file is rolled over, the names of earlier log files have the format messages_timestamp.log" },
        { "com.ibm.ws.logging.message.format", serverenvProps.getString("WLP_LOGGING_MESSAGE_FORMAT") },
        { "com.ibm.ws.logging.message.source", serverenvProps.getString("WLP_LOGGING_MESSAGE_SOURCE") },
        { "com.ibm.ws.logging.trace.file.name", "This setting specifies the name of the trace log file. The trace.log file is created only if additional or detailed trace is enabled. `stdout` is recognized as a special value, and causes trace to be directed to the original standard out stream." },
        { "com.ibm.ws.logging.trace.format", "This setting controls the format of the trace log. The default format for Liberty is `ENHANCED`. You can also use `BASIC` and `ADVANCED` formats" },
        { "com.ibm.ws.logging.trace.specification", "This setting is used to selectively enable trace. The log detail level specification is in the following format: `component = level`" },
        { "com.ibm.ws.logging.apps.write.json", serverenvProps.getString("WLP_LOGGING_APPS_WRITE_JSON") },
        { "com.ibm.ws.json.access.log.fields", serverenvProps.getString("WLP_LOGGING_JSON_ACCESS_LOG_FIELDS") },
        { "websphere.log.provider=binaryLogging-1.0", "Enable binary logging for the server" },
        { "com.ibm.hpel.log.purgeMaxSize", "Specifies the maximum size for the binary log repository in megabytes" },
        { "com.ibm.hpel.log.purgeMinTime", "Specifies the duration, in hours, after which a server can remove a log record." },
        { "com.ibm.hpel.log.fileSwitchTime", "Makes the server close the active log file and start a new one at the specified hour of the day. When the value for fileSwitchTime is specified, file switching is enabled, otherwise it is disabled." },
        { "com.ibm.hpel.log.bufferingEnabled", "Specifies whether to allow a small delay in saving records to the disk for improved performance. When bufferingEnabled is set to true, records will be briefly held in memory before being written to disk." },
        { "com.ibm.hpel.log.outOfSpaceAction", "Specifies the action to perform when the file system where records are kept runs out of free space." }
    };
}
