package fr.openmc.riftengine.core.registry.scanner;

import fr.openmc.core.bootstrap.registries.KeyedRegistry;
import fr.openmc.core.bootstrap.registries.Registry;
import fr.openmc.riftengine.core.scanner.general.YamlNamespaceIAScanner;
import fr.openmc.riftengine.core.scanner.general.YamlScanner;
import fr.openmc.riftengine.core.scanner.icons.IconScanner;

public class ScannerRegistry extends Registry<String, AbstractScanner<?, ?>>
    implements KeyedRegistry<String, AbstractScanner<?, ?>> {

    public final IconScanner ICONS = register(new IconScanner());
    public final YamlNamespaceIAScanner YAML_ITEMSADDER_NAMESPACE = register(new YamlNamespaceIAScanner());
    public final YamlScanner YAML = register(new YamlScanner());

    @Override
    public String key(AbstractScanner<?, ?> abstractScanner) {
        return abstractScanner.getClass().getSimpleName();
    }
}