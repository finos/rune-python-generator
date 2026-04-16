/*
 * Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
 * SPDX-License-Identifier: Apache-2.0
 */
package com.regnosys.rosetta.generator.python;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.xtext.util.CancelIndicator;
import org.eclipse.xtext.validation.CheckMode;
import org.eclipse.xtext.validation.IResourceValidator;
import org.eclipse.xtext.validation.Issue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.regnosys.rosetta.RosettaRuntimeModule;
import com.regnosys.rosetta.RosettaStandaloneSetup;
import com.regnosys.rosetta.builtin.RosettaBuiltinsService;
import com.regnosys.rosetta.generator.external.ExternalGenerator;
import com.regnosys.rosetta.generator.external.ExternalGenerators;
import com.regnosys.rosetta.generator.python.util.PythonCodeGeneratorConstants;
import com.regnosys.rosetta.rosetta.RosettaModel;
import com.regnosys.rosetta.rosetta.RosettaNamed;

/**
 * Command-line interface for generating Python code from Rosetta models.
 * <p>
 * This CLI tool loads Rosetta model files (either from a directory or a single
 * file),
 * invokes the {@link PythonCodeGenerator}, and writes the generated Python code
 * to the specified target directory.
 * It is intended for use by developers and build systems to automate the
 * translation of Rosetta DSL models to Python.
 * </p>
 *
 * <h2>Usage</h2>
 * 
 * <pre>
 *   java -cp &lt;your-jar-or-classpath&gt; com.regnosys.rosetta.generator.python.PythonCodeGeneratorCLI -s &lt;source-dir&gt; -t &lt;target-dir&gt;
 *   java -cp &lt;your-jar-or-classpath&gt; com.regnosys.rosetta.generator.python.PythonCodeGeneratorCLI -f &lt;source-file&gt; -t &lt;target-dir&gt;
 * </pre>
 * <ul>
 * <li><b>-s, --dir &lt;source-dir&gt;</b>: Source directory containing Rosetta
 * files (all <code>.rosetta</code> files will be processed)</li>
 * <li><b>-f, --file &lt;source-file&gt;</b>: Single Rosetta file to
 * process</li>
 * <li><b>-t, --tgt &lt;target-dir&gt;</b>: Target directory for generated
 * Python code (defaults to <code>./python</code> if not specified)</li>
 * <li><b>-v, --version &lt;version&gt;</b>: Version number for the generated
 * package, in <code>#.#.#</code> format (defaults to
 * <code>0.0.0</code>)</li>
 * <li><b>-p, --project-name &lt;projectName&gt;</b>: Override the
 * <code>pyproject.toml</code> project name (defaults to
 * <code>python-&lt;first-namespace-segment&gt;</code>)</li>
 * <li><b>-e, --allow-errors</b>: Continue generation even if validation
 * errors are present</li>
 * <li><b>-w, --fail-on-warnings</b>: Treat validation warnings as
 * errors</li>
 * <li><b>-h</b>: Print usage/help</li>
 * </ul>
 *
 * <h2>Example</h2>
 * 
 * <pre>
 *   java -jar target/python-0.0.0.main-SNAPSHOT-shaded.jar -s src/main/rosetta -t build/python
 * </pre>
 *
 * <h2>Notes</h2>
 * <ul>
 * <li>Either <b>-s</b> or <b>-f</b> must be specified.</li>
 * <li>The tool will clean the target directory before writing new files.</li>
 * <li>Requires a Java 11+ runtime and all dependencies on the classpath (or use
 * the shaded/uber jar).</li>
 * </ul>
 *
 * @author Plamen Neykov
 * @author Daniel Schwartz
 * @see PythonCodeGenerator
 */

public class PythonCodeGeneratorCLI {
    /**
     * Logger for the PythonCodeGeneratorCLI class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(PythonCodeGeneratorCLI.class);

    /**
     * Default version used when no {@code -v} option is provided.
     */
    static final String DEFAULT_VERSION = "0.0.0";

    /**
     * Regex that a version string must fully match: three dot-separated integers.
     */
    private static final Pattern VALID_VERSION_PATTERN = Pattern.compile("\\d+\\.\\d+\\.\\d+");
    private static final Pattern DEV_VERSION_PATTERN = Pattern.compile("(\\d+\\.\\d+\\.\\d+)-dev\\.(\\d+)");

    /**
     * Public constructor for the CLI tool.
     */
    public PythonCodeGeneratorCLI() {
    }

