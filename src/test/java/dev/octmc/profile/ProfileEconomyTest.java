package dev.octmc.profile;

import com.google.gson.JsonParser;
import dev.lyn.eco.api.EcoApi;
import dev.lyn.eco.api.EconomyService;
import dev.lyn.eco.api.Money;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.lang.reflect.Proxy;
import java.net.URLClassLoader;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class ProfileEconomyTest {
    @AfterEach void reset() {
        ProfileEconomy.initialize(false);
        EcoApi.shutdown();
    }

    @Test void loadsAndWorksWithNoEcoClassesOnTheClasspath() throws Exception {
        var profileClasses = ProfileEconomy.class.getProtectionDomain().getCodeSource().getLocation();
        try (var isolated = new URLClassLoader(new java.net.URL[]{profileClasses}, ClassLoader.getPlatformClassLoader())) {
            assertThrows(ClassNotFoundException.class, () -> isolated.loadClass("dev.lyn.eco.api.EcoApi"));
            var profile = isolated.loadClass("dev.octmc.profile.ProfileEconomy");
            var initialize = profile.getDeclaredMethod("initialize", boolean.class);
            var ensureAccount = profile.getDeclaredMethod("ensureAccount", UUID.class, String.class);
            var balance = profile.getDeclaredMethod("balance", UUID.class);
            initialize.setAccessible(true);
            ensureAccount.setAccessible(true);
            balance.setAccessible(true);
            initialize.invoke(null, false);
            UUID player = UUID.randomUUID();
            ensureAccount.invoke(null, player, "OfflinePlayer");
            assertEquals("Unavailable", balance.invoke(null, player));
        }
    }

    @Test void installedEcoProvidesItsAccountAndLiveFormattedBalance() {
        UUID player = UUID.randomUUID();
        var cents = new AtomicLong(12345);
        var registered = new AtomicReference<EconomyService.Account>();
        var service = (EconomyService) Proxy.newProxyInstance(EconomyService.class.getClassLoader(),
                new Class<?>[]{EconomyService.class}, (proxy, method, arguments) -> switch (method.getName()) {
                    case "ensureAccount" -> {
                        var account = new EconomyService.Account((UUID) arguments[0], (String) arguments[1], cents.get());
                        registered.set(account);
                        yield account;
                    }
                    case "balance" -> {
                        assertEquals(player, arguments[0]);
                        yield cents.get();
                    }
                    case "format" -> "Coins " + Money.format((long) arguments[0]);
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        EcoApi.initialize(service);
        ProfileEconomy.initialize(true);
        ProfileEconomy.ensureAccount(player, "OfflinePlayer");
        assertEquals(player, registered.get().id());
        assertEquals("OfflinePlayer", registered.get().name());
        assertEquals("Coins 123.45", ProfileEconomy.balance(player));
        cents.set(5050);
        assertEquals("Coins 50.50", ProfileEconomy.balance(player));
    }

    @Test void installedButNotReadyEcoIsUnavailable() {
        EcoApi.shutdown();
        ProfileEconomy.initialize(true);
        UUID player = UUID.randomUUID();
        assertDoesNotThrow(() -> ProfileEconomy.ensureAccount(player, "Player"));
        assertEquals("Unavailable", ProfileEconomy.balance(player));
    }

    @Test void ecoIsSuggestedRatherThanRequiredInFabricMetadata() throws Exception {
        try (var reader = new InputStreamReader(Objects.requireNonNull(getClass().getResourceAsStream("/fabric.mod.json")))) {
            var metadata = JsonParser.parseReader(reader).getAsJsonObject();
            assertFalse(metadata.getAsJsonObject("depends").has("eco_api_lyn"));
            assertEquals(">=1.0.0", metadata.getAsJsonObject("suggests").get("eco_api_lyn").getAsString());
            assertEquals("server", metadata.get("environment").getAsString());
            assertEquals("1.0.0", metadata.get("version").getAsString());
        }
    }
}
