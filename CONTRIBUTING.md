# How to contribute
We would love to have your help. Before you start working however, please read
and follow this short guide.

# Reporting Issues
Before you open an issue on GitHub, please discuss it on the
[community forum](https://community.jitsi.org) and wait for a 
confirmation from one of the committers. Once you have that confirmation, 
please proceed to reporting the issue on GitHub, while providing as much 
information as possible. Mention the version of Jitsi you are using, your 
operating system and explain how the problem can be reproduced.

Please note that issues that are reported against a version older than the 
current stable release will be closed without further investigation. If you 
discover however that the issue also exists in an older version, this is 
useful information. Even more useful is to know if it worked in a specific 
version and stopped working in a subsequent version. This will help 
developers pinpoint the issue. Also useful is to test the nightly build.

# Code contributions
Found a bug and know how to fix it? Great! Please read on.

## Contributor License Agreement
While Jitsi is released under the
[Apache License](https://github.com/jitsi/jitsi/blob/master/LICENSE), the copyright
holder and principal creator is [8x8](https://www.8x8.com/). To
ensure that we can continue making Jitsi available under an open source license,
we need you to sign our Apache-based contributor 
license agreement as either a [corporation](https://jitsi.org/ccla) or an 
[individual](https://jitsi.org/icla). If you cannot accept the terms laid out 
in the agreement, unfortunately, we cannot accept your contribution.

## Coding Rules
- Please read and follow the [code conventions](https://desktop.jitsi.org/Documentation/CodeConvention),
  especially the limit on 80 characters per line.
- Do not reformat existing code.
- Command-Line [build instructions](https://desktop.jitsi.org/Documentation/RetrievingAndBuildingTheSources)
- How to set up [Eclipse](https://desktop.jitsi.org/Documentation/ConfigureEclipseNew)
- Read the [tutorials](https://desktop.jitsi.org/Documentation/DeveloperDocumentation) (some of this information might be a bit dated, but it is still a very useful resource)

## Creating Pull Requests
- Perform **one** logical change per pull request.
- Maintain a clean list of commits, squash them if necessary.
- Rebase your topic branch on top of the master branch before creating the pull request.
