package dev.mrtecno.juno.loaders;

import dev.mrtecno.juno.plugin.Plugin;
import dev.mrtecno.juno.plugin.PluginLoader;
import dev.mrtecno.juno.plugin.PluginManifest;
import dev.mrtecno.juno.plugin.identifier.PluginIdentifier;
import dev.mrtecno.juno.plugin.identifier.PluginWildcard;
import dev.mrtecno.juno.plugin.identifier.Version;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

@Getter
@RequiredArgsConstructor
public class FileLoader implements PluginLoader, LocalLoader {
	private final File directory;
	private ClassLoader parentClassLoader;

	private final Map<PluginManifest, File> discoveredFiles = new HashMap<>();
	private final Map<PluginIdentifier, WeakReference<URLClassLoader>> openLoaders = new HashMap<>();

	public void discoverFiles() {
		discoveredFiles.clear();
		if(!directory.exists() && !directory.mkdirs())
			throw new IllegalArgumentException(
					"Could not create directory: " + directory.getAbsolutePath());

		if(directory.isFile())
			checkJar(directory).ifPresent(
					m -> discoveredFiles.put(m, directory));

		File[] files = directory.listFiles();
		if(files == null) return;

		for(File file : files)
			checkJar(file).ifPresent(
					m -> discoveredFiles.put(m, file));
	}

	@SuppressWarnings("unchecked")
	public Optional<PluginManifest> checkJar(File file) {
		if(!file.getName().endsWith(".jar")) return Optional.empty();

		try(JarFile jarFile = new JarFile(file)) {
			ZipEntry manifest = jarFile.getEntry("manifest.json");
			if(manifest == null) return Optional.empty();

			try(BufferedReader reader = new BufferedReader(
					new InputStreamReader(jarFile.getInputStream(manifest)))) {
				JSONObject parsed = (JSONObject) new JSONParser().parse(reader);

				PluginManifest.PluginManifestBuilder builder = PluginManifest.builder();

				String name = (String) parsed.get("name");

				Object versions = parsed.get("version");

				Version version;
				if(versions instanceof JSONArray array)
					version = new Version(
						(Integer) array.get(0), (Integer) array.get(1), (Integer) array.get(2));
				else version = Version.parseVersion((String) versions);

				builder.id(new PluginIdentifier(name, version));
				builder.entrypoint((String) parsed.get("entrypoint"));

				List<String> dependencies = (JSONArray) parsed.get("dependencies");
				builder.dependencies(dependencies.stream()
						.map(PluginWildcard::parseWildcard).toArray(PluginWildcard[]::new));

				return Optional.of(builder.loader(this).build());
			} catch (ParseException e) {
				throw new RuntimeException(e);
			}
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void initialize(ClassLoader parent) {
		discoverFiles();
		this.parentClassLoader = parent;
	}

	@Override
	public Collection<PluginManifest> availablePlugins() {
		return discoveredFiles().keySet();
	}

	// REMEMBER TO DESTROY THIS REFERENCE
	protected URLClassLoader requireLoader(PluginManifest manifest) {
		URLClassLoader loader;

		if(!openLoaders().containsKey(manifest.id()) || (loader = openLoaders.get(manifest.id()).get()) == null) {
			try {
				loader = new URLClassLoader(new URL[] {discoveredFiles.get(manifest).toURI().toURL()}, parentClassLoader);
			} catch (MalformedURLException e) {
				throw new IllegalArgumentException("Could not create class loader for plugin " + manifest.name());
			}
			openLoaders.put(manifest.id(), new WeakReference<>(loader));
		}

		return loader;
	}

	@Override
	public Plugin load(PluginManifest manifest) {
		try {
			return Class.forName(manifest.entrypoint(), true, requireLoader(manifest))
					.asSubclass(Plugin.class).getConstructor(PluginManifest.class).newInstance(manifest);
		} catch (ReflectiveOperationException e) {
			throw new IllegalArgumentException("Could not load plugin " + manifest.name(), e);
		}
	}

	@Override
	public void unload(Plugin pl) {
		WeakReference<URLClassLoader> ref = openLoaders().get(pl.id());

		if(!ref.refersTo(null)) {
			try {
				Objects.requireNonNull(ref.get()).close();
			} catch (IOException e) {
				throw new IllegalArgumentException("Could not close class loader for plugin " + pl.manifest().name());
			} catch(NullPointerException ignored) {} // In case the GC sweeps us
		}

		ref.clear();
		openLoaders().remove(pl.id());

		System.gc(); // Ask GC to clean up the class loader (hopefully)
	}
}
