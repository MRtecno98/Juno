package dev.mrtecno.juno.loaders;

import dev.mrtecno.juno.plugin.Plugin;
import dev.mrtecno.juno.plugin.PluginLoader;
import dev.mrtecno.juno.plugin.PluginManifest;
import dev.mrtecno.juno.util.Pair;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

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
}
