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

public class ServerEnvOptions extends ListResourceBundle {
    public Object[][] getContents() {
        return resources;
    }

    private final static Object[][] resources = {
        { "JAVA_HOME", "Indicates which Java Virtual Machine (JVM) to use. If this variable is not set, the system default is used." },
        { "JVM_ARGS", "A list of command-line options, such as system properties or -X parameters, that are passed to the JVM when the server starts. Any values that contain spaces must be enclosed in quotes." },
        { "LOG_DIR", "The directory that contains the log file. The default value is `%WLP_OUTPUT_DIR%/serverName/logs`." },
        { "LOG_FILE", "The log file name. This log file is only used if the server start command is run in the background through the start action." },
        { "SERVER_WORKING_DIR", "The directory that contains output files from the JVM that the server uses, such as the `javadump` files. The default value is the `${WLP_OUTPUT_DIR}/serverName` location. If this variable is set to a relative path, the resulting path is relative to this default location. For example, a value of `SERVER_WORKING_DIR=logs/javadumps` results in a path of `${WLP_OUTPUT_DIR}/serverName/logs/javadumps`." },
        { "VARIABLE_SOURCE_DIRS", "The directories that contain files to be loaded as configuration variables. The default value is `${server.config.dir}/variables`." },
        { "WLP_USER_DIR", "The user or custom configuration directory that is used to store shared and server-specific configuration. See the `path_to_liberty/wlp/README.TXT` file for details about shared resource locations. A server configuration is at the `%WLP_USER_DIR%/servers/serverName` location. The default value is the user directory in the installation directory." },
        { "WLP_OUTPUT_DIR", "The directory that contains output files for defined servers. This directory must have both read and write permissions for the user or users who start servers. By default, the server output logs and work area are stored at the `%WLP_USER_DIR%/servers/serverName` location alongside configuration and applications. If this variable is set, the output logs and work area are stored at the `%WLP_OUTPUT_DIR%/serverName location`." },
        { "WLP_DEBUG_ADDRESS", "The port to use for running the server in debug mode. The default value is `7777`." },
        { "WLP_DEBUG_SUSPEND", "Whether to suspend the JVM on startup or not. This variable can be set to `y` to suspend the JVM on startup until a debugger attaches, or set to `n` to start up without waiting for a debugger to attach. The default value is `y`." },
        { "WLP_DEBUG_REMOTE", "Whether to allow remote debugging or not. This variable can be set to `y` to allow remote debugging. By default, this value is not defined, which does not allow remote debugging on newer JDK/JREs." },
        { "WLP_OUTPUT_DIR", "The directory that contains output files for defined servers. This directory must have both read and write permissions for the user or users who start servers. By default, the server output logs and work area are stored at the `%WLP_USER_DIR%/servers/serverName` location alongside configuration and applications. If this variable is set, the output logs and work area are stored at the `%WLP_OUTPUT_DIR%/serverName location`." },
        { "WLP_DEBUG_ADDRESS", "The port to use for running the server in debug mode. The default value is `7777`." },
        { "WLP_DEBUG_SUSPEND", "Whether to suspend the JVM on startup or not. This variable can be set to `y` to suspend the JVM on startup until a debugger attaches, or set to `n` to start up without waiting for a debugger to attach. The default value is `y`." },
        { "WLP_DEBUG_REMOTE", "Whether to allow remote debugging or not. This variable can be set to `y` to allow remote debugging. By default, this value is not defined, which does not allow remote debugging on newer JDK/JREs." },
        { "LIBPATH", "This environment variable can be used to specify the setting for the path to the libraries in the Liberty `server.env` file" },
        { "WLP_LOGGING_JSON_FIELD_MAPPINGS", "When logs are in JSON format, use this setting to replace default field names with new field names or to omit fields from the logs. For more information, see Configurable JSON field names." },
        { "WLP_LOGGING_CONSOLE_FORMAT", "This setting specifies the required format for the console. Valid values are `dev`, `simple`, or `json` format. By default, consoleFormat is set to `dev`." },
        { "WLP_LOGGING_CONSOLE_LOGLEVEL", "This setting controls the granularity of messages that go to the console. The valid values are `INFO`, `AUDIT`, `WARNING`, `ERROR`, and `OFF`. The default is `AUDIT`. If using with the Eclipse developer tools this must be set to the default." },
        { "WLP_LOGGING_CONSOLE_SOURCE", "This setting specifies a comma-separated list of sources that route to the console. It applies only when the console format is set to `json`. The valid values are `message`, `trace`, `accessLog`, `ffdc`, and `audit`. By default, consoleSource is set to `message`. To use the audit source, enable the Liberty `audit-1.0` feature. To use the accessLog source you need to have configured `httpAccessLogging`." },
        { "WLP_LOGGING_MESSAGE_FORMAT", "This setting specifies the required format for the messages.log file. Valid values are `simple` or `json` format. By default, messageFormat is set to `simple`." },
        { "WLP_LOGGING_MESSAGE_SOURCE", "This setting specifies a list of comma-separated sources that route to the messages.log file. This setting applies only when the message format is set to `json`. The valid values are `message`, `trace`, `accessLog`, `ffdc`, and `audit`. By default, messageSource is set to `message`. To use the audit source, enable the Liberty `audit-1.0` feature. To use the accessLog source you need to have configured `httpAccessLogging`." },
        { "WLP_LOGGING_APPS_WRITE_JSON", "When the message log or console is in JSON format, this setting allows applications to write JSON-formatted messages to those destinations, without modification." },
        { "WLP_LOGGING_JSON_ACCESS_LOG_FIELDS", "When logs are in JSON format, you can use this setting to replace the default HTTP access log JSON fields with fields that are specified by the `logFormat` attribute of the accessLogging element." }
    };
}
