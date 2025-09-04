package dev.mrtecno.juno.plugin;

import dev.mrtecno.juno.plugin.identifier.NamedIdentifier;
import dev.mrtecno.juno.plugin.identifier.PluginWildcard;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PluginLoader {
	void initialize(ClassLoader parent);

	Flux<PluginManifest> availablePlugins();

	Mono<PluginManifest> lookup(PluginWildcard name);

	default Mono<PluginManifest> lookup(String name) {
		return lookup(new NamedIdentifier(name));
	}

	Mono<Plugin> load(PluginManifest manifest);

	void unload(Plugin pl);
}
