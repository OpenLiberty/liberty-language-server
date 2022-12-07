# Liberty Config Language Server

The Liberty Config Language Server provides language server features for Liberty server configuration files through any of the supported client IDEs. It adheres to the [language server protocol](https://github.com/Microsoft/language-server-protocol).

## Client IDEs

The Liberty Config Language Server is included in the following client IDEs.

* [Liberty Tools for Visual Studio Code](https://github.com/OpenLiberty/liberty-tools-vscode)
* [Liberty Tools for Eclipse IDE](https://github.com/OpenLiberty/liberty-tools-eclipse)
* [Liberty Tools for IntelliJ IDEA](https://github.com/OpenLiberty/liberty-tools-intellij)

## Supported files

Liberty Config Language Server features are avaialble for the following configuration files.

- `server.env`
- `bootstrap.properties`
- `server.xml` and any XML files that are referenced through the `include` element in the `server.xml` file.
- Any XML files that contain the `server` root element and exist in the `src/main/liberty/config`, `configDropins/overrides`, `configDropins/defaults`, `usr/shared/config`, or `usr/servers` directory.

## Features

The following language server features are avaialble through any of the supported client IDEs.

### Completion for Liberty server configuration files

Start typing a Liberty property, variable, or XML configuration to view a list of possible options.

* Completion for Liberty properties and values 

![Screen capture of Liberty property name suggestions in a bootstrap.properties file](./docs/images/property-completion.png "Completion suggestions for Liberty properties in bootstrap.properties") 
![Screen capture of value suggestions for a Liberty property in a bootstrap.properties file. If there is a default value, it is preselected.](./docs/images/property-value-completion.png "Completion suggestions for Liberty property values in bootstrap.properties")
* Completion for Liberty variables and values 

![Screen capture of Liberty variable suggestions in a server.env file](./docs/images/variable-completion.png "Completion suggestions for Liberty variables in server.env")
![Screen capture of value suggestions for a Liberty variable in a server.env file. If there is a default value, it is preselected](./docs/images/variable-value-completion.png "Completion suggestions for Liberty variable values in server.env")
* Completion for Liberty XML configs

![Screen capture of Liberty feature suggestions in a feature block in a server.xml file](./docs/images/feature-completion.png "Completion suggestions for Liberty configuration in server.xml")

### Hover on Liberty server configuration files

Hover your cursor over Liberty properties, variables, or XML configuration to view a description.

* Hover for Liberty properties and variables

![Screen capture of a documentation dialog appearing when hovering over a Liberty property in a bootstrap.properties file](./docs/images/property-hover.png "Hover on Liberty properties in bootstrap.properties")
![Screen capture of a documentation dialog appearing when hovering over a Liberty variable in a server.env file](./docs/images/variable-hover.png "Hover on Liberty server variables in server.env")

* Hover for Liberty XML configs

![Screen capture of feature documentation appearing when hovering over a Liberty feature in a server.xml file](./docs/images/feature-hover.png "Hover on Liberty features in server.xml")

### Diagnostics on Liberty server configuration files

Diagnostics highlight potential problems in your configuration, such as invalid values. 

* Diagnostics on Liberty properties and variables

![Screen capture showing diagnostics marking an invalid value for a Liberty property in a bootstrap.properties file. Hovering over the diagnostic will provide more details.](./docs/images/property-diagnostic.png "Diagnostics on Liberty properties in bootstrap.properties")
![Screen capture showing diagnostics marking an invalid value for a Liberty variable in a server.env file. Hovering over the diagnostic will provide more details.](./docs/images/variable-diagnostic.png "Diagnostics on Liberty variables in server.env")

* Diagnostics for Liberty XML configs

![Screen capture showing diagnostics marking an invalid feature defined in a server.xml file. Hovering over the diagnostic will provide more details.](./docs/images/feature-diagnostic.png "Diagnostics on Liberty features in server.xml")

## Minimum version recommendations for the Liberty Maven and Gradle plug-ins

A minimum version of the Liberty Maven Plug-in 3.7.1 or Liberty Gradle Plug-in 3.5.1 is recommended. If the [Liberty Maven Plug-in](https://github.com/OpenLiberty/ci.maven) or [Liberty Gradle Plug-in](https://github.com/OpenLiberty/ci.gradle) is configured with the Liberty project, the Liberty Config Language Server automatically generates a schema file based on the Liberty runtime and version. This schema file provides relevant information about the supported `server.xml` elements and Liberty features. If an earlier version of either plug-in is used, the schema file is not regenerated when additional features are installed. This limitation might result in a lack of hover, completion, or diagnostic support for elements related to those newly installed features.

If you are using dev mode for containers, a minimum version of the Liberty Maven Plug-in 3.7 or Liberty Gradle Plug-in 3.5 is recommended. If an earlier version is used, the Liberty Config Language Server cannot generate a schema file for use with `server.xml` editing. In this case, a default schema that is based on Liberty 22.0.0.9 is used instead.

## Contributing
See the [DEVELOPING](./DEVELOPING.md) and [CONTRIBUTING](./CONTRIBUTING.md) documents for more details.
## License
Eclipse Public License - v 2.0 See [LICENSE](./LICENSE) file.
