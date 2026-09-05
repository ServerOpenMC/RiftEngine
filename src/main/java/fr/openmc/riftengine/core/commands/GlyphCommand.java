package fr.openmc.riftengine.core.commands;

import fr.openmc.riftengine.core.RiftRegistry;
import fr.openmc.riftengine.core.commands.autocomplete.GlyphAutocomplete;
import fr.openmc.shaded.revxrsal.commands.annotation.Command;
import fr.openmc.shaded.revxrsal.commands.annotation.Subcommand;
import fr.openmc.shaded.revxrsal.commands.annotation.SuggestWith;
import fr.openmc.shaded.revxrsal.commands.bukkit.annotation.CommandPermission;
import org.bukkit.entity.Player;

@Command({"glyph", "remoji"})
@CommandPermission("omc.commands.rift.glyph")
public class GlyphCommand {
    @Subcommand("test")
    @CommandPermission("omc.commands.rift.glyph.set")
    public void set(
            Player player,
            @SuggestWith(GlyphAutocomplete.class) String namespacedId
    ) {
        if (RiftRegistry.GLYPHS.get(namespacedId).isEmpty()) return;

        player.sendMessage("caractere mis : " + RiftRegistry.GLYPHS.get(namespacedId).get().getBedrockChar());
    }
}
