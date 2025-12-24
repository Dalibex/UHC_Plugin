# ⚔️ ELOUD UHC

![Plugin Version](https://img.shields.io/badge/Plugin_Version-1.2.0--SNAPSHOT-orange)
![MC Version](https://img.shields.io/badge/MC_Version-1.21.11-gold)
![License](https://img.shields.io/badge/License-MIT-green)
![Platform](https://img.shields.io/badge/Platform-Spigot%20%7C%20Paper-blue)

Un plugin de **UHC (Ultra Hardcore)** ligero y eficiente. Este plugin gestiona automáticamente las fases de la partida, el panel de puntuación dinámico, el sistema de victoria y un combate híbrido optimizado.

Basado en el UHC creado por **ElRichMC** | **UHC ESPAÑA**

---

## ✨ Características Principales

* 📊 **Scoreboard Dinámico:** Panel lateral inteligente que muestra fase la actual, timers, equipo, vida de tus compañeros y estado del PVP. Al ganar, muestra un panel exclusivo de victoria.
* ⚔️ **Combate Híbrido 1.8:** Sistema opcional activable desde el panel de admin que simula el PvP antiguo:
    * **Spam-Click:** Velocidad de ataque eliminada.
    * **Balanceo de Daño:** Las hachas hacen daño reducido (estilo 1.8) para no desbalancear el juego.
    * **Sin Barrido:** Elimina el *Sweep Attack* / daño de área (visualmente se sigue viendo pero no tiene efecto).
    * **Bloqueo de Escudos:** Opción para desactivar la mano secundaria y el bloqueo de escudos.
* 💬 **Gestión de Chat:** Sistema de chat integrado con formato limpio.
    * **Global:** Usa `!` al principio para hablar a todos.
    * **Equipo/Privado:** Mensajes para tu equipo o privados si estás solo.
* 🏆 **Sistema de Victoria:** Detección automática del ganador cuando queda un solo equipo. Incluye celebración con cohetes y anuncio global.
* ⚙️ **Panel Admin (GUI):** Interfaz visual avanzada (`/uhcadmin`) para gestionar toda la partida, reglas y combate sin comandos complejos.
* 🎯 **Scatter Seguro:** Teletransporte aleatorio inteligente, evita spawnear y caer en agua o lava, y asegura que los jugadores aparezcan centrados en el bloque y no bajo tierra o fuera de la barrera.
* ⏳ **Automatización:** Gestión de capítulos, reducción de bordes y bloqueo de opciones una vez inicia la partida.
* 🧑‍🤝‍🧑 **Sistema de Equipos:** Formación de equipos automática. (Solos, Equipos de 2-4 jugadores) Puedes hacerlos también manualmente con los propios comandos del juego si quieres.

## 🛠️ Comandos

| Comando | Descripción                                                    | Permiso Requerido |
| :--- |:---------------------------------------------------------------| :--- |
| `/uhccommands` | Muestra la lista de comandos disponibles.                      | `user` |
| `/start <tamaño>` | Abre el menú de confirmación para iniciar el UHC.              | `admin` |
| `/confirmarstart` | Inicia la cuenta atrás final de 10 segundos. (Lo llama /start) | `auto` |
| `/reset` | Detiene la partida, limpia equipos y resetea reglas.           | `admin` |
| `/uhcadmin` | Panel de administración con ajustes generales del UHC.         | `admin` |
| `/nequipo <nombre>` | Permite renombrar tu equipo (Solo si tienes equipo).           | `user` |

## 🚀 Instalación

* Requiere para esta versión estrictamente **Java 22**
* Versión de la API de Paper - 1.21

1. Descarga el archivo `.jar` (compilado con `./gradlew build`).
2. Colócalo en la carpeta `/plugins` de tu servidor.
3. Reinicia el servidor para que cargue el plugin.
4. Listo!

## 💻 Desarrollo

Este proyecto utiliza **Gradle** como gestor de dependencias.

```bash
# Clonar el repositorio
git clone [https://github.com/Dalibex/UHC_Plugin](https://github.com/Dalibex/UHC_Plugin)

# Compilar el proyecto
./gradlew build
```

## 💡 Ideas y Próximas Implementaciones (Roadmap)

Estas son algunas de las funciones planificadas para futuras versiones:

* ⚙️ **Panel de Configuración Avanzado:** Añadir más mejoras para mayor personalización y ajustes (voy poco a poco añadiendo ajustes).
* ⚡ **Eventos Dinámicos:** Sistemas de juego para diversificar tipos de partidas (ahora mismo básica, más a futuro).
