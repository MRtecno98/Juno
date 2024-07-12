package dev.mrtecno.juno.util;

import lombok.AccessLevel;
import lombok.Getter;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

@Getter(AccessLevel.PRIVATE)
public class Index<T> {
	private final Map<Class<?>, Map<Object, T>> indexes = new HashMap<>();
	private final Collection<Function<T, ?>> keys = new ArrayList<>();

	@SafeVarargs
	public Index(Function<T, ?>... keys) {
		for (Function<T, ?> key : keys)
			keys().add(key);
	}

	protected Stream<Object> streamKeys(T val) {
		return keys().stream().map(f -> f.apply(val));
	}

	public void put(T val) {
		streamKeys(val).forEach(k -> indexes().computeIfAbsent(
						k.getClass(), _ -> new HashMap<>()).put(k, val));
	}

	public Map<Object, T> getIndex(Object key) {
		return indexes().computeIfAbsent(key.getClass(), _ -> new HashMap<>());
	}

	public T get(Object key) {
		return getIndex(key).get(key);
	}

	public void remove(Object key) {
		getIndex(key).remove(key);
	}

	public void removeValue(T val) {
		streamKeys(val).forEach(k -> indexes().get(k.getClass()).remove(k));
	}

	public void clear() {
		indexes().clear();
	}

	public boolean containsKey(Object key) {
		return indexes().containsKey(key.getClass()) && getIndex(key).containsKey(key);
	}

	public boolean containsValue(T val) {
		return streamKeys(val).anyMatch(k -> getIndex(k).containsValue(val));
	}

	public int size() {
		return indexes().values().stream()
				.mapToInt(Map::size).distinct().findFirst().orElse(0);
	}

	public Collection<T> values() {
		return indexes().values().stream()
				.flatMap(m -> m.values().stream()).distinct().toList();
	}
}
