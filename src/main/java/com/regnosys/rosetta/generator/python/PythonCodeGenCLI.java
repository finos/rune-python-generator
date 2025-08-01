package com.regnosys.rosetta.generator.python;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.regnosys.rosetta.RosettaRuntimeModule;
import com.regnosys.rosetta.RosettaStandaloneSetup;
import com.regnosys.rosetta.common.util.ClassPathUtils;
import com.regnosys.rosetta.common.util.UrlUtils;
import com.regnosys.rosetta.generator.external.ExternalGenerator;
import com.regnosys.rosetta.generator.external.ExternalGenerators;
import com.regnosys.rosetta.rosetta.RosettaModel;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Command-line interface for generating Python code from Rosetta models.
 * <p>
 * This CLI tool loads Rosetta model files (either from a directory or a single file),
 * invokes the {@link PythonCodeGenerator}, and writes the generated Python code to the specified target directory.
 * It is intended for use by developers and build systems to automate the translation of Rosetta DSL models to Python.
 * </p>
 *
 * <h2>Usage</h2>
 * <pre>
 *   java -cp &lt;your-jar-or-classpath&gt; com.regnosys.rosetta.generator.python.PythonCodeGenCLI -s &lt;source-dir&gt; -t &lt;target-dir&gt;
 *   java -cp &lt;your-jar-or-classpath&gt; com.regnosys.rosetta.generator.python.PythonCodeGenCLI -f &lt;source-file&gt; -t &lt;target-dir&gt;
 * </pre>
 * <ul>
 *   <li><b>-s, --dir &lt;source-dir&gt;</b>: Source directory containing Rosetta files (all <code>.rosetta</code> files will be processed)</li>
 *   <li><b>-f, --file &lt;source-file&gt;</b>: Single Rosetta file to process</li>
 *   <li><b>-t, --tgt &lt;target-dir&gt;</b>: Target directory for generated Python code (defaults to <code>./python</code> if not specified)</li>
 *   <li><b>-h</b>: Print usage/help</li>
 * </ul>
 *
 * <h2>Example</h2>
 * <pre>
 *   java -jar target/python-0.0.0.main-SNAPSHOT-shaded.jar -s src/main/rosetta -t build/python
 * </pre>
 *
 * <h2>Notes</h2>
 * <ul>
 *   <li>Either <b>-s</b> or <b>-f</b> must be specified.</li>
 *   <li>The tool will clean the target directory before writing new files.</li>
 *   <li>Requires a Java 11+ runtime and all dependencies on the classpath (or use the shaded/uber jar).</li>
 * </ul>
 *
 * @author Plamen Neykov
 * @author Daniel Schwartz
 * @see PythonCodeGenerator
 */

public class PythonCodeGenCLI {
    private static final Logger LOGGER = LoggerFactory.getLogger(PythonCodeGenCLI.class);

