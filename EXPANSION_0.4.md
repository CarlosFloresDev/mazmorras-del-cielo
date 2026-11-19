# Mazmorras del Cielo 0.4.0

Fabric / Minecraft 26.3, Mojang mappings, Java 25. Se conserva el contenido 0.3.

## Generación

| Estructura | Dimensión y altura de base | Separación de regiones en chunks / separación mínima de candidatos |
| --- | --- | --- |
| `cielo:fortin_jean_beck` | Overworld; superficie + 24, mínimo Y=96; desde 700 bloques del origen | 80 / 28 |
| `cielo:bastion_ceniza` | Nether; Y=40–51, sobre lagos de lava o excavado en netherrack; desde 700 bloques | 72 / 24 |
| `cielo:santuario_vacio` | End exterior; superficie + 18, mínimo Y=82; desde 1600 bloques | 88 / 30 |

Son estructuras de 129 × 33 bloques, con cinco secciones, cuatro llaves y sellos de nueve bloques, ocho spawners y cinco cofres con tres niveles de botín. La semilla determina puertas, anchos de sección, lado de las llaves y mosaicos. Incluyen entrada con cuerda, ventanas cerradas, torres y arena final. El santuario solo usa biomas exteriores del End.

`MazmorraStructure` y `MazmorraPiece` comparten la infraestructura de las tres paletas. Guardan tema, origen y semilla en NBT; `Constructor` recorta todas las escrituras por chunk. Se generan en chunks nuevos. La brújula busca la más cercana entre las grandes mazmorras de la dimensión actual; no cambia de dimensión.

`cielo:protegidas` generaliza la zona de la Ciudadela. Impide construir, romper (excepto spawners), usar cubetas, fuego, perlas, chorus, el orbe y elitros. Los mixins protegen contra explosiones, incendios y demolición por mobs. `cielo:expediciones` impide mover las nuevas estructuras con pistones, conservando las trampas de pistones de la Ciudadela. Creativo conserva las excepciones existentes de construcción.

## Combate y objetos

- **Jean Beck:** 420 PV, embestida con empuje, salto con onda que se esquiva saltando, comida arrojada desde fase 2 (náuseas y lentitud), velocidad y tres ayudantes en fase 3. Cinturón en **mano secundaria**: +8 de vida máxima y +0,6 de resistencia al empuje. También deja raciones y manzanas doradas.
- **Señor de la Ceniza:** 420 PV, estelas de fuego y círculos de erupción anunciados, tres blazes desde fase 2. En fase 3 su escudo dura 75 ticks y queda abierto 45 de cada 120; partículas, sonido y color de barra indican el estado. La Hoja de Ceniza prende al objetivo cinco segundos y concede cuatro segundos de resistencia al fuego al acertar. También deja fragmentos de netherita.
- **Heraldo del Vacío:** 420 PV, teletransportes, proyectiles rastreadores que chocan con paredes, zonas de levitación y tres ecos de 6 PV en fase 3. Los ecos usan su mismo modelo, no tienen barra ni botín y caducan. Deja fragmentos y un Orbe del Vacío: paso horizontal seguro de hasta ocho bloques, recarga de seis segundos. Cuatro fragmentos y una perla permiten fabricar otro orbe.
- **Ayudante de cocina:** bloquea proyectiles frontales; deja raciones.
- **Bombardero de ceniza:** anuncia su explosión durante 35 ticks, prende a los jugadores cercanos y muere sin romper bloques; deja brasas para fabricar cargas ígneas.
- **Fuego fatuo:** parpadea durante el combate; deja fruta chorus.

Los jefes tienen fases al 66 % y 33 %, barra, inmunidad a caídas y asfixia, persistencia y 300 XP. Cada núcleo se consume una sola vez cuando ve un jugador en supervivencia: una pared o sello cerrado impide activarlo desde otra sección. Los temporizadores de combate no dependen de la hora del mundo. Los refuerzos ya invocados se guardan al descargar el jefe.

## Archivos y recursos

- Nuevos Java: `world/MazmorraStructure`, `world/MazmorraPiece`; `entity/JefeExpedicion`, los tres jefes, `EnemigoExpedicion`, `ProyectilExpedicion`; `item/HojaDeCenizaItem`, `item/OrbeDelVacioItem`; cuatro mixins de protección; `client/ExpedicionRenderer`; `test/ExpansionGameTests`.
- Registros, núcleo, protección, brújula, sello y cliente existentes ampliados. El sello ahora cuenta solo bloques de sello al aplicar su límite de apertura.
- `tools/gen_expansion.py`: texturas UV 128×128 sombreadas, iconos, huevos, modelos de objetos, recetas, nueve cofres, seis tablas de entidades, worldgen y traducciones. Puede ejecutarse solo; `gen_assets.py` lo llama al final para conservar todo al regenerar.
- Seis siluetas con piezas y animaciones propias: barriga, delantal, bigote y cucharón; armadura, cuernos y espada; túnicas, mangas y cristales orbitales; gorro y sartén; barril, gafas, bomba y mecha. Los modelos antiguos del Capitán y Cóndor se conservan.

## Verificación

Ocho gametests nuevos: tres generaciones en orígenes absolutos separados con carga forzada, comprobación de recorte, llaves, cofres, núcleo y BFS antes/después de cada sello; tres activaciones únicas con barra, daño y fases; ventana del escudo; botín real de jefes y cofres. Las comprobaciones de entidades se retrasan después de cargar chunks.

`CiudadelaClientTests` añade `dungeon_fortin_jean_beck`, `dungeon_bastion_ceniza`, `dungeon_santuario_vacio` y sus capturas `jefe_...`, en vitrinas separadas del Overworld. La prueba original usa `solo_ciudadela` para que su búsqueda no seleccione un fortín.

Comprobado con `javac --release 25` contra los JAR locales, compilando main, client y gametest completos sin usar clases previamente compiladas del proyecto. JSON, referencias de assets y traducciones comprobados; generador ejecutado. Solo aparecen avisos de APIs obsoletas existentes.

Pendiente de ejecutar fuera del sandbox, como se indicó:

```bat
gradlew-d.cmd build runGametest runClientGameTest --offline
```

La compilación directa no valida la aplicación de mixins, la carga de datapacks ni las capturas dentro del juego. El ajuste de dificultad y la revisión visual final también requieren esa ejecución.
