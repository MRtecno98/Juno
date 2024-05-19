package dev.mrtecno.juno.plugin;

import dev.mrtecno.juno.plugin.graph.PluginGraph;
import dev.mrtecno.juno.plugin.identifier.PluginIdentifier;
import dev.mrtecno.juno.plugin.identifier.PluginWildcard;
import dev.mrtecno.juno.plugin.identifier.Version;
import dev.mrtecno.juno.service.Service;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.*;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static dev.mrtecno.juno.util.Optionals.peek;

@Getter
@RequiredArgsConstructor
public class PluginManager implements Service, PluginLoader {
	private final Collection<PluginLoader> loaders = new ArrayList<>();

	private final PluginGraph dependencyGraph = new PluginGraph();
	private final Map<String, Plugin> plugins = new HashMap<>();
	private final Set<Plugin> enabled = new HashSet<>();

	private final boolean recursiveLookup;

	public PluginManager(boolean recursiveLookup, PluginLoader... loaders) {
		this(recursiveLookup);
		for(PluginLoader loader : loaders) registerLoader(loader);
	}

	public PluginManager(PluginLoader... loaders) {
		this(false, loaders);
	}

	public void registerLoader(PluginLoader loader) {
		loaders().add(loader);
	}

	@Override
	public void initialize(ClassLoader parent) {
		loaders().forEach(loader -> loader.initialize(parent));
	}

	public void initialize() {
		initialize(getClass().getClassLoader());
	}

	public Map<String, SortedMap<Version, PluginManifest>> discoveredPlugins() {
		return dependencyGraph().pluginNames().entrySet()
				.stream().filter(e -> !e.getValue().versions().isEmpty())
				.collect(Collectors.toMap(
						Map.Entry::getKey,
						e -> e.getValue().versions()));
	}

	@Override
	public Collection<PluginManifest> availablePlugins() {
		return discoveredPlugins().values().stream()
				.flatMap(m -> m.values().stream()).toList();
	}

	public Optional<PluginManifest> lookup(PluginWildcard id) {
		if(discoveredPlugins().containsKey(id.name()))
			if(id instanceof PluginIdentifier identifier)
				return Optional.ofNullable(
						knownVersions(id.name()).get(identifier.version()));
			else return knownVersions(id.name()).reversed()
					.values().stream().filter(id).findFirst();

		return loaders().stream()
				.map(loader -> loader.lookup(id))
				.filter(Optional::isPresent).map(Optional::get)
				.peek(this::discover).findFirst().map(peek(m -> {
					if(recursiveLookup()) lookupDependencies(m);
				}));
	}

	public Collection<PluginManifest> lookupDependencies(PluginManifest manifest) {
		return Arrays.stream(manifest.dependencies()).map(d ->
			lookup(d).orElseThrow(() -> new IllegalArgumentException(
					"Dependency for plugin " + manifest.name() + " not found: " + d)))
				.toList();
	}

	public Optional<PluginManifest> knownManifest(PluginIdentifier id) {
		return Optional.ofNullable(knownVersions(id.name()).get(id.version()));
	}

	public SortedMap<Version, PluginManifest> knownVersions(String name) {
		return discoveredPlugins().computeIfAbsent(name, x -> new TreeMap<>());
	}

	public void discover(PluginManifest manifest) {
		if(!loaders().contains(manifest.loader()))
			throw new IllegalArgumentException("Can't discover a manifest from an unknown loader");

		if(discoveredPlugins().containsKey(manifest.name())
				&& knownManifest(manifest.id()).map(
						m -> m.loader() != manifest.loader()).orElse(false))
			throw new IllegalArgumentException("Manifest already discovered by loader "
					+ manifest.loader().getClass().getName());

		knownVersions(manifest.name()).put(manifest.version(), manifest);
		dependencyGraph().add(manifest);
	}

	public boolean isKnown(String name) {
		return discoveredPlugins().containsKey(name);
	}

	public boolean isKnown(PluginWildcard wildcard) {
		return isKnown(wildcard.name()) && knownVersions(wildcard.name())
				.values().stream().anyMatch(wildcard);
	}

	public boolean isKnown(PluginIdentifier id) {
		return isKnown(id.name()) && knownVersions(id.name()).containsKey(id.version());
	}

	public boolean isKnown(PluginManifest manifest) {
		return isKnown(manifest.id());
	}

	public Optional<Plugin> get(PluginManifest id) {
		return Optional.ofNullable(plugins.get(id.name()));
	}

	public Collection<Plugin> plugins() {
		return plugins.values();
	}

	public Set<PluginManifest> loadedManifests() {
		return plugins().stream().map(Plugin::manifest)
				.collect(Collectors.toUnmodifiableSet());
	}

	public boolean isLoaded(PluginManifest manifest) {
		return plugins.containsKey(manifest.name());
	}

	public boolean isLoaded(PluginIdentifier id) {
		return plugins.containsKey(id.name());
	}

	public boolean isLoaded(String name) {
		return plugins.containsKey(name);
	}

	public Plugin registerPlugin(Plugin plugin) {
		if(isLoaded(plugin.manifest()))
			throw new IllegalArgumentException(
				"Plugin already registered: " + plugin.manifest().name());

		if(plugin.linked() && !plugin.linkedWith(this))
			throw new IllegalArgumentException(
				"Plugin already linked to another manager: " + plugin.manifest().name());
		plugin.link(this);

		plugins.put(plugin.manifest().name(), plugin);

		return plugin;
	}

	public Plugin load(PluginManifest manifest) {
		if(!isKnown(manifest)) discover(manifest);
		if(isLoaded(manifest)) return plugins.get(manifest.name());

		lookupDependencies(manifest).stream()
				.filter(Predicate.not(this::isLoaded)).forEach(this::load);

		return registerPlugin(manifest.load());
	}

	@Override
	public void unload(Plugin pl) {
		if(!isLoaded(pl.manifest()))
			throw new IllegalArgumentException("Plugin not loaded: " + pl.manifest().name());

		dependencyGraph().traverseDependents(pl.manifest(), false, false)
				.filter(this::isLoaded).map(this::get)
				.forEach(p -> unload(p.orElseThrow()));

		try {
			pl.unload0();
		} catch(UnsupportedOperationException e) {
			return; // Plugin does not support unloading
		} catch (Exception e) {
			// Can't abort unloading process if we want to
			// maintain the state of the manager
			// TODO: Create logger service
			Logger.getAnonymousLogger().log(Level.SEVERE,
					"Could not unload plugin " + pl.manifest().name(), e);
		}

		plugins.remove(pl.manifest().name());
		dependencyGraph().remove(pl.manifest());

		// TODO: Unload unused dependencies?
		/* lookupDependencies(pl.manifest()).stream()
				.filter(this::isLoaded).forEach(d -> get(d).ifPresent(this::unload)); */
	}

	public SequencedSet<Plugin> topologicalSort() {
		SequencedSet<PluginManifest> progress = new LinkedHashSet<>();

		plugins().stream().map(Plugin::manifest)
				.forEachOrdered(p -> topologicalSort(p, progress));

		return progress.stream().map(this::get)
				.map(Optional::orElseThrow)
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	private void topologicalSort(PluginManifest target, SequencedSet<PluginManifest> progress) {
		if(progress.contains(target)) return;

		lookupDependencies(target).stream().filter(Predicate.not(progress::contains))
				.forEachOrdered(d -> topologicalSort(d, progress));

		progress.add(target);
	}

	@Override
	public void enable() {
		dependencyGraph().traverse().map(PluginManifest::name)
				.map(plugins::get).map(peek(enabled()::add)).forEachOrdered(Plugin::enable);
	}

	@Override
	public void load() {
		loaders().stream().flatMap(loader
				-> loader.availablePlugins().stream()).forEach(this::load);
	}

	@Override
	public void disable() {
		dependencyGraph().traverse(true).map(PluginManifest::name)
				.map(plugins::get).filter(enabled()::contains)
				.map(peek(enabled()::remove)).forEachOrdered(Plugin::disable);
		dependencyGraph().clear();
		plugins().clear();
	}
}
