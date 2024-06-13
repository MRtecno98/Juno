package dev.mrtecno.juno.tests;

import dev.mrtecno.juno.Juno;
import dev.mrtecno.juno.plugin.Plugin;
import dev.mrtecno.juno.plugin.PluginManifest;

public class TestFilePlugin extends Plugin {
	public TestFilePlugin(PluginManifest manifest) {
		super(manifest);
	}

	@Override
	public void enable() {
		System.out.println("Hello from file!");

		System.out.println("Testing selective disable");
		Plugin pl = Juno.instance().pluginManager().get("TestPlugin").orElseThrow();
		Juno.instance().pluginManager().disable(pl);
		Juno.instance().pluginManager().enable(pl);
	}

	@Override
	public void disable() {
		System.out.println("Goodbye from file!");
	}
}
