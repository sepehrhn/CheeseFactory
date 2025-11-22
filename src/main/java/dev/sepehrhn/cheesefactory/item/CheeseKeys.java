package dev.sepehrhn.cheesefactory.item;

import dev.sepehrhn.cheesefactory.CheeseFactoryPlugin;
import org.bukkit.NamespacedKey;

public final class CheeseKeys {
    private final NamespacedKey cheeseId;
    private final NamespacedKey customType;
    private final NamespacedKey customItem;
    private final NamespacedKey bacteria;
    private final NamespacedKey curd;
    private final NamespacedKey barrelItem;
    private final NamespacedKey cauldronState;
    private final NamespacedKey curdUnique;

    public CheeseKeys(CheeseFactoryPlugin plugin) {
        this.cheeseId = new NamespacedKey(plugin, "cheese_id");
        this.customType = new NamespacedKey(plugin, "custom_type");
        this.customItem = new NamespacedKey(plugin, "custom_item");
        this.bacteria = new NamespacedKey(plugin, "bacteria");
        this.curd = new NamespacedKey(plugin, "curd");
        this.barrelItem = new NamespacedKey(plugin, "cheese_barrel_item");
        this.cauldronState = new NamespacedKey(plugin, "cauldron_state");
        this.curdUnique = new NamespacedKey(plugin, "curd_unique_id");
        this.inoculatedMilk = new NamespacedKey(plugin, "inoculated_milk");
    }

    public NamespacedKey cheeseId() {
        return cheeseId;
    }

    public NamespacedKey customType() {
        return customType;
    }

    public NamespacedKey customItem() {
        return customItem;
    }

    public NamespacedKey bacteria() {
        return bacteria;
    }

    public NamespacedKey curd() {
        return curd;
    }

    public NamespacedKey barrelItem() {
        return barrelItem;
    }

    public NamespacedKey cauldronState() {
        return cauldronState;
    }

    public NamespacedKey curdUnique() {
        return curdUnique;
    }

    private final NamespacedKey inoculatedMilk;

    public NamespacedKey inoculatedMilk() {
        return inoculatedMilk;
    }
}
