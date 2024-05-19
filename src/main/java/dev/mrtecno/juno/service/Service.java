package dev.mrtecno.juno.service;

public interface Service {
	void enable();
	void disable();

	default void load() {}
	default void unload() {}

	default void reload() {
		disable();
		load();
	}

	default void restart() {
		disable();
		enable();
	}

	default void startup() {
		load();
		enable();
	}

	default void run() {
		try {
			load();
			enable();
		} finally {
			disable();
			unload();
		}
	}
}
