"""Ciudadela de las Nubes: estructura, botin por pisos, nuevos mobs (pieles), Llave y Sello Celestial.

Lo llama gen_assets.py: NAMES.update(gen_ciudadela.run(globals()))
"""
import os
import random

from PIL import Image, ImageDraw

HERE = os.path.dirname(__file__)
TEX = os.path.join(HERE, "..", "src", "main", "resources", "assets", "cielo", "textures")


def hexc(h, a=255):
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


def caja(img, u, v, w, h, d, color, rnd, var=8):
    caras = {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "right": (u, v + d, d, h), "front": (u + d, v + d, w, h),
             "left": (u + d + w, v + d, d, h), "back": (u + d + w + d, v + d, w, h)}
    for cara, (x0, y0, cw, ch) in caras.items():
        c = hexc(color[cara] if isinstance(color, dict) and cara in color else color if isinstance(color, str) else color["all"])
        for y in range(y0, y0 + ch):
            for x in range(x0, x0 + cw):
                dv = rnd.randint(-var, var)
                img.putpixel((x, y), tuple(max(0, min(255, c[i] + dv)) for i in range(3)) + (255,))
    return caras


def piel(nombre, piel_c, cabeza, torso, brazos, piernas, extra, seed):
    rnd = random.Random(seed)
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    cab = caja(img, 0, 0, 8, 8, 8, cabeza, rnd, 5)
    fx, fy = cab["front"][0], cab["front"][1]
    for x in range(1, 7):
        for y in range(3, 8):
            img.putpixel((fx + x, fy + y), hexc(piel_c))
    for (x, y, c) in ((2, 4, "1a1a1a"), (5, 4, "1a1a1a")):
        img.putpixel((fx + x, fy + y), hexc(c))
    caja(img, 16, 16, 8, 12, 4, torso, rnd)
    caja(img, 40, 16, 4, 12, 4, brazos, rnd)
    caja(img, 0, 16, 4, 12, 4, piernas, rnd)
    if extra:
        extra(img, rnd)
    img.save(os.path.join(TEX, "entity", nombre + ".png"))


def guardian_extra(img, rnd):
    # tabardo azul con estrella dorada, yelmo con visera
    for y in range(20, 32):
        for x in range(21, 27):
            img.putpixel((x, y), hexc("1e4fa3"))
    for (x, y) in ((23, 23), (24, 23), (22, 24), (25, 24), (23, 25), (24, 25)):
        img.putpixel((x, y), hexc("f5c400"))
    for x in range(9, 15):
        img.putpixel((x, 8 + 3), hexc("2a2f36"))   # visera
    for x in range(8, 16):
        img.putpixel((x, 8 + 2), hexc("b8c0cc"))


def brujo_extra(img, rnd):
    # capucha que cubre la frente y runas en la tunica
    for x in range(8, 16):
        for y in range(8, 11):
            img.putpixel((x, y), hexc("16615f"))
    for (x, y) in ((22, 22), (25, 22), (23, 26), (24, 28), (21, 29)):
        img.putpixel((x, y), hexc("7ef0e6"))
    for x in range(9, 15):
        img.putpixel((x, 8 + 7), hexc("d0d0d0"))   # barba


def texturas():
    os.makedirs(os.path.join(TEX, "entity"), exist_ok=True)
    piel("guardian_celeste", "d9a07a", {"all": "b8c0cc", "top": "d0d6de"}, "9aa3ad", "8a929c", "6e757c", guardian_extra, 31)
    piel("brujo_del_viento", "c48a62", {"all": "1c7c79", "top": "16615f"}, "1c7c79", "1a6f6c", "145b59", brujo_extra, 32)

    # llave celestial
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.ellipse([1, 1, 7, 7], outline=hexc("8fd3ff"), width=2)
    d.point((4, 4), fill=hexc("ffffff"))
    d.line([(6, 6), (13, 13)], fill=hexc("8fd3ff"), width=2)
    d.line([(10, 12), (12, 10)], fill=hexc("d9a520"), width=1)
    d.line([(12, 14), (14, 12)], fill=hexc("d9a520"), width=1)
    img.save(os.path.join(TEX, "item", "llave_celestial.png"))

    # sello celestial (animado: runas que brillan)
    frames = []
    for f in range(4):
        rnd = random.Random(40 + f)
        im = Image.new("RGBA", (16, 16), hexc("3a5fa8"))
        dd = ImageDraw.Draw(im)
        dd.rectangle([0, 0, 15, 15], outline=hexc("d9a520"))
        dd.rectangle([2, 2, 13, 13], outline=hexc("8fd3ff"))
        for _ in range(6):
            x, y = rnd.randint(4, 11), rnd.randint(4, 11)
            dd.point((x, y), fill=hexc("ffffff" if f % 2 == 0 else "bfe8ff"))
        dd.line([(7, 4), (7, 11)], fill=hexc("f5c400"))
        dd.line([(4, 7), (11, 7)], fill=hexc("f5c400"))
        frames.append(im)
    tira = Image.new("RGBA", (16, 64))
    for i, im in enumerate(frames):
        tira.paste(im, (0, i * 16))
    tira.save(os.path.join(TEX, "block", "sello_celestial.png"))


