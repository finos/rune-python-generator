package com.regnosys.rosetta.generator.python.expressions;

import java.util.HashMap;
import java.util.Map;
import com.regnosys.rosetta.rosetta.RosettaSymbol;

/**
 * Tracks the scope of a Python expression, including the default receiver and shadowed symbols.
 * 
 * @param receiver        The default receiver (e.g., "self" or "item").
 * @param shadowedSymbols A map of Rosetta symbols to their Python names in this scope.
 */
public record PythonExpressionScope(
    String receiver, 
    Map<RosettaSymbol, String> shadowedSymbols
) {
    /**
     * Creates a new scope with the given receiver and no shadowed symbols.
     * 
     * @param receiver The default receiver (e.g., "self" or "item").
     * @return A new scope.
     */
    public static PythonExpressionScope of(String receiver) {
        return new PythonExpressionScope(receiver, Map.of());
    }

    /**
     * Creates a new scope with a different receiver but the same shadowed symbols.
     * 
     * @param newReceiver The new default receiver.
     * @return A new scope.
     */
    public PythonExpressionScope withReceiver(String newReceiver) {
        return new PythonExpressionScope(newReceiver, shadowedSymbols);
    }

    /**
     * Creates a new scope with an additional shadowed symbol.
     * 
     * @param symbol The Rosetta symbol to shadow.
     * @param pythonName The Python name to map it to.
     * @return A new scope.
     */
    public PythonExpressionScope withShadow(RosettaSymbol symbol, String pythonName) {
        Map<RosettaSymbol, String> newShadowed = new HashMap<>(shadowedSymbols);
        newShadowed.put(symbol, pythonName);
        return new PythonExpressionScope(receiver, Map.copyOf(newShadowed));
    }
}
