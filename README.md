# liberty-language-server

Monorepo for projects providing IDE / language support for Open Liberty using the [Language Server Protocol](https://microsoft.github.io/language-server-protocol/).

## Projects

* [lemminx-liberty](./lemminx-liberty) - an extension to the [Eclipse LemMinX](https://github.com/eclipse/lemminx) XML language server providing language features for the Liberty server.xml file.
    * `mvn clean install` to build. Produces the `/lemminx-liberty/target/lemminx-liberty-1.0-SNAPSHOT.jar`.
* [liberty-ls](./liberty-ls) - a language server providing language features for the Liberty bootstrap.properties and server.env files.
    * `mvn clean install` to build. Produces the `/liberty-ls/target/liberty.ls-1.0-SNAPSHOT.jar`.

## Building with VS Code

To build the language server for Liberty configuration prototype with a VS Code Client, see the [liberty-ls-prototype branch](https://github.com/OpenLiberty/open-liberty-tools-vscode/tree/liberty-ls-prototype) of the Open Liberty Tools VS Code extension.

## Debugging with VS Code

Prerequisites: [Debugger for Java extension](https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-java-debug) for VS Code

1. Create a VS Code workspace with both [open-liberty-tools-vscode](https://github.com/OpenLiberty/open-liberty-tools-vscode) and this project at the root of the workspace. The folder structure should look something like this:
```
| > open-liberty-tools-vscode
| v liberty-language-server
| | > lemminx-liberty
| | > liberty-ls
```

2. In the `open-liberty-tools-vscode` directory, run `npm run build`.

3. Open the debug view, select and launch `Run Extension (open-liberty-tools-vscode)`. It will open a new window with the extension running in debug mode.

4. In the same debug view, now select and launch one of the following to debug each respective project:
    * `Debug attach liberty-lemminx`
    * `Debug attach liberty-ls` (in progress)