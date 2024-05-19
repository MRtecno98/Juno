package dev.mrtecno.juno.plugin.graph;

import dev.mrtecno.juno.plugin.graph.PluginGraph.PluginNode.SelectionResult;

public class CompatibilityException extends IllegalStateException {
	private final SelectionResult result;

	CompatibilityException(String message, SelectionResult result) {
		super(message);
		this.result = result;
	}

	CompatibilityException(String message, SelectionResult result, Throwable cause) {
		super(message, cause);
		this.result = result;
	}
}
