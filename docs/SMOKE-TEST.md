# Minecraft gameplay smoke test

Use a backup/test world with Fabric 26.2, Loader 0.20.1+, Fabric API 0.161.0+26.2, Java 25+, PROFILE-lyn 1.0.0 and ECO-API-lyn 1.1.0. Accept Mojang's EULA yourself if setting up a new test server. Use Fabric 26.2 clients with Fabric API and matching ECO-API-lyn 1.1.0; use two test players for payments. A server with SHOP-MOD-lyn 1.1.0 also requires its matching client JAR.

Before the gameplay checks, verify plain vanilla clients and Fabric clients missing ECO-API-lyn are refused with installation instructions. On a shop server, also try ECO without SHOP. Check that an older 1.0.0 JAR is refused, both matching 1.1.0 mods are admitted, and a client that advertises support but never replies is disconnected after the handshake timeout. Repeat after reconnecting and test an integrated singleplayer/LAN server too.

1. Confirm both mods load and `/profile` opens the default six-row inventory.
2. Give Alice `$100` using `/eco set Alice 100`. Check `/balance`, `/bal`, `/baltop`, and the emerald's hover text.
3. Pay Bob `12.50`, then check Alice has `87.50` and Bob received exactly `12.50`. Reject zero, negative, fractional-cent, self-pay, offline-recipient, and unaffordable payments.
4. While the menu is open, run an operator balance change from the console. Verify the emerald updates without reopening.
5. Click the barrier. Verify the menu closes. Test message, refresh, and a permitted player command. As a non-operator, verify an OP-only menu command does not gain OP privileges.
6. Try shift-click, number keys, dragging, dropping, double-clicking, and clicking outside with an item on the cursor. Verify no display items can be taken and no real player items are lost. Try creative-mode mouse actions too.
7. Open `/profile editor` as an operator, clear the menu, place a barrier and emerald, edit lore, and export. Paste the command into the console. Verify the in-game layout matches and the backup exists.
8. Import invalid JSON, unknown item IDs, bad slots, and invalid action types. Verify the existing layout remains usable. Non-operators must not be able to import, reload, edit, or run `/eco`.
9. Change inventory rows, import a Unicode title, and use the JSON download/reload route.
10. Stop/restart the server. Confirm balances and the installed layout persist. Back up both the world and config before further testing.

The automated tests exercise the economy ledger and layout/editor logic; they do not replace these multiplayer and inventory packet checks.
