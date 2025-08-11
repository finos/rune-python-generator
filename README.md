[![FINOS - Incubating](https://cdn.jsdelivr.net/gh/finos/contrib-toolbox@master/images/badge-incubating.svg)](https://community.finos.org/docs/governance/Software-Projects/stages/incubating)[![OpenSSF Best Practices](https://www.bestpractices.dev/projects/10725/badge)](https://www.bestpractices.dev/projects/10725)[![Maven CI](https://github.com/finos/rune-python-generator/actions/workflows/cve-scanning.yml/badge.svg)](https://github.com/finos/rune-python-generator/actions/workflows/cve-scanning.yml)


<img align="right" width="15%" src="https://www.finos.org/hubfs/FINOS/finos-logo/FINOS_Icon_Wordmark_Name_RGB_horizontal.png">

# Rune Python Generator

*Rune Python Generator* - the generator enables creation of Python from [Rune](https://github.com/finos/rune-dsl).  It supports the full Rune type syntax, and, as described in [EXPRESSION_SUPPORT.md](./EXPRESSION_SUPPORT.md), expression coverage is comprehensive.  The generator does not yet fully implement function generation.

The generated Python relies upon the [RunePythonRuntime](https://github.com/finos/rune-python-runtime) library and requires Python version 3.11+.

## Release Notes
The features of the current version can be found in the [release notes](./RELEASE.md)

## Repository Organization

- `README.md` - this file, for documentation purposes
- `BUILDANDTEST.md` - instructions on building and testing the repo
- `RELEASE.md` - information about the current release
- `src/main`  - Java/Xtend code to generate Python from Rune
- `src/test`  - Java/Xtend code to run JUnit tests on the code generation process
- `build` - configuration scripts to setup and tear down the Python unit testing environment
- `build/build_cdm.sh` - used to create a Python package from code generated using CDM Rune definitions
- `test` - Python unit tests and scripts to run the tests

## Development setup

### Setup for developers
This guide is meant for everyone who wants to contribute to the Rune Python Generator and needs to get things up and running.

Detailed build and testing instructions can be found in [BUILDANDTEST.md](./BUILDANDTEST.md)

If this guide does not work for you, be sure to raise an issue. This way we can help you figure out what the problem is and update this guide to prevent the same problem for future users.

### 1. Building with Maven
Start by cloning the project: `git clone https://github.com/finos/rune-python-generator`

Our project runs with Java 21. Make sure that your Maven also uses this version of Java by running `mvn -v`.

To build the project, run `mvn clean package`.

#### UNIT Testing
Building the project using Maven will run JUNIT-based unit tests.  All tests should pass.  To run the Python unit tests follow the instructions in [BUILDANDTEST.md](./BUILDANDTEST.md)

### 2. Setting things up in Eclipse
#### Install Eclipse IDE for Java and DSL Developers
Install version `2025-06` of the "Eclipse IDE for Java and DSL Developers" using the [Eclipse Installer](https://www.eclipse.org/downloads/packages/installer). You might have to enable "Advanced Mode" in the settings of the Eclipse Installer to install a specific version.

#### Configure Eclipse with the right version of Java
Xtend files cannot be built with any Java version later than 21. In Eclipse, go to Settings... > Java > Installed JREs and make sure the checked JRE points to a Java version of 21.

#### Install the Checkstyle plugin
We use [Checkstyle](https://checkstyle.sourceforge.io/) for enforcing good coding practices. The Eclipse plugin for Checkstyle can be found here: [https://checkstyle.org/eclipse-cs/#!/](https://checkstyle.org/eclipse-cs/#!/).

#### Open the project in Eclipse
Go to Import... > Existing Maven Project, select the right folder, click Finish.

### Standalone CLI
The generator includes a standalone CLI which can be invoked to generate Python from a single file or from directory.  To invoke the CLI, first build the project and then:

```sh
java -cp target/python-0.0.0.main-SNAPSHOT.jar com.regnosys.rosetta.generator.python.PythonCodeGeneratorCLI
```

### To Generate CDM from Rune

Use this script to generated the Python version of CDM
```sh
test/cdm_tests/cdm_setup/build_cdm.sh
```
The script will use the CDM from the branch specified in the file (E.G. master) of the [FINOS Repo](https://github.com/finos/common-domain-model) and generate a wheel in the project directory `target/python-cdm`

To use a different version of CDM, update CDM_VERSION in the script.

## Roadmap

The Roadmap will be aligned to the [Rune-DSL](https://github.com/finos/rune-dsl/) and [CDM](https://github.com/finos/common-domain-model/blob/master/ROADMAP.md) roadmaps.

### Rune-DSL Updates

Renovate will generate a PR when the version of the DSL has been updated at com.regnosys.rosetta:com.regnosys.rosetta.  The PR will calrify whether the change succsessfully builds and passes JUNIT and Python unit testing.

Any maintainer can merge changes that successfully build and pass the tests.

Build or testing failures should be escalated to [@plamen-neykov](https://github.com/plamen-neykov) or [@dschwartznyc](https://github.com/dschwartznyc) for remediation.

## Contributing
For any questions, bugs or feature requests please open an [issue](https://github.com/finos/rune-python-generator/issues)
For anything else please send an email to {project mailing list}.

To submit a contribution:
1. Fork it (<https://github.com/finos/rune-python-generator/fork>)
2. Create your feature branch (`git checkout -b feature/fooBar`)
3. Read our [contribution guidelines](.github/CONTRIBUTING.md) and [Community Code of Conduct](https://www.finos.org/code-of-conduct)
4. Commit your changes (`git commit -am 'Add some fooBar'`)
5. Push to the branch (`git push origin feature/fooBar`)
6. Create a new Pull Request

_NOTE:_ Commits and pull requests to FINOS repositories will only be accepted from those contributors with an active, executed Individual Contributor License Agreement (ICLA) with FINOS OR
who are covered under an existing and active Corporate Contribution License Agreement (CCLA) executed with FINOS. Commits from individuals not covered under an ICLA or CCLA will be flagged
and blocked by the FINOS Clabot tool (or EasyCLA). Please note that some CCLAs require individuals/employees to be explicitly named on the CCLA.

If you are unsure if you are covered under an existing CCLA send an email to help@finos.org

## Get in touch with the Rune Python Generator Team

 Get in touch with the Rune team by creating a [GitHub issue](https://github.com/finos/rune-python-generator/issues/new) and labelling it with "help wanted".

 We encourage the community to get in touch via the [FINOS Slack](https://www.finos.org/blog/finos-announces-new-community-slack).

## Governance

This project implements https://community.finos.org/docs/governance/#open-source-software-projects

## License

Copyright 2023-2025 CLOUDRISK Limited and FT Advisory LLC

Distributed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).

SPDX-License-Identifier: [Apache-2.0](https://spdx.org/licenses/Apache-2.0)

## Contributors

- [CLOUDRISK Limited](https://www.cloudrisk.uk), email: info@cloudrisk.com
- [FT Advisory LLC](https://www.ftadvisory.co), email: info@ftadvisory.co
- [TradeHeader SL](https://www.tradeheader.com), email: info@tradeheader.com

SPDX-License-Identifier: [Apache-2.0](https://spdx.org/licenses/Apache-2.0)
