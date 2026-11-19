"""Texturas de Mazmorras del Cielo: pieles de los tripulantes (formato humanoide 64x64), capitan, condor, items y bloques.

Uso: python tools/gen_textures.py
"""
import os
import random

from PIL import Image, ImageDraw

HERE = os.path.dirname(__file__)
ROOT = os.path.join(HERE, "..", "src", "main", "resources", "assets", "cielo", "textures")
ENT = os.path.join(ROOT, "entity")
ITEM = os.path.join(ROOT, "item")
BLOCK = os.path.join(ROOT, "block")
EQ = os.path.join(ROOT, "entity", "equipment")
for d in (ENT, ITEM, BLOCK, os.path.join(EQ, "humanoid"), os.path.join(EQ, "wings")):
    os.makedirs(d, exist_ok=True)


def hexc(h, a=255):
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


def caja(img, u, v, w, h, d, colores, rnd, var=10):
    """Pinta las 6 caras de una caja en layout de Minecraft. colores: dict cara->hex (top,bottom,right,front,left,back)."""
    caras = {
        "top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d),
        "right": (u, v + d, d, h), "front": (u + d, v + d, w, h),
        "left": (u + d + w, v + d, d, h), "back": (u + d + w + d, v + d, w, h),
    }
    for cara, (x0, y0, cw, ch) in caras.items():
        col = hexc(colores.get(cara, colores.get("all")))
        for y in range(y0, y0 + ch):
            for x in range(x0, x0 + cw):
                dv = rnd.randint(-var, var)
                img.putpixel((x, y), tuple(max(0, min(255, col[i] + dv)) for i in range(3)) + (255,))
    return caras


def piel(nombre, piel_c, camisa, pantalon, botas, sombrero=None, extras=None, seed=1):
    rnd = random.Random(seed)
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    cabeza = caja(img, 0, 0, 8, 8, 8, {"all": piel_c}, rnd, 6)
    # cara
    fx, fy = cabeza["front"][0], cabeza["front"][1]
    for (x, y, c) in ((1, 4, "ffffff"), (2, 4, "2b1d14"), (5, 4, "2b1d14"), (6, 4, "ffffff"), (3, 6, "a0522d"), (4, 6, "a0522d")):
        img.putpixel((fx + x, fy + y), hexc(c))
    caja(img, 16, 16, 8, 12, 4, {"all": camisa}, rnd)          # torso
    caja(img, 40, 16, 4, 12, 4, {"all": camisa}, rnd)          # brazo
    caja(img, 0, 16, 4, 12, 4, {"all": pantalon}, rnd)         # pierna
    # manos y botas
    for x in range(40, 56):
        for y in range(29, 32):
            img.putpixel((x, y), hexc(piel_c))
    for x in range(0, 16):
        for y in range(28, 32):
            img.putpixel((x, y), hexc(botas))
    # cinturon
    for x in range(16, 40):
        img.putpixel((x, 26), hexc("3b2a1e"))
        img.putpixel((x, 27), hexc("3b2a1e"))
    img.putpixel((23, 26), hexc("d9a520"))
    img.putpixel((24, 26), hexc("d9a520"))
    if sombrero:  # capa del sombrero (hat layer) en 32,0
        caja(img, 32, 0, 8, 8, 8, {"top": sombrero, "front": sombrero, "left": sombrero, "right": sombrero, "back": sombrero,
                                    "bottom": sombrero}, rnd, 8)
        # dejar ver la cara: solo la franja superior del sombrero en las caras laterales
        for x in range(32, 64):
            for y in range(8 + 3, 16):
                img.putpixel((x, y), (0, 0, 0, 0))
    if extras:
        extras(img, rnd)
    img.save(os.path.join(ENT, nombre + ".png"))
    return img


def parche(img, rnd):  # parche en el ojo y barba de tres dias
    img.putpixel((8 + 1, 8 + 4), hexc("1a1a1a"))
    img.putpixel((8 + 2, 8 + 4), hexc("1a1a1a"))
    img.putpixel((8 + 1, 8 + 3), hexc("1a1a1a"))
    for x in range(8, 16):
        img.putpixel((x, 8 + 2), hexc("1a1a1a") if x in (8, 9, 10) else img.getpixel((x, 10)))
    for x in range(9, 15):
        img.putpixel((x, 15), hexc("5c4a3c"))


