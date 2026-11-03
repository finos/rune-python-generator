[![FINOS - Incubating](https://cdn.jsdelivr.net/gh/finos/contrib-toolbox@master/images/badge-incubating.svg)](https://community.finos.org/docs/governance/Software-Projects/stages/incubating)[![OpenSSF Best Practices](https://www.bestpractices.dev/projects/10725/badge)](https://www.bestpractices.dev/projects/10725)[![Maven CI](https://github.com/finos/rune-python-generator/actions/workflows/scan-cve.yml/badge.svg)](https://github.com/finos/rune-python-generator/actions/workflows/scan-cve.yml)

<img align="right" width="15%" alt="FINOS" src="https://www.finos.org/hubfs/FINOS/finos-logo/FINOS_Icon_Wordmark_Name_RGB_horizontal.png">


# Rune Python Generator

The Rune Python Generator creates Python code from [Rune](https://github.com/finos/rune-dsl).  It fully supports the Rune type syntax and has comprehensive [expression coverage](./docs/EXPRESSION_SUPPORT.md), though function generation is not yet fully implemented.

The generated code requires the [RunePythonRuntime](https://github.com/finos/rune-python-runtime) library and Python 3.11+.

## Releases

Releases are [triggered manually](./.github/workflows.yml) and follow a four-part semantic versioning scheme (x.y.z.n). The first three numbers match the **rosetta.dsl.version** in the **pom.xml**, and the fourth number tracks generator changes.

For example, when the DSL version updates to **9.64.1**, the first release will be tagged **9.64.1.0**. Subsequent releases will only increment the fourth digit (e.g., **9.64.1.1**) until the DSL version changes again.

Releases **should** not be manually tagged.

### Release Notes

The features of the current version can be found in the [release notes](./RELEASE.md)

### Rune-DSL Updates

The Renovate bot automatically handles DSL updates by creating a pull request. Maintainers can merge this PR if it passes the automated build and unit tests. Manual changes including a new PR are required if the tests fail.

## Repository Organization

- `README.md` - this file, for documentation purposes
- `RELEASE.md` - information about the current release
- `src/main`  - Java/Xtend code to generate Python from Rune
- `src/test`  - Java/Xtend code to run JUnit tests on the code generation process
- `docs/BUILD_AND_TEST.md` - instructions on building and testing the repo
- `test` - Python unit tests of the generated code

## Development setup

### For developers

This guide is meant for everyone who wants to contribute to the Rune Python Generator and needs to get things up and running.

Detailed build and testing instructions can be found in [docs/BUILD_AND_TEST.md](./docs/BUILD_AND_TEST.md)

Troubleshooting: If you run into issues, please open an [issue](https://github.com/finos/rune-python-generator/issues). This helps us improve the guide for everyone.

### Coding Standards

- Indentation: Since Python is sensitive to indentation, ensure your submission uses four spaces for each indentation instead of tabs.
- Java vs xTend: since the intention is to replace xTend, all new code should be written in Java.  Ideally, changes to existing code will migrate xTend code to Java.

### 1. Building with Maven

Start by cloning the project: `git clone https://github.com/finos/rune-python-generator`

Our project runs with Java 21. Make sure that your Maven also uses this version of Java by running `mvn -v`.

To build the project, run `mvn clean package`.

#### UNIT Testing

Building the project using Maven will run JUNIT-based unit tests.  All tests should pass.  To run the Python unit tests follow the instructions in [docs/BUILD_AND_TEST.md](./docs/BUILD_AND_TEST.md)

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

To generate Python code from a single file or directory, after building the project use the standalone CLI by executing the following command:

```bash
java -cp target/python-0.0.0.main-SNAPSHOT.jar com.regnosys.rosetta.generator.python.PythonCodeGeneratorCLI
```

### Using the Generated Code

The generated Python classes are standard [Pydantic v2](https://docs.pydantic.dev/) models that extend `BaseDataClass` from the [rune-python-runtime](https://github.com/finos/rune-python-runtime). Each generated package exposes a `rune_deserialize` helper and every model instance exposes `rune_serialize`.

#### Constructing objects

Import and instantiate a generated class like any Pydantic model:

```python
from finos.cdm.base.math.UnitType import UnitType
from finos.cdm.base.math.FinancialUnitEnum import FinancialUnitEnum

unit = UnitType(financialUnit=FinancialUnitEnum.SHARE)
```

#### Serializing to JSON

Call `rune_serialize()` on any root model object. The method validates the model, then returns a compact or indented JSON string that includes the Rune `@type`, `@model`, and `@version` fields required for round-trip deserialization.

```python
json_str = unit.rune_serialize(indent=2)
# {
#   "@type": "cdm.base.math.UnitType",
#   "@model": "cdm",
#   "@version": "0.0.0",
#   "financialUnit": "Share"
# }
```

To skip pre-serialization validation (for example when working with incomplete objects during construction):

```python
json_str = unit.rune_serialize(validate_model=False)
```

#### Deserializing from JSON

Use the package-level `rune_deserialize` wrapper rather than calling `model_validate` directly. The wrapper resolves the correct namespace prefix so that `@type` strings in the JSON map to the right Python class:

```python
import finos

obj = finos.rune_deserialize(json_str)
print(type(obj).__name__)   # UnitType
print(obj.financialUnit)    # FinancialUnitEnum.SHARE
```

Pass `validate_model=False` to skip post-deserialization constraint checking (useful when ingesting data that may not yet satisfy all conditions):

```python
obj = finos.rune_deserialize(json_str, validate_model=False)
```

The function accepts either a JSON string or a pre-parsed `dict`.

### Generating CDM from Rune

To generate the Python version of the Common Domain Model (CDM), use the **build_cdm.sh** script. This script uses the branch specified in the file and creates a wheel in target/python-cdm.

```bash
test/cdm_tests/cdm_setup/build_cdm.sh
```

To use a different version of CDM, simply update the **CDM_VERSION** variable in the script.

## Roadmap, Contributing, Getting in Touch, and Governance

### Roadmap

The project roadmap is aligned with the [Rune-DSL](https://github.com/finos/rune-dsl/) and [CDM](https://github.com/finos/common-domain-model/blob/master/ROADMAP.md) roadmaps.

### Contributing

We welcome contributions! Please follow these steps to get started:

1. Fork the repository (<https://github.com/finos/rune-python-generator/fork>)
2. Create your feature branch (`git checkout -b feature/fooBar`)
3. Read our [contribution guidelines](.github/CONTRIBUTING.md) and [Community Code of Conduct](https://www.finos.org/code-of-conduct)
4. Commit your changes (`git commit -am 'Add some fooBar'`)
5. Push to the branch (`git push origin feature/fooBar`)
6. Create a new Pull Request: the intention is to ensure that Main is always production ready and matches a release.
   - Simple / short running changes : the PR should be to Main
   - Complex / long running changes: the recommendation is to complete all the changes in a staging branch, such as Develop or another branch of your choosing.  Once the change is complete and passes all tests, then raise the PR to main from your staging branch.

**NOTE: FINOS repositories require an executed and active FINOS Individual Contributor License Agreement (ICLA) or an executed and active FINOS Corporate Contribution License Agreement (CCLA). The FINOS Clabot tool (or EasyCLA) will flag and block commits from individuals not covered. Please note that some CCLAs require individuals/employees to be explicitly named on the CCLA.**

If you are unsure if you are covered under an existing CCLA send an email to <help@finos.org>

### Get In Touch

Get in touch with the Rune Python Generator team by creating a [GitHub issue](https://github.com/finos/rune-python-generator/issues/new) and labelling it with "help wanted".

We also encourage the community to get in touch via the [FINOS Slack](https://www.finos.org/blog/finos-announces-new-community-slack).

### Governance

This project is governed by the FINOS Open Source Software Project Governance <https://community.finos.org/docs/governance/#open-source-software-projects>

### License

Copyright 2023-2026 CLOUDRISK Limited and FT Advisory LLC

Distributed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).

## Contributors

- [CLOUDRISK Limited](https://www.cloudrisk.uk), email: <info@cloudrisk.com>
- [FT Advisory LLC](https://www.ftadvisory.co), email: <info@ftadvisory.co>
- [TradeHeader SL](https://www.tradeheader.com), email: <info@tradeheader.com>

SPDX-License-Identifier: [Apache-2.0](https://spdx.org/licenses/Apache-2.0)
