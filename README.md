[![FINOS - Incubating](https://cdn.jsdelivr.net/gh/finos/contrib-toolbox@master/images/badge-incubating.svg)](https://community.finos.org/docs/governance/Software-Projects/stages/incubating)

<img align="right" width="15%" src="https://www.finos.org/hubfs/FINOS/finos-logo/FINOS_Icon_Wordmark_Name_RGB_horizontal.png">

# Rune Python Generator

This repository creates a generator that will produce Python from a model developed using [Rune](https://github.com/finos/rune-dsl).  The generated Python relies upon the [RunePythonRuntime]() library.

**Continuous Integration:** 

*Rune Python Generator* - the generator supports creation of Python for the full Rune type syntax, and, as described in [EXPRESSION_SUPPORT.md](./EXPRESSION_SUPPORT.md), expression coverage is comprehensive.  The generator does not yet fully implement function generation.
 
The Python package requires Python version 3.11+.

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
This guide is meant for everyone who wants to contribute to the Rune Pyhton Generator and needs to get things up and running.

Detailed build and testing instructions can be found in [BUILDANDTEST.md](./BUILDANDTEST.md)

If this guide does not work for you, be sure to raise an issue. This way we can help you figure out what the problem is and update this guide to prevent the same problem for future users.

### 1. Building with Maven
Start by cloning the project: `git clone https://github.com/regnosys/rune-python-generator`

Our project runs with Java 21. Make sure that your Maven also uses this version of Java by running `mvn -v`.

To build the project, run `mvn clean install`.

All the tests should pass.

### 2. Setting things up in Eclipse
#### Install Eclipse IDE for Java and DSL Developers
Install version `2025-06` of the "Eclipse IDE for Java and DSL Developers" using the [Eclipse Installer](https://www.eclipse.org/downloads/packages/installer). You might have to enable "Advanced Mode" in the settings of the Eclipse Installer to install a specific version.

#### Configure Eclipse with the right version of Java
Xtend files cannot be build with any Java version later than 21. In Eclipse, go to Settings... > Java > Installed JREs and make sure the checked JRE points to a Java version of 21.

#### Install the Checkstyle plugin
We use [Checkstyle](https://checkstyle.sourceforge.io/) for enforcing good coding practices. The Eclipse plugin for Checkstyle can be found here: [https://checkstyle.org/eclipse-cs/#!/](https://checkstyle.org/eclipse-cs/#!/).

#### Open the project in Eclipse
Go to Import... > Existing Maven Project, select the right folder, click Finish.

### To Generate CDM from Rune

Use this script to generated the Python version of CDM
```sh
build/build_cdm.sh
```
The script will use the CDM from the branch specified in the file (E.G. master) of the [FINOS Repo](https://github.com/finos/common-domain-model) and generate a wheel in the project directory `target/python-cdm`

To use a different version of CDM, update CDM_VERSION in the script.

## Roadmap

The roadmap follows the roadmap for the [Rune-DSL](https://github.com/finos/rune-dsl/)

## Contributing
For any questions, bugs or feature requests please open an [issue](https://github.com/regnosys/rune-python-generator/issues)
For anything else please send an email to {project mailing list}.

To submit a contribution:
1. Fork it (<https://github.com/regnosys/rune-python-generator/fork>)
2. Create your feature branch (`git checkout -b feature/fooBar`)
3. Read our [contribution guidelines](.github/CONTRIBUTING.md) and [Community Code of Conduct](https://www.finos.org/code-of-conduct)
4. Commit your changes (`git commit -am 'Add some fooBar'`)
5. Push to the branch (`git push origin feature/fooBar`)
6. Create a new Pull Request

_NOTE:_ Commits and pull requests to FINOS repositories will only be accepted from those contributors with an active, executed Individual Contributor License Agreement (ICLA) with FINOS OR who are covered under an existing and active Corporate Contribution License Agreement (CCLA) executed with FINOS. Commits from individuals not covered under an ICLA or CCLA will be flagged and blocked by the FINOS Clabot tool (or EasyCLA). Please note that some CCLAs require individuals/employees to be explicitly named on the CCLA.

Unsure if you are covered under an existing CCLA? Email help@finos.org*

## Get in touch with the Rune Python Generator Team

 Get in touch with the Rune team by creating a [GitHub issue](https://github.com/REGnosys/rune-python-dsl/issues/new) and labelling it with "help wanted".

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