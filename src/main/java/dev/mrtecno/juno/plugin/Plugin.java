package dev.mrtecno.juno.plugin;

import dev.mrtecno.juno.service.Service;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public abstract class Plugin implements Service {
	private final PluginManifest manifest;
}
