"""JSON de Mazmorras del Cielo: estructuras, sets, tags, botin, modelos, equipo, recetas y traducciones.

Uso: python tools/gen_assets.py
"""
import json
import os
import shutil

NS = "cielo"
RES = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources")
A = os.path.join(RES, "assets", NS)
D = os.path.join(RES, "data", NS)
for sub in ("blockstates", "models", "items", "lang", "equipment"):
    shutil.rmtree(os.path.join(A, sub), ignore_errors=True)
for sub in ("loot_table", "recipe", "worldgen", "tags"):
    shutil.rmtree(os.path.join(D, sub), ignore_errors=True)


def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)


# ---------------------------------------------------------------- estructuras
NAVES = {  # nombre: (spacing, separation, salt)
    "globo": (22, 8, 72003101),
    "avioneta": (34, 12, 72003102),
    "dirigible": (48, 18, 72003103),
    "fortaleza": (70, 25, 72003104),
}
for nave, (spacing, sep, salt) in NAVES.items():
    write(os.path.join(D, "worldgen", "structure", nave + ".json"), {
        "type": f"{NS}:nave_del_cielo", "tipo": nave, "biomes": "#minecraft:is_overworld",
        "step": "surface_structures", "spawn_overrides": {}, "terrain_adaptation": "none"})
    write(os.path.join(D, "worldgen", "structure_set", nave + ".json"), {
        "placement": {"type": "minecraft:random_spread", "salt": salt, "spacing": spacing, "separation": sep},
        "structures": [{"structure": f"{NS}:{nave}", "weight": 1}]})
write(os.path.join(D, "tags", "worldgen", "structure", "mazmorras.json"), {"values": [f"{NS}:{n}" for n in NAVES]})
write(os.path.join(D, "tags", "worldgen", "structure", "con_jefe.json"), {"values": [f"{NS}:dirigible", f"{NS}:fortaleza"]})


