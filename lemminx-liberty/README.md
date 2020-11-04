# lemminx-liberty

[![License](https://img.shields.io/badge/License-EPL%202.0-red.svg?label=license&logo=eclipse)](https://www.eclipse.org/legal/epl-2.0/)
![Lemmix Liberty build status](https://github.com/OpenLiberty/liberty-language-server/workflows/Java%20CI%20-%20Lemminx%20Liberty/badge.svg)

Extension to the [Eclipse LemMinX](https://github.com/eclipse/lemminx) XML language server providing language features for the Open Liberty server.xml file.

## Features

- Schema element completion, hover and validation using the [server.xsd](https://github.com/OpenLiberty/liberty-language-server/blob/master/lemminx-liberty/src/main/resources/schema/xsd/liberty/server.xsd) file

![XSD Validation](../docs/xsd.png)

- Completion, hover and validation support for liberty features

![Feature Completion](../docs/feature-completion.png)

- Document Links for `<include>` elements

![Document Links](../docs/document-link.gif)
