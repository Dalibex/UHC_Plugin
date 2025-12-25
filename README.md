# ⚔️ ELOUD UHC

![Plugin Version](https://img.shields.io/badge/Plugin_Version-1.3.1-orange)
![Minecraft Version](https://img.shields.io/badge/Minecraft_Version-1.21.11-gold)
![License](https://img.shields.io/badge/License-MIT-green)
![Platform](https://img.shields.io/badge/Platform-Spigot%20%7C%20Paper-blue)

Un plugin de **UHC (Ultra Hardcore)** ligero y eficiente. Este plugin gestiona automáticamente las fases de la partida, el panel de puntuación dinámico, el sistema de victoria y mucho más!

Basado en **UHC ESPAÑA** creado por **ElRichMC**

---

## ✨ Características Principales

* 📊 **Scoreboard Dinámico:** Panel lateral inteligente que muestra la fase actual, timers, equipo, vida de tus compañeros y estado del PVP. Al ganar, muestra un panel exclusivo de victoria.
* 🍎 **Nueva Mecánica: Golden Heads:** Al morir, los jugadores sueltan su **cabeza**.
  * Se puede craftear una **Golden Head** (Cabeza + 8 lingotes de oro).
  * Otorga: **Regeneración II** (12s) y **Absorción II** (5 min).
* ⚔️ **Combate Híbrido 1.8:** Sistema opcional activable desde el panel de admin:
  * **Spam-Click:** Velocidad de ataque eliminada.
  * **Balanceo de Daño:** Daño de hachas reducido (estilo 1.8).
  * **Sin Barrido:** Elimina el *Sweep Attack* base de la Espada (El efecto es visible pero no hace nada).
  * **Bloqueo de Escudos:** Opción para desactivar mano secundaria y escudos.
* 🧭 **Rastreo Automático de Compañeros:** Brújula especial que apunta automáticamente al aliado más cercano.
* 💬 **Gestión de Chat:** 
* * **Global:** Usa `!` al principio para hablar a todos.
  * **Equipo:** Chat privado automático para miembros del equipo.
* 🏨 **Sistema de Lobby :** El comando `/reset` ahora activa un **modo standby** real: pone a los jugadores en modo Aventura, les otorga **Resistencia e Invulnerabilidad infinita** hasta que empiece la partida.
* ⚙️ **Panel Admin (GUI):** Interfaz visual (`/uhcadmin`) para gestionar reglas, bordes, tiempos y ajustes generales.

## 🛠️ Comandos

| Comando                | Descripción                                                                                                                                           | Permiso |
|:-----------------------|:------------------------------------------------------------------------------------------------------------------------------------------------------| :--- |
| `/uhccommands`         | Muestra la lista de comandos disponibles                                                                                                              | `user` |
| `/uhcadmin`            | Panel de administración (GUI) con todos los ajustes                                                                                                   | `admin` |
| `/start <tamaño>`      | Llama a la confirmación para iniciar el UHC                                                                                                           | `admin` |
| `/reset`               | **Modo Lobby:** Limpia equipos, pone modo Aventura e <br/>Invulnerabilidad. Resetea también la partida completamente <br/>si se necesita empezar de 0 | `admin` |
| `/tpartes <H> <M> <S>` | Ajusta manualmente la duración de los capítulos                                                                                                       | `admin` |
| `/nequipo <nombre>`    | Renombra tu equipo                                                                                                                                    | `user` |
| `/test`                | Verifica el estado de los sistemas del plugin                                                                                                         | `admin` |

## 🚀 Instalación

* **Requisito:** Java 22
* **API:** Paper - Using *paper-api:1.21.11-R0.1-SNAPSHOT*

1. Descarga el archivo `.jar` compilado.
2. Colócalo en la carpeta `/plugins` de tu servidor.
3. Reinicia el servidor.
4. Ejecuta `/reset` para preparar el Lobby inicial.

## 💻 Desarrollo (Novedades Técnicas)

En la versión **1.3.1** se ha realizado una refactorización profunda:
* **UHC_EventManager:** Centralización de todos los listeners para optimizar el rendimiento.
* **Managers independientes:** Separación de lógica para `TeamManager`, `RightPanelManager` y `SpecialCraftsManager`.
* **Optimización de Tareas:** El rastreo de brújulas ahora se ejecuta en el bucle principal de la partida (`20 ticks`), eliminando la necesidad de interacción por clicks.

```bash
# Compilar el proyecto
./gradlew build
```

## 💡 Ideas y Próximas Implementaciones (Roadmap)

Estas son algunas de las funciones planificadas para futuras versiones:

* ⚙️ **Panel de Configuración:** Añadir más mejoras para mayor personalización y ajustes (voy poco a poco añadiendo ajustes).
* ⚡ **Eventos Dinámicos:** Sistemas de juego para diversificar tipos de partidas (ahora mismo básica, más a futuro).
