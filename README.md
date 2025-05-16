[![FINOS - Incubating](https://cdn.jsdelivr.net/gh/finos/contrib-toolbox@master/images/badge-incubating.svg)](https://community.finos.org/docs/governance/Software-Projects/stages/incubating)

<img align="right" width="40%" src="https://www.finos.org/hubfs/FINOS/finos-logo/FINOS_Icon_Wordmark_Name_RGB_horizontal.png">

# Rune Python Generator

This repository creates a generator that will produce Python from a model developed using [Rune](https://github.com/finos/rune-dsl).  The generated Python relies upon the [RunePythonRuntime]() library.
 
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

## Installation Steps
Detailed build and testing instructions can be found in [BUILDANDTEST.md](./BUILDANDTEST.md)

A quick overview follows:

1. Make a local copy of this repo
2. Build and test using Maven
```
cd rune-python-generator
mvn -s clean install
```
All the tests should pass.


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
The script will use the CDM from the branch specified in the file (E.G. master) of the [FINOS Repo](https://github.com/finos/common-domain-model) and generate a wheel in the project directory `target/python-cdm`

To use a different version of CDM, update CDM_VERSION in the script.

## Contributors
- [CLOUDRISK Limited](https://www.cloudrisk.uk), email: info@cloudrisk.com
- [FT Advisory LLC](https://www.ftadvisory.co), email: info@ftadvisory.co
- [TradeHeader SL](https://www.tradeheader.com), email: info@tradeheader.com

## Governance

This project implements https://community.finos.org/docs/governance/#open-source-software-projects

## License

Copyright 2019 Fintech Open Source Foundation

Distributed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).

SPDX-License-Identifier: [Apache-2.0](https://spdx.org/licenses/Apache-2.0)
