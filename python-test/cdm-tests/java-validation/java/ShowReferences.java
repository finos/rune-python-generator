/*
 * Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
 * SPDX-License-Identifier: Apache-2.0
 */
import com.google.inject.*;
import com.regnosys.rosetta.common.hashing.ReferenceResolverProcessStep;
import com.rosetta.model.lib.RosettaModelObject;
import org.finos.cdm.CdmRuntimeModule;
import org.finos.cdm.reference.CdmReferenceConfig;
import org.finos.rune.mapper.RuneJsonObjectMapper;
import java.nio.file.*;
public class ShowReferences { public static void main(String[] a) throws Exception {
  Guice.createInjector(new CdmRuntimeModule());
  cdm.event.common.TradeState m=new RuneJsonObjectMapper().readValue(Files.readString(Paths.get(a[0])),cdm.event.common.TradeState.class);
  RosettaModelObject b=m.toBuilder(); new ReferenceResolverProcessStep(CdmReferenceConfig.get()).runProcessStep(b.getType(),b);
  var t=(cdm.event.common.TradeState)b.build();
  var p=t.getTrade().getProduct().getEconomicTerms().getPayout().get(0).getInterestRatePayout();
  var r=p.getResetDates().getFixingDates().getDateRelativeTo();
  System.out.println("dateRelativeTo: "+r+" | value="+r.getValue());
  var bc=p.getCalculationPeriodDates().getCalculationPeriodDatesAdjustments().getBusinessCenters();
  System.out.println("businessCenters: reference="+bc.getBusinessCentersReference()+" value="+bc.getBusinessCentersReference().getValue()+" businessCenter="+bc.getBusinessCenter());
}}
