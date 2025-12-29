# ⚔️ UHC ELOUD

![Plugin Version](https://img.shields.io/badge/Plugin_Version-1.4.5--SNAPSHOT-orange)
![Minecraft Version](https://img.shields.io/badge/Minecraft_Version-1.21.11-gold)
![License](https://img.shields.io/badge/License-MIT-green)
![Platform](https://img.shields.io/badge/Platform-Spigot%20%7C%20Paper-blue)

Un plugin de **UHC (Ultra Hardcore)** ligero y eficiente. Este plugin gestiona automáticamente las fases de la partida, el panel de puntuación dinámico, el sistema de victoria y un sistema avanzado de anonimato visual.

Basado en **UHC ESPAÑA** creado por **ElRichMC**.

---

## ✨ Novedades de la Versión 1.4.5 (Actual)

* ⚖️ **Sistema de Equipos Equitativo:**
  * **Balanceo Vivos/Muertos:** Nuevo algoritmo de reparto que garantiza que los jugadores vivos se distribuyan equitativamente entre los equipos, evitando que los jugadores eliminados concentren un solo bando.
  * **Consistencia Competitiva:** Si quedan 7 vivos para 4 equipos, el sistema garantiza un reparto de (2, 2, 2, 1) vivos, rellenando los huecos con espectadores.

* 🛡️ **Blindaje de Comandos y Seguridad:**
  * **Validación Global:** Implementación de un sistema estricto para evitar errores por números negativos, letras en argumentos numéricos o tamaños de borde excesivamente pequeños (Mínimo 20 bloques).
  * **Confirmación de Inicio:** Actualizado el sistema interactivo en chat para `/start`. Ahora se cancela el inicio correctamente al pulsar el botón de *Cancelar*.
  * **Protección de Estado:** Bloqueo inteligente de comandos (`/tpartes`, `/lang`, `/start`) una vez iniciada la partida para evitar corromper el Scoreboard o el estado del juego.

* 🌐 **Sincronización Multilingüe:**
  * Rediseño de los archivos `messages_es.yml` y `messages_en.yml`.
  * Todos los mensajes han sido revisados y ajustados para consistencia en formato y colores.

---

## ✨ Novedades de la Versión 1.4.4

* 🏆 **Fix Sistema de Victoria:**
  * El chat muestra a todos los integrantes del equipo ganador. Los compañeros caídos aparecen tachados (`§7§m`).
  * **Inmunidad de Campeón:** Los ganadores reciben Resistencia 255 y curación instantánea durante la celebración.

* 🏨 **Lobby & Reset Fix:**
  * El comando `/reset` realiza una limpieza profunda: inventarios, XP, estados de victoria y teletransporte automático al spawn (`0, 0`).

* 💉 **Control Estricto de Salud:**
  * Desactivación forzada de la regeneración natural en todos los mundos al iniciar. Sincronización optimizada de corazones en el TAB.

---

## ✨ Características Principales

* 🌍 **Sistema Multi-idioma:** Soporte dinámico para **Español** e **Inglés**. Cambio instantáneo de Scoreboards y mensajes mediante `/lang`.
* 📊 **Scoreboard Dinámico:** Muestra fase, timer de capítulo, tiempo total acumulado y vida de aliados con iconos de salud en tiempo real.
* 🍎 **Golden Heads:** Crafteo de cabezas de jugadores caídos con 8 lingotes de oro. Otorga Regeneración II (12s) y Absorción II (5 min).
* ⚔️ **Combate Híbrido 1.8:** Configurable desde el panel: Spam-click habilitado, daño de hachas ajustado y eliminación de *Sweep Attack*.
* 🧭 **Rastreo Automático:** Brújula que apunta automáticamente al aliado más cercano de forma constante.
* 🏨 **Sistema de Lobby:** Modo standby con `/reset` que otorga Resistencia e Invulnerabilidad infinita en modo aventura hasta el inicio.
* 💬 **Gestión de Chat:** Chat global mediante el prefijo `!` y chat privado de equipo automático por defecto para una comunicación estratégica rápida.
* 📦 **Suministros Automáticos:** Entrega programada de *Shulker Boxes* en episodios clave (1 y 8) para facilitar la gestión del inventario.
* ⚙️ **Panel Admin (GUI):** Interfaz visual (`/uhcadmin`) para gestionar todas las reglas, bordes y configuraciones de la partida.

---

## 🛠️ Comandos

| Comando                | Descripción | Permiso |
|:-----------------------|:---|:--------|
| `/uhcadmin`            | Abre el panel de administración principal. | `admin` |
| `/start <size>`        | Inicia validación y confirmación de comienzo. | `admin` |
| `/tpartes <H> <M> <S>` | Ajusta el tiempo de capítulos con validación de límites. | `admin` |
| `/reset`               | Prepara el mundo, reglas y limpia estados para el lobby. | `admin` |
| `/lang <idioma>`       | Cambia el idioma personal del jugador. | `user`  |
| `/nequipo <nombre>`    | Renombra o funda el nombre de tu equipo. | `user`  |

---

## 🚀 Instalación

* **Requisito:** Java 21 o superior.
* **API:** Paper / Spigot / Purpur para MC 1.21.11

1. Coloca el archivo `.jar` en la carpeta `/plugins`.
2. Reinicia el servidor.
3. Ejecuta `/reset` para preparar el Lobby una vez estén todos los jugadores.
4. Ejecuta `/start [tamaño]` para iniciar la partida.