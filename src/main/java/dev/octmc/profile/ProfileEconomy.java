package dev.octmc.profile;

import java.util.UUID;

/** Keeps the normal profile menu independent of the optional economy mod. */
final class ProfileEconomy {
    static final String UNAVAILABLE = "Unavailable";
    private static final Access NONE = new Access() {
        @Override public void ensureAccount(UUID player, String name) { }
        @Override public String balance(UUID player) { return UNAVAILABLE; }
    };
    private static Access access = NONE;

    private ProfileEconomy() { }

    static void initialize(boolean ecoLoaded) {
        // Load the class containing ECO references only when Fabric found the mod.
        access = ecoLoaded ? new EcoIntegration() : NONE;
    }

    static void ensureAccount(UUID player, String name) { access.ensureAccount(player, name); }
    static String balance(UUID player) { return access.balance(player); }

    interface Access {
        void ensureAccount(UUID player, String name);
        String balance(UUID player);
    }
}
