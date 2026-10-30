globalThis.DEFAULT_LAYOUT={
  "schemaVersion": 1,
  "title": "{player}'s Profile",
  "rows": 6,
  "filler": "minecraft:gray_stained_glass_pane",
  "items": [
    {"slot":13,"item":"minecraft:player_head","name":"&b{player}'s Profile","lore":["&7Your survival story, in one place.","&7Players online: &f{online}"],"action":{"type":"none"}},
    {"slot":19,"item":"minecraft:diamond_sword","name":"&cYour Stats","lore":["&7Health: &f{health}/20","&7Hunger: &f{food}/20"],"action":{"type":"none"}},
    {"slot":20,"item":"minecraft:map","name":"&eLocation","lore":["&7X: &f{x} &7Y: &f{y} &7Z: &f{z}"],"action":{"type":"none"}},
    {"slot":21,"item":"minecraft:book","name":"&6Welcome","lore":["&7Click for a friendly message."],"action":{"type":"message","value":"&aWelcome, {player}! Have fun on the server."}},
    {"slot":22,"item":"minecraft:emerald","name":"&aYour Wallet","lore":["&7Balance: &a{money}","","&8Powered by ECO-API-lyn","&eClick to check your balance"],"glint":true,"action":{"type":"command","value":"balance"}},
    {"slot":23,"item":"minecraft:experience_bottle","name":"&dExperience","lore":["&7Level: &d{level}"],"action":{"type":"none"}},
    {"slot":24,"item":"minecraft:clock","name":"&eRefresh Profile","lore":["&7Stats also update once a second.","&eClick to refresh now"],"action":{"type":"refresh"}},
    {"slot":25,"item":"minecraft:ender_chest","name":"&5Economy Leaders","lore":["&7See the richest players.","&eClick to view /baltop"],"action":{"type":"command","value":"baltop"}},
    {"slot":31,"item":"minecraft:crafting_table","name":"&6Your Adventure","lore":["&7Build something great, {player}!"],"action":{"type":"none"}},
    {"slot":45,"item":"minecraft:compass","name":"&bYour Coordinates","lore":["&7{x}, {y}, {z}"],"action":{"type":"none"}},
    {"slot":49,"item":"minecraft:barrier","name":"&cClose Profile","lore":["&7Click to return to the game."],"action":{"type":"close"}}
  ]
}
;