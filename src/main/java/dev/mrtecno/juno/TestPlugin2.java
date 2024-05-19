package dev.mrtecno.juno;

import dev.mrtecno.juno.plugin.Manifest;
import dev.mrtecno.juno.plugin.Plugin;
import dev.mrtecno.juno.plugin.PluginManifest;

@Manifest(name = "TestPlugin2", version = "1.0.0", dependencies = "TestPlugin")
public class TestPlugin2 extends Plugin {
	public TestPlugin2(PluginManifest manifest) {
		super(manifest);
	}

	@Override
	public void enable() {
		System.out.println("Hello, world 2!");
	}

	@Override
	public void disable() {
		System.out.println("Goodbye, world 2!");
	}
}
