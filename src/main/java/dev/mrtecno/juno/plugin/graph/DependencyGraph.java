package dev.mrtecno.juno.plugin.graph;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class DependencyGraph<T, N extends DependencyGraph.Node<T>> {
	private final Map<T, N> nodes = new ConcurrentHashMap<>();

	private final Set<N> inodes = new HashSet<>();
	private final Set<N> onodes = new HashSet<>();

	@SafeVarargs
	public final void add(T element, T... dependencies) {
		add(element, List.of(dependencies));
	}

	public void add(T element, List<T> dependencies) {
		addNodes(element, dependencies.stream()
				.map(this::node).collect(Collectors.toList()));
	}

	protected N addNodes(T element, List<N> dependencies) {
		N node = node(element);

		node.dependencies().addAll(dependencies);
		dependencies.forEach(d -> d.dependents().add(node));

		if(!node.dependencies().isEmpty()) {
			onodes.add(node);
			inodes.addAll(node.dependencies());
		}

		return node;
	}

	public void addDependency(T element, T dependency) {
		addDependency(element, node(dependency));
	}

	protected void addDependency(T element, N dependency) {
		N node = node(element);

		onodes.add(node);
		inodes.add(dependency);

		node.dependencies().add(dependency);
		dependency.dependents().add(node);
	}

	public void updateRoots() {
		onodes.clear();
		inodes.clear();

		uniqueNodes().forEach(n -> {
			if(!n.dependencies().isEmpty()) inodes.add(n);
			else onodes.add(n);
		});
	}

	public Set<N> roots() {
		Set<N> roots = new HashSet<>(onodes);
		roots.removeAll(inodes);

		return Collections.unmodifiableSet(roots);
	}

	public Set<N> leaves() {
		Set<N> leaves = new HashSet<>(inodes);
		leaves.removeAll(onodes);

		return Collections.unmodifiableSet(leaves);
	}

	protected void remove(N node) {
		nodes.remove(node.element());

		node.dependencies().forEach(d -> d.dependents().remove(node));
		node.dependents().forEach(d -> d.dependencies().remove(node));

		inodes.remove(node);
		onodes.remove(node);
	}

	public void clear() {
		nodes.clear();
		inodes.clear();
		onodes.clear();
	}

	public void removeRecursively(T element) {
		removeRecursively(node(element));
	}

	protected void removeRecursively(N node) {
		remove(node);
		node.dependents().forEach(n
				-> removeRecursively(n.element()));
	}

	public N remove(T element) {
		N node = nodes.get(element);
		if(node == null) return null;

		remove(node);
		return node;
	}

	private N node(T element) {
		return nodes.computeIfAbsent(element, _ -> makeNode(element, new ArrayList<>(), new ArrayList<>()));
	}

	protected abstract N makeNode(T element, List<Node<T>> dependencies, List<Node<T>> dependents);

	public Set<T> elements() {
		return nodes.keySet();
	}

	public Set<N> uniqueNodes() {
		return new HashSet<>(nodes.values());
	}

	public Set<T> dependencies(T element) {
		return nodes.get(element).dependenciesStream(element)
				.collect(Collectors.toUnmodifiableSet());
	}

	public Set<T> dependents(T element) {
		return nodes.get(element).dependentsStream(element)
				.collect(Collectors.toUnmodifiableSet());
	}

	public Set<T> transitiveDependencies(T element) {
		return nodes.get(element).transitiveDependenciesStream()
				.collect(Collectors.toUnmodifiableSet());
	}

	public Set<T> transitiveDependents(T element) {
		return nodes.get(element).transitiveDependentsStream()
				.collect(Collectors.toUnmodifiableSet());
	}

	public int layer(N node) {
		return layer0(node, new LinkedHashMap<>());
	}

	private int layer0(N node, SequencedMap<N, Integer> visited) {
		if(visited.containsKey(node))
			if(visited.get(node) == -1) { // Circular dependency
				// Build detailed report of circular dependencies
				StringBuilder sb = new StringBuilder();
				sb.append("Circular dependency detected at graph node: \n\t\t").append(node.element());
				sb.append("\n");

				var visitedEntries = visited.sequencedEntrySet();

				// skip nodes not in the circular sequence
				int index = visitedEntries.stream().map(Map.Entry::getKey).toList().indexOf(node);

				visitedEntries.stream().skip(index).filter(e -> e.getValue() == -1).forEach(
						e -> {
							sb.append("\t-> ").append(e.getKey()).append(" depends: [");
							e.getKey().dependencies()
									.forEach(d -> sb.append(d).append(", "));
							sb.append("]\n");
						});

				sb.delete(sb.length() - 1, sb.length()); // Remove last newline
				throw new IllegalStateException(sb.toString());
			} else return visited.get(node);

		visited.put(node, -1); // Mark as visited (to avoid circular dependencies)

		int layer = node.<N>dependencies().stream()
				.mapToInt(n -> layer0(n, visited))
				.max().orElse(-1) + 1;

		visited.put(node, layer);
		return layer;
	}

	public Stream<N> layer(int layer) {
		return uniqueNodes().stream()
				.filter(n -> layer(n) == layer);
	}

	public List<Set<N>> layers() {
		return layers(uniqueNodes());
	}

	public List<Set<N>> layers(Collection<N> nodes) {
		Map<Integer, Set<N>> layers = new HashMap<>();
		nodes.forEach(n -> layers.computeIfAbsent(layer(n), _ -> new HashSet<>()).add(n));
		return layers.keySet().stream().mapToInt(i -> i)
				.sorted().mapToObj(layers::get).toList();
	}

	public Stream<N> layeredNodeTraversal() {
		return layeredNodeTraversal(false);
	}

	public Stream<N> layeredNodeTraversal(boolean reversed) {
		return layeredNodeTraversal(uniqueNodes(), reversed);
	}

	public Stream<N> layeredNodeTraversal(Collection<N> nodes, boolean reversed) {
		return (reversed ? layers(nodes).reversed() : layers(nodes)).stream().flatMap(Set::stream);
	}

	public Stream<N> traverseDependencies(N node, boolean self, boolean reversed) {
		return layeredNodeTraversal(self ? node.dependenciesAndSelf() : node.dependencies(), reversed);
	}

	public Stream<N> traverseDependents(N node, boolean self, boolean reversed) {
		return layeredNodeTraversal(self ? node.dependentsAndSelf() : node.dependents(), reversed);
	}

	public Stream<T> traverse() {
		return layeredNodeTraversal().map(Node::element);
	}

	public Stream<T> traverse(Consumer<N> consumer) {
		return layeredNodeTraversal().peek(consumer).map(Node::element);
	}

	public Stream<T> traverse(boolean reversed) {
		return layeredNodeTraversal(reversed).map(Node::element);
	}

	public Stream<T> traverse(Consumer<N> consumer, boolean reversed) {
		return layeredNodeTraversal(reversed).peek(consumer).map(Node::element);
	}

	public Stream<T> traverseDependencies(T element, boolean self) {
		return traverseDependencies(element, self, false);
	}

	public Stream<T> traverseDependents(T element, boolean self) {
		return traverseDependents(element, self, false);
	}

	public Stream<T> traverseDependencies(T element, boolean self, boolean reversed) {
		return traverseDependencies(node(element), self, reversed).map(Node::element);
	}

	public Stream<T> traverseDependents(T element, boolean self, boolean reversed) {
		return traverseDependents(node(element), self, reversed).map(Node::element);
	}

	public interface Node<T> {
		T element();

		<N extends Node<T>> Collection<N> dependencies();
		<N extends Node<T>> Collection<N> dependents();

		@SuppressWarnings("unchecked")
		default <N extends Node<T>> Collection<N> dependenciesAndSelf() {
			Collection<N> dependencies = new ArrayList<>(dependencies());
			dependencies.add((N) this);
			return dependencies;
		}

		@SuppressWarnings("unchecked")
		default <N extends Node<T>> Collection<N> dependentsAndSelf() {
			Collection<N> dependents = new ArrayList<>(dependents());
			dependents.add((N) this);
			return dependents;
		}

		default Stream<T> transitiveDependenciesStream() {
			return dependencies().stream().flatMap(n -> Stream.concat(
					Stream.of(n.element()), n.transitiveDependenciesStream()));
		}

		default Stream<T> transitiveDependentsStream() {
			return dependents().stream().flatMap(n -> Stream.concat(
					Stream.of(n.element()), n.transitiveDependentsStream()));
		}

		default Stream<T> dependenciesStream(T element) {
			return dependencies().stream().map(Node::element);
		}

		default Stream<T> dependentsStream(T element) {
			return dependents().stream().map(Node::element);
		}
	}

	class FixedDependencyGraph<E> extends DependencyGraph<E, FixedDependencyGraph.FixedNode<E>> {
		@Override
		protected FixedNode<E> makeNode(E element, List<Node<E>> dependencies, List<Node<E>> dependents) {
			return new FixedNode<>(element, dependencies, dependents);
		}

		record FixedNode<T>(T element, List<Node<T>> dependencies, List<Node<T>> dependents) implements Node<T> {}
	}
}
