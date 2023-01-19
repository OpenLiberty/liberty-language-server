# Contributing to Liberty Config Language Server
We welcome contributions, and request you follow these guidelines.

> Note: Starting with the [Liberty LemMinX Language Server 1.0-M1 early release](https://github.com/OpenLiberty/liberty-language-server/releases/tag/lemminx-liberty-1.0-M1) and [Liberty Config Language Server 1.0-M1 early release](https://github.com/OpenLiberty/liberty-language-server/releases/tag/liberty-langserver-1.0-M1), Java 17 is required.

 - [Raising issues](#raising-issues)
 - [Legal](#legal)
 - [Coding standards](#coding-standards)
 - [Plugin development](#plugin-development)

## Raising issues
Please raise any bug reports on the [issue tracker](https://github.com/OpenLiberty/liberty-language-server/issues). Be sure to search the list to see if your issue has already been raised.

A good bug report is one that make it easy for us to understand what you were trying to do and what went wrong. Provide as much context as possible so we can try to recreate the issue.

### Legal

In order to make contribution as easy as possible, we follow the same approach as the [Developer's Certificate of Origin 1.1 (DCO)](https://developercertificate.org/) - that the Linux® Kernel [community](https://elinux.org/Developer_Certificate_Of_Origin) uses to manage code contributions.

We simply ask that when submitting a pull request for review, the developer
must include a sign-off statement in the commit message.

Here is an example Signed-off-by line, which indicates that the
submitter accepts the DCO:

```text
Signed-off-by: John Doe <john.doe@example.com>
```

You can include this automatically when you commit a change to your
local git repository using the following command:

```bash
git commit -s
```

### Coding standards

This project follows Eclipse standard Java language [coding conventions](https://wiki.eclipse.org/Coding_Conventions).

Please note:
 - all PRs must have passing builds

### Language Server development

To learn how to setup, run, and test your development environment, follow the provided [ development instructions](./DEVELOPING.md).