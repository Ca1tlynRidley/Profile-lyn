package dev.octmc.profile;

import dev.lyn.eco.api.EcoApi;
import java.util.UUID;

/** All direct ECO references stay in this lazily loaded optional bridge. */
final class EcoIntegration implements ProfileEconomy.Access {
    @Override public void ensureAccount(UUID player, String name) {
        if (EcoApi.isReady()) EcoApi.get().ensureAccount(player, name);
    }

    @Override public String balance(UUID player) {
        if (!EcoApi.isReady()) return ProfileEconomy.UNAVAILABLE;
        var economy = EcoApi.get();
        return economy.format(economy.balance(player));
    }
}
