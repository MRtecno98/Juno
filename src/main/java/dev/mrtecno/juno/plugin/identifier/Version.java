package dev.mrtecno.juno.plugin.identifier;

public record Version(int major, int minor, int patch) implements Comparable<Version> {
	@Override
	public int compareTo(Version o) {
		int result = Integer.compare(major, o.major);
		if (result != 0) {
			return result;
		}

		result = Integer.compare(minor, o.minor);
		if (result != 0) {
			return result;
		}

		return Integer.compare(patch, o.patch);
	}

	@Override
	public boolean equals(Object o) {
		if(o instanceof Version version) {
			return major == version.major && minor == version.minor && patch == version.patch;
		} else return false;
	}

	@Override
	public String toString() {
		return major + "." + minor + "." + patch;
	}

	public static Version parseVersion(String version) {
		String[] split = version.split("\\.");
		return new Version(Integer.parseInt(split[0]), Integer.parseInt(split[1]), Integer.parseInt(split[2]));
	}
}
