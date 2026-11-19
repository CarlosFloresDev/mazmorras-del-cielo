"""Assets reproducibles de 0.4. Ejecutar solo o desde gen_assets.py; no requiere descargas."""
from pathlib import Path
import json
import random
from PIL import Image, ImageDraw
from gen_ciudadela import caja

ROOT = Path(__file__).resolve().parents[1] / 'src/main/resources'
A = ROOT / 'assets/cielo'
D = ROOT / 'data/cielo'
MOBS = {
    'jean_beck': ('JEAN BECK', 'JEAN BECK', 'd89e77', 'e9dbb9'),
    'senor_de_la_ceniza': ('Señor de la Ceniza', 'Lord of Ash', '322f39', '4d4246'),
    'heraldo_del_vacio': ('Heraldo del Vacío', 'Void Herald', '292238', '5d397c'),
    'ayudante_cocina': ('Ayudante de cocina', 'Kitchen Helper', 'ba8462', 'd9d4b8'),
    'bombardero_ceniza': ('Bombardero de ceniza', 'Ash Bomber', '52443c', '9b5730'),
    'fuego_fatuo': ('Fuego fatuo del vacío', 'Void Wisp', '392f61', '745ea3'),
}
ITEMS = {
    'cinturon_jean_beck': ('Cinturón de Jean Beck', "Jean Beck's Belt"),
    'hoja_de_ceniza': ('Hoja de Ceniza', 'Ash Blade'),
    'fragmento_del_vacio': ('Fragmento del Vacío', 'Void Fragment'),
    'orbe_del_vacio': ('Orbe del Vacío', 'Void Orb'),
    'racion_de_cocina': ('Ración de cocina', 'Kitchen Ration'),
    'brasa_inestable': ('Brasa inestable', 'Unstable Ember'),
}
DUNGEONS = ['fortin_jean_beck', 'bastion_ceniza', 'santuario_vacio']


def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')


