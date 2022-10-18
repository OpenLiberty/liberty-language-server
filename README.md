# Liberty Config Language Server

Provides language server features for Liberty server configuration files. Supported files include 
- `server.env`
- `bootstrap.properties`
- `server.xml` (or any XML file) and its referenced XML files through the `<include>` element
    - XML file must contain the root element `<server>` and exist in `src/main/liberty/config`, `configDropins/overrides`, `configDropins/defaults`, `usr/shared/config`, or `usr/servers` directory

Liberty Config Language Server adheres to the [language server protocol](https://github.com/Microsoft/language-server-protocol)
and is available for use with the following clients.

# Client IDEs
* [VS Code](https://github.com/OpenLiberty/liberty-tools-vscode)
* [Eclipse](https://github.com/OpenLiberty/liberty-tools-eclipse)
* [Intellij](https://github.com/OpenLiberty/liberty-tools-intellij)

## Features

### Completion for Liberty server configuration files
* Completion for Liberty properties and values 

![Screenshot of Liberty property name suggestions in a bootstrap.properties file](./docs/images/property-completion.png "Completion suggestions for Liberty properties in bootstrap.properties") 
![Screenshot of value suggestions for a Liberty property in a bootstrap.properties file. If there is a default value, it is preselected.](./docs/images/property-value-completion.png "Completion suggestions for Liberty property values in bootstrap.properties")
* Completion for Liberty variables and values 

![Screenshot of Liberty variable suggestions in a server.env file](./docs/images/variable-completion.png "Completion suggestions for Liberty variables in server.env")
![Screenshot of value suggestions for a Liberty variable in a server.env file. If there is a default value, it is preselected](./docs/images/variable-value-completion.png "Completion suggestions for Liberty variable values in server.env")
* Completion for Liberty config in `server.xml`

![Screenshot of Liberty feature suggestions in a feature block in a server.xml file](./docs/images/feature-completion.png "Completion suggestions for Liberty configuration in server.xml")

### Hover on Liberty server configuration files
* Hover for Liberty properties and variables

![Screenshot of a documentation dialog appearing when hovering over a Liberty property in a bootstrap.properties file](./docs/images/property-hover.png "Hover on Liberty properties in bootstrap.properties")
![Screenshot of a documentation dialog appearing when hovering over a Liberty variable in a server.env file](./docs/images/variable-hover.png "Hover on Liberty server variables in server.env")
* Hover for Liberty config in `server.xml` 

![Screenshot of feature documentation appearing when hovering over a Liberty feature in a server.xml file](./docs/images/feature-hover.png "Hover on Liberty features in server.xml")

### Diagnostics on Liberty server configuration files
* Diagnostics on Liberty properties and variables

![Screenshot showing diagnostics marking an invalid value for a Liberty property in a bootstrap.properties file. Hovering over the diagnostic will provide more details.](./docs/images/property-diagnostic.png "Diagnostics on Liberty properties in bootstrap.properties")
![Screenshot showing diagnostics marking an invalid value for a Liberty variable in a server.env file. Hovering over the diagnostic will provide more details.](./docs/images/variable-diagnostic.png "Diagnostics on Liberty variables in server.env")
* Diagnostics for Liberty features

![Screenshot showing diagnostics marking an invalid feature defined in a server.xml file. Hovering over the diagnostic will provide more details.](./docs/images/feature-diagnostic.png "Diagnostics on Liberty features in server.xml")

## Contributing
See the [DEVELOPING](./DEVELOPING.md) and [CONTRIBUTING](./CONTRIBUTING.md) documents for more details.

## Feedback
Open a [GitHub issue](https://github.com/OpenLiberty/liberty-language-server/issues).
## License
Eclipse Public License - v 2. See [LICENSE](./LICENSE) file.