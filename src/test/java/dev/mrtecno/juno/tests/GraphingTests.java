package dev.mrtecno.juno.tests;

import dev.mrtecno.juno.plugin.PluginManifest;
import dev.mrtecno.juno.plugin.graph.PluginGraph;
import dev.mrtecno.juno.plugin.identifier.PluginIdentifier;
import dev.mrtecno.juno.plugin.identifier.PluginWildcard;
import dev.mrtecno.juno.plugin.identifier.Version;
import org.junit.jupiter.api.Test;

public class GraphingTests {
	PluginManifest A = new PluginManifest(null, "xxx", new PluginIdentifier(
			"A", Version.parseVersion("1.0.0")
	), new PluginWildcard[] { PluginWildcard.parseWildcard("B:1.1.0") });

	PluginManifest B = new PluginManifest(null, "xxx", new PluginIdentifier(
			"B", Version.parseVersion("1.0.0")
	), new PluginWildcard[] { PluginWildcard.parseWildcard("C:1.0.0") });

	PluginManifest B2 = new PluginManifest(null, "xxx", new PluginIdentifier(
			"B", Version.parseVersion("1.1.0")
	), new PluginWildcard[] {  });

	PluginManifest B3 = new PluginManifest(null, "xxx", new PluginIdentifier(
			"B", Version.parseVersion("1.2.0")
	), new PluginWildcard[] { PluginWildcard.parseWildcard("A:1.0.0") });

	PluginManifest C = new PluginManifest(null, "xxx", new PluginIdentifier(
			"C", Version.parseVersion("1.0.0")
	), new PluginWildcard[] { PluginWildcard.parseWildcard("B:1.1.0") });

	@Test
	public void testDependencyGraph() {
		PluginGraph graph = new PluginGraph();

		graph.add(A);
		graph.add(B);
		graph.add(B2);
		graph.add(B3);
		graph.add(C);

		graph.traverse(false).forEachOrdered(System.out::println);
	}
}
