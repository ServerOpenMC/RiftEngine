package fr.openmc.riftengine.core.listeners;

import dev.lone.itemsadder.api.Events.ItemsAdderLoadDataEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.List;

public class LoadAfterItemsAdderListener implements Listener {
    public static List<Runnable> actionToLaunch = new ArrayList<>();

    @EventHandler
    public void onItemsAdderLoadFinish(ItemsAdderLoadDataEvent event) {
        actionToLaunch.forEach(Runnable::run);
        actionToLaunch.clear();
    }

}
