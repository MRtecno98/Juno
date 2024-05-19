package dev.mrtecno.juno.plugin;

import dev.mrtecno.juno.plugin.identifier.PluginIdentifier;
import dev.mrtecno.juno.plugin.identifier.PluginWildcard;
import dev.mrtecno.juno.plugin.identifier.Version;
import lombok.Builder;
import lombok.EqualsAndHashCode;

@Builder
public record PluginManifest(PluginLoader loader, String entrypoint,
							 @EqualsAndHashCode.Include PluginIdentifier id, PluginWildcard[] dependencies) {
	public Plugin load() {
		return loader.load(this);
	}

	public String name() {
		return id.name();
	}

	public Version version() {
		return id().version();
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}
}
