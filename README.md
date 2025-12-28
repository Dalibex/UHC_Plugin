# ⚔️ UHC ELOUD

![Plugin Version](https://img.shields.io/badge/Plugin_Version-1.4.3-orange)
![Minecraft Version](https://img.shields.io/badge/Minecraft_Version-1.21.11-gold)
![License](https://img.shields.io/badge/License-MIT-green)
![Platform](https://img.shields.io/badge/Platform-Spigot%20%7C%20Paper-blue)

Un plugin de **UHC (Ultra Hardcore)** ligero y eficiente. Este plugin gestiona automáticamente las fases de la partida, el panel de puntuación dinámico, el sistema de victoria y un sistema avanzado de anonimato visual.

Basado en **UHC ESPAÑA** creado por **ElRichMC**.

---

## ✨ Novedades de la Versión 1.4.3

* 🏆 **Fix Sistema de Victoria:**
  * Al finalizar la partida, el chat muestra a todos los integrantes del equipo ganador.
  * **Reconocimiento a los Caídos:** Los compañeros que murieron aparecen en la lista final con formato gris y tachado (`§7§m`), mientras que los supervivientes resaltan en blanco.
  * **Inmunidad de Campeón:** Los ganadores reciben Resistencia 255 y curación instantánea durante la celebración.

* 🏨 **Lobby & Reset Fix y Mejora:**
  * El comando `/reset` realiza una limpieza profunda y ejecuta un **Teletransporte Automático** de todos los jugadores al punto de spawn (`0, 0`).
  * Limpieza total de inventarios y estados de victoria al reiniciar.

* 💉 **Fix Control Estricto de Salud:**
  * **UHC Real:** Desactivación forzada de la regeneración natural (`NATURAL_HEALTH_REGENERATION`) en TODOS los mundos al iniciar.
  * **Sincronización de Corazones:** Sistema optimizado para que la vida en el TAB se renderice correctamente desde el primer segundo.

* 📦 **Suministros Automáticos:** Entrega programada de *Shulker Boxes* para mayor espacio en los episodios 1 y 8 (desactivable desde el panel en ajustes generales).

---

## ✨ Características Principales

* 🌍 **Sistema Multi-idioma:** Soporte completo actualmente para **Español** e **Inglés**. Todos los mensajes, menús y scoreboards cambian dinámicamente según la preferencia del administrador mediante `/lang`.
* 📊 **Scoreboard Dinámico:** Panel lateral inteligente que muestra:
  * Fase actual y Timer de capítulo.
  * Tiempo total de juego.
  * Vida de aliados en tiempo real.
  * Estado del PVP (Pacto o Activo).
* 🍎 **Mecánica: Golden Heads:** Al morir, los jugadores sueltan su cabeza.
  * **Crafteo:** Cabeza + 8 lingotes de oro.
  * **Efectos:** Regeneración II (12s) y Absorción II (5 min).
* ⚔️ Combate Híbrido 1.8: Configurable desde el panel de admin:
  * Spam-Click: Velocidad de ataque eliminada.
  *  Balanceo de Daño: Daño de hachas ajustado al estilo 1.8.
  *  Sin Barrido: Eliminación del Sweep Attack de las espadas.
  *  Bloqueo de Escudos: Opción para desactivar escudos y mano secundaria.
* 🧭 **Rastreo Automático:** Brújula que apunta automáticamente al aliado más cercano.
* 🏨 **Sistema de Lobby:** Modo standby con `/reset` que otorga Resistencia e Invulnerabilidad infinita en modo aventura.
* 💬 **Gestión de Chat:** Chat global mediante el prefijo ! y chat privado o de equipo automático en chat normal.
* ⚙️ **Panel Admin (GUI):** Interfaz visual (`/uhcadmin`) para gestionar todas las reglas, bordes y configuraciones de la partida.

---

## 🛠️ Comandos

| Comando | Descripción                                                      | Permiso |
|:---|:-----------------------------------------------------------------|:--------|
| `/uhccommands` | Muestra la lista de comandos disponibles.                        | `user`  |
| `/uhcadmin` | Panel de administración (GUI) con todos los ajustes.             | `admin` |
| `/start <tamaño>` | Inicia el proceso de confirmación y comienzo del UHC.            | `admin` |
| `/reset` | **Modo Lobby:** Limpia equipos, vacía inventarios y TP al spawn. | `admin` |
| `/lang <idioma>` | Cambia el idioma del plugin.                                     | `user`  |
| `/nequipo <nombre>` | Renombra tu equipo actual.                                       | `user`  |

---

## 🚀 Instalación y Testeo

* **Requisito:** Java 21 o superior.
* **API:** Paper.
* **Compatibilidad de Bots:** Para testear, se puede usar **Minecraft Console Client (MCC)**. Requiere *ViaBackwards* ya que estamos en la versión 1.21.11.

1. Coloca el archivo `.jar` en la carpeta `/plugins`.
2. Reinicia el servidor.
3. Ejecuta `/reset` para preparar el Lobby inicial y limpiar objetivos antiguos.
4. Ejecuta `/start [tamaño_borde]` para empezar la partida.

---

## 💡 Próximas Implementaciones (Roadmap)

* ⚡ **Eventos Dinámicos:** Sistemas de juego para diversificar tipos de partidas.
* ⚙️ **Configuración Avanzada:** Panel extendido para añadir más ajustes y mejoras.
* 🎭 **Tabulador Anónimo:** Sistema de ocultación de nombres por equipos en el TAB para aumentar el misterio.
* 👤 **Skins Aleatorias:** Asignación de skins automáticas para garantizar el anonimato visual total entre equipos.