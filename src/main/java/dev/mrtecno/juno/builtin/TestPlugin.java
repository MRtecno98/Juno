package dev.mrtecno.juno.builtin;

import dev.mrtecno.juno.plugin.Manifest;
import dev.mrtecno.juno.plugin.Plugin;
import dev.mrtecno.juno.plugin.PluginManifest;

@Manifest(name = "TestPlugin", version = "1.0.0")
public class TestPlugin extends Plugin {
	public TestPlugin(PluginManifest manifest) {
		super(manifest);
	}

	@Override
	public void enable() {
		System.out.println("Hello, world!");
	}

	@Override
	public void disable() {
		System.out.println("Goodbye, world!");
	}
}
