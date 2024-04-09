package dev.mrtecno.juno.util;

import lombok.experimental.UtilityClass;

import java.util.function.Consumer;
import java.util.function.UnaryOperator;

@UtilityClass
public class Optionals {
	public <T> UnaryOperator<T> peek(Consumer<T> c) {
		return x -> {
			c.accept(x);
			return x;
		};
	}
}
