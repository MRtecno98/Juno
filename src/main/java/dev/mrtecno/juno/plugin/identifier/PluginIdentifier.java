package dev.mrtecno.juno.plugin.identifier;

import dev.mrtecno.juno.plugin.PluginManifest;

public record PluginIdentifier(String name, Version version)
		implements Comparable<PluginIdentifier>, PluginWildcard {

	@Override
	public boolean accept(PluginManifest manifest) {
		return manifest.id().equals(this);
	}

	public boolean equalsVersion(PluginIdentifier o) {
		return version.equals(o.version);
	}

	public String toString() {
		return name + ":" + version;
	}

	@Override
	public int compareTo(PluginIdentifier o) {
		if(!name().equals(o.name()))
			throw new IllegalArgumentException("Cannot compare different plugins \""
					+ name() + "\" and \"" + o.name() + "\"");

		int result = name.compareTo(o.name);
		if (result != 0) {
			return result;
		}

		return Integer.compare(version.major(), o.version.major());
	}

	public static PluginIdentifier parseIdentifier(String identifier) {
		String[] split = identifier.split(":");
		return new PluginIdentifier(split[0], Version.parseVersion(split[1]));
	}
}