def run(g):
    texturas()
    write, D, A, NS = g["write"], g["D"], g["A"], g["NS"]
    item, cofre, entidad, pool, huevo = g["item"], g["cofre"], g["entidad"], g["pool"], None

    # estructura y aparicion (muy rara)
    write(os.path.join(D, "worldgen", "structure", "ciudadela.json"), {
        "type": f"{NS}:ciudadela", "biomes": "#minecraft:is_overworld", "step": "surface_structures", "spawn_overrides": {},
        "terrain_adaptation": "none"})
    write(os.path.join(D, "worldgen", "structure_set", "ciudadela.json"), {
        "placement": {"type": "minecraft:random_spread", "salt": 72003105, "spacing": 64, "separation": 24},
        "structures": [{"structure": f"{NS}:ciudadela", "weight": 1}]})
    tags = os.path.join(D, "tags", "worldgen", "structure")
    write(os.path.join(tags, "mazmorras.json"), {"values": [f"{NS}:{n}" for n in ("globo", "avioneta", "dirigible", "fortaleza", "ciudadela")]})
    write(os.path.join(tags, "con_jefe.json"), {"values": [f"{NS}:dirigible", f"{NS}:fortaleza", f"{NS}:ciudadela"]})
    write(os.path.join(tags, "ciudadelas.json"), {"values": [f"{NS}:ciudadela"]})

    # botin por tercio de la torre
    cofre("ciudadela_1", (4, 7), [
        item("minecraft:iron_ingot", 10, 2, 6), item("minecraft:gold_ingot", 6, 2, 5), item("minecraft:bread", 6, 3, 6),
        item("minecraft:arrow", 6, 6, 16), item("minecraft:golden_carrot", 5, 3, 8), item(f"{NS}:escalera_de_cuerda", 3, 4, 8),
        item("minecraft:emerald", 4, 2, 5), item("minecraft:iron_sword", 3, encantar=(10, 20)), item("minecraft:book", 3, encantar=(10, 20)),
        item("minecraft:diamond", 2, 1, 2)])
    cofre("ciudadela_2", (5, 8), [
        item("minecraft:diamond", 6, 1, 3), item("minecraft:gold_ingot", 6, 3, 8), item("minecraft:emerald", 6, 3, 8),
        item("minecraft:golden_apple", 4, 1, 2), item("minecraft:experience_bottle", 5, 4, 8), item("minecraft:ender_pearl", 3, 1, 3),
        item("minecraft:diamond_sword", 2, encantar=(20, 30)), item("minecraft:diamond_boots", 2, encantar=(20, 30)),
        item("minecraft:book", 4, encantar=(20, 30)), item(f"{NS}:bomba", 4, 2, 5)])
    cofre("ciudadela_3", (6, 9), [
        item("minecraft:diamond", 8, 2, 5), item("minecraft:emerald", 6, 4, 10), item("minecraft:netherite_scrap", 3, 1, 2),
        item("minecraft:golden_apple", 5, 1, 3), item("minecraft:totem_of_undying", 1),
        item("minecraft:diamond_chestplate", 2, encantar=(30, 39)), item("minecraft:diamond_pickaxe", 2, encantar=(30, 39)),
        item("minecraft:book", 5, encantar=(30, 39)), item("minecraft:experience_bottle", 5, 8, 14), item(f"{NS}:pluma_de_acero", 3, 1, 2)])
    cofre("ciudadela_tesoro", (7, 10), [
        item("minecraft:diamond_block", 6, 1, 3), item("minecraft:netherite_ingot", 4, 1, 2), item("minecraft:netherite_scrap", 4, 2, 4),
        item("minecraft:enchanted_golden_apple", 4, 1, 2), item("minecraft:totem_of_undying", 4), item("minecraft:elytra", 1),
        item("minecraft:diamond_helmet", 3, encantar=(35, 39)), item("minecraft:diamond_leggings", 3, encantar=(35, 39)),
        item("minecraft:netherite_upgrade_smithing_template", 3), item("minecraft:book", 5, encantar=(35, 39)),
        item("minecraft:experience_bottle", 5, 16, 32), item(f"{NS}:alas_del_condor", 1)])
    entidad("guardian_celeste", [pool([item("minecraft:iron_ingot", 1, 1, 3)]), pool([item("minecraft:shield")], chance=0.05),
                                 pool([item("minecraft:diamond")], chance=0.06)])
    entidad("brujo_del_viento", [pool([item("minecraft:breeze_rod", 1, 1, 2)]), pool([item("minecraft:wind_charge", 1, 2, 5)]),
                                 pool([item("minecraft:emerald", 1, 1, 2)], chance=0.3)])

    # items y bloque
    for it in ("llave_celestial", "huevo_guardian_celeste", "huevo_brujo_del_viento"):
        write(os.path.join(A, "models", "item", it + ".json"), {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/{it}"}})
        write(os.path.join(A, "items", it + ".json"), {"model": {"type": "minecraft:model", "model": f"{NS}:item/{it}"}})
    write(os.path.join(A, "textures", "block", "sello_celestial.png.mcmeta"), {"animation": {"frametime": 6}})
    write(os.path.join(A, "models", "block", "sello_celestial.json"),
          {"parent": "minecraft:block/cube_all", "textures": {"all": f"{NS}:block/sello_celestial"}})
    write(os.path.join(A, "blockstates", "sello_celestial.json"), {"variants": {"": {"model": f"{NS}:block/sello_celestial"}}})
    write(os.path.join(A, "items", "sello_celestial.json"), {"model": {"type": "minecraft:model", "model": f"{NS}:block/sello_celestial"}})
    return {
        "entity.cielo.guardian_celeste": "Guardián Celeste",
        "entity.cielo.brujo_del_viento": "Brujo del Viento",
        "item.cielo.huevo_guardian_celeste": "Huevo de Guardián Celeste",
        "item.cielo.huevo_brujo_del_viento": "Huevo de Brujo del Viento",
        "item.cielo.llave_celestial": "Llave Celestial",
        "block.cielo.sello_celestial": "Sello Celestial",
    }
