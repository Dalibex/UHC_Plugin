# ⚔️ UHC ELOUD

![Plugin Version](https://img.shields.io/badge/Plugin_Version-1.5.2--SNAPSHOT-orange)
![Minecraft Version](https://img.shields.io/badge/Minecraft_Version-1.21.11-gold)
![License](https://img.shields.io/badge/License-MIT-green)
![Platform](https://img.shields.io/badge/Platform-Spigot%20%7C%20Paper-blue)

Un plugin de **UHC (Ultra Hardcore)** ligero y eficiente. Este plugin gestiona automáticamente las fases de la partida, el panel de puntuación dinámico, el sistema de victoria y un sistema de anonimato visual.

Basado en **UHC ESPAÑA** creado por **ElRichMC**.

---

## 🚀 Novedades de la Versión 1.5.2-SNAPSHOT (Actual)

Esta versión representa una evolución significativa en la arquitectura del plugin, mejorando la escalabilidad y la experiencia del usuario.

* 💎 **Nuevo Modo: Resource Rush:**
  - Compite por ser el primer equipo en conseguir 10 objetos aleatorios.
  - El Scoreboard se adapta dinámicamente para mostrar el progreso de recolección en tiempo real.
  - Finalización automática al completar la lista de recursos.

* 🎭 **Sistema de Identidades Dinámicas (SkinsRestorer):**
  - Rotación automática de skins en cada cambio de capítulo.
  - Sistema inteligente que evita asignar la propia skin o repetir skins recientes.
  - Integración con **SkinsRestorer API** para un anonimato visual perfecto.

* 📊 **Integración Avanzada con TAB:**
  - Prefijos coloreados y nombres dinámicos sincronizados con el sistema de identidades.
  - Visualización de salud en tiempo real en la lista de jugadores.

* 🛡️ **Gestión de Equipos Refinada:**
  - **Equipos Personalizados:** Interfaz visual con estrella del nether para elegir bando.
  - **Validación Automática:** Bloqueo de modos incompatibles (ej. Equipos en Solos).
  - **Nombres Estandarizados:** Los equipos aleatorios ahora usan nombres de colores coherentes (Rojo, Azul, etc.).

* ⚙️ **Arquitectura Modular:**
  - Refactorización interna para permitir la adición sencilla de nuevos modos de juego en el futuro.
  - Centralización de lógicas de tiempo, scoreboards y eventos.

---

## ✨ Características Principales

* 🌍 **Sistema Multi-idioma:** Soporte dinámico para **Español** e **Inglés**. Cambio instantáneo de Scoreboards y mensajes mediante `/lang`.
* 📊 **Scoreboard Dinámico:** Muestra fase, timer de capítulo, tiempo total acumulado y vida de aliados con iconos de salud en tiempo real.
* 🍎 **Golden Heads:** Crafteo de cabezas de jugadores caídos con 8 lingotes de oro. Otorga Regeneración II (12s) y Absorción II (5 min).
* ⚔️ **Combate Híbrido 1.8:** Configurable desde el panel: Spam-click habilitado, daño de hachas ajustado y eliminación de *Sweep Attack*.
* 🧭 **Rastreo Automático:** Brújula que apunta automáticamente al aliado más cercano.
* 🏨 **Sistema de Lobby:** Modo standby con `/reset` que prepara el mundo y los jugadores para el inicio.
* 💬 **Gestión de Chat:** Chat por equipos por defecto y chat global mediante el prefijo `!`.
* 📦 **Suministros Automáticos:** Entrega programada de *Shulker Boxes* en episodios clave.
* ⚙️ **Panel Admin (GUI):** Interfaz visual poderosa (`/uhcadmin`) para gestionar todas las reglas y configuraciones.

---

## 🛠️ Comandos

| Comando                 | Descripción                                       | Permiso |
|:------------------------|:--------------------------------------------------|:--------|
| `/uhcadmin`             | Abre el panel de administración principal.        | `admin` |
| `/start <size>`         | Inicia la partida con el tamaño de equipo elegido.| `admin` |
| `/tpartes <M>`          | Ajusta el tiempo de capítulos en minutos.        | `admin` |
| `/reset`                | Limpia estados y prepara el lobby.                | `admin` |
| `/asignarequipo <P> <C>`| Asigna un jugador a un equipo específico.         | `admin` |
| `/lang <idioma>`        | Cambia el idioma personal (`es` / `en`).          | `user`  |
| `/nequipo <nombre>`     | Renombra tu equipo.                               | `user`  |

---

## 🚀 Instalación y Requisitos

* **Requisitos:** 
  - Java 21+
  - [SkinsRestorer](https://skinsrestorer.net/) (Obligatorio para rotación de skins)
  - [TAB](https://github.com/NEZNAMY/TAB) (Recomendado para visualización pro)
* **PLATAFORMA/API:** Paper / Spigot / Purpur para MC 1.21.11

1. Coloca `ELOUD_UHC.jar` en la carpeta `/plugins`.
2. Asegúrate de tener instalados SkinsRestorer y TAB.
3. Reinicia el servidor.
4. Usa `/reset` para preparar el mundo.
5. Usa `/uhcadmin` para elegir el modo y configurar las reglas antes de empezar.
. Ejecuta `/reset` para preparar el Lobby una vez estén todos los jugadores.
4. Ejecuta `/start [tamaño]` para iniciar la partida.