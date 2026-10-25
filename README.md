# ShopPro

Target server: **Paper 1.21.11**, with **Java 21** for both building and running.
This build targets the modern Paper API and no longer runs on older Minecraft versions.

The compatibility update upgrades TabooLib, uses the public Paper player-profile API
for head textures, and updates potion types, item damage and CustomModelData handling.
Material names in shop configurations are resolved against the server API first,
including 1.21.11 items such as `DIAMOND_SPEAR`.

## Building

* [Gradle](https://gradle.org/) - Dependency Management

The Gradle Wrapper is included in this project. Install JDK 21 and set `JAVA_HOME`
to that installation. The first build requires internet access to download dependencies.

**Windows:**

```
gradlew.bat clean build
```

**macOS/Linux:**

```
./gradlew clean build
```

Build artifacts should be found in `./build/libs` folder.

The build runs MockBukkit regression tests for the item compatibility changes.
These tests do not replace testing on a running Paper server.

## Updating a server

1. Stop the server and back up `plugins/ShopPro`, including its database.
2. Replace the existing ShopPro JAR with `build/libs/ShopPro-1.3.5-SNAPSHOT.jar`.
3. Start Paper 1.21.11 using Java 21. TabooLib downloads its runtime libraries on first startup.
4. Check startup logs and verify `/sp open <shop>`, buying, selling, limits,
   head icons and custom model items on a staging server before deployment.

Existing shop and currency YAML formats are retained. Vault currencies require Vault
and an economy provider; custom PlaceholderAPI currencies require PlaceholderAPI;
points currencies require PlayerPoints. The supplied `currencys/Diy1.yml` is an example
and must be configured before using custom currencies. Optional item integrations also require
their corresponding plugins. Their individual compatibility with Paper 1.21.11 must
be checked separately. MythicMobs items use the shared Bukkit item manager API available
in MythicMobs 4.x and 5.x, without referencing either version's internal package names.

Optional item plugins are checked before their item loaders run. If a plugin is absent
or disabled, its commodities are skipped with a single warning rather than a stack trace.
New example configurations disable the Zaphkiel, MythicMobs and MMOItems examples by
default. Existing server configurations are retained: add `enabled: false` inside an
unused item node (for example `items.C` or `items.D`), then run `/sp reload` to disable it.
This does not require removing the whole shop. Enable these examples only after setting
up the corresponding plugin and valid item IDs.

The database module's private SLF4J API and matching Java logging provider are bundled
in the plugin JAR, together with their relocated service descriptor. Database logging
does not depend on a runtime provider download or another plugin's logging libraries.
The tests verify provider discovery and actual Java logging output from the final JAR.
