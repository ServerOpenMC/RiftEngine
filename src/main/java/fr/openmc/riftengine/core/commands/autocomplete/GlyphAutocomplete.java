package fr.openmc.riftengine.core.commands.autocomplete;

import fr.openmc.riftengine.core.RiftRegistry;
import fr.openmc.riftengine.core.registry.glyphs.Glyph;
import fr.openmc.shaded.revxrsal.commands.autocomplete.SuggestionProvider;
import fr.openmc.shaded.revxrsal.commands.bukkit.actor.BukkitCommandActor;
import fr.openmc.shaded.revxrsal.commands.node.ExecutionContext;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class GlyphAutocomplete implements SuggestionProvider<BukkitCommandActor> {

    @Override
    public @NotNull List<String> getSuggestions(@NotNull ExecutionContext<BukkitCommandActor> context) {
        return RiftRegistry.GLYPHS.values()
                .stream()
                .map(Glyph::getNamespacedId)
                .toList();
    }
}