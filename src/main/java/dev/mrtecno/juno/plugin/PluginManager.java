package dev.mrtecno.juno.plugin;

import dev.mrtecno.juno.plugin.identifier.PluginIdentifier;
import dev.mrtecno.juno.plugin.identifier.PluginWildcard;
import dev.mrtecno.juno.service.Service;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static dev.mrtecno.juno.util.Optionals.peek;

@Getter
@RequiredArgsConstructor
public class PluginManager implements Service, PluginLoader {
	private final Collection<PluginLoader> loaders = new ArrayList<>();

	private final Map<String, SortedMap<PluginIdentifier, PluginManifest>> discoveredPlugins = new HashMap<>();
	private final Map<PluginIdentifier, Plugin> plugins = new HashMap<>();

	private final Set<String> pluginNames = new HashSet<>();

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

	@Override
	public Collection<PluginManifest> availablePlugins() {
		return discoveredPlugins().values().stream()
				.flatMap(m -> m.values().stream()).toList();
	}

	public Optional<PluginManifest> lookup(PluginWildcard id) {
		if(discoveredPlugins().containsKey(id.name()))
			if(id instanceof PluginIdentifier)
				return Optional.ofNullable(knownVersions(id.name()).get(id));
			else return knownVersions(id.name()).reversed()
					.values().stream().filter(id::accept).findFirst();

		return loaders().stream()
				.map(loader -> loader.lookup(id))
				.filter(Optional::isPresent).map(Optional::get)
				.peek(this::discover).findFirst().map(peek(m ->{
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
		return Optional.ofNullable(knownVersions(id.name()).get(id));
	}

	public SortedMap<PluginIdentifier, PluginManifest> knownVersions(String name) {
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

		knownVersions(manifest.name()).put(manifest.id(), manifest);
	}

	public boolean isKnown(String name) {
		return discoveredPlugins().containsKey(name);
	}

	public boolean isKnown(PluginWildcard wildcard) {
		return isKnown(wildcard.name()) && knownVersions(wildcard.name())
				.values().stream().anyMatch(wildcard::accept);
	}

	public boolean isKnown(PluginIdentifier id) {
		return isKnown(id.name()) && knownVersions(id.name()).containsKey(id);
	}

	public boolean isKnown(PluginManifest manifest) {
		return isKnown(manifest.id());
	}

	public Optional<Plugin> get(PluginManifest id) {
		return Optional.ofNullable(plugins.get(id.id()));
	}

	public Collection<Plugin> plugins() {
		return plugins.values();
	}

	public Set<PluginManifest> loadedManifests() {
		return plugins().stream().map(Plugin::manifest)
				.collect(Collectors.toUnmodifiableSet());
	}

	public boolean isLoaded(PluginManifest manifest) {
		return plugins.containsKey(manifest.id());
	}

	public boolean isLoaded(PluginIdentifier id) {
		return plugins.containsKey(id);
	}

	public boolean isLoaded(String name) {
		return pluginNames.contains(name);
	}

	public Plugin registerPlugin(Plugin plugin) {
		if(isLoaded(plugin.manifest()))
			throw new IllegalArgumentException(
				"Plugin already registered: " + plugin.manifest().name());

		plugins.put(plugin.manifest().id(), plugin);
		pluginNames().add(plugin.manifest().name());

		return plugin;
	}

	public Plugin load(PluginManifest manifest) {
		if(!isKnown(manifest)) discover(manifest);
		if(isLoaded(manifest)) return plugins.get(manifest.id());

		lookupDependencies(manifest).stream()
				.filter(Predicate.not(this::isLoaded)).forEach(this::load);

		return registerPlugin(manifest.load());
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
		load();
		topologicalSort().forEach(Plugin::enable);
	}

	@Override
	public void load() {
		loaders().stream().flatMap(loader
				-> loader.availablePlugins().stream()).forEach(this::load);
	}

	@Override
	public void disable() {
		plugins().forEach(Plugin::disable);
		discoveredPlugins().clear();
		plugins().clear();
		pluginNames.clear();
	}
}
