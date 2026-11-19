# Mazmorras del Cielo

Mod de Fabric para **Minecraft 26.3**: mazmorras que **flotan en el cielo** (globos, avionetas, dirigibles y una
fortaleza flotante y la inmensa **Ciudadela de las Nubes**), así que no rompen la generación normal del mundo ni otros mods de terreno.

## Instalar
Fabric Loader 0.19.5+ y Fabric API para 26.3, y `build/libs/mazmorras-del-cielo-0.2.0.jar` en `mods`
(en el servidor **y** en cada cliente). Necesita un **mundo nuevo** o chunks nuevos.

## Las naves (más lejos del spawn = más difíciles)

| Nave | Distancia del spawn | Qué hay |
|---|---|---|
| **Globo Aerostático** | 300 - 1600 bloques (a veces más lejos) | canasta con cofre, piratas, escalera de cuerda hasta el suelo |
| **Avioneta Pirata** | desde 900 (rara desde 450) | biplano con spawner de piratas y cofre |
| **Dirigible Pirata** | desde 1700 (raro desde 900) | casco, puente, motores, varios spawners y **jefe: Capitán Barbanube** |
| **Fortaleza Flotante** | desde 3000 (rara desde 2000) | isla con minerales, 4 torres con arcabuceros, arena y **jefe: Cóndor de Acero** |

El globo, la avioneta y el dirigible cuelgan una **escalera de cuerda** hasta el suelo para poder subir.
La **Fortaleza Flotante** no tiene: flota ~70 bloques sobre el terreno y tienes que ingeniártelas para subir
(torre de bloques, perlas de ender...) mientras los arcabuceros te disparan desde las torres.
La **Brújula del Cielo** (pluma + 3 oro + brújula), con click derecho, dice a cuántos bloques y en qué dirección está
la nave más cercana, la nave con jefe más cercana y la Ciudadela de las Nubes más cercana, con sus coordenadas.

## Ciudadela de las Nubes (la mazmorra gigante)
Torre de **8 pisos + azotea** (unos 65x65 bloques y 130 de alto) que se genera **al azar**: cada ciudadela es distinta.
Aparece muy rara vez, desde **1200 bloques** del spawn (a veces desde 700). Se entra por una escalera de cuerda que baja
hasta el suelo.

- **Cada piso es un laberinto** de salas flotantes unidas por puentes.
  - Pisos 1-2: 3x3 salas; pisos 3-5: 4x4; pisos 6-8: 5x5.
  - Cuanto más subes, más monstruos y spawners hay, menos barandas, y más puentes **rotos (hay que saltar)**, **angostos** o de cristal.
- **Para subir**: la escalera al siguiente piso está en la sala **más lejana** y la cierra un **Sello Celestial**.
  - En otra sala lejana hay un cofre sobre oro con la **Llave Celestial** del piso.
  - Usa la llave en el sello y se abre.
- **Piso 4**: el **Capitán Barbanube** guarda la escalera.
- **Azotea**: arena del **Cóndor de Acero**, arcabuceros en las torres y 2 cofres con el **tesoro final**
  (netherite, bloques de diamante, tótems, manzanas encantadas, plantilla de netherite y a veces élitros).
- El botín mejora cada 3 pisos.
- **Sin atajos**: dentro de la ciudadela (y en toda su columna hasta el suelo), en supervivencia:
  - **no se pueden poner ni romper bloques** (solo se pueden romper los spawners);
  - no funcionan las **perlas de ender**, las **frutas chorus** ni las **cubetas**;
  - no se puede **planear con élitros**.

  Hay que subir piso por piso.
- Monstruos nuevos de la ciudadela:
  - **Guardián Celeste**: caballero con escudo. Las flechas de frente le hacen solo 25 % y sus golpes empujan fuerte (cuidado en los puentes).
  - **Brujo del Viento**: lanza **cargas de viento** que te empujan y te pueden tirar del puente.

## Enemigos
- **Pirata del Cielo**: espada, no recibe daño por caída.
- **Arcabucero del Cielo**: dispara desde lejos.
- **Mecánico del Dirigible**: pequeño, lanza **bombas** y es inmune a explosiones.

## Jefes (con modelo 3D, animaciones y barra de vida)
**Capitán Barbanube** (320 de vida)
- *Salto Aplastante*: se agacha (aviso) y cae con una onda expansiva. Aléjate cuando lo veas cargar.
- Al 66 % de vida: llama a 3 piratas y empieza a disparar su **cañón de bombas** si te alejas.
- Al 33 %: **Furia**: gira como torbellino; las flechas le hacen la mitad de daño, hay que pelear cuerpo a cuerpo.
- Suelta el **Sombrero del Capitán** (casco).

**Cóndor de Acero** (450 de vida)
- Vuela en círculos sobre la arena; las flechas le hacen la mitad mientras vuela.
- **Picada**: chilla, se lanza en picado y, si falla, queda **aturdido** en el suelo 3 s recibiendo **doble daño**. Ese es el momento.
- **Lluvia de plumas** en abanico y **ráfaga de viento** que empuja (cuidado con los bordes).
- A mitad de vida llama a 2 arcabuceros.
- Suelta las **Alas del Cóndor** (planeador como élitros) y Plumas de Acero.

Los jefes aparecen con el **Núcleo del Jefe** cuando un jugador en supervivencia se acerca (una sola vez por nave).
Los cofres traen botín por dificultad y, si están instalados, objetos de Arequipa y Tecsup.

## Desarrollo
```
gradlew-d.cmd build              # jar en build/libs
gradlew-d.cmd runGametest        # distancias, naves, núcleo del jefe, pelea de jefes
gradlew-d.cmd runClientGameTest  # capturas de mobs y naves en build/clientgametest/screenshots
python tools/gen_textures.py     # pieles, cóndor, items
python tools/gen_assets.py       # estructuras, loot, modelos, recetas, traducciones
```
