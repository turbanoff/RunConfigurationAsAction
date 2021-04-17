# Run Configuration as Action plugin

Plugins for Jetbrains IDEs. It's provides a way to use run configurations as buttons on toolbar. Or assign shortcuts to execute specific run configuration.

Plugin is compatible with all major IDEs based on IntelliJ Platform starting from version 2019.2:
* IntelliJ IDEA
* Android Studio
* PhpStorm
* PyCharm
* RubyMine
* CLion
* WebStorm
* AppCode
* Rider
* GoLand

## Repository link
https://plugins.jetbrains.com/idea/plugin/9448-run-configuration-as-action

## Configuration
To use plugin after installation you should tweak IDE settings.

##### To add an icon too toolbar go to:
  1. File > Settings > Appearance & Behavior > Menus and Toolbars
  2. Choose a toolbar to add a button: Main Toolbar or Navigation Bar Toolbar
  3. Click 'Add after' button and choose action in the tree: All actions > Plug-ins > Run Configurations as Action
  
  ![plugin_actions_in_customize_toolbars](https://cloud.githubusercontent.com/assets/741251/22664412/a620b70e-ecc1-11e6-84e1-4e0e2987d43e.png)
  

##### To assign shortcut go to:
   File > Settings > Keymap > Plug-ins > Run Configuration As Action
   ![plugin_actions_in_keymap](https://cloud.githubusercontent.com/assets/741251/22664411/a3ece9da-ecc1-11e6-99f0-bc2b9766b5c1.png)

## Generate custom icons
   By default the plugin uses not user-friendly icons. Of course, you can always create an icon by yourself.
   But developers are lazy (_I know_). Plugin can help with generating custom icons.
   
   Tools > Create Icon with text  

![create_custom_icon_with_text](https://cloud.githubusercontent.com/assets/741251/22664415/a77b5096-ecc1-11e6-8051-51c4bf9cd3d3.png)
