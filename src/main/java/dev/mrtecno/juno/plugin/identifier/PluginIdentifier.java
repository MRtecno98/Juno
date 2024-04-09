package dev.mrtecno.juno.plugin.identifier;

import dev.mrtecno.juno.plugin.PluginManifest;

public record PluginIdentifier(String name, int major, int minor, int patch)
		implements Comparable<PluginIdentifier>, PluginWildcard {

	@Override
	public boolean accept(PluginManifest manifest) {
		return manifest.id().equals(this);
	}

	public boolean equalsVersion(PluginIdentifier o) {
		return major == o.major() && minor == o.minor() && patch == o.patch();
	}

	public String toString() {
		return name + "==" + major + "." + minor + "." + patch;
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

		result = Integer.compare(major, o.major);
		if (result != 0) {
			return result;
		}

		result = Integer.compare(minor, o.minor);
		if (result != 0) {
			return result;
		}

		return Integer.compare(patch, o.patch);
	}
}
