package dev.mrtecno.juno.util;

import java.util.Map;
import java.util.function.BinaryOperator;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public record Pair<K, V>(K key, V value) {
	public static <K, V> Pair<K, V> of(K key, V value) {
		return new Pair<>(key, value);
	}

	public static <K, V> Collector<Pair<K, V>, ?, Map<K,V>> toMap() {
		return Collectors.toMap(Pair::key, Pair::value);
	}

	public static <K, V> Collector<Pair<K, V>, ?, Map<K,V>> toMap(
			BinaryOperator<V> mergeFunction) {
		return Collectors.toMap(Pair::key, Pair::value, mergeFunction);
	}

	public static <K, V, M extends Map<K, V>> Collector<Pair<K, V>, ?, M> toMap(
			BinaryOperator<V> mergeFunction, Supplier<M> mapFactory) {
		return Collectors.toMap(Pair::key, Pair::value, mergeFunction, mapFactory);
	}
}
