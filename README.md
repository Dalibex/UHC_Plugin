# ⚔️ ELOUD UHC

![Plugin Version](https://img.shields.io/badge/Version-1.0--SNAPSHOT-orange)
![MC Version](https://img.shields.io/badge/Version-1.21.11-gold)
![License](https://img.shields.io/badge/License-MIT-green)
![Platform](https://img.shields.io/badge/Platform-Spigot%20%7C%20Paper-blue)

Un plugin de **UHC (Ultra Hardcore)** ligero y eficiente. Este plugin gestiona automáticamente las fases de la partida, el panel de puntuación dinámico y los eventos de PvP.

Basado en el UHC creado por **ElRichMC** | **UHC ESPAÑA**

---

## ✨ Características Principales

* 📊 **Scoreboard Dinámico:** Panel lateral que se adapta si la partida está en espera o en curso.
* ⏳ **Sistema de "Capítulos":** Gestión automática de tiempos y fases del juego.
* 🛡️ **Pacto de Caballeros:** Sistema que desactiva el PvP durante los primeros capítulos (No como tal, solo para avisar a jugadores).
* ⚡ **Interfaz por Chat:** Inicio de partida mediante botones interactivos solo para administradores del servidor.
* 🕒 **Cronómetros Duales:** Rastreo del tiempo por capítulo y tiempo total acumulado.
* 🎯 **Sistema de Scatter:** Teletransporte aleatorio automático de jugadores al inicio de la partida dentro del rango del mapa.

## 🛠️ Comandos

| Comando | Descripción | Permiso Requerido |
| :--- | :--- | :--- |
| `/uhccommands` | Muestra la lista de comandos disponibles. | `user` |
| `/start <tamaño>` | Abre el menú de confirmación para iniciar el UHC. | `admin` |
| `/confirmarstart` | Inicia la cuenta atrás final de 10 segundos. | `admin` |
| `/reset` | Detiene la partida y limpia los cronómetros. | `admin` |

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

* 🗣️ **Sistema de Chat Separado:** Implementación de un chat global y un chat privado para equipos para evitar que los enemigos lean las estrategias.
* 👥 **Gestión de Equipos:** Sistema integrado para crear equipos de forma automática, pudiendo elegir individual, dúos, tríos o cuartetos según los jugadores.
* ⚙️ **Panel de Configuración:** Gestor avanzado para modificar manualmente los tiempos de cada parte y otros ajustes en tiempo real sin reiniciar el plugin.
* ⚡ **Eventos Dinámicos:** Sistemas de juego para diversificar tipos de partidas (ahora mismo básica).
