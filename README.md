# liberty-language-server

Monorepo for projects providing IDE / language support for Open Liberty using the [Language Server Protocol](https://microsoft.github.io/language-server-protocol/).

## Projects

* [lemminx-liberty](./lemminx-liberty) - an extension to the [Eclipse LemMinX](https://github.com/eclipse/lemminx) XML language server providing language features for the Liberty server.xml file.
    * `mvn clean install` to build. Produces the `/lemminx-liberty/target/lemminx-liberty-1.0-SNAPSHOT.jar`.
* [liberty-ls](./liberty-ls) - a language server providing language features for the Liberty bootstrap.properties and server.env files.
    * `mvn clean install` to build. Produces the `/liberty-ls/target/liberty.ls-1.0-SNAPSHOT.jar`.

## Building with VS Code

To build the language server for Liberty configuration prototype with a VS Code Client, see the [liberty-ls-prototype branch](https://github.com/OpenLiberty/open-liberty-tools-vscode/tree/liberty-ls-prototype) of the Open Liberty Tools VS Code extension.