package dev.mrtecno.juno.plugin.identifier;

import dev.mrtecno.juno.plugin.PluginManifest;

import java.util.function.Predicate;

public interface PluginFilter extends Predicate<PluginManifest> {

}
