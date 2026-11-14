# Haunt Client — Fabric 1.21.4

Purple/black Halloween-themed client menu. Press **P** in-game to open it.

## Requirements
- Minecraft 1.21.4
- Java 21
- Fabric Loader 0.16.10+
- Fabric API 0.119.2+1.21.4
- Gradle 8.10+ (or a compatible Gradle installation)

## Build
From this folder run:

```bash
gradle build
```

The mod jar will be created in `build/libs/`.

## Current scope
This is a starter project: the `P` keybind and ClickGUI are implemented. The listed module switches are UI toggles only; they do **not** automate fishing, dropping items, trapping players, aiming, or attacking. The project intentionally keeps combat-related features to local practice/training UI rather than automating attacks against other players.

## Install
Copy the built `hauntclient-1.0.0.jar` into your Minecraft instance's `mods` folder alongside Fabric API.
