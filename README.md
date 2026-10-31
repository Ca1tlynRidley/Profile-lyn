# PROFILE-lyn

A profile menu mod for Minecraft Java 26.2 using Fabric. It displays player information, with optional ECO-API-lyn integration for balances.

## Requirements


- Fabric Loader 0.20.1 or newer
- Fabric API for Minecraft 26.2

## Compatibility

- ECO-API-lyn-1.0.0

Install PROFILE-lyn 1.0.0 on the dedicated server only. 

Without ECO, the profile menu still works and the {money} placeholder shows "Unavailable". The default balance and leaderboard buttons need an economy mod that supplies /balance and /baltop; missing commands show an unavailable message. Custom menu commands keep the player's normal permissions.

## Usage

- /profile 
- /profile editor 
- /profile reload 
- /profile import 

The layout file is config/profile-lyn/profile.json. You can also open editor/dist/index.html locally to edit and export a layout. The included hosted editor has access controlled by its owner.

Names and descriptions support Minecraft color codes and placeholders such as {player}, {money}, and {level}. Menu actions can display a message, run a player command, refresh the menu, or close it.

After editing your profile on the website, click Export and copy the layout code. Paste it in your server console.

## License

Original code is licensed under MIT. Minecraft item preview textures belong to Mojang/Microsoft and are not covered by that license.
