package dev.sepehrhn.cheesefactory.cheese;

import org.bukkit.potion.PotionEffectType;

public class CheeseEffectDefinition {
    private final PotionEffectType type;
    private final int durationTicks;
    private final int amplifier;

    public CheeseEffectDefinition(PotionEffectType type, int durationTicks, int amplifier) {
        this.type = type;
        this.durationTicks = durationTicks;
        this.amplifier = amplifier;
    }

    public PotionEffectType type() {
        return type;
    }

    public int durationTicks() {
        return durationTicks;
    }

    public int amplifier() {
        return amplifier;
    }
}