# ---------------------------------------------------------------- botin
def item(name, weight=1, lo=1, hi=1, encantar=None):
    e = {"type": "minecraft:item", "name": name, "weight": weight}
    mods = []
    if hi > 1:
        mods.append({"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}})
    if encantar:
        mods.append({"type": "minecraft:enchant_with_levels", "levels": {"type": "minecraft:uniform", "min": encantar[0],
                                                                         "max": encantar[1]}, "options": "#minecraft:on_random_loot"})
    if len(mods) == 1:
        e["modifier"] = mods[0]
    elif mods:
        e["modifier"] = mods
    return e


def cofre(nombre, rolls, entradas, extra=None):
    pools = [{"rolls": {"type": "minecraft:uniform", "min": rolls[0], "max": rolls[1]}, "entries": entradas}]
    if extra:
        pools.append(extra)
    write(os.path.join(D, "loot_table", "chests", nombre + ".json"), {"type": "minecraft:chest", "pools": pools})


cofre("globo", (4, 7), [
    item("minecraft:bread", 10, 2, 5), item("minecraft:iron_ingot", 8, 1, 4), item("minecraft:gold_nugget", 8, 3, 9),
    item("minecraft:arrow", 6, 4, 12), item(f"{NS}:bomba", 6, 1, 3), item(f"{NS}:escalera_de_cuerda", 5, 4, 12),
    item("minecraft:emerald", 3, 1, 3), item("minecraft:saddle", 2), item("minecraft:name_tag", 2),
    item("minecraft:book", 3, encantar=(5, 15)), item("minecraft:golden_apple", 1)])
cofre("avioneta", (5, 8), [
    item("minecraft:iron_ingot", 10, 2, 6), item("minecraft:gold_ingot", 8, 2, 5), item("minecraft:emerald", 6, 2, 5),
    item("minecraft:diamond", 3, 1, 2), item(f"{NS}:bomba", 6, 2, 5), item("minecraft:golden_apple", 3),
    item("minecraft:iron_sword", 3, encantar=(10, 25)), item("minecraft:iron_chestplate", 2, encantar=(10, 25)),
    item("minecraft:book", 4, encantar=(15, 25)), item("minecraft:experience_bottle", 4, 2, 6), item("minecraft:firework_rocket", 4, 3, 8)])
cofre("dirigible", (6, 9), [
    item("minecraft:diamond", 8, 1, 4), item("minecraft:emerald", 8, 3, 8), item("minecraft:gold_ingot", 8, 3, 8),
    item("minecraft:diamond_sword", 3, encantar=(20, 35)), item("minecraft:diamond_chestplate", 2, encantar=(20, 35)),
    item("minecraft:diamond_pickaxe", 2, encantar=(20, 35)), item("minecraft:book", 5, encantar=(25, 35)),
    item("minecraft:golden_apple", 4, 1, 2), item("minecraft:netherite_scrap", 2), item("minecraft:totem_of_undying", 1),
    item("minecraft:experience_bottle", 5, 4, 10), item(f"{NS}:brujula_del_cielo", 2)])
cofre("fortaleza", (6, 10), [
    item("minecraft:diamond", 10, 3, 8), item("minecraft:emerald", 6, 5, 12), item("minecraft:netherite_scrap", 4, 1, 2),
    item("minecraft:netherite_ingot", 1), item("minecraft:enchanted_golden_apple", 2), item("minecraft:totem_of_undying", 2),
    item("minecraft:diamond_helmet", 3, encantar=(30, 39)), item("minecraft:diamond_leggings", 3, encantar=(30, 39)),
    item("minecraft:book", 6, encantar=(30, 39)), item("minecraft:experience_bottle", 6, 8, 16), item(f"{NS}:pluma_de_acero", 4, 1, 3)])


def entidad(nombre, pools):
    write(os.path.join(D, "loot_table", "entities", nombre + ".json"), {"type": "minecraft:entity", "pools": pools})


def pool(entries, rolls=1, chance=None):
    p = {"rolls": rolls, "entries": entries}
    if chance is not None:
        p["condition"] = {"type": "minecraft:random_chance", "chance": chance}
    return p


entidad("pirata_del_cielo", [pool([item("minecraft:gold_nugget", 1, 1, 4)]), pool([item(f"{NS}:bomba")], chance=0.08)])
entidad("arcabucero_del_cielo", [pool([item("minecraft:arrow", 1, 2, 6)]), pool([item("minecraft:leather", 1, 1, 2)], chance=0.5)])
entidad("mecanico_del_dirigible", [pool([item(f"{NS}:bomba", 1, 1, 3)]), pool([item("minecraft:iron_nugget", 1, 2, 6)])])
entidad("capitan_barbanube", [
    pool([item(f"{NS}:sombrero_del_capitan")]),
    pool([item("minecraft:diamond", 1, 4, 8)]), pool([item("minecraft:emerald", 1, 6, 12)]),
    pool([item("minecraft:enchanted_golden_apple")]), pool([item(f"{NS}:brujula_del_cielo")])])
entidad("condor_de_acero", [
    pool([item(f"{NS}:alas_del_condor")]), pool([item(f"{NS}:pluma_de_acero", 1, 4, 8)]),
    pool([item("minecraft:netherite_ingot", 1, 1, 2)]), pool([item("minecraft:diamond", 1, 6, 12)]),
    pool([item("minecraft:totem_of_undying")])])

# ---------------------------------------------------------------- modelos e items
ITEMS = ["brujula_del_cielo", "bomba", "pluma_de_acero", "sombrero_del_capitan", "alas_del_condor",
         "huevo_pirata_del_cielo", "huevo_arcabucero_del_cielo", "huevo_mecanico_del_dirigible", "huevo_capitan_barbanube",
         "huevo_condor_de_acero"]
for it in ITEMS:
    write(os.path.join(A, "models", "item", it + ".json"), {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/{it}"}})
    write(os.path.join(A, "items", it + ".json"), {"model": {"type": "minecraft:model", "model": f"{NS}:item/{it}"}})

write(os.path.join(A, "models", "block", "escalera_de_cuerda.json"),
      {"parent": "minecraft:block/cross", "textures": {"cross": f"{NS}:block/escalera_de_cuerda"}})
write(os.path.join(A, "blockstates", "escalera_de_cuerda.json"), {"variants": {"": {"model": f"{NS}:block/escalera_de_cuerda"}}})
write(os.path.join(A, "items", "escalera_de_cuerda.json"), {"model": {"type": "minecraft:model", "model": f"{NS}:item/escalera_de_cuerda"}})
write(os.path.join(A, "models", "item", "escalera_de_cuerda.json"),
      {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:block/escalera_de_cuerda"}})
for estado in ("activo", "apagado"):
    write(os.path.join(A, "models", "block", f"nucleo_{estado}.json"),
          {"parent": "minecraft:block/cube_all", "textures": {"all": f"{NS}:block/nucleo_{estado}"}})
write(os.path.join(A, "blockstates", "nucleo_del_jefe.json"), {"variants": {
    "activo=true": {"model": f"{NS}:block/nucleo_activo"}, "activo=false": {"model": f"{NS}:block/nucleo_apagado"}}})
write(os.path.join(A, "items", "nucleo_del_jefe.json"), {"model": {"type": "minecraft:model", "model": f"{NS}:block/nucleo_activo"}})

write(os.path.join(A, "equipment", "alas_del_condor.json"), {"layers": {"wings": [{"texture": f"{NS}:alas_del_condor"}]}})
write(os.path.join(A, "equipment", "sombrero_del_capitan.json"), {"layers": {"humanoid": [{"texture": f"{NS}:sombrero_del_capitan"}]}})

# ---------------------------------------------------------------- recetas y tags
write(os.path.join(D, "recipe", "brujula_del_cielo.json"), {
    "type": "minecraft:crafting_shaped", "pattern": [" F ", "GCG", " G "],
    "key": {"F": "minecraft:feather", "G": "minecraft:gold_ingot", "C": "minecraft:compass"}, "result": {"id": f"{NS}:brujula_del_cielo"}})
write(os.path.join(D, "recipe", "escalera_de_cuerda.json"), {
    "type": "minecraft:crafting_shaped", "pattern": ["S", "T", "S"],
    "key": {"S": "minecraft:string", "T": "minecraft:stick"}, "result": {"id": f"{NS}:escalera_de_cuerda", "count": 6}})
write(os.path.join(D, "recipe", "bomba.json"), {
    "type": "minecraft:crafting_shapeless", "ingredients": ["minecraft:gunpowder", "minecraft:iron_nugget", "minecraft:string"],
    "result": {"id": f"{NS}:bomba", "count": 2}})
TAGS = os.path.join(RES, "data", "minecraft", "tags")
write(os.path.join(TAGS, "block", "climbable.json"), {"values": [f"{NS}:escalera_de_cuerda"]})
write(os.path.join(TAGS, "item", "head_armor.json"), {"values": [f"{NS}:sombrero_del_capitan"]})

NAMES = {
    "itemGroup.cielo.cielo": "Mazmorras del Cielo",
    "entity.cielo.pirata_del_cielo": "Pirata del Cielo",
    "entity.cielo.arcabucero_del_cielo": "Arcabucero del Cielo",
    "entity.cielo.mecanico_del_dirigible": "Mecánico del Dirigible",
    "entity.cielo.capitan_barbanube": "Capitán Barbanube",
    "entity.cielo.condor_de_acero": "Cóndor de Acero",
    "entity.cielo.bomba": "Bomba",
    "item.cielo.brujula_del_cielo": "Brújula del Cielo",
    "item.cielo.bomba": "Bomba",
    "item.cielo.pluma_de_acero": "Pluma de Acero",
    "item.cielo.sombrero_del_capitan": "Sombrero del Capitán Barbanube",
    "item.cielo.alas_del_condor": "Alas del Cóndor",
    "item.cielo.huevo_pirata_del_cielo": "Huevo de Pirata del Cielo",
    "item.cielo.huevo_arcabucero_del_cielo": "Huevo de Arcabucero del Cielo",
    "item.cielo.huevo_mecanico_del_dirigible": "Huevo de Mecánico del Dirigible",
    "item.cielo.huevo_capitan_barbanube": "Huevo del Capitán Barbanube",
    "item.cielo.huevo_condor_de_acero": "Huevo del Cóndor de Acero",
    "block.cielo.escalera_de_cuerda": "Escalera de Cuerda",
    "block.cielo.nucleo_del_jefe": "Núcleo del Jefe",
}
import sys as _sys
_sys.path.insert(0, os.path.dirname(__file__))
import gen_ciudadela  # noqa: E402

NAMES.update(gen_ciudadela.run(globals()))
for lang in ("en_us", "es_es", "es_mx", "es_ar", "es_cl", "es_ec", "es_uy", "es_ve"):
    write(os.path.join(A, "lang", lang + ".json"), NAMES)
print("Assets del cielo generados")

# La expansion se aplica al final para conservar tags y traducciones al regenerar.
import gen_expansion
gen_expansion.run()
