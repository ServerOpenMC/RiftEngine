package fr.openmc.riftengine.core.listeners;

import dev.lone.itemsadder.api.Events.ItemsAdderLoadDataEvent;
import fr.openmc.riftengine.core.RiftPlugin;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class LoadAfterItemsAdderListener implements Listener {
    public static boolean reloadToLaunch = false;

    @EventHandler
    public void onItemsAdderLoadFinish(ItemsAdderLoadDataEvent event) {
        if (reloadToLaunch) {
            Bukkit.getScheduler().runTask(RiftPlugin.getInstance(), () ->
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "iazip")
            );
            reloadToLaunch = false;
        }
    }

}
