package dev.mrtecno.juno.plugin.graph;

import dev.mrtecno.juno.plugin.PluginManifest;
import dev.mrtecno.juno.plugin.identifier.PluginWildcard;
import dev.mrtecno.juno.plugin.identifier.Version;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.*;
import java.util.function.Predicate;

@Getter
public class PluginGraph extends DependencyGraph<PluginManifest, PluginGraph.PluginNode> {
	private final Map<String, PluginNode> pluginNames = new HashMap<>();

	@Override
	protected PluginNode makeNode(PluginManifest element,
								  List<Node<PluginManifest>> dependencies,
								  List<Node<PluginManifest>> dependents) {
		PluginNode n = makeNode(element.name());
		n.addVersion(element);
		n.addDependency(element.version(), dependencies);
		n.addDependents(dependents);

		return n.pin(element.version());
	}

	protected PluginNode makeNode(String name) {
		return pluginNames().computeIfAbsent(name, PluginNode::new);
	}

	public void reselectAll() {
		layeredNodeTraversal()
				.forEach(n -> n.reselect(false));
		// Updating roots at the end all in once
		updateRoots();
	}

	@Override
	public void remove(PluginNode node) {
		if(!node.dependents().isEmpty())
			throw new IllegalStateException("Cannot remove plugin with dependents");

		node.versions().values().forEach(super::remove);
		pluginNames().remove(node.name());
	}

	@Override
	public void clear() {
		super.clear();
		pluginNames().clear();
	}

	@Override
	public PluginNode addNodes(PluginManifest plugin, List<PluginNode> dependencies) {
		dependencies.addAll(Arrays.stream(
				plugin.dependencies()).map(PluginWildcard::name)
				.map(this::makeNode).toList());

		// Kinda of a hack to make sure the dep graph knows which version to add the deps to
		return super.addNodes(plugin, dependencies).unpin();
	}

	@Getter
	@RequiredArgsConstructor
	public class PluginNode implements DependencyGraph.Node<PluginManifest> {
		private final String name;
		private final NavigableMap<Version, PluginManifest> versions = new TreeMap<>();
		private final List<Node<PluginManifest>> dependents = new ArrayList<>();
		private final Map<Version, List<Node<PluginManifest>>> versionDependencies = new HashMap<>();

		private Version selectedVersion;
		private boolean pinned;

		public PluginNode(PluginManifest plugin,
						  List<Node<PluginManifest>> dependencies,
						  List<Node<PluginManifest>> dependents) {
			this(plugin.name(), dependents);

			addVersion(plugin);
			versionDependencies().put(plugin.version(), dependencies);
			reselect();
		}

		public PluginNode(String name,
						  List<Node<PluginManifest>> dependents) {
			this(name);
			dependents().addAll(dependents);
		}

		public void addDependency(Version version, List<Node<PluginManifest>> dependencies) {
			versionDependencies().put(version, dependencies);
		}

		public void addDependents(List<Node<PluginManifest>> dependents) {
			dependents().addAll(dependents);
		}

		public void addVersion(PluginManifest plugin) {
			if(!plugin.name().equals(name()))
				throw new IllegalArgumentException(
						"Different versions of plugins cannot have different names.");
			versions().put(plugin.version(), plugin);
		}

		public Version selectedVersion() {
			if(selectedVersion == null)
				reselectDetailed(true).orElseThrow();
			return selectedVersion;
		}

		public Optional<Version> versionIfSelected() {
			return Optional.ofNullable(selectedVersion);
		}

		public void deselect() {
			selectedVersion = null;
		}

		public void select(Version version) {
			if(!versions().containsKey(version))
				throw new IllegalArgumentException(
						"Version " + version + " of plugin " + name() + " is not available");
			selectedVersion = version;
		}

		public PluginNode pin(Version version) {
			select(version);
			return pin();
		}

		protected PluginNode pin() {
			pinned = true;
			return this;
		}

		public PluginNode unpin() {
			pinned = false;
			reselect();
			return this;
		}

		public Optional<Version> reselect() {
			return reselect(true);
		}

		protected Optional<Version> reselect(boolean updateRoots) {
			return reselectDetailed(updateRoots).result();
		}

		protected SelectionResult reselectDetailed(boolean updateRoots) {
			if(pinned())
				return SelectionResult.trivial(name(), versionIfSelected().orElse(null));

			Optional<Version> opt;
			Map<Version, Map<PluginManifest, Boolean>> compatibilities = new HashMap<>();
			(opt = versions().descendingKeySet().stream().filter(v -> {
				Map<PluginManifest, Boolean> vercomp;
				compatibilities.put(v, (vercomp = new HashMap<>()));

				for(Node<PluginManifest> d : dependents())
					for(PluginWildcard wildcard : d.element().dependencies())
						if(wildcard.name().equals(name())) {
							boolean test = wildcard.test(versions().get(v));
							vercomp.put(d.element(), test);

							if (!test) return false;
							else break; // Only one wildcard matches per dependent
						}
				return true;
			// findFirst returns the highest version because descendingKeySet is ordered
			}).findFirst()).ifPresentOrElse(this::select, this::deselect);

			Optional<SelectionResult> result = Optional.empty();
			if(opt.isPresent()) {
				// Update any downstream dependencies to check for possible incompatible versions
				result = this.<PluginNode>dependencies().stream()
						.map(n -> n.reselectDetailed(false)).findFirst();
			}

			if(updateRoots)
				PluginGraph.this.updateRoots();

			return result.orElseGet(() -> new SelectionResult(name(), opt.orElse(null), compatibilities));
		}

		@Override
		public PluginManifest element() {
			return versions().get(selectedVersion());
		}

		@Override
		@SuppressWarnings("unchecked")
		public <N extends Node<PluginManifest>> Collection<N> dependencies() {
			return versionIfSelected().map(versionDependencies()::get).orElse(Collections.EMPTY_LIST);
		}

		@Override
		public String toString() {
			return "PluginNode[" + name() + " : " + versionIfSelected()
					.map(Version::toString).orElse("<unselected>") + "]";
		}

		public record SelectionResult(String name, Version selected,
									  Map<Version, Map<PluginManifest, Boolean>> compatibilities) {
			public Optional<Version> result() {
				return Optional.ofNullable(selected);
			}

			public Optional<SelectionResult> ifSuccesfull() {
				return result().map(_ -> this);
			}

			public Version orElseThrow() {
				return ifSuccesfull().orElseThrow(this::toException).selected();
			}

			public CompatibilityException toException() {
				return new CompatibilityException(this);
			}

			public String message() {
				StringBuilder sb = new StringBuilder("No version of plugin ");
				sb.append(name());
				sb.append(" is compatible with its dependents\n");
				compatibilities().forEach((k, v) -> {
					sb.append("\t\t- ").append(k).append(" denied by: [");
					v.entrySet().stream().filter(Predicate.not(Map.Entry::getValue))
							.map(Map.Entry::getKey).forEach(e -> sb.append(e.id()).append(", "));
					sb.append("]\n");
				});

				sb.delete(sb.length() - 1, sb.length()); // Remove last newline
				return sb.toString();
			}

			public static SelectionResult trivial(String name, Version selected) {
				return new SelectionResult(name, selected, Map.of());
			}
		}
	}
}
