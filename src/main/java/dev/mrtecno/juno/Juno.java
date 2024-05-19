package dev.mrtecno.juno;

import dev.mrtecno.juno.loaders.IntegratedLoader;
import dev.mrtecno.juno.plugin.PluginManager;
import dev.mrtecno.juno.service.Service;
import dev.mrtecno.juno.service.ServiceManager;
import lombok.Getter;

@Getter
public class Juno implements Service {
	private final ServiceManager serviceManager = new ServiceManager();
	private final PluginManager pluginManager = new PluginManager();

	private static final @Getter Juno instance = new Juno();

	@Override
	public void enable() {
		serviceManager.register(this).ifPresent(_ -> {
			throw new IllegalStateException("Juno was already registered");
		});

		// pluginManager().registerLoader(new FileLoader(new File("plugins")));
		pluginManager().registerLoader(
				new IntegratedLoader(
						"dev.mrtecno.juno.TestPlugin",
						"dev.mrtecno.juno.TestPlugin2"));
		pluginManager().initialize();

		pluginManager().startup();
	}

	@Override
	public void disable() {
		pluginManager().disable();

		serviceManager.unregister(getClass()).orElseThrow(
			() -> new IllegalStateException("Juno was not registered"));
	}

	@SuppressWarnings("unchecked")
	public static <T extends Service> T service(Class<T> clazz) {
		return (T) instance().serviceManager().require(clazz);
	}

	public static void main(String[] args) {
		instance().run();
	}
}
