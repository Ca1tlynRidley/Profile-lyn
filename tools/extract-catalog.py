"""Build the editor catalog from the official local Minecraft client jar. No network dependencies."""
import json, pathlib, sys, zipfile

jar, output = pathlib.Path(sys.argv[1]), pathlib.Path(sys.argv[2])
output.mkdir(parents=True, exist_ok=True)
(output / 'icons').mkdir(exist_ok=True)
with zipfile.ZipFile(jar) as z:
    files = set(z.namelist())
    lang = json.loads(z.read('assets/minecraft/lang/en_us.json'))
    def read(path):
        return json.loads(z.read(path)) if path in files else {}
    def model_names(obj):
        if isinstance(obj, dict):
            if isinstance(obj.get('model'), str):
                yield obj['model']
            for value in obj.values():
                yield from model_names(value)
        elif isinstance(obj, list):
            for value in obj:
                yield from model_names(value)
    def texture(model, seen=None):
        seen = set() if seen is None else seen
        if model in seen: return None
        seen.add(model)
        data = read('assets/minecraft/models/' + model.removeprefix('minecraft:') + '.json')
        textures = data.get('textures', {})
        for key in ['layer0', 'all', 'front', 'side', 'top', 'texture', 'particle'] + list(textures):
            value = textures.get(key, '')
            if not isinstance(value, str): continue
            visited = set()
            while value.startswith('#') and value not in visited:
                visited.add(value)
                value = textures.get(value[1:], '')
                if not isinstance(value, str): value = ''
            if value and not value.startswith('#'):
                path = 'assets/minecraft/textures/' + value.removeprefix('minecraft:') + '.png'
                if path in files: return path
        if 'parent' in data: return texture(data['parent'], seen)
        return None
    catalog = []
    for path in sorted(files):
        if not path.startswith('assets/minecraft/items/') or not path.endswith('.json'): continue
        name = pathlib.PurePosixPath(path).stem
        if name == 'air': continue
        icon = None
        for model in model_names(read(path)):
            icon = texture(model)
            if icon: break
        if not icon:
            for candidate in [f'assets/minecraft/textures/item/{name}.png', f'assets/minecraft/textures/block/{name}.png']:
                if candidate in files: icon = candidate; break
        sprite = None
        if name == 'player_head':
            icon = 'assets/minecraft/textures/entity/player/wide/steve.png'
            sprite = 'head'
        if name in ('chest', 'ender_chest', 'trapped_chest'):
            icon = 'assets/minecraft/textures/entity/chest/' + ('ender' if name == 'ender_chest' else 'normal') + '.png'
            sprite = 'chest'
        if icon: (output / 'icons' / f'{name}.png').write_bytes(z.read(icon))
        catalog.append({'id': 'minecraft:' + name, 'name': lang.get('item.minecraft.' + name, lang.get('block.minecraft.' + name, name.replace('_',' ').title())), 'icon': 'icons/' + name + '.png' if icon else None, 'sprite': sprite})
    (output / 'catalog.js').write_text('globalThis.ITEM_CATALOG=' + json.dumps(catalog, ensure_ascii=False, separators=(',',':')) + ';\n', encoding='utf-8')
    print(f'Created catalog: {len(catalog)} vanilla items; {sum(bool(x["icon"]) for x in catalog)} texture previews.')
