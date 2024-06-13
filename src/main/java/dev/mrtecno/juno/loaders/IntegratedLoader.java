package dev.mrtecno.juno.loaders;

import dev.mrtecno.juno.plugin.Plugin;
import dev.mrtecno.juno.plugin.PluginLoader;
import dev.mrtecno.juno.plugin.PluginManifest;
import dev.mrtecno.juno.util.Pair;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.InputStream;
import java.util.*;

@Getter
@RequiredArgsConstructor
public class IntegratedLoader implements PluginLoader, LocalLoader {
	private final List<String> classNames;

	private Map<PluginManifest, Class<Plugin>> manifests;

	public IntegratedLoader(String... classNames) {
		this(List.of(classNames));
	}

	@Override
	public void initialize(ClassLoader parent) {
		this.manifests = classNames().stream().map(c -> {
			try {
				@SuppressWarnings("unchecked")
				Class<Plugin> clazz = (Class<Plugin>) Class.forName(c, true, parent);
				return Pair.of(Plugin.parseManifest(this, clazz).orElseThrow(), clazz);
			} catch (ClassNotFoundException | NoSuchElementException e) {
				throw new IllegalArgumentException("Could not find class: " + c, e);
			}
		}).collect(Pair.<PluginManifest, Class<Plugin>>toMap());
	}

	@Override
	public Collection<PluginManifest> availablePlugins() {
		return manifests.keySet();
	}

	@Override
	public Plugin load(PluginManifest manifest) {
		return Plugin.loadFromClass(manifests.get(manifest), manifest);
	}

	@Override
	public void unload(Plugin pl) {
		throw new UnsupportedOperationException("Cannot unload integrated plugins");
	}

	public static IntegratedLoader fromListFile(String resource) {
		return fromListFile(IntegratedLoader.class.getResourceAsStream(resource));
	}

	public static IntegratedLoader fromListFile(String resource, ClassLoader loader) {
		return fromListFile(loader.getResourceAsStream(resource));
	}

	public static IntegratedLoader fromListFile(InputStream stream) {
		try {
			String[] lines = new String(stream.readAllBytes()).trim().split("\n");
			return new IntegratedLoader(Arrays.stream(lines)
					.filter(l -> !l.isBlank()).map(String::trim)
					.filter(l -> !l.startsWith("#")).toArray(String[]::new));
		} catch (Exception e) {
			throw new RuntimeException("Could not read classpath list file", e);
		}
	}
}