    public static void main(String[] args) {
        Options options = new Options();
        Option help = new Option("h", "Print usage");
        Option srcDirOpt = Option.builder("s").longOpt("dir").argName("srcDir").desc("Source Rosetta directory").hasArg().build();
        Option srcFileOpt = Option.builder("f").longOpt("file").argName("srcFile").desc("Source Rosetta file").hasArg().build();
        Option tgtDirOpt = Option.builder("t").longOpt("tgt").argName("tgtDir").desc("Target Python directory (default: ./python)").hasArg().build();

        options.addOption(help);
        options.addOption(srcDirOpt);
        options.addOption(srcFileOpt);
        options.addOption(tgtDirOpt);

        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cmd = parser.parse(options, args);
            if (cmd.hasOption("h")) {
                printUsage(options);
                return;
            }
            String tgtDir = cmd.getOptionValue("t", "./python");
            if (cmd.hasOption("s")) {
                String srcDir = cmd.getOptionValue("s");
                translateFromSourceDir(srcDir, tgtDir);
            } else if (cmd.hasOption("f")) {
                String srcFile = cmd.getOptionValue("f");
                translateFromSourceFile(srcFile, tgtDir);
            } else {
                System.err.println("Either a source directory (-s) or source file (-f) must be specified.");
                printUsage(options);
            }
        } catch (ParseException e) {
            System.err.println("Failed to parse command line arguments: " + e.getMessage());
            printUsage(options);
        }
    }

    private static void printUsage(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("PythonCodeGenCLI", options, true);
    }

    private static void translateFromSourceDir(String srcDir, String tgtDir) {
        LOGGER.info("Reading from directory: {} and writing to: {}", srcDir, tgtDir);
        Path srcDirPath = Paths.get(srcDir);
        if (!Files.exists(srcDirPath)) {
            System.err.println("Source directory does not exist: " + srcDir);
            return;
        }
        if (!Files.isDirectory(srcDirPath)) {
            System.err.println("Source directory is not a directory: " + srcDir);
            return;
        }
        List<Path> rosettaFiles = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(srcDirPath)) {
            paths.filter(Files::isRegularFile)
                    .filter(f -> f.getFileName().toString().endsWith(".rosetta"))
                    .forEach(rosettaFiles::add);
        } catch (IOException e) {
            System.err.println("Error reading source directory: " + e.getMessage());
            return;
        }
        LOGGER.info("Found {} .rosetta files in directory {}", rosettaFiles.size(), srcDir);
        translateRosetta(rosettaFiles, tgtDir);
    }

    private static void translateFromSourceFile(String srcFile, String tgtDir) {
        LOGGER.info("Reading from file: {} and writing to: {}", srcFile, tgtDir);
        Path srcFilePath = Paths.get(srcFile);
        if (!Files.exists(srcFilePath)) {
            System.err.println("Source file does not exist: " + srcFile);
            return;
        }
        if (Files.isDirectory(srcFilePath)) {
            System.err.println("Source file is a directory: " + srcFile);
            return;
        }
        translateRosetta(Collections.singletonList(srcFilePath), tgtDir);
    }

    private static void translateRosetta(List<Path> rosettaFiles, String tgtDir) {
        LOGGER.info("Cleaning target directory: {}", tgtDir);
        File directory = new File(tgtDir);
        try {
            if (directory.exists() && directory.isDirectory()) {
                FileUtils.cleanDirectory(directory);
            }
        } catch (IOException e) {
            System.err.println("Failed to clean the directory: " + e.getMessage());
            return;
        }

        Injector injector = new PythonRosettaStandaloneSetup().createInjectorAndDoEMFRegistration();
        PythonCodeGenerator pythonCodeGenerator = injector.getInstance(PythonCodeGenerator.class);
        PythonModelLoader modelLoader = injector.getInstance(PythonModelLoader.class);

        List<Path> staticRosettaFilePaths = ClassPathUtils.findStaticRosettaFilePaths();
        List<RosettaModel> models = modelLoader.rosettaModels(staticRosettaFilePaths, rosettaFiles);
        if (models.isEmpty()) {
            System.err.println("No valid Rosetta models found.");
            return;
        }
        XtextResourceSet resourceSet = modelLoader.getResourceSet();
        String version = models.getFirst().getVersion();

        LOGGER.info("Processing {} models, version: {}", models.size(), version);

        Map<String, CharSequence> generatedPython = new HashMap<>();
        pythonCodeGenerator.beforeAllGenerate(resourceSet, models, version);
        for (RosettaModel model : models) {
            generatedPython.putAll(pythonCodeGenerator.beforeGenerate(model.eResource(), model, version));
            generatedPython.putAll(pythonCodeGenerator.generate(model.eResource(), model, version));
            generatedPython.putAll(pythonCodeGenerator.afterGenerate(model.eResource(), model, version));
        }
        generatedPython.putAll(pythonCodeGenerator.afterAllGenerate(resourceSet, models, version));

        writePythonFiles(generatedPython, tgtDir);
    }

    private static void writePythonFiles(Map<String, CharSequence> generatedPython, String tgtDir) {
        for (Map.Entry<String, CharSequence> entry : generatedPython.entrySet()) {
            String filePath = entry.getKey();
            CharSequence content = entry.getValue();
            Path outputPath = Paths.get(tgtDir, filePath);
            try {
                Files.createDirectories(outputPath.getParent());
                Files.writeString(outputPath, content.toString());
                LOGGER.info("Wrote file: {}", outputPath);
            } catch (IOException e) {
                System.err.println("Failed to write file: " + outputPath + " - " + e.getMessage());
            }
        }
        LOGGER.info("Wrote {} files to {}", generatedPython.size(), tgtDir);
    }

    // --- Helper classes for model loading and Guice setup ---

    static class PythonModelLoader {
        @Inject Provider<XtextResourceSet> resourceSetProvider;

        public List<RosettaModel> rosettaModels(List<Path> statics, List<Path> sourceFiles) {
            XtextResourceSet resourceSet = resourceSetProvider.get();
            return Stream.concat(statics.stream(), sourceFiles.stream())
                    .map(UrlUtils::toUrl)
                    .map(PythonModelLoader::url)
                    .map(f -> getResource(resourceSet, f))
                    .filter(Objects::nonNull)
                    .map(Resource::getContents)
                    .flatMap(Collection::stream)
                    .filter(r -> r instanceof RosettaModel)
                    .map(r -> (RosettaModel) r)
                    .collect(Collectors.toList());
        }

        public XtextResourceSet getResourceSet() {
            return resourceSetProvider.get();
        }

        private static String url(@NotNull java.net.URL c) {
            try {
                return c.toURI().toURL().toURI().toASCIIString();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private static Resource getResource(ResourceSet resourceSet, String f) {
            try {
                return resourceSet.getResource(org.eclipse.emf.common.util.URI.createURI(f, true), true);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
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

    public static class PythonCodeGeneratorInstance implements Provider<ExternalGenerators> {
        @Override
        public ExternalGenerators get() {
            return new ExternalGenerators() {
                @NotNull
                @Override
                public Iterator<ExternalGenerator> iterator() {
                    return List.of((ExternalGenerator) new PythonCodeGenerator()).iterator();
                }
            };
        }
    }

    static class PythonRosettaStandaloneSetup extends RosettaStandaloneSetup {
        @Override
        public Injector createInjector() {
            return Guice.createInjector(new PythonRosettaRuntimeModule());
        }
    }
}
