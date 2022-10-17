# Liberty Config Language Server

Provides language server features for Liberty server configuration files.

Liberty Config Language Server adheres to the [language server protocol](https://github.com/Microsoft/language-server-protocol)
and is available for use with the following clients.

# Client IDEs
* [VS Code](https://github.com/OpenLiberty/liberty-tools-vscode/tree/liberty-ls-prototype)
* [Eclipse](https://github.com/OpenLiberty/liberty-tools-eclipse)
* [Intellij](https://github.com/OpenLiberty/liberty-tools-intellij)

## Features

### Completion for Liberty server configuration files
* Completion for Liberty properties and values 
![Completion for Liberty properties](./docs/images/property-completion.png) 
![Completion for Liberty property values](./docs/images/property-value-completion.png)
* Completion for Liberty variables and values 
![Completion for Liberty variables](./docs/images/variable-completion.png)
![Completion for Liberty variable values](./docs/images/variable-value-completion.png)
* Completion for Liberty config in `server.xml`
![Completion for Liberty config](./docs/images/feature-completion.png)

### Hover on Liberty server configuration files
* Hover for Liberty properties and variables
![Hover on Liberty properties](./docs/images/property-hover.png)
![Hover on Liberty server variables](./docs/images/variable-hover.png)
* Hover for Liberty config in `server.xml` ![Hover on Liberty features](./docs/images/feature-hover.png)

### Diagnostics on Liberty server configuration files
* Diagnostics on Liberty properties and variables
![Diagnostics on Liberty properties](./docs/images/property-diagnostic.png)
![Diagnostics on Liberty variables](./docs/images/variable-diagnostic.png)
* Diagnostics for Liberty features
![Diagnostics on Liberty features](./docs/images/feature-diagnostic.png)

## Contributing
See the [CONTRIBUTING](./CONTRIBUTING.md) doc for more details.

## Feedback
Open a [GitHub issue](https://github.com/OpenLiberty/liberty-language-server/issues).
## License
Eclipse Public License - v 2. See [LICENSE](./LICENSE) file.