package dev.mrtecno.juno.loaders;

import dev.mrtecno.juno.plugin.PluginLoader;
import dev.mrtecno.juno.plugin.PluginManifest;
import dev.mrtecno.juno.plugin.identifier.PluginWildcard;

import java.util.Optional;

public interface LocalLoader extends PluginLoader {
	default Optional<PluginManifest> lookup(PluginWildcard name) {
		return availablePlugins().stream()
				.filter(m -> m.name().equals(name.name()))
				.filter(name).findAny();
	}
}
