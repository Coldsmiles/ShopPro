package com.github.xbaimiao.shoppro.util;

public enum ItemFlag {
    HIDE_ENCHANTS,
    HIDE_ATTRIBUTES,
    HIDE_UNBREAKABLE,
    HIDE_DESTROYS,
    HIDE_PLACED_ON,
    HIDE_POTION_EFFECTS,
    HIDE_DYE,
    HIDE_ADDITIONAL_TOOLTIP,
    HIDE_ARMOR_TRIM,
    HIDE_STORED_ENCHANTS;

    public org.bukkit.inventory.ItemFlag toBukkit() {
        String currentName = switch (this) {
            case HIDE_POTION_EFFECTS -> "HIDE_ADDITIONAL_TOOLTIP";
            default -> name();
        };
        try {
            return org.bukkit.inventory.ItemFlag.valueOf(currentName);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
