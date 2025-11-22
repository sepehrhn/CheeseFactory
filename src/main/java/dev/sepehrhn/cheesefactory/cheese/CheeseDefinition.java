package dev.sepehrhn.cheesefactory.cheese;

import net.kyori.adventure.text.Component;

import java.util.List;

public class CheeseDefinition {
    private final String id;
    private final String rawName;
    private final Component displayName;
    private final int weight;
    private final List<CheeseEffectDefinition> effects;
    private final List<String> rawLore;
    private final List<Component> lore;
    private final CustomIdDefinition customId;
    private final Integer customModelData;

    public CheeseDefinition(
            String id,
            String rawName,
            Component displayName,
            int weight,
            List<CheeseEffectDefinition> effects,
            List<String> rawLore,
            List<Component> lore,
            CustomIdDefinition customId,
            Integer customModelData
    ) {
        this.id = id;
        this.rawName = rawName;
        this.displayName = displayName;
        this.weight = weight;
        this.effects = effects;
        this.rawLore = rawLore;
        this.lore = lore;
        this.customId = customId;
        this.customModelData = customModelData;
    }

    public String id() {
        return id;
    }

    public String rawName() {
        return rawName;
    }

    public Component displayName() {
        return displayName;
    }

    public int weight() {
        return weight;
    }

    public List<CheeseEffectDefinition> effects() {
        return effects;
    }

    public List<String> rawLore() {
        return rawLore;
    }

    public List<Component> lore() {
        return lore;
    }

    public CustomIdDefinition customId() {
        return customId;
    }

    public Integer customModelData() {
        return customModelData;
    }
}
