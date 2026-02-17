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

Chats can be positioned anywhere on the screen using x/y coordinates. By default, chats are stacked from right to left.

### Position Commands

- `/chatpos set <chatId> <x> <y>` - Set the position of a chat window
  - `x`: Horizontal position (negative values = from right edge)
  - `y`: Vertical position from bottom
- `/chatpos reset <chatId>` - Reset a chat to default position
- `/chatpos list` - Show all chat positions

### Examples

```
/chatpos set 1 10 10        # Position chat #1 at x=10, y=10 from bottom-left
/chatpos set 2 -300 50      # Position chat #2 300 pixels from right edge, 50 up
/chatpos reset 1            # Reset chat #1 to default stacked position
```

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
