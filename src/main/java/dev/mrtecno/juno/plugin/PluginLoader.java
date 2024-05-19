package dev.mrtecno.juno.plugin;

import dev.mrtecno.juno.plugin.identifier.NamedIdentifier;
import dev.mrtecno.juno.plugin.identifier.PluginWildcard;

import java.util.Collection;
import java.util.Optional;

public interface PluginLoader {
	void initialize(ClassLoader parent);

	Collection<PluginManifest> availablePlugins();

	Optional<PluginManifest> lookup(PluginWildcard name);

	default Optional<PluginManifest> lookup(String name) {
		return lookup(new NamedIdentifier(name));
	}

	Plugin load(PluginManifest manifest);

	void unload(Plugin pl);
}
