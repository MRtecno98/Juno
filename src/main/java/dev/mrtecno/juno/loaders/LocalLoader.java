package dev.mrtecno.juno.loaders;

import dev.mrtecno.juno.plugin.PluginLoader;
import dev.mrtecno.juno.plugin.PluginManifest;
import dev.mrtecno.juno.plugin.identifier.PluginWildcard;
import reactor.core.publisher.Mono;

public interface LocalLoader extends PluginLoader {
	default Mono<PluginManifest> lookup(PluginWildcard name) {
		return availablePlugins()
				.filter(m -> m.name().equals(name.name()))
				.filter(name).next();
	}
}
