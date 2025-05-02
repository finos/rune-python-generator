<img align="right" width="40%" src="https://www.finos.org/hubfs/FINOS/finos-logo/FINOS_Icon_Wordmark_Name_RGB_horizontal.png">

# Rune Python Generator

This repository creates a generator that will produce Python from a model developed using [Rune](https://github.com/finos/rune-dsl).  The generated Python relies upon the [RunePythonRuntime]() library.
 
The Python package requires Python version 3.11+.

## Repository Organization

- `README.md` - this file, for documentation purposes
- `src/main`  - Java/Xtend code to generate Python from Rune
- `src/test`  - Java/Xtend code to run JUnit tests on the code generation process
- `build` - configuration scripts to setup and tear down the Python unit testing environment
- `build/build_cdm.sh` - used to create a Python package from code generated using CDM Rune definitions
- `test` - Python unit tests and scripts to run the tests

## Prerequisites

[Eclipse 2021 (JSEE) + xtend support Eclipse IDE for Java and DSL Developers](https://www.eclipse.org/downloads/packages/release/2021-12/r/eclipse-ide-java-and-dsl-developers)

[Git](https://git-scm.com/)

[Maven](http://maven.apache.org/)

## Installation Steps
1. Create a directory structure hereafter referred to as [CODEGEN]
```
mkdir -p [CODEGEN]
```
2. Fork and clone the generator 
Fork a copy from `https://github.com/REGnosys/rune-python-generator` ([MYREPO])
```
cd [CODEGEN]
git clone https://github.com/[MYREPO]/rune-python-generator.git
```
3. Build and test using Maven
```
cd rune-python-generator
mvn -s clean install
```
All the tests should pass.

More build and testing instructions can be found in [BUILDANDTEST.md](./BUILDANDTEST.md)

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

# To Generate CDM from Rune

Use this script to generated the Python version of CDM
```sh
build/build_cdm.sh
```
The script will use the CDM from the master branch of the [FINOS Repo](https://github.com/finos/common-domain-model) and generate a wheel in the project directory `target/python-cdm`

To use a different version of CDM, update CDM_VERSION in the script.

## Contributors
- [CloudRisk](https://www.cloudrisk.uk)
- [FT Advisory LLC](https://www.ftadvisory.co)
- [TradeHeader SL](https://www.tradeheader.com)

## Governance

This project implements https://community.finos.org/docs/governance/#open-source-software-projects

## License

Copyright 2019 Fintech Open Source Foundation

Distributed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).

SPDX-License-Identifier: [Apache-2.0](https://spdx.org/licenses/Apache-2.0)
