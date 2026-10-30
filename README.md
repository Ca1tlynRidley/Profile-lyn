# PROFILE-lyn

A profile menu mod for Minecraft Java 26.2 using Fabric. It displays player information.

## Requirements


- Fabric Loader 0.20.1 or newer
- Fabric API for Minecraft 26.2

## Compatibility

- ECO-API-lyn

Install PROFILE-lyn on the server. ECO-API-lyn must be installed on both the server and players' clients. Display Currency in profile.

## Usage

- /profile opens the menu.
- /profile editor opens the layout editor link.
- /profile reload reloads the configuration.
- /profile import imports a layout code from the editor.

The layout file is config/profile-lyn/profile.json. You can also open editor/dist/index.html locally to edit and export a layout. The included hosted editor has access controlled by its owner.

Names and descriptions support Minecraft color codes and placeholders such as {player}, {money}, and {level}. Menu actions can display a message, run a player command, refresh the menu, or close it.

## License

Original code is licensed under MIT. Minecraft item preview textures belong to Mojang/Microsoft and are not covered by that license.
