# Development setup for Liberty Config Language Server

This repo contains a couple projects providing IDE / language support for Open Liberty using the [Language Server Protocol](https://microsoft.github.io/language-server-protocol/).

## Projects

* [lemminx-liberty](./lemminx-liberty) - an extension to the [Eclipse LemMinX](https://github.com/eclipse/lemminx) XML language server providing language features for the Liberty server.xml file.
    * `mvn clean install` to build. Produces the `/lemminx-liberty/target/lemminx-liberty-1.0-SNAPSHOT.jar`.
* [liberty-ls](./liberty-ls) - a language server providing language features for the Liberty bootstrap.properties and server.env files.
    * `mvn clean install` to build. Produces the `/liberty-ls/target/liberty.ls-1.0-SNAPSHOT.jar`.

To test the changes interactively, you must use a language client. 

Below, we will document how to build and test using the VS Code language client for Liberty Config Language Server.

## Project setup in VS Code

Clone the [Liberty Tools for VS Code](https://github.com/OpenLiberty/liberty-tools-vscode) repo as a sibling folder to this repo. Create a VS Code workspace these two repos at the root of the workspace. The folder structure should look something like this:
```
| > liberty-tools-vscode
| v liberty-language-server
| | > lemminx-liberty
| | > liberty-ls
```

## Building and Testing with the VS Code client

Prerequisites: [Debugger for Java extension](https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-java-debug) for VS Code


1. In the `liberty-tools-vscode` directory, run `npm run buildLocal`. 
    
    a) Optionally, `vsce package` may be run next to ensure the VS Code extension can be built properly.

2. Open the debug view, select and launch `Run Extension (liberty-tools-vscode)`. It will open a new window with the extension running in debug mode.

## Debugging the Liberty Config Language Server

After building and running with the instructions above, you may start debugging the Liberty Config Language Server by running one of the Debug tasks.

In the same debug view, select and launch one of the following to debug each respective project:
* `Debug attach liberty-lemminx`
* `Debug attach liberty-ls` (in progress)