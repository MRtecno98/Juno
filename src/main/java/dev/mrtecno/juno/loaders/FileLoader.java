package dev.mrtecno.juno.loaders;

import dev.mrtecno.juno.plugin.Plugin;
import dev.mrtecno.juno.plugin.PluginLoader;
import dev.mrtecno.juno.plugin.PluginManifest;
import dev.mrtecno.juno.plugin.identifier.PluginIdentifier;
import dev.mrtecno.juno.plugin.identifier.PluginWildcard;
import dev.mrtecno.juno.plugin.identifier.Version;
import lombok.Getter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

@Getter
public class FileLoader implements PluginLoader {
	private final File directory;
	private URLClassLoader classLoader;

	private final Map<PluginManifest, File> discoveredFiles = new HashMap<>();

	public FileLoader(File directory) {
		if(!directory.exists() && !directory.mkdirs())
			throw new IllegalArgumentException("Could not create directory: " + directory.getAbsolutePath());

		this.directory = directory;
	}

	public void discoverFiles() {
		discoveredFiles.clear();
		if(!directory.exists()) return;

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
				JSONArray versions = (JSONArray) parsed.get("version");
				Version version = new Version(
						(Integer) versions.get(0), (Integer) versions.get(1), (Integer) versions.get(2));

				builder.id(new PluginIdentifier(name, version));
				builder.entrypoint((String) parsed.get("entrypoint"));

				List<String> dependencies = (JSONArray) parsed.get("dependencies");
				builder.dependencies(dependencies.stream()
						.map(PluginWildcard::parseWildcard).toArray(PluginWildcard[]::new));

				return Optional.of(builder.build());
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
		classLoader = new URLClassLoader(
			discoveredFiles().values().stream().map(f -> {
				try {
					return f.toURI().toURL();
				} catch (MalformedURLException e) {
					throw new RuntimeException(e);
				}
			}).toArray(URL[]::new), parent);
	}

	@Override
	public Collection<PluginManifest> availablePlugins() {
		return discoveredFiles().keySet();
	}

	@Override
	public Optional<PluginManifest> lookup(PluginWildcard name) {
		return availablePlugins().stream()
				.filter(m -> m.name().equals(name.name()))
				.filter(name::accept).findAny();
	}

	@Override
	public Plugin load(PluginManifest manifest) {
		return null;
	}
}
