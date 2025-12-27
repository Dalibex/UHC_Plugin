# ⚔️ UHC ELOUD

![Plugin Version](https://img.shields.io/badge/Plugin_Version-1.4.0-orange)
![Minecraft Version](https://img.shields.io/badge/Minecraft_Version-1.21.1-gold)
![License](https://img.shields.io/badge/License-MIT-green)
![Platform](https://img.shields.io/badge/Platform-Spigot%20%7C%20Paper-blue)

Un plugin de **UHC (Ultra Hardcore)** ligero y eficiente. Este plugin gestiona automáticamente las fases de la partida, el panel de puntuación dinámico, el sistema de victoria y un sistema avanzado de anonimato visual.

Basado en **UHC ESPAÑA** creado por **ElRichMC**.

---

## ✨ Novedades de la Versión 1.4.0

* ⚙️ **Implementación de Idiomas:**
  * Sistema escalable que permite cambiar entre idiomas, **Español e Inglés (ES/EN)** actualmente, y de manera muy sencilla usando `/lang <idioma>`.
* ⚙️ **Nuevo Panel de Ajustes Generales:**
  * Opción para entrega automática de *Shulkers* en los episodios 1 y 8 de la partida.
* 💖 **Sistema de Corazones (TAB):**
  * Visualización nativa de la salud de los jugadores en el tabulador.
  * **Sincronización Forzada:** "Terapia de choque" (Instant Damage/Heal) al inicio para asegurar que los corazones se rendericen correctamente desde el segundo cero.
* 📊 **Scoreboards Persistentes:**
  * Refactorización total del motor de scoreboards: reutiliza la tabla existente en lugar de crear una nueva cada segundo.
  * **Resultado:** Cero parpadeo y actualización fluida de la vida de compañeros.
* 🎭 **Anonimato Asimétrico por Equipos:** *(Bajo testeo)*
  * **Enemigos:** Se visualizan con nombre gris e ID única (ej: `[#14]`).
  * **Aliados:** Se ven siempre con nombre real y color de equipo.
  * Visibilidad individualizada: identifica a tu equipo mientras mantienes el anonimato frente al resto.
* 🏨 **Lobby & Reset Total:**
  * El comando `/reset` realiza una limpieza profunda de equipos de anonimato y objetivos, devolviendo la interfaz al estado original de Minecraft (Lobby).

---

## ✨ Características Principales

* 📊 **Scoreboard Dinámico:** Panel lateral inteligente con fases, timers, vida de aliados y estado del PVP. Incluye panel exclusivo de victoria.
* 🍎 **Mecánica: Golden Heads:** Al morir, los jugadores sueltan su cabeza.
  * Crafteo: Cabeza + 8 lingotes de oro.
  * Efectos: Regeneración II (12s) y Absorción II (5 min).
* ⚔️ **Combate Híbrido 1.8:** Configurable desde el panel de admin:
  * **Spam-Click:** Velocidad de ataque eliminada.
  * **Balanceo de Daño:** Daño de hachas ajustado al estilo 1.8.
  * **Sin Barrido:** Eliminación del *Sweep Attack* de las espadas.
  * **Bloqueo de Escudos:** Opción para desactivar escudos y mano secundaria.
* 🧭 **Rastreo Automático:** Brújula que apunta automáticamente al aliado más cercano.
* 💬 **Gestión de Chat:** Chat global mediante el prefijo `!` y chat privado de equipo automático.
* 🏨 **Sistema de Lobby:** Modo standby con `/reset` que otorga Resistencia e Invulnerabilidad infinita en modo aventura.
* ⚙️ **Panel Admin (GUI):** Interfaz visual (`/uhcadmin`) para gestionar todas las reglas y bordes.

---

## 🛠️ Comandos

| Comando | Descripción | Permiso |
|:---|:---|:---|
| `/uhccommands` | Muestra la lista de comandos disponibles. | `user` |
| `/uhcadmin` | Panel de administración (GUI) con todos los ajustes. | `admin` |
| `/start <tamaño>` | Inicia el proceso de confirmación y comienzo del UHC. | `admin` |
| `/reset` | **Modo Lobby:** Limpia equipos, pone modo Aventura e Invulnerabilidad. | `admin` |
| `/tpartes <H> <M> <S>` | Ajusta manualmente la duración de los capítulos. | `admin` |
| `/nequipo <nombre>` | Renombra tu equipo actual. | `user` |
| `/lang <idioma>` | Cambia el idioma del plugin (ES/EN). | `admin` |
| `/test` | Verifica el estado de los sistemas del plugin. | `admin` |

---

## 🚀 Instalación

* **Requisito:** Java 21 o superior.
* **API:** Paper (1.21.1).

1. Coloca el archivo `.jar` en la carpeta `/plugins`.
2. Reinicia el servidor.
3. Ejecuta `/reset` para preparar el Lobby inicial y limpiar objetivos antiguos.

```bash
# Compilar el proyecto manualmente
./gradlew build
```

## 💡 Ideas y Próximas Implementaciones (Roadmap)

Estas son algunas de las funciones planificadas para futuras versiones:

* ⚙️ **Panel de Configuración:** Añadir más mejoras para mayor personalización y ajustes (voy poco a poco añadiendo ajustes).
* ⚡ **Eventos Dinámicos:** Sistemas de juego para diversificar tipos de partidas (ahora mismo básica, más a futuro).
