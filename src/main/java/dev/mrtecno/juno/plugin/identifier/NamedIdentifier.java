package dev.mrtecno.juno.plugin.identifier;

import dev.mrtecno.juno.plugin.PluginManifest;

public record NamedIdentifier(String name) implements PluginWildcard {
	@Override
	public boolean accept(PluginManifest manifest) {
		return name().equals(manifest.name());
	}

	@Override
	public String toString() {
		return name;
	}
}
