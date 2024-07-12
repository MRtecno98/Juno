package dev.mrtecno.juno.plugin.graph;

import dev.mrtecno.juno.plugin.graph.PluginGraph.PluginNode.SelectionResult;
import lombok.Getter;

@Getter
public class CompatibilityException extends IllegalStateException {
	private final SelectionResult result;

	CompatibilityException(SelectionResult result) {
		super(result.message());
		this.result = result;
	}

	CompatibilityException(SelectionResult result, Throwable cause) {
		super(result.message(), cause);
		this.result = result;
	}
}
