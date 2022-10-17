# Liberty Config Language Server

Provides language server features for Liberty server configuration files, which include `server.env`, `bootstrap.properties`, and `server.xml` files.

Liberty Config Language Server adheres to the [language server protocol](https://github.com/Microsoft/language-server-protocol)
and is available for use with the following clients.

# Client IDEs
* [VS Code](https://github.com/OpenLiberty/liberty-tools-vscode/tree/liberty-ls-prototype)
* [Eclipse](https://github.com/OpenLiberty/liberty-tools-eclipse)
* [Intellij](https://github.com/OpenLiberty/liberty-tools-intellij)

## Features

### Completion for Liberty server configuration files
* Completion for Liberty properties and values 

![Screenshot of Liberty property name suggestions in a bootstrap.properties file](./docs/images/property-completion.png) 
![Screenshot of value suggestions for a Liberty property in a bootstrap.properties file. If there is a default value, it is preselected.](./docs/images/property-value-completion.png)
* Completion for Liberty variables and values 

![Screenshot of Liberty variable suggestions in a server.env file](./docs/images/variable-completion.png)
![Screenshot of value suggestions for a Liberty variable in a server.env file. If there is a default value, it is preselected](./docs/images/variable-value-completion.png)
* Completion for Liberty config in `server.xml`

![Screenshot of Liberty feature suggestions in a feature block in a server.xml file](./docs/images/feature-completion.png)

### Hover on Liberty server configuration files
* Hover for Liberty properties and variables

![Screenshot of a documentation dialog appearing when hovering over a Liberty property in a bootstrap.properties file](./docs/images/property-hover.png)
![Screenshot of a documentation dialog appearing when hovering over a Liberty variable in a server.env file](./docs/images/variable-hover.png)
* Hover for Liberty config in `server.xml` 

![Screenshot of feature documentation appearing when hovering over a Liberty feature in a server.xml file](./docs/images/feature-hover.png)

### Diagnostics on Liberty server configuration files
* Diagnostics on Liberty properties and variables

![Screenshot showing diagnostics marking an invalid value for a Liberty property in a bootstrap.properties file. Hovering over the diagnostic will provide more details.](./docs/images/property-diagnostic.png)
![Screenshot showing diagnostics marking an invalid value for a Liberty variable in a server.env file. Hovering over the diagnostic will provide more details.](./docs/images/variable-diagnostic.png)
* Diagnostics for Liberty features

![Screenshot showing diagnostics marking an invalid feature defined in a server.xml file. Hovering over the diagnostic will provide more details.](./docs/images/feature-diagnostic.png)

## Contributing
See the [DEVELOPING]() and [CONTRIBUTING](./CONTRIBUTING.md) documents for more details.

## Feedback
Open a [GitHub issue](https://github.com/OpenLiberty/liberty-language-server/issues).
## License
Eclipse Public License - v 2. See [LICENSE](./LICENSE) file.