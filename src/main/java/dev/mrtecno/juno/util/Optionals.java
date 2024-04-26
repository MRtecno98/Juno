package dev.mrtecno.juno.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.function.Consumer;
import java.util.function.UnaryOperator;

@NoArgsConstructor(access=AccessLevel.PRIVATE)
public final class Optionals {
	public static <T> UnaryOperator<T> peek(Consumer<T> c) {
		return x -> {
			c.accept(x);
			return x;
		};
	}
}
