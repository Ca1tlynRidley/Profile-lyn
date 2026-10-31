# Minecraft gameplay smoke test

Use a backup/test world on a dedicated Fabric 26.2 server with Loader 0.20.1+, Fabric API 0.161.0+26.2, Java 25+ and PROFILE-lyn 1.0.0. ECO-API-lyn 1.0.0 is optional; install it for the payment checks. Accept Mojang's EULA yourself if setting up a test server. Use two test players for payments. PROFILE and ECO are server-only; clients do not install either mod.

First start with PROFILE and Fabric API only, without ECO or SHOP. Verify /profile opens, player/stats/location placeholders work, {money} shows "Unavailable", and the default balance/leaderboard buttons show a helpful message if their commands are absent. Test closing, refreshing, editing, importing and reloading without ECO. Then restart with ECO installed and run the checks below.

Before the gameplay checks, verify ordinary vanilla 26.2 clients can join the PROFILE/ECO server and use /profile and the economy commands. If ECO-SHOP-lyn is installed, both server and clients also need ECO-SHOP-lyn 1.0.0 and Fabric API. In that setup, a client with SHOP but without ECO must join successfully; clients missing SHOP or carrying a mismatched SHOP version must be refused. PROFILE and ECO do not require a client installation handshake.

1. Confirm both mods load and `/profile` opens the default six-row inventory.
2. Give Alice `$100` using `/eco set Alice 100`. Check `/balance`, `/bal`, `/baltop`, and the emerald's hover text.
3. Pay Bob `12.50`, then check Alice has `87.50` and Bob received exactly `12.50`. Reject zero, negative, fractional-cent, self-pay, offline-recipient, and unaffordable payments.
4. While the menu is open, run an operator balance change from the console. Verify the emerald updates without reopening.
5. Click the barrier. Verify the menu closes. Test message, refresh, and a permitted player command. As a non-operator, verify an OP-only menu command does not gain OP privileges.
6. Try shift-click, number keys, dragging, dropping, double-clicking, and clicking outside with an item on the cursor. Verify no display items can be taken and no real player items are lost. Try creative-mode mouse actions too.
7. After startup, verify config/profile-lyn/editor/index.html and config/profile-lyn/profile-editor.zip were created automatically. Run `/profile editor` as an operator. For a local server, open that index.html in a browser; for a hosted server, download the ZIP, extract it on your PC, and open editor/index.html. Disconnect the editor PC from the Internet and verify the page, icons, Clear, drag/drop and export still work. Place a barrier and emerald, edit lore, export, and paste the import command into the console without its leading slash. Verify the in-game layout matches and the backup exists. Change the layout, rerun /profile editor, download a fresh ZIP, and confirm its snapshot reflects the changed layout.
8. Import invalid JSON, unknown item IDs, bad slots, and invalid action types. Verify the existing layout remains usable. Non-operators must not be able to import, reload, edit, or run `/eco`.
9. Change inventory rows, import a Unicode title, and use the JSON download/reload route.
10. Stop/restart the server. Confirm balances and the installed layout persist. Back up both the world and config before further testing.

The automated tests exercise editor extraction, offline assets, layout snapshots and ZIP exports, optional economy class loading, live balance integration and layout/editor logic; they do not replace these multiplayer and inventory packet checks.
