package dev.mrtecno.juno.plugin;

import dev.mrtecno.juno.plugin.identifier.PluginIdentifier;
import dev.mrtecno.juno.plugin.identifier.PluginWildcard;
import dev.mrtecno.juno.plugin.identifier.Version;
import dev.mrtecno.juno.service.Service;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Optional;

@Getter
@RequiredArgsConstructor
public abstract class Plugin implements Service {
	private final PluginManifest manifest;
	private PluginManager manager;

	public Plugin(PluginLoader loader) {
		this.manifest = parseManifest(loader, getClass()).orElseThrow(
				() -> new IllegalArgumentException("Class "
						+ getClass().getName() + " is missing @Manifest annotation"));
	}

	public PluginIdentifier id() {
		return manifest.id();
	}

	public void link(PluginManager manager) {
		if(linked())
			throw new IllegalStateException("Plugin is already linked to a manager");
		this.manager = manager;
	}

	public void unlink() {
		this.manager = null;
	}

	public boolean linkedWith(PluginManager manager) {
		return this.manager == manager;
	}

	public boolean linked() {
		return manager != null;
	}

	public PluginManager manager() {
		if(!linked())
			throw new IllegalStateException("Plugin is not linked to a manager");
		return manager;
	}

	public void unload() {
		if(linked()) manager().unload(this);
		else unload0();
	}

	// Only to be used by PluginManager, or by an unlinked plugin
	void unload0() {
		unlink();
		manifest().loader().unload(this);
	}

	public static Optional<Manifest> parseClass(Class<?> clazz) {
		return Optional.ofNullable(clazz.getAnnotation(Manifest.class))
				.filter(m -> !(m.version().isEmpty() &&
						(m.major() == 0 && m.minor() == 0 && m.patch() == 0)));
	}

	public static PluginManifest parseManifest(PluginLoader loader, Class<?> clazz, Manifest manifest) {
		return new PluginManifest(loader, clazz.getName(),
				new PluginIdentifier(manifest.name(), manifest.version().isEmpty()
						? new Version(manifest.major(), manifest.minor(), manifest.patch())
						: Version.parseVersion(manifest.version())),
				Arrays.stream(manifest.dependencies())
						.map(PluginWildcard::parseWildcard)
						.toArray(PluginWildcard[]::new));
	}

	public static Optional<PluginManifest> parseManifest(PluginLoader loader, Class<?> clazz) {
		return parseClass(clazz).map(m -> parseManifest(loader, clazz, m));
	}

	public static <P extends Plugin> P loadFromClass(Class<P> clazz, PluginManifest manifest) {
		try {
			return clazz.getConstructor(PluginManifest.class).newInstance(manifest);
		} catch (ReflectiveOperationException e) {
			throw new IllegalArgumentException(
					"Plugin class needs a constructor with PluginManifest parameter");
		}
	}
}
