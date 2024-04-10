package dev.mrtecno.juno.plugin.identifier;

public sealed interface PluginWildcard extends PluginFilter
		permits NamedIdentifier, PluginIdentifier {
	String name();

	static PluginWildcard parseWildcard(String wildcard) {
		if(wildcard.contains(":")) {
			return PluginIdentifier.parseIdentifier(wildcard);
		} else return new NamedIdentifier(wildcard);
	}
}
