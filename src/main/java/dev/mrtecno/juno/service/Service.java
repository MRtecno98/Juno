package dev.mrtecno.juno.service;

public interface Service {
	void enable();
	void disable();

	default void load() {}

	default void reload() {
		disable();
		load();
	}

	default void restart() {
		reload();
		enable();
	}

	default void startup() {
		try {
			load();
			enable();
		} finally {
			disable();
		}
	}
}
