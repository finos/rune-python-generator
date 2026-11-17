/*
 * Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
 * SPDX-License-Identifier: Apache-2.0
 */
import com.fasterxml.jackson.databind.*;
import com.google.inject.*;
import com.regnosys.rosetta.common.hashing.ReferenceResolverProcessStep;
import com.regnosys.rosetta.common.validation.*;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.validation.ValidationResult;
import org.finos.cdm.CdmRuntimeModule;
import org.finos.cdm.reference.CdmReferenceConfig;
import org.finos.rune.mapper.RuneJsonObjectMapper;
import java.nio.file.*; import java.io.*; import java.util.*;
/** args: resourcesDir samples.tsv summary.tsv failures.tsv */
public class ValidateSamples {
  static final Comparator<JsonNode> NUM = (x, y) -> {
    if (x.equals(y)) return 0;
    if (x.isNumber() && y.isNumber()) return x.decimalValue().compareTo(y.decimalValue());
    return 1;
  };
  public static void main(String[] a) throws Exception {
    Path base = Paths.get(a[0]);
    Injector inj = Guice.createInjector(new CdmRuntimeModule());
    RosettaTypeValidator val = new RosettaTypeValidator(); inj.injectMembers(val);
    ObjectMapper mapper = new RuneJsonObjectMapper();
    ObjectMapper plain = new ObjectMapper();
    try (PrintWriter sum = new PrintWriter(a[2]); PrintWriter fail = new PrintWriter(a[3])) {
      sum.println("pack\tsample\texpected\tjava_failures\troundtrip\terror");
      for (String line : Files.readAllLines(Paths.get(a[1]))) {
        String[] t = line.split("\t");
        String pack = t[0], f = t[1], expected = t[2];
        Class<? extends RosettaModelObject> c = pack.startsWith("workflow-step")
            ? cdm.event.workflow.WorkflowStep.class : cdm.event.common.TradeState.class;
        String rt = "", fails = "", err = "";
        try {
          String json = Files.readString(base.resolve(f));
          RosettaModelObject m = mapper.readValue(json, c);
          JsonNode in = plain.readTree(json), out = plain.readTree(mapper.writeValueAsString(m));
          ((com.fasterxml.jackson.databind.node.ObjectNode) in).remove("@version");
          ((com.fasterxml.jackson.databind.node.ObjectNode) out).remove("@version");
          rt = in.equals(NUM, out) ? "same" : "DIFF";
          RosettaModelObject b = m.toBuilder();
          new ReferenceResolverProcessStep(CdmReferenceConfig.get()).runProcessStep(b.getType(), b);
          ValidationReport r = val.runProcessStep(c, b.build());
          List<ValidationResult<?>> vf = r.validationFailures();
          fails = String.valueOf(vf.size());
          for (ValidationResult<?> v : vf)
            fail.println(String.join("\t", pack, f, String.valueOf(v.getValidationType()), v.getModelObjectName(),
                v.getName(), String.valueOf(v.getPath()), v.getFailureReason().orElse("").replaceAll("\\s+", " ")));
        } catch (Exception e) {
          err = e.getClass().getSimpleName() + ": " + String.valueOf(e.getMessage()).replaceAll("\\s+", " ");
          if (err.length() > 300) err = err.substring(0, 300);
        }
        sum.println(String.join("\t", pack, f, expected, fails, rt, err));
        sum.flush();
      }
    }
  }
}
