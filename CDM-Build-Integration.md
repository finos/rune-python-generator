# Feature Request - CDM Python Generator Integration

## Description of Problem

With the Python generator now a standalone component, its integration with the CDM build needs to be updated. This presents two main challenges:

1. **Consistency with CDM's Rosetta Bundle Version:**
To keep CDM in sync with all elements of the Rune DSL infrastructure (rosetta-common, rosetta-testing, rosetta-code-generators, rosetta-ingest), the CDM build specifies which version to use. Previously, the Python generator was kept in sync as part of the rosetta-code-generators repository, with updates whenever there was an update to the version in that repository. While the generated Python code does not directly depend on it, coordinating updates across all elements of the Rune infrastructure helps prevent inconsistencies in enterprise production enviroments — especially if a dsl update addresses a defect or changes behavior in a way that impacts the generators.

2. **Invocation of the standalone Python generator**:
The CDM build now needs to explicitly invoke the unbundled Python generator.

---

## Potential Solutions

Below are both a target state and interim steps to enable integration before the target state is fully realized.

### Target state

#### Assumptions

- The generator includes an executable Main class that accepts a Rune source and a target folder for the generated Python.
- The Rune Python Runtime is available via PyPI.

#### Process

1. An update to the Rosetta DSL Version triggers a rebuild of the generator.  If successful, a release tagged with the DSL version is generated.

2. The CDM build sources a generator version that matches its Rosetta DSL Version.  The sequence is:

    1. Source the generator
    2. If found:
        1. Generate Python by invoking the new Main class using the source CDM Rune files.
        2. Package the resultung Python.
        3. Upload the package to PyPI.
    3. If not found: fail gracefully generating notice of the error.

### Interim state

#### Assumptions

- The generator is invoked using the current mechanism.
- Python artifacts are distributed via the CDM Maven Repository.

1. A scheduled action rebuilds the generator if the Rosetta Bundle Version used in CDM does not match any of tagged versions.  If successful, a release tagged with the bundle version is generated and stored as a repo artifact.  

_Note: The downside of this approach is that there will be CDM builds that will not find a matching generator._
2. The CDM build sources a generator that matches its Rosetta Bundle Version by querying the generator repo.  The sequence is:
    1. Source the generator
    2. If found:
        1. Generate Python by executing

        ```sh
        mvn clean install -DskipTests -P python
        ```

        2. Package the Python.
        3. Upload the package to the Maven repository.
    3. If not found: fail gracefully generating notice of the error

## Questions

- Should the package also be made available as a Maven artifact?
