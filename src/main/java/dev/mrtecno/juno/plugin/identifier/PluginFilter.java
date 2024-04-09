package dev.mrtecno.juno.plugin.identifier;

import dev.mrtecno.juno.plugin.PluginManifest;

public interface PluginFilter {
	boolean accept(PluginManifest manifest);
}
