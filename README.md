# SecondChat

Fabric mod to add unlimited optional chat huds.

## Preview

![Preview](.github/images/preview.png)
You can scroll in the additional chats by moving your mouse to the right side of the screen. Multiple chats can be displayed side-by-side.

## Configuration

![Configuration](.github/images/configuration.png)
You can define multiple rules with different filter types and chat IDs. Every message matching one of the rules will be added
to the specified chat. The mod supports unlimited chat windows - simply assign any positive chat ID to your filter rules. The mod supports the following filter types:

- EQUALS
- EQUALS IGNORE CASE
- STARTS WITH
- ENDS WITH
- CONTAINS
- REGEX

## Chat Positioning

Chats can be positioned anywhere on the screen using x/y coordinates through the configuration GUI.

### Accessing Position Settings

1. Open the SecondChat configuration (via Mod Menu)
2. Click the "Positions" button at the bottom
3. Set X and Y coordinates for each chat
4. Click "Set" to apply or "Reset" to restore default stacking

### Coordinate System

- **X coordinate**: Horizontal position
  - Positive values = distance from left edge
  - Negative values = distance from right edge (e.g., -300 = 300 pixels from right)
- **Y coordinate**: Vertical offset from default position

### Examples

- X=10, Y=10: Position chat near bottom-left corner
- X=-300, Y=50: Position chat 300 pixels from right edge, 50 pixels up
- X=500, Y=100: Position chat 500 pixels from left, 100 pixels up

Positions are saved in `config/secondchat-positions.json` and persist across restarts.

## Downloads

Modrinth - https://modrinth.com/mod/secondchat

Curseforge - https://curseforge.com/minecraft/mc-mods/secondchat

Dev builds - https://build.florianreuth.de/job/SecondChat

## Use in Gradle

To use SecondChat with Gradle you can
use [my own repository](https://maven.florianreuth.de/#/releases/de/florianreuth/secondchat).  
You will find instructions on how to add it into your build script there.

## Contact

If you encounter any issues, please report them on
the [issue tracker](https://github.com/florianreuth/SecondChat/issues). If you just want to talk or need help with
SecondChat feel free to join my [Discord](https://florianreuth.de/discord).
