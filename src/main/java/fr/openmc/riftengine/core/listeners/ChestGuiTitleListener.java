package fr.openmc.riftengine.core.listeners;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import fr.openmc.core.OMCRegistry;
import fr.openmc.riftengine.api.utils.IdentifierUtils;
import fr.openmc.riftengine.core.converter.writers.glyph.chestgui.ChestScreenWriter;
import fr.openmc.riftengine.core.utils.FloodgateUtils;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ChestGuiTitleListener extends PacketAdapter {
    private final Set<Character> offsetCaracter;
    private final Map<Character, String> cacheUnicode;

    public ChestGuiTitleListener(Path javaRootPath, Path itemsAdderDataPath, Plugin plugin) throws IOException {
        super(plugin, ListenerPriority.LOWEST, PacketType.Play.Server.OPEN_WINDOW);
        this.offsetCaracter = OMCRegistry.SCANNERS.OFFSET_CARACTER.scan(javaRootPath);
        this.cacheUnicode = OMCRegistry.SCANNERS.CARACTER_UNICODE_CACHE.scan(itemsAdderDataPath);
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        Player player = event.getPlayer();
        if (!FloodgateUtils.isBedrock(player)) return;

        PacketContainer packet = event.getPacket();
        WrappedChatComponent title = packet.getChatComponents().read(0);
        if (title == null) return;

        String raw = PlainTextComponentSerializer.plainText()
                .serialize(GsonComponentSerializer.gson().deserialize(title.getJson()));

        // * si l'inv est fait en sorte que le title se resolve au client ex. §r§f:offset_-48::city_template3x9:
        // * sinon on nettoie le string pour savoir si il contient un offset, donc un potentiel menu
        if (raw.contains(":offset_")) {
            String[] parts = raw.split("::");
            if (parts.length < 2) return;

            String menuName = parts[1].replace(":", "");
            packet.getChatComponents().write(0, WrappedChatComponent.fromText(ChestScreenWriter.PREFIX + ":" + menuName));
        } else {
            boolean hasOffset = raw.chars()
                    .mapToObj(c -> (char) c)
                    .anyMatch(offsetCaracter::contains);

            if (!hasOffset) return;

            // * on supprime les offsets et les caracteres basiques pour ne garder que le caractere du menu
            String cleaned = raw.chars()
                    .mapToObj(c -> String.valueOf((char) c))
                    .filter(c -> c.length() == 1 && !offsetCaracter.contains(c.charAt(0)))
                    .collect(Collectors.joining())
                    .replaceAll("§[0-9a-fk-or]", "");

            char menuChar = cleaned.charAt(0);

            packet.getChatComponents().write(0, WrappedChatComponent.fromText(
                    ChestScreenWriter.PREFIX + ":" + IdentifierUtils.removeNamespace(cacheUnicode.get(menuChar))));
        }
    }
}