package dev.sepehrhn.cheesefactory.listener;

import dev.sepehrhn.cheesefactory.cheese.CheeseDefinition;
import dev.sepehrhn.cheesefactory.cheese.CheeseEffectDefinition;
import dev.sepehrhn.cheesefactory.cheese.CheeseRegistry;
import dev.sepehrhn.cheesefactory.item.CheeseKeys;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;

public class CheeseConsumeListener implements Listener {

    private final CheeseKeys keys;
    private final CheeseRegistry registry;

    public CheeseConsumeListener(CheeseKeys keys, CheeseRegistry registry) {
        this.keys = keys;
        this.registry = registry;
    }

    @EventHandler(ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        var item = event.getItem();
        if (item == null || !item.hasItemMeta()) {
            return;
        }
        var pdc = item.getItemMeta().getPersistentDataContainer();
        String id = pdc.get(keys.cheeseId(), PersistentDataType.STRING);
        if (id == null) {
            return;
        }
        registry.getById(id).ifPresent(def -> applyEffects(def, event));
    }

    private void applyEffects(CheeseDefinition def, PlayerItemConsumeEvent event) {
        var player = event.getPlayer();
        for (CheeseEffectDefinition effect : def.effects()) {
            player.addPotionEffect(new PotionEffect(effect.type(), effect.durationTicks(), effect.amplifier()));
        }
    }
}
