# Contributing to Liberty Config Language Server

## Opening issues
Please raise any bug reports on the [Github issues page](https://github.com/OpenLiberty/liberty-language-server/issues). Be sure to search the existing issues to see if your issue has already been raised.

A good bug report is one that makes it easy for everyone to understand what you were trying to do and what went wrong. Provide as much context as possible so we can try to recreate the issue.

## Contributor License Agreement
If you are contributing code changes via a pull request for anything except trivial changes, you must signoff on the [Individual Contributor License Agreement](https://github.com/OpenLiberty/open-liberty/blob/release/cla/open-liberty-cla-individual.pdf). If you are doing this as part of your job you may also wish to get your employer to sign a CCLA [Corporate Contributor License Agreement](https://github.com/OpenLiberty/open-liberty/blob/release/cla/open-liberty-cla-corporate.pdf). Instructions on how to sign and submit these agreements are located at the top of each document. Trivial changes such as typos, redundant spaces, minor formatting, and spelling errors will be labeled as `CLA trivial`, and don't require a signed CLA for consideration.

After we obtain the signed CLA, you are welcome to open a pull request, and the team will be notified for review. We ask that you follow these steps through the submission process.
1. Ensure you run a passing local maven build explained in [DEVELOPING](./DEVELOPING.md) before opening a PR.
2. Open PRs against the `main` branch.
3. A `CLA signed` or `CLA trivial` label will be added, depending on the nature of the change.
4. A team of "reviewers" will be notified, will perform a review, and if approved, will merge the PR.
5. Reviewer comments, questions, or suggestions must be addressed before the PR is approved.

## Coding Standards
Please ensure you follow the coding standards used throughout the existing code base. Some basic rules include:
* All files must have a Copyright including the Eclipse Public License in the header.
* Indent with 4 spaces, no tabs.
* Opening brace on same line as `if` / `else` / `for` / etc. statements. Closing brace on its own line.
* All PRs must have a passing build.