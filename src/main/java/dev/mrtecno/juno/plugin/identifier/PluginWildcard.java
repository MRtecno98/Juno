package dev.mrtecno.juno.plugin.identifier;

public sealed interface PluginWildcard extends PluginFilter
		permits NamedIdentifier, PluginIdentifier {
	String name();
}
