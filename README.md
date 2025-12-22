# ⚔️ ELOUD UHC

![Plugin Version](https://img.shields.io/badge/Plugin_Version-1.1--SNAPSHOT-orange)
![MC Version](https://img.shields.io/badge/MC_Version-1.21.11-gold)
![License](https://img.shields.io/badge/License-MIT-green)
![Platform](https://img.shields.io/badge/Platform-Spigot%20%7C%20Paper-blue)

Un plugin de **UHC (Ultra Hardcore)** ligero y eficiente. Este plugin gestiona automáticamente las fases de la partida, el panel de puntuación dinámico y los eventos de PvP.

Basado en el UHC creado por **ElRichMC** | **UHC ESPAÑA**

---

## ✨ Características Principales

* 📊 **Scoreboard Dinámico:** Panel lateral inteligente que muestra fase actual, tiempo del capítulo, tiempo total, jugadores de tu equipo y nombre y estado del PVP.
* ⚙️ **Panel Admin (GUI):** Interfaz visual avanzada (`/uhcadmin`) para gestionar toda la partida sin comandos complejos, antes y durante la misma.
* ⏳ **Gestión de Tiempo Flexible:** Configuración de la duración de los capítulos (+1, +5, +10 min) con anuncios globales al servidor.
* 🔒 **Sistema de Bloqueo Inteligente:** Los ajustes de equipos y tiempos se bloquean automáticamente una vez iniciada la partida para evitar errores.
* 🌍 **Control de Borde (WorldBorder):** Ajuste dinámico del tamaño del mapa desde el panel con incrementos de 10 a 1000 bloques, puede ser ajustado en mitad de la partida.
* 🛡️ **GameRules en Vivo:** Activa/Desactiva la regeneración natural, PvP, ciclo día/noche y spawn de mobs desde la interfaz.
* 🎯 **Sistema de Scatter:** Teletransporte aleatorio automático de jugadores al iniciar la partida, todos equidistantes entre ellos empezando por las esquinas.
* ⚡ **Efectos Inmersivos:** Sonidos de interfaz personalizados, rayos al morir un jugador y mensajes de broadcast estilizados.
* 🧑‍🤝‍🧑 **Sistema de Equipos:** Formación de equipos (Seleccionable de 1-4 jugadores) automática en el episodio 4.

## 🛠️ Comandos

| Comando | Descripción | Permiso Requerido |
| :--- | :--- | :--- |
| `/uhccommands` | Muestra la lista de comandos disponibles. | `user` |
| `/start <tamaño>` | Abre el menú de confirmación para iniciar el UHC. | `admin` |
| `/confirmarstart` | Inicia la cuenta atrás final de 10 segundos. | `auto` |
| `/reset` | Detiene la partida y limpia los cronómetros. | `admin` |
| `/uhcadmin` | Panel de administración con ajustes del UHC. | `admin` |
| `/nequipo` | Permite crear un equipo una vez se formen los equipos en partida por cualquiera de los miembros. | `user` |

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
git clone [https://github.com/TU_USUARIO/UHC-DBasic.git](https://github.com/Dalibex/UHC-DBasic.git)

# Compilar el proyecto
./gradlew build
```

## 💡 Ideas y Próximas Implementaciones (Roadmap)

Estas son algunas de las funciones planificadas para futuras versiones:

* 🎉 **Finalización de partida:** (IMPORTANTE), todavía por implementar, la idea es anunciar al ganador, para empezar otra partida, usar `/reset`
* ⚙️ **Panel de Configuración Avanzado:** Añadir más mejoras para mayor personalización y ajustes.
* ⚡ **Eventos Dinámicos:** Sistemas de juego para diversificar tipos de partidas (ahora mismo básica).