    public static void main(String[] args) {
        int exitCode = new PythonCodeGeneratorCLI().run(args);
        System.exit(exitCode);
    }

    /**
     * Executes the CLI tool.
     *
     * @param args command line arguments
     * @return exit code
     */
    public final int run(String[] args) {
        LOGGER.info("***** Running PythonCodeGeneratorCLI v2 *****");
        Options options = new Options();
        Option help = new Option("h", "Print usage");
        Option srcDirOpt = Option.builder("s").longOpt("dir").argName("srcDir").desc("Source Rosetta directory")
                .hasArg().build();
        Option srcFileOpt = Option.builder("f").longOpt("file").argName("srcFile").desc("Source Rosetta file").hasArg()
                .build();
        Option tgtDirOpt = Option.builder("t").longOpt("tgt").argName("tgtDir")
                .desc("Target Python directory (default: ./python)").hasArg().build();
        Option allowErrorsOpt = Option.builder("e").longOpt("allow-errors").desc("Continue even if there are validation errors").build();
        Option failOnWarningsOpt = Option.builder("w").longOpt("fail-on-warnings").desc("Fail if there are validation warnings").build();
        Option projectNameOpt = Option.builder("p").longOpt("project-name").argName("projectName")
                .desc("Override the pyproject.toml project name (default: python-<first-namespace-segment>)")
                .hasArg().build();
        Option versionOpt = Option.builder("v").longOpt("version").argName("version")
                .desc("Package version in #.#.# format (default: " + DEFAULT_VERSION + ")")
                .hasArg().build();
        Option namespacePrefixOpt = Option.builder("x").longOpt("namespace-prefix").argName("namespacePrefix")
                .desc("Prefix to prepend to every generated namespace (e.g. finos)")
                .hasArg().build();
        Option nativeDirOpt = Option.builder("n").longOpt("native-dir").argName("nativeDir")
                .desc("Source directory containing native function implementations to copy into the generated package")
                .hasArg().build();

        options.addOption(help);
        options.addOption(srcDirOpt);
        options.addOption(srcFileOpt);
        options.addOption(tgtDirOpt);
        options.addOption(allowErrorsOpt);
        options.addOption(failOnWarningsOpt);
        options.addOption(projectNameOpt);
        options.addOption(versionOpt);
        options.addOption(namespacePrefixOpt);
        options.addOption(nativeDirOpt);

        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cmd = parser.parse(options, args);
            if (cmd.hasOption("h")) {
                printUsage(options);
                return 0;
            }
            String tgtDir = cmd.getOptionValue("t", "./python");
            boolean allowErrors = cmd.hasOption("e");
            boolean failOnWarnings = cmd.hasOption("w");
            String projectName = cmd.getOptionValue("p");
            String namespacePrefix = cmd.getOptionValue("x");
            String nativeDir = cmd.getOptionValue("n");

            String version = DEFAULT_VERSION;
            if (cmd.hasOption("v")) {
                String rawVersion = cmd.getOptionValue("v");
                java.util.regex.Matcher devMatcher = DEV_VERSION_PATTERN.matcher(rawVersion);
                if (devMatcher.matches()) {
                    version = devMatcher.group(1) + ".dev" + devMatcher.group(2);
                } else if (VALID_VERSION_PATTERN.matcher(rawVersion).matches()) {
                    version = rawVersion;
                } else {
                    LOGGER.error("Invalid version format '{}'. Expected #.#.# (e.g. 1.2.3) or #.#.#-dev.# (e.g. 1.2.3-dev.4).", rawVersion);
                    return 1;
                }
            }

            if (projectName == null || projectName.isBlank()) {
                LOGGER.error("Project name (-p / --project-name) is required.");
                printUsage(options);
                return 1;
            }

            if (cmd.hasOption("s")) {
                String srcDir = cmd.getOptionValue("s");
                return translateFromSourceDir(srcDir, tgtDir, allowErrors, failOnWarnings, projectName, version, namespacePrefix, nativeDir);
            } else if (cmd.hasOption("f")) {
                String srcFile = cmd.getOptionValue("f");
                return translateFromSourceFile(srcFile, tgtDir, allowErrors, failOnWarnings, projectName, version, namespacePrefix, nativeDir);
            } else {
                LOGGER.error("Either a source directory (-s) or source file (-f) must be specified.");
                printUsage(options);
                return 1;
            }
        } catch (ParseException e) {
            LOGGER.error("Failed to parse command line arguments: {}", e.getMessage());
            printUsage(options);
            return 1;
        }
    }

    private static void printUsage(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("PythonCodeGeneratorCLI", options, true);
    }

    private int translateFromSourceDir(
        String srcDir,
        String tgtDir,
        boolean allowErrors,
        boolean failOnWarnings,
        String projectName,
        String version,
        String namespacePrefix,
        String nativeDir
    ) {
        // Find all .rosetta files in a directory
        Path srcDirPath = Paths.get(srcDir);
        if (!Files.exists(srcDirPath)) {
            LOGGER.error("Source directory does not exist: {}", srcDir);
            return 1;
        }
        if (!Files.isDirectory(srcDirPath)) {
            LOGGER.error("Source directory is not a directory: {}", srcDir);
            return 1;
        }
        try {
            List<Path> rosettaFiles = Files.walk(srcDirPath)
                    .filter(Files::isRegularFile)
                    .filter(f -> f.getFileName().toString().endsWith(".rosetta"))
                    .collect(Collectors.toList());
            return processRosettaFiles(rosettaFiles, tgtDir, allowErrors, failOnWarnings, projectName, version, namespacePrefix, nativeDir);
        } catch (IOException e) {
            LOGGER.error("Failed to process source directory: {}", srcDir, e);
            return 1;
        }
    }

    private int translateFromSourceFile(
        String srcFile,
        String tgtDir,
        boolean allowErrors,
        boolean failOnWarnings,
        String projectName,
        String version,
        String namespacePrefix,
        String nativeDir
    ) {
        Path srcFilePath = Paths.get(srcFile);
        if (!Files.exists(srcFilePath)) {
            LOGGER.error("Source file does not exist: {}", srcFile);
            return 1;
        }
        if (Files.isDirectory(srcFilePath)) {
            LOGGER.error("Source file is a directory: {}", srcFile);
            return 1;
        }
        if (!srcFilePath.toString().endsWith(".rosetta")) {
            LOGGER.error("Source file does not end with .rosetta: {}", srcFile);
            return 1;
        }
        List<Path> rosettaFiles = List.of(srcFilePath);
        return processRosettaFiles(rosettaFiles, tgtDir, allowErrors, failOnWarnings, projectName, version, namespacePrefix, nativeDir);
    }

    // Common processing function
    private int processRosettaFiles(
        List<Path> rosettaFiles,
        String tgtDir,
        boolean allowErrors,
        boolean failOnWarnings,
        String projectName,
        String version,
        String namespacePrefix,
        String nativeDir
    ) {
        LOGGER.info("Processing {} .rosetta files, writing to: {}", rosettaFiles.size(), tgtDir);

        if (rosettaFiles.isEmpty()) {
            LOGGER.error("No .rosetta files found to process.");
            return 1;
        }

        Injector injector = new PythonRosettaStandaloneSetup().createInjectorAndDoEMFRegistration();
        ResourceSet resourceSet = injector.getInstance(ResourceSet.class);
        List<Resource> resources = new LinkedList<>();
        RosettaBuiltinsService builtins = injector.getInstance(RosettaBuiltinsService.class);
        resources.add(resourceSet.getResource(builtins.basicTypesURI, true));
        resources.add(resourceSet.getResource(builtins.annotationsURI, true));
        rosettaFiles.stream()
                .map(path -> resourceSet.getResource(URI.createFileURI(path.toString()), true))
                .forEach(resources::add);

        PythonCodeGenerator pythonCodeGenerator = injector.getInstance(PythonCodeGenerator.class);
        pythonCodeGenerator.setProjectName(projectName);
        pythonCodeGenerator.setNamespacePrefix(namespacePrefix);
        PythonModelLoader modelLoader = injector.getInstance(PythonModelLoader.class);

        List<RosettaModel> models = modelLoader.getRosettaModels(resources);
        if (models.isEmpty()) {
            LOGGER.error("No valid Rosetta models found.");
            return 1;
        }

        LOGGER.info("Processing {} models, version: {}", models.size(), version);

        IResourceValidator validator = getValidator(injector);
        Map<String, CharSequence> generatedPython = new HashMap<>();

        List<RosettaModel> validModels = new ArrayList<>();

        for (RosettaModel model : models) {
            Resource resource = model.eResource();
            boolean hasErrors = false;
            try {
                List<Issue> issues = validator.validate(resource, CheckMode.ALL, CancelIndicator.NullImpl);
                for (Issue issue : issues) {
                    switch (issue.getSeverity()) {
                        case ERROR:
                            EObject offender = resource.getEObject(issue.getUriToProblem().fragment());
                            String identification = (offender instanceof RosettaNamed)
                                    ? ((RosettaNamed) offender).getName()
                                    : (offender != null ? offender.eClass().getName() : "Unknown");

                            // Traverse up to find context (e.g. function or type name) if the offender
                            // itself isn't the root context
                            if (offender != null && !(offender instanceof com.regnosys.rosetta.rosetta.RosettaModel)) {
                                EObject current = offender.eContainer();
                                while (current != null) {
                                    if (current instanceof RosettaNamed) {
                                        String contextName = ((RosettaNamed) current).getName();
                                        if (contextName != null && !contextName.equals(identification)) {
                                            identification += " (in " + contextName + ")";
                                        }
                                        break;
                                    }
                                    current = current.eContainer();
                                }
                            }

                            LOGGER.error("Validation ERROR in {} (Line {}): {} on element '{}'",
                                    model.getName(), issue.getLineNumber(), issue.getMessage(), identification);
                            hasErrors = true;
                            break;
                        case WARNING:
                            LOGGER.warn("Validation WARNING in {} (Line {}): {}", model.getName(),
                                    issue.getLineNumber(), issue.getMessage());
                            if (failOnWarnings) {
                                hasErrors = true;
                            }
                            break;
                        default:
                            break;
                    }
                }
            } catch (Exception e) {
                LOGGER.warn("Validation skipped for {} due to exception: {}", model.getName(), e.getMessage());
                validModels.add(model);
                continue;
            }

            if (hasErrors) {
                LOGGER.error("Skipping model {} due to validation errors.", model.getName());
            } else {
                validModels.add(model);
            }
        }

        if (validModels.isEmpty()) {
            if (allowErrors) {
                LOGGER.warn("No valid models found after validation, but --allow-errors is set. Continuing with potentially partial generation.");
            } else {
                LOGGER.error("No valid models found after validation. Exiting.");
                return 1;
            }
        }

        // Use validModels for generation
        // Re-determine version based on valid models? Or keep original version?
        // Assuming version is consistent across all loaded models or derived from the
        // first one.
        // The original code took version from models.getFirst().getVersion();

        LOGGER.info("Proceeding with generation for {} valid models.", validModels.size());

        pythonCodeGenerator.beforeAllGenerate(resourceSet, validModels, version);
        for (RosettaModel model : validModels) {
            LOGGER.info("Processing: " + model.getName());
            generatedPython.putAll(pythonCodeGenerator.beforeGenerate(model.eResource(), model, version));
            generatedPython.putAll(pythonCodeGenerator.generate(model.eResource(), model, version));
            generatedPython.putAll(pythonCodeGenerator.afterGenerate(model.eResource(), model, version));
        }
        generatedPython.putAll(pythonCodeGenerator.afterAllGenerate(resourceSet, models, version));

        writePythonFiles(generatedPython, tgtDir);
        copyNativeFunctions(nativeDir, tgtDir, generatedPython.keySet());
        return 0;
    }

    /**
     * Gets the resource validator from the injector.
     * This method is designed for extension to allow mocking in unit tests.
     *
     * @param injector the Guice injector
     * @return the resource validator
     */
    protected IResourceValidator getValidator(Injector injector) {
        return injector.getInstance(IResourceValidator.class);
    }

    private static void writePythonFiles(Map<String, CharSequence> generatedPython, String tgtDir) {
        Path targetRoot = Paths.get(tgtDir);
        File targetDirFile = targetRoot.toFile();
        // Remove existing target directory (if any), then (re)create it
        try {
            if (targetDirFile.exists()) {
                if (targetDirFile.isDirectory()) {
                    FileUtils.deleteDirectory(targetDirFile);
                } else {
                    FileUtils.forceDelete(targetDirFile);
                }
            }
            FileUtils.forceMkdir(targetDirFile);
        } catch (IOException e) {
            LOGGER.error("Failed to reset target directory {}: {}", targetRoot, e.getMessage(), e);
            return;
        }
        for (Map.Entry<String, CharSequence> entry : generatedPython.entrySet()) {
            String filePath = entry.getKey();
            CharSequence content = entry.getValue();
            Path outputPath = Paths.get(tgtDir, filePath);
            try {
                Files.createDirectories(outputPath.getParent());
                String body = content.toString();
                if (filePath.endsWith(".py") || filePath.endsWith(".toml")) {
                    body = PythonCodeGeneratorConstants.LICENSE_HEADER + body;
                }
                Files.writeString(outputPath, body);
                LOGGER.info("Wrote file: {}", outputPath);
            } catch (IOException e) {
                System.err.println("Failed to write file: " + outputPath + " - " + e.getMessage());
            }
        }
        LOGGER.info("Wrote {} files to {}", generatedPython.size(), tgtDir);
    }

    private static void copyNativeFunctions(String nativeDir, String tgtDir, Set<String> generatedPaths) {
        if (nativeDir == null || nativeDir.isBlank()) {
            return;
        }

        Path nativeDirPath = Paths.get(nativeDir);
        if (!Files.exists(nativeDirPath) || !Files.isDirectory(nativeDirPath)) {
            LOGGER.warn("Native function directory does not exist or is not a directory: {}", nativeDir);
            return;
        }

        // Native implementations are sourced from <nativeDir>/rune/native/ and deployed
        // to src/rune/native/ in the output, preserving the full sub-path underneath.
        // Example: <nativeDir>/rune/native/rosetta_dsl/test/functions/RoundToNearest.py
        //       -> src/rune/native/rosetta_dsl/test/functions/RoundToNearest.py
        Path runeNativePath = nativeDirPath.resolve("rune/native");
        if (!Files.exists(runeNativePath) || !Files.isDirectory(runeNativePath)) {
            LOGGER.warn("Expected rune/native sub-directory not found under native dir: {}", runeNativePath);
            return;
        }

        int[] copied = {0};
        int[] skipped = {0};

        try {
            Files.walk(runeNativePath)
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".py"))
                    .forEach(sourcePath -> {
                        String relative = runeNativePath.relativize(sourcePath).toString().replace(File.separator, "/");
                        String targetRelative = "src/rune/native/" + relative;
                        Path targetPath = Paths.get(tgtDir, targetRelative);

                        if (generatedPaths.contains(targetRelative)) {
                            LOGGER.warn("Native function file '{}' conflicts with generated file '{}'; skipping.", sourcePath, targetRelative);
                            skipped[0]++;
                        } else {
                            try {
                                Files.createDirectories(targetPath.getParent());
                                Files.copy(sourcePath, targetPath);
                                LOGGER.info("Copied native function: {}", targetRelative);
                                copied[0]++;
                            } catch (IOException e) {
                                LOGGER.error("Failed to copy native function file '{}': {}", sourcePath, e.getMessage(), e);
                            }
                        }
                    });
        } catch (IOException e) {
            LOGGER.error("Failed to walk native function directory '{}': {}", nativeDir, e.getMessage(), e);
            return;
        }

        LOGGER.info("Native functions: copied {} file(s), skipped {} collision(s).", copied[0], skipped[0]);
    }

    // --- Helper classes for model loading and Guice setup ---

    static class PythonModelLoader {

        public List<RosettaModel> getRosettaModels(List<Resource> resources) {
            return resources.stream()
                    .filter(Objects::nonNull)
                    .map(Resource::getContents)
                    .flatMap(Collection::stream)
                    .filter(r -> r instanceof RosettaModel)
                    .map(r -> (RosettaModel) r)
                    .collect(Collectors.toList());
        }
    }

    static class PythonRosettaRuntimeModule extends RosettaRuntimeModule {
        @Override
        public void configure(com.google.inject.Binder binder) {
            super.configure(binder);
            binder.bind(PythonModelLoader.class);
        }

        @Override
        public Class<? extends Provider<ExternalGenerators>> provideExternalGenerators() {
            return PythonCodeGeneratorInstance.class;
        }
    }

    /**
     * Provider for ExternalGenerators.
     */
    public static final class PythonCodeGeneratorInstance implements Provider<ExternalGenerators> {
        @Override
        public ExternalGenerators get() {
            return () -> List.of((ExternalGenerator) new PythonCodeGenerator()).iterator();
        }
    }

    static class PythonRosettaStandaloneSetup extends RosettaStandaloneSetup {
        @Override
        public Injector createInjector() {
            return Guice.createInjector(new PythonRosettaRuntimeModule());
        }
    }
}
