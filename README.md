# Petunia

Petunia is the mod that aims inter-loader compatibility.

Currently, Petunia supports loading [Fabric](https://fabricmc.net) mods.

Tested mods are listed below:
| Mod                | Version        | In     |
|--------------------|----------------|--------|
| Fabric Loader      | 0.16.14        | 1.21.7 |
| Fabric API         | 0.128.2+1.21.7 | 1.21.7 |
| Apple Skin         | 3.0.6          | 1.21.5 |
| CICADA             | 0.13.1         | 1.21.7 |
| Do a Barrel Roll   | 3.8.2          | 1.21.7 |
| Entity Culling     | 1.7.4          | 1.21.5 |
| Lithium            | 0.18.0         | 1.21.7 |
| Lot Tweaks         | 2.3.5          | 1.21.5 |
| Mod Menu           | 15.0.0-beta.3  | 1.21.7 |
| Sodium             | 0.6.13         | 1.21.7 |
| Tech Reborn        | 5.14.1         | 1.21.7 |
| Ultimate Furnace   | 1.3.2          | 1.21.5 |

(These are just random mods I've chosen for testing, sorted alphabetically except Fabric)

If you have tested a mod and found it incompatible, please open an issue.
If you have tested it and found it compatible, you can also open an issue to have the mods added to the list above.

## Development
Please refer to [Berry Loader](https://github.com/VoidSingularity/berry) for more information.

To set up the build environment, run `init.py`, this will automatically download the necessary buildscript templates.

To build a jar, run task `main`. The output jar will be in `output/`.

To add Fabric mods for testing, you can simply put the mods into `.cache/extramods/`. The buildscript will automatically add them to the runtime mods' directory.

## License
**SPECIAL NOTICE: To work with this project, you agree with Mojang's [End User License Agreement](https://www.minecraft.net/en-us/eula). If you do not agree, you may not download
resources from Minecraft.**

Files in this repository are licensed under the GNU Lesser General Public License, unless otherwise specified.
