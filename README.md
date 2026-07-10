# Petunia

Petunia is the mod that aims inter-loader compatibility.

Currently, Petunia supports loading [Fabric](https://fabricmc.net) mods.

## Development
Please refer to [Berry Loader](https://github.com/VoidSingularity/berry) for more information.

To set up the build environment, run `init.py`, this will automatically download the necessary buildscript templates.

To build a jar, run task `main`. The output jar will be in `output/`.

To add Fabric mods for testing, you can simply put the mods into `.cache/extramods/`. The buildscript will automatically add them to the runtime mods' directory.

## License
**SPECIAL NOTICE: To work with this project, you agree with Mojang's [End User License Agreement](https://www.minecraft.net/en-us/eula). If you do not agree, you may not download
resources from Minecraft.**

Files in this repository are licensed under the GNU Lesser General Public License, unless otherwise specified.