piel("pirata_del_cielo", "c68642", "b03a2e", "3a3a4a", "2b1d14", sombrero="2a2a33", extras=parche, seed=2)


def gafas(img, rnd):
    for x in range(9, 15):
        img.putpixel((x, 12), hexc("8a6a3a"))
    img.putpixel((10, 12), hexc("7ec8e3"))
    img.putpixel((13, 12), hexc("7ec8e3"))


piel("arcabucero_del_cielo", "e0ac69", "2e5a3a", "4a3a2a", "2b1d14", sombrero="4a3a2a", extras=gafas, seed=3)


def mecanico_extra(img, rnd):
    gafas(img, rnd)
    for y in range(20, 32):  # tirantes del overol
        img.putpixel((21, y), hexc("d9a520"))
        img.putpixel((26, y), hexc("d9a520"))
    for x in range(24, 28):  # llave en el bolsillo
        img.putpixel((x, 23), hexc("b8bec6"))


piel("mecanico_del_dirigible", "a86f3a", "6e757c", "3a5a8a", "1c1c22", sombrero=None, extras=mecanico_extra, seed=4)


def capitan_extra(img, rnd):
    parche(img, rnd)
    # botones dorados del abrigo
    for y in (21, 23, 25):
        img.putpixel((23, y), hexc("d9a520"))
        img.putpixel((24, y), hexc("d9a520"))
    # charreteras
    for x in range(44, 48):
        img.putpixel((x, 20), hexc("d9a520"))
        img.putpixel((x, 16 + 2), hexc("d9a520"))
    # tricornio (ala 12x1x12 en 0,32 / copa 9x3x9 en 0,45 / punta 4x2x2 en 36,45)
    caja(img, 0, 32, 12, 1, 12, {"all": "1a1a22", "top": "22222c"}, rnd, 6)
    for x in range(12, 24):
        img.putpixel((x, 32), hexc("d9a520"))  # borde dorado
    caja(img, 0, 45, 9, 3, 9, {"all": "1a1a22"}, rnd, 6)
    caja(img, 48, 32, 4, 2, 2, {"all": "d9a520"}, rnd, 6)
    # calavera en la copa
    img.putpixel((9 + 4, 45 + 9 + 1), hexc("ffffff"))
    img.putpixel((9 + 3, 45 + 9 + 1), hexc("ffffff"))
    # barba gris (8x5x1 en 48,32)
    caja(img, 36, 45, 8, 5, 1, {"all": "9a9a9a"}, rnd, 14)
    # faldones (8x6x1 en 36,50)
    caja(img, 36, 52, 8, 6, 1, {"all": "1f2a5a"}, rnd, 8)


piel("capitan_barbanube", "c68642", "1f2a5a", "2a2a33", "1a1a1a", sombrero=None, extras=capitan_extra, seed=5)

