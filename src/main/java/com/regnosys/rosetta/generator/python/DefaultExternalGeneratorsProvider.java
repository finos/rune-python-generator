package com.regnosys.rosetta.generator.python;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import com.regnosys.rosetta.generator.external.ExternalGenerator;
import com.regnosys.rosetta.generator.external.ExternalGenerators;

public final class DefaultExternalGeneratorsProvider implements Provider<ExternalGenerators> {

	/**
	 * The DefaultExternalGeneratorsProvider.
	 */
	@Inject
	private PythonCodeGenerator pythonGenerator;

	@Override
	public ExternalGenerators get() {
		return new DefaultGenerators();
	}

	/**
	 * The DefaultGenerators class.
	 */
	private final class DefaultGenerators implements ExternalGenerators {

		/**
		 * The list of generators.
		 */
		private List<ExternalGenerator> gens = Arrays.asList(pythonGenerator);

		@Override
		public Iterator<ExternalGenerator> iterator() {
			return gens.iterator();
		}
	}
}
