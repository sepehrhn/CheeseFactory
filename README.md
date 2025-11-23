# 🧀 CheeseFactory

A sophisticated Minecraft plugin that adds a realistic cheese-making system with fermentation mechanics, custom items, and barrel aging.

## Features

### 🏺 Cheese Barrel System
- **Custom Fermentation**: Age cheese in specially crafted barrels
- **Visual Effects**: Bubbling particles and brewing sounds during fermentation
- **Smart Barrel Detection**: Works with both vanilla barrels and custom Nexo blocks
- **Open/Close Sounds**: Realistic audio feedback when accessing barrels
- **Visual State**: Barrel texture changes when opened (Nexo integration)

### 🧪 Cheese Making Process
1. **Inoculate Milk**: Combine milk bucket with bacteria in a cauldron over heat
2. **Cook Curd**: Cook inoculated milk on a campfire to create curd
3. **Ferment**: Place curd in a cheese barrel and wait for fermentation
4. **Harvest**: Collect your finished cheese!

### 🔧 Custom Items
- **Bacteria**: Required for milk inoculation
- **Inoculated Milk**: Milk infused with bacteria
- **Curd**: Raw cheese base
- **Cheese**: Fully fermented final product

### 💻 Admin Tools
- `/cf give <item> [amount]` - Give cheese-making items
- `/cf inspect` - View detailed barrel fermentation status
- `/cf reload` - Reload configuration
- `/cf debugbarrel` - Toggle barrel debug mode

### 🎨 Customization
- **Full Configuration**: Fermentation times, effects, sounds, and item definitions
- **Smart Config Migration**: Preserves your customizations across updates
- **Locale Support**: Customizable messages (currently English)
- **External Item Support**: Integrates with Nexo, CraftEngine, RoseLoot

### 🔌 Integrations
- **Nexo**: Custom item models and block states
- **RoseLoot**: Bacteria drops from blocks
- **CraftEngine**: Alternative custom item provider

## Installation

1. Download `CheeseFactory-0.1.0.jar`
2. Place in your server's `plugins/` folder
3. Restart your server
4. Configure `plugins/CheeseFactory/config.yml` as desired

## Configuration

### Basic Settings
```yaml
fermentation:
  time_ticks: 9600  # 8 minutes
  tick_interval: 20  # Check every second
```

### Visual Effects
```yaml
fermentation:
  effects:
    particles:
      enabled: true
      type: "DUST"
      colors: ["#FFFF00", "#FFD700"]
    sounds:
      enabled: true
      sound: "BLOCK_BREWING_STAND_BREW"
```

### Custom Items
Supports both vanilla Minecraft items and external plugin items (Nexo, CraftEngine):
```yaml
cheese_barrel:
  custom_id:
    type: nexo
    item: cheese_barrel
```

## Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/cf give cheese_barrel [amount]` | `cheesefactory.give.barrel` | Give cheese barrels |
| `/cf give bacteria [amount]` | `cheesefactory.give.bacteria` | Give bacteria |
| `/cf give curd [amount]` | `cheesefactory.give.curd` | Give curd |
| `/cf give inoculated_milk [amount]` | `cheesefactory.give.inoculated_milk` | Give inoculated milk |
| `/cf inspect` | `cheesefactory.inspect` | Inspect barrel contents |
| `/cf reload` | `cheesefactory.admin` | Reload configuration |
| `/cf debugbarrel` | `cheesefactory.admin` | Toggle barrel debug |

## Permissions

- `cheesefactory.admin` - Access to admin commands (default: op)
- `cheesefactory.inspect` - Inspect cheese barrels (default: true)
- `cheesefactory.give.*` - Give commands (default: op)

## Requirements

- **Server**: Paper/Purpur 1.20+
- **Java**: 17 or higher
- **Optional**: Nexo, RoseLoot, CraftEngine

## Building from Source

```bash
git clone https://github.com/Semarina/CheeseFactory.git
cd CheeseFactory
mvn clean package
```

Built jar will be in `target/CheeseFactory-0.1.0.jar`

## Support

- **Issues**: [GitHub Issues](https://github.com/Semarina/CheeseFactory/issues)
- **Wiki**: [Documentation](https://github.com/Semarina/CheeseFactory/wiki)

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Credits

Created by [sepehrhn](https://github.com/sepehrhn)

---

Made with ❤️ for the Minecraft community