def entry(name, lo=1, hi=1):
    result = {'type': 'minecraft:item', 'name': name}
    if hi != 1 or lo != 1:
        result['modifier'] = {'type': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': lo, 'max': hi}}
    return result


def loot(path, rewards, kind='entity'):
    write(D / 'loot_table' / path, {'type': 'minecraft:' + kind,
          'pools': [{'rolls': 1, 'entries': [entry(*r)]} for r in rewards]})


def texture(name, index, skin, cloth):
    rnd = random.Random(400 + index)
    img = Image.new('RGBA', (128, 128))
    # Caja y UV identicos a ExpedicionRenderer. Sombrear caras en vez de una piel plana.
    def box(u, v, w, h, d, color):
        rgb = tuple(int(color[i:i+2], 16) for i in (0, 2, 4))
        palette = {'all': color}
        for face, mult in [('top', 1.18), ('bottom', .62), ('right', .8), ('left', .9), ('back', .75)]:
            palette[face] = ''.join(f'{min(255,int(c*mult)):02x}' for c in rgb)
        return caja(img, u, v, w, h, d, palette, rnd, 5)
    box(0, 0, 8, 8, 8, skin)
    box(0, 32, 16 if index == 0 else 11 if index == 1 else 8, 13, 10 if index == 0 else 6, cloth)
    box(64, 32, 5 if index == 0 else 4, 8 if index == 0 else 11, 4, skin if index in (0, 3) else cloth)
    box(96, 32, 4, 7, 5, '443943')
    if index == 0:
        box(0, 64, 14, 11, 6, skin); box(48, 64, 10, 7, 2, skin)
        box(0, 90, 10, 12, 1, 'eee2c4'); box(40, 0, 2, 2, 2, skin)
        box(52, 0, 5, 1, 1, '49302a'); box(40, 8, 2, 3, 2, skin)
        box(72, 70, 2, 11, 2, '69442c'); box(80, 70, 6, 4, 3, '969aa0')
    elif index == 1:
        box(32, 0, 10, 2, 2, 'c67831'); box(56, 0, 2, 7, 2, 'ffbd55')
        box(32, 16, 7, 5, 7, '463941'); box(72, 80, 2, 17, 3, 'f89738')
        box(0, 90, 12, 5, 8, cloth); box(100, 0, 2, 4, 2, 'ffce73')
    elif index in (2, 5):
        box(32, 0, 10, 10, 8, cloth); box(0, 90, 12, 7, 8, cloth)
        box(48, 90, 6, 6, 6, cloth); box(100, 0, 2, 6, 2, 'c7a5ff')
    elif index == 3:
        box(32, 0, 10, 4, 10, 'f1eddd'); box(0, 90, 8, 12, 1, 'ddd9c5')
        box(72, 70, 8, 9, 2, '5d6670'); box(96, 70, 2, 7, 1, 'ced3d8')
    else:
        box(0, 64, 12, 14, 7, '7f4e29'); box(32, 0, 8, 2, 2, 'ffd486')
        box(100, 0, 2, 6, 2, 'eec881'); box(72, 70, 6, 6, 6, '39363a')
    draw = ImageDraw.Draw(img)
    eyes = '#faf0a0' if index == 1 else '#debeff' if index in (2, 5) else '#292322'
    draw.rectangle((10, 11, 11, 12), fill=eyes); draw.rectangle((13, 11, 14, 12), fill=eyes)
    draw.line((10, 14, 14, 14), fill='#6f423c' if index in (0, 3) else eyes)
    # Costuras, remaches y bordado sobre las caras frontales.
    for y in range(40, 50, 3):
        draw.point((11 if index == 0 else 7, y), fill='#f6d08b')
    if index in (0, 3):
        draw.rectangle((3, 96, 7, 99), outline='#987c5f')
    if index == 1:
        draw.line((7, 39, 10, 43, 8, 46, 13, 49), fill='#ffbc55', width=1)
    if index in (2, 5):
        draw.line((6, 104, 16, 104), fill='#d7aefa', width=1)
    path = A / 'textures/entity' / (name + '.png'); path.parent.mkdir(parents=True, exist_ok=True); img.save(path)


def icon(name, color):
    img = Image.new('RGBA', (32, 32)); d = ImageDraw.Draw(img)
    if name.startswith('huevo_'):
        d.ellipse((7, 3, 25, 29), fill=color, outline='#25212e', width=2)
        for x, y in [(11, 10), (20, 15), (13, 23)]: d.rectangle((x, y, x+3, y+3), fill='#e6cda4')
    elif name == 'hoja_de_ceniza':
        d.polygon([(7, 27), (4, 24), (20, 7), (28, 3), (25, 11)], fill='#ffb24d', outline='#47383c')
        d.line((8, 23, 26, 5), fill='#ffe6a2', width=2); d.line((5, 18, 14, 27), fill='#a6652d', width=3)
    elif name == 'cinturon_jean_beck':
        d.rounded_rectangle((2, 9, 29, 23), 4, fill='#69412d', outline='#2f2327', width=2)
        d.rectangle((11, 8, 22, 24), fill='#e8bb52', outline='#fff0a0', width=2)
        d.rectangle((14, 12, 19, 20), fill='#69412d'); d.line((3, 12, 9, 12), fill='#c79461')
    elif name == 'racion_de_cocina':
        d.ellipse((3, 9, 29, 26), fill='#d8cbad', outline='#4f4341', width=2)
        d.ellipse((8, 7, 24, 21), fill='#b96939', outline='#6f3926', width=2)
        d.line((11, 12, 21, 17), fill='#e9aa62', width=2)
    else:
        d.polygon([(16, 2), (27, 12), (24, 25), (13, 30), (4, 21), (6, 9)], fill=color, outline='#282038')
        d.polygon([(16, 4), (20, 15), (8, 10)], fill='#f1d3ff' if 'vacio' in name else '#ffe7a1')
        d.line((20, 15, 23, 24, 13, 28), fill='#8a5ec3' if 'vacio' in name else '#e17737', width=2)
    path = A / 'textures/item' / (name + '.png'); path.parent.mkdir(parents=True, exist_ok=True); img.save(path)


def run():
    write(ROOT / 'data/minecraft/tags/item/swords.json', {'values': ['cielo:hoja_de_ceniza']})
    for i, (name, (_, _, skin, cloth)) in enumerate(MOBS.items()): texture(name, i, skin, cloth)
    names = dict(ITEMS)
    for name, (es, en, skin, _) in MOBS.items(): names['huevo_' + name] = ('Huevo de ' + es, en + ' Spawn Egg')
    for name in names:
        color = '#ab7cdd' if 'vacio' in name or 'fatuo' in name else '#d68b46'
        icon(name, color)
        write(A / 'models/item' / (name + '.json'), {'parent': 'minecraft:item/handheld' if name == 'hoja_de_ceniza' else 'minecraft:item/generated', 'textures': {'layer0': 'cielo:item/' + name}})
        write(A / 'items' / (name + '.json'), {'model': {'type': 'minecraft:model', 'model': 'cielo:item/' + name}})
    for i, name in enumerate(DUNGEONS):
        biome = ['#minecraft:is_overworld', '#minecraft:is_nether', 'minecraft:end_highlands'][i]
        values = [biome] if i < 2 else ['minecraft:end_highlands', 'minecraft:end_midlands', 'minecraft:small_end_islands', 'minecraft:end_barrens']
        write(D / 'tags/worldgen/biome' / (name + '.json'), {'values': values})
        write(D / 'worldgen/structure' / (name + '.json'), {'type': 'cielo:mazmorra', 'tema': name, 'biomes': '#cielo:' + name,
              'step': 'surface_structures', 'spawn_overrides': {}, 'terrain_adaptation': 'none'})
        write(D / 'worldgen/structure_set' / (name + '.json'), {'structures': [{'structure': 'cielo:' + name, 'weight': 1}],
              'placement': {'type': 'minecraft:random_spread', 'spacing': [80, 72, 88][i], 'separation': [28, 24, 30][i], 'salt': 72003401+i}})
        for tier in range(1, 4):
            reward = ['minecraft:cooked_beef', 'minecraft:quartz', 'minecraft:ender_pearl'][i]
            rewards = [(reward, tier*2, tier*4), ('minecraft:iron_ingot' if tier == 1 else 'minecraft:diamond', tier, tier+2)]
            if tier == 3: rewards += [('minecraft:golden_apple', 1, 2), ('minecraft:experience_bottle', 6, 12)]
            loot('chests/' + name + '_' + str(tier) + '.json', rewards, 'chest')
    write(D / 'tags/worldgen/structure/solo_ciudadela.json', {'values': ['cielo:ciudadela']})
    write(D / 'tags/worldgen/structure/expediciones.json', {'values': ['cielo:' + n for n in DUNGEONS]})
    for tag in ['mazmorras', 'con_jefe', 'ciudadelas', 'protegidas']:
        path = D / 'tags/worldgen/structure' / (tag + '.json')
        data = json.loads(path.read_text(encoding='utf-8')) if path.exists() else {'values': ['cielo:ciudadela']}
        data['values'] = list(dict.fromkeys(data['values'] + ['cielo:' + n for n in DUNGEONS]))
        write(path, data)
    loot('entities/jean_beck.json', [('cielo:cinturon_jean_beck', 1, 1), ('cielo:racion_de_cocina', 8, 16), ('minecraft:golden_apple', 2, 4)])
    loot('entities/senor_de_la_ceniza.json', [('cielo:hoja_de_ceniza', 1, 1), ('minecraft:netherite_scrap', 2, 4)])
    loot('entities/heraldo_del_vacio.json', [('cielo:fragmento_del_vacio', 4, 8), ('cielo:orbe_del_vacio', 1, 1)])
    loot('entities/ayudante_cocina.json', [('cielo:racion_de_cocina', 1, 3)])
    loot('entities/bombardero_ceniza.json', [('cielo:brasa_inestable', 1, 2)])
    loot('entities/fuego_fatuo.json', [('minecraft:chorus_fruit', 1, 2)])
    write(D / 'recipe/orbe_del_vacio.json', {'type': 'minecraft:crafting_shaped', 'pattern': [' F ', 'FEF', ' F '],
          'key': {'F': 'cielo:fragmento_del_vacio', 'E': 'minecraft:ender_pearl'}, 'result': {'id': 'cielo:orbe_del_vacio'}})
    write(D / 'recipe/carga_de_brasa.json', {'type': 'minecraft:crafting_shapeless', 'ingredients': ['cielo:brasa_inestable', 'minecraft:gunpowder'], 'result': {'id': 'minecraft:fire_charge', 'count': 3}})
    for path in (A / 'lang').glob('*.json'):
        index = 1 if path.stem == 'en_us' else 0
        lang = json.loads(path.read_text(encoding='utf-8'))
        lang.update({'entity.cielo.' + n: v[index] for n, v in MOBS.items()})
        lang.update({'item.cielo.' + n: v[index] for n, v in names.items()})
        for n, es, en in [('eco_del_vacio', 'Eco del Vacío', 'Void Echo'), ('comida_arrojada', 'Comida arrojada', 'Thrown Food'), ('orbe_hostil', 'Orbe rastreador', 'Seeking Orb')]:
            lang['entity.cielo.' + n] = en if index else es
        lang['message.cielo.jefe_despierta'] = '%s awakens!' if index else '¡%s despierta!'
        for n, es, en in zip(DUNGEONS, ['Fortín de Jean Beck', 'Bastión de Ceniza', 'Santuario del Vacío'], ["Jean Beck's Fort", 'Ash Bastion', 'Void Sanctuary']):
            lang['structure.cielo.' + n] = en if index else es
        write(path, lang)


if __name__ == '__main__':
    run()
    print('Expansión 0.4: recursos y texturas generados')
