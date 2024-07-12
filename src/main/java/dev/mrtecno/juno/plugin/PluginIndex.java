package dev.mrtecno.juno.plugin;

import dev.mrtecno.juno.util.Index;

public class PluginIndex extends Index<Plugin> {
	public PluginIndex() {
		super(p -> p.manifest().name(), Plugin::getClass);
	}

	public Plugin get(String name) {
		return super.get(name);
	}

	public Plugin get(Class<? extends Plugin> clazz) {
		return super.get(clazz);
	}

	public void remove(String name) {
		super.remove(name);
	}

	public void remove(Class<? extends Plugin> clazz) {
		super.remove(clazz);
	}
}