# ---------------------------------------------------------------- condor (128x64)
rnd = random.Random(7)
img = Image.new("RGBA", (128, 64), (0, 0, 0, 0))
NEGRO, ACERO, BLANCO = "1e1e24", "8fa3b5", "f2f2ee"
caja(img, 0, 0, 10, 8, 18, {"all": NEGRO, "top": "2a2a33", "bottom": "3a3a44"}, rnd, 8)          # cuerpo
for x in range(18, 28):  # placas de acero en el lomo
    for y in range(0, 18):
        if (y // 3) % 2 == 0 and (x % 4) != 0:
            img.putpixel((x, y), hexc(ACERO))
caja(img, 0, 26, 4, 4, 5, {"all": "c47a6a"}, rnd, 6)        # cuello (piel rojiza del condor)
caja(img, 56, 0, 7, 6, 2, {"all": BLANCO}, rnd, 6)          # gorguera blanca
caja(img, 18, 26, 6, 5, 5, {"all": "b06a5a", "top": "8a4a3a"}, rnd, 6)  # cabeza
caras_cabeza = (18 + 5, 26 + 5)
img.putpixel((caras_cabeza[0] + 1, caras_cabeza[1] + 1), hexc("ff2020"))  # ojos rojos (mecanicos)
img.putpixel((caras_cabeza[0] + 4, caras_cabeza[1] + 1), hexc("ff2020"))
caja(img, 40, 26, 2, 2, 4, {"all": "d9c9a0"}, rnd, 6)      # pico
caja(img, 52, 26, 2, 2, 1, {"all": "d9c9a0"}, rnd, 6)      # gancho del pico
caja(img, 0, 40, 14, 2, 12, {"all": NEGRO, "top": "26262e"}, rnd, 8)   # ala interior
for x in range(12, 26):
    for y in range(40, 52):
        if x % 3 == 0:
            img.putpixel((x, y), hexc(BLANCO))  # franja blanca de las alas del condor
caja(img, 52, 40, 18, 1, 12, {"all": NEGRO}, rnd, 8)       # punta del ala
for x in range(64, 82):
    for y in range(40, 52):
        if (x + y) % 5 == 0:
            img.putpixel((x, y), hexc(ACERO))  # plumas de acero
caja(img, 72, 0, 10, 2, 9, {"all": NEGRO}, rnd, 8)          # cola
caja(img, 90, 26, 2, 6, 2, {"all": "6e757c"}, rnd, 6)       # garras
img.save(os.path.join(ENT, "condor_de_acero.png"))

# ---------------------------------------------------------------- equipamiento
masks = "D:/DevTools/mc-sources-26.3/data/armor"
hum = Image.open(os.path.join(masks, "hum", "iron.png")).convert("RGBA")
out = Image.new("RGBA", (64, 32))
for y in range(16):
    for x in range(32):
        if hum.getpixel((x, y))[3]:
            out.putpixel((x, y), hexc("d9a520") if y in (8, 9) and x < 32 else hexc("1a1a22"))
out.save(os.path.join(EQ, "humanoid", "sombrero_del_capitan.png"))

# alas del condor en el layout del elitro (64x32; ala en u=22..46, v=0..22)
import zipfile  # noqa: E402

with zipfile.ZipFile("D:/gradle-home/caches/fabric-loom/26.3/minecraft-client.jar") as z:
    with z.open("assets/minecraft/textures/entity/equipment/wings/elytra.png") as fh:
        elytra = Image.open(fh).convert("RGBA")
out = Image.new("RGBA", elytra.size)
for y in range(elytra.size[1]):
    for x in range(elytra.size[0]):
        if elytra.getpixel((x, y))[3]:
            c = hexc(NEGRO)
            if (x - y) % 6 == 0:
                c = hexc(ACERO)
            if y > 16:
                c = hexc(BLANCO) if x % 2 == 0 else hexc("d0d0cc")
            out.putpixel((x, y), c)
out.save(os.path.join(EQ, "wings", "alas_del_condor.png"))


# ---------------------------------------------------------------- items
def sprite(name, rows, pal):
    p = {".": (0, 0, 0, 0), "k": hexc("1c1c22")}
    p.update({k: hexc(v) for k, v in pal.items()})
    im = Image.new("RGBA", (16, 16))
    for y, r in enumerate(rows):
        assert len(r) == 16, (name, y)
        for x, ch in enumerate(r):
            im.putpixel((x, y), p[ch])
    im.save(os.path.join(ITEM, name + ".png"))


sprite("brujula_del_cielo", [
    "................",
    ".....kkkkkk.....",
    "...kkGGGGGGkk...",
    "..kGGCCCCCCGGk..",
    "..kGCCCCWCCCGk..",
    ".kGCCCCCWWCCCGk.",
    ".kGCCCCRWCCCCGk.",
    ".kGCCCRRkCCCCGk.",
    ".kGCCCCkCCCCCGk.",
    ".kGCCCCCCCCCCGk.",
    "..kGCCCCCCCCGk..",
    "..kGGCCCCCCGGk..",
    "...kkGGGGGGkk...",
    ".....kkkkkk.....",
    "................",
    "................",
], {"G": "d9a520", "C": "7ec8e3", "W": "ffffff", "R": "c0392b"})
sprite("bomba", [
    "................",
    "..........kY....",
    ".........kOk....",
    "........kk......",
    ".....kkkkk......",
    "....kBBBBBk.....",
    "...kBWBBBBBk....",
    "..kBWBBBBBBBk...",
    "..kBBBBBBBBBk...",
    "..kBBBBBBBBBk...",
    "..kBBBBBBBBBk...",
    "...kBBBBBBBk....",
    "....kBBBBBk.....",
    ".....kkkkk......",
    "................",
    "................",
], {"B": "2a2a33", "W": "6e6e7a", "Y": "f5c400", "O": "e8743b"})
sprite("pluma_de_acero", [
    "................",
    "............kk..",
    "...........kWAk.",
    "..........kWAAk.",
    ".........kWAADk.",
    "........kWAADk..",
    ".......kWAADk...",
    "......kWAADk....",
    ".....kWAADk.....",
    "....kWAADk......",
    "...kAADk........",
    "..kADk..........",
    "..kk............",
    ".k..............",
    "k...............",
    "................",
], {"W": "d5e2ec", "A": "8fa3b5", "D": "5b6d7d"})
sprite("sombrero_del_capitan", [
    "................",
    "................",
    "................",
    "......kkkk......",
    ".....kBBBBk.....",
    "....kBBWBBBk....",
    "....kBBBBBBk....",
    "..kkkkkkkkkkkk..",
    ".kGGGGGGGGGGGGk.",
    "kBBBBBBBBBBBBBBk",
    ".kkkkkkkkkkkkkk.",
    "................",
    "................",
    "................",
    "................",
    "................",
], {"B": "1a1a22", "G": "d9a520", "W": "ffffff"})
sprite("alas_del_condor", [
    "................",
    "..kk........kk..",
    ".kBBk......kBBk.",
    ".kBBBk....kBBBk.",
    ".kBABk....kBABk.",
    ".kBBBBk..kBBBBk.",
    ".kBABBk..kBBABk.",
    ".kBBBBBkkBBBBBk.",
    "..kBABBBBBBABk..",
    "..kBBBBBBBBBBk..",
    "...kWWBBBBWWk...",
    "...kWWWkkWWWk...",
    "....kkk..kkk....",
    "................",
    "................",
    "................",
], {"B": "1e1e24", "A": "8fa3b5", "W": "f2f2ee"})


def huevo(nombre, base, puntos):
    sprite(nombre, [
        "................",
        "......kkkk......",
        ".....kBBBBk.....",
        "....kBPBBBBk....",
        "....kBBBBPBk....",
        "...kBBBBBBBBk...",
        "...kBPBBBBBBk...",
        "...kBBBBBPBBk...",
        "...kBBBBBBBBk...",
        "...kBBPBBBBBk...",
        "...kBBBBBBPBk...",
        "....kBBBBBBk....",
        ".....kkkkkk.....",
        "................",
        "................",
        "................",
    ], {"B": base, "P": puntos})


huevo("huevo_pirata_del_cielo", "b03a2e", "2a2a33")
huevo("huevo_arcabucero_del_cielo", "2e5a3a", "c68642")
huevo("huevo_mecanico_del_dirigible", "6e757c", "f5c400")
huevo("huevo_capitan_barbanube", "1f2a5a", "d9a520")
huevo("huevo_condor_de_acero", "1e1e24", "f2f2ee")
huevo("huevo_guardian_celeste", "b8c0cc", "1e4fa3")
huevo("huevo_brujo_del_viento", "1c7c79", "7ef0e6")

# ---------------------------------------------------------------- bloques
img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
for y in range(16):
    for x in (7, 8):
        img.putpixel((x, y), hexc("b5813f") if (y + x) % 3 else hexc("8a5a2b"))
    if y % 4 == 0:
        for x in range(5, 11):
            img.putpixel((x, y), hexc("6b4423"))
img.save(os.path.join(BLOCK, "escalera_de_cuerda.png"))
for nombre, base, brillo in (("nucleo_activo", "4b1458", "c77dff"), ("nucleo_apagado", "2a2a33", "555566")):
    img = Image.new("RGBA", (16, 16), hexc(base))
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, 15, 15), outline=hexc("1c1c22"))
    d.ellipse((4, 4, 11, 11), fill=hexc(brillo))
    d.ellipse((6, 6, 9, 9), fill=hexc("ffffff") if nombre == "nucleo_activo" else hexc("777788"))
    img.save(os.path.join(BLOCK, nombre + ".png"))

icon = Image.new("RGBA", (64, 64), hexc("7ec8e3"))
d = ImageDraw.Draw(icon)
d.ellipse((14, 4, 50, 40), fill=hexc("c0392b"))
d.rectangle((26, 4, 38, 40), fill=hexc("f2f2ee"))
d.rectangle((24, 46, 40, 56), fill=hexc("b5813f"))
d.line((20, 34, 26, 46), fill=hexc("2b1d14"))
d.line((44, 34, 38, 46), fill=hexc("2b1d14"))
icon.save(os.path.join(ROOT, "..", "icon.png"))
print("Texturas del cielo generadas")
