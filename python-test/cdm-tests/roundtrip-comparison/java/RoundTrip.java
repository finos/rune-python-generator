/*
 * Copyright (c) 2023 CLOUDRISK Limited and FT Advisory LLC
 * SPDX-License-Identifier: Apache-2.0
 */
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.regnosys.rosetta.common.hashing.ReferenceResolverProcessStep;
import com.regnosys.rosetta.common.validation.RosettaTypeValidator;
import com.regnosys.rosetta.common.validation.ValidationReport;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.validation.ValidationResult;
import org.finos.cdm.CdmRuntimeModule;
import org.finos.cdm.reference.CdmReferenceConfig;
import org.finos.rune.mapper.RuneJsonObjectMapper;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Reads each sample with CDM's Java code, writes it back, and validates it.
 *
 * <p>Args: {@code <samples.tsv> <input-root> <output-root> <results.jsonl>}. Each sample's
 * {@code outputPath} is read from {@code <input-root>} and written to {@code <output-root>}.
 * The comparison of input and output is done by {@code compare.py}, so that both languages
 * are compared with the same rules.
 *
 * <p>The root type is the optional tenth column, a Java class name; without it, it follows from
 * the test pack.
 */
public final class RoundTrip {
    private static final int MAX_ERROR = 400;

    private RoundTrip() {
    }

    public static void main(String[] args) throws Exception {
        Path samples = Paths.get(args[0]);
        Path inRoot = Paths.get(args[1]);
        Path outRoot = Paths.get(args[2]);
        Injector injector = Guice.createInjector(new CdmRuntimeModule());
        RosettaTypeValidator validator = new RosettaTypeValidator();
        injector.injectMembers(validator);
        ObjectMapper mapper = new RuneJsonObjectMapper();
        ObjectMapper plain = new ObjectMapper();
        try (PrintWriter results = new PrintWriter(args[3])) {
            for (String line : Files.readAllLines(samples)) {
                String[] t = line.split("\t");
                String pack = t[0];
                String sample = t[1];
                ObjectNode r = plain.createObjectNode();
                r.put("sample", sample);
                Class<? extends RosettaModelObject> rootType;
                try {
                    rootType = rootType(t, pack);
                } catch (ClassNotFoundException e) {
                    r.put("read", "error");
                    r.put("read_error", "unknown root type: " + e.getMessage());
                    results.println(plain.writeValueAsString(r));
                    continue;
                }
                Path in = inRoot.resolve(sample);
                if (!Files.exists(in)) {
                    r.put("read", "no input");
                    results.println(plain.writeValueAsString(r));
                    continue;
                }
                RosettaModelObject model;
                try {
                    model = mapper.readValue(Files.readString(in), rootType);
                    r.put("read", "ok");
                } catch (Exception e) {
                    r.put("read", "error");
                    r.put("read_error", describe(e));
                    results.println(plain.writeValueAsString(r));
                    results.flush();
                    continue;
                }
                try {
                    Path out = outRoot.resolve(sample);
                    Files.createDirectories(out.getParent());
                    Files.writeString(out, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(model));
                    r.put("write", "ok");
                } catch (Exception e) {
                    r.put("write", "error");
                    r.put("write_error", describe(e));
                }
                try {
                    RosettaModelObject builder = model.toBuilder();
                    new ReferenceResolverProcessStep(CdmReferenceConfig.get())
                            .runProcessStep(builder.getType(), builder);
                    ValidationReport report = validator.runProcessStep(rootType, builder.build());
                    ArrayNode failures = r.putArray("failures");
                    for (ValidationResult<?> v : report.validationFailures()) {
                        ObjectNode f = failures.addObject();
                        f.put("kind", String.valueOf(v.getValidationType()));
                        f.put("type", v.getModelObjectName());
                        f.put("rule", v.getName());
                        f.put("path", String.valueOf(v.getPath()));
                        f.put("reason", v.getFailureReason().orElse("").replaceAll("\\s+", " "));
                    }
                    r.put("validate", "ok");
                } catch (Exception e) {
                    r.put("validate", "error");
                    r.put("validate_error", describe(e));
                }
                results.println(plain.writeValueAsString(r));
                results.flush();
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends RosettaModelObject> rootType(String[] t, String pack)
            throws ClassNotFoundException {
        if (t.length > 9 && !t[9].isEmpty()) {
            return (Class<? extends RosettaModelObject>) Class.forName(t[9]);
        }
        return pack.startsWith("workflow-step")
                ? cdm.event.workflow.WorkflowStep.class : cdm.event.common.TradeState.class;
    }

    private static String describe(Exception e) {
        Throwable root = e;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String s = root.getClass().getSimpleName() + ": " + String.valueOf(root.getMessage()).replaceAll("\\s+", " ");
        return s.length() > MAX_ERROR ? s.substring(0, MAX_ERROR) : s;
    }
}
