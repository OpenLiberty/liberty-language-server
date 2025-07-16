# Liberty Config Language Server

> Note: Starting with the [Liberty LemMinX Language Server 1.0-M1 early release](https://github.com/OpenLiberty/liberty-language-server/releases/tag/lemminx-liberty-1.0-M1) and [Liberty Config Language Server 1.0-M1 early release](https://github.com/OpenLiberty/liberty-language-server/releases/tag/liberty-langserver-1.0-M1), Java 17 is required.

The Liberty Config Language Server provides language server features for Liberty server configuration files through any of the supported client IDEs. It adheres to the [language server protocol](https://github.com/Microsoft/language-server-protocol).

## Client IDEs

The Liberty Config Language Server is included in the following client IDEs.

* [Liberty Tools for Visual Studio Code](https://github.com/OpenLiberty/liberty-tools-vscode)
* [Liberty Tools for Eclipse IDE](https://github.com/OpenLiberty/liberty-tools-eclipse)
* [Liberty Tools for IntelliJ IDEA](https://github.com/OpenLiberty/liberty-tools-intellij)

## Supported files

Liberty Config Language Server features are available for the following configuration files.

- Any XML file that contains a `server` root element.
- A `server.env` file located in a directory specified by the `configDirectory` configuration in the Liberty Maven or Gradle plugin, or in the default `src/main/liberty/config` directory.
- Any file with the extension `.env` that is specified by the `serverEnvFile` configuration in the Liberty Maven or Gradle plugin.
- A `bootstrap.properties` file located in a directory specified by the `configDirectory` configuration in the Liberty Maven or Gradle plugin, or in the default `src/main/liberty/config` directory.
- Any file with the extension `.properties` that is specified by the `bootstrapPropertiesFile` configuration in the Liberty Maven or Gradle plugin.

## Features

The following language server features are available through any of the supported client IDEs.

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

### Diagnostics and quick fixes on Liberty server configuration files

Diagnostics highlight potential problems in your configuration, such as invalid values. Quick fixes provide potential solutions to those problems.

* Diagnostics and quick fixes for Liberty properties and variables

![Screen capture showing diagnostics marking an invalid value for a Liberty property in a bootstrap.properties file. Hovering over the diagnostic will provide more details.](./docs/images/property-diagnostic.png "Diagnostics on Liberty properties in bootstrap.properties")

![Screen capture showing a quick fix for a diagnostic of an invalid value in a bootstrap.properties file. Opening the quick fixes menu will show all the available quick fix options.](./docs/images/property-quickFix.png "Quick fixes on bootstrap.properties")

![Screen capture showing diagnostics marking an invalid value for a Liberty variable in a server.env file. Hovering over the diagnostic will provide more details.](./docs/images/variable-diagnostic.png "Diagnostics on Liberty variables in server.env")

![Screen capture showing a quick fix for a diagnostic of an invalid value in a server.env file. Opening the quick fixes menu will show all the available quick fix options.](./docs/images/variable-quickFix.png "Quick fixes on server.env")

* Diagnostics and quick fixes for Liberty XML configs

![Screen capture showing diagnostics marking an invalid feature defined in a server.xml file. Hovering over the diagnostic will provide more details.](./docs/images/feature-diagnostic.png "Diagnostics on Liberty features in server.xml")

![Screen capture showing a quick fix for a diagnostic on an invalid feature defined in a server.xml file. Hovering over the diagnostic and selecting Quick Fix... will provide more details.](./docs/images/feature-quickFix.png "Quick fixes on Liberty features in server.xml")

![Screen capture showing diagnostics marking an configuration element defined in a server.xml file. Hovering over the diagnostic will provide more details.](./docs/images/config-diagnostic.png "Diagnostics on configuration elements in server.xml")

![Screen capture showing a quick foix for a diagnostics on a configuration element defined in a server.xml file. Hovering over the diagnostic and selecting Quick Fix... will provide more details.](./docs/images/config-quickFix.png "Quick fixes on configuration elements in server.xml")

## Minimum version recommendations for the Liberty Maven and Gradle plug-ins

A minimum version of the Liberty Maven Plug-in 3.7.1 or Liberty Gradle Plug-in 3.5.1 is recommended. 

If you are using dev mode for containers, a minimum version of the Liberty Maven Plug-in 3.7 or Liberty Gradle Plug-in 3.5 is recommended. If an earlier version is used, the Liberty Config Language Server cannot generate a schema file for use with `server.xml` editing. In this case, a default schema that is based on Liberty 25.0.0.6 is used instead.

### Schema and Feature Validation
If the [Liberty Maven Plug-in](https://github.com/OpenLiberty/ci.maven) or [Liberty Gradle Plug-in](https://github.com/OpenLiberty/ci.gradle) is configured with the Liberty project, the Liberty Config Language Server automatically generates a schema file based on the Liberty runtime and version. This schema file provides relevant information about the supported `server.xml` elements and Liberty features. If an earlier version of either plug-in is used, the schema file is not regenerated when additional features are installed. This limitation might result in a lack of hover, completion, or diagnostic support for elements related to those newly installed features.

Liberty Tools prioritizes accurate and up-to-date schema and feature validation. While it includes periodically updated cached data, the most reliable validation occurs when you **build your project**. Building your project allows Liberty Tools to generate schema and feature information directly from the Open Liberty runtime specified in your project's configuration. This ensures that validation is based on the exact version of Open Liberty you are using, preventing issues caused by potentially outdated cached data.

**How to build your Open Liberty project:**

* **With Maven:** Navigate to your project's root directory in your terminal and run:
    ```bash
    mvn clean liberty:run
    ```
  For fast, iterative development with automatic hot deployment and on-demand testing, use dev mode:
    ```bash
    mvn liberty:dev
    ```
  Refer [Goals](https://github.com/OpenLiberty/ci.maven?tab=readme-ov-file#goals) for more details
* **With Gradle:** Navigate to your project's root directory in your terminal and run:
    ```bash
    ./gradlew libertyRun
    ```
  For fast, iterative development with automatic hot deployment and on-demand testing, use dev mode:
    ```bash
    ./gradlew libertyDev
    ```
  Refer [Tasks](https://github.com/OpenLiberty/ci.gradle?tab=readme-ov-file#tasks) for more details

For the most accurate validation, always ensure your project has been successfully built.
## Contributing
See the [DEVELOPING](./DEVELOPING.md) and [CONTRIBUTING](./CONTRIBUTING.md) documents for more details.
## License
Eclipse Public License - v 2.0 See [LICENSE](./LICENSE) file.
