![Just Some Switches](src/main/resources/justsomeswitches.png)

**Just Some Switches** adds 5 new switch models to Minecraft that function just like the vanilla redstone lever, in two tiers: Basic (fixed appearance) and Customizable (fully customizable textures). Unpowered and powered indicators are built into the model - no particles.

---

### New Model Styles

- **Lever** - Classic light switch/lever style
- **Rocker** - Rocker switch style
- **Slide** - Slide switch style with indicator blending modes
- **Button** - Two button switch style (toggles on/off like a lever, does not auto-depress like a vanilla button)
- **Touch** - Touch-pad style with no moving parts (powered state indicated by indicator texture change only)

### Basic Blocks

- Includes normal and inverted variants of each style


  ![BasicModels_Normal](https://i.imgur.com/HkPDYLF.png)



  ![BasicModels_Inverted](https://i.imgur.com/rtgkFzD.png)


### Customizable Blocks

![CustomizableModels](https://i.imgur.com/jvKZqnU.png)
- Can be placed in any orientation on a block face (wall, ceiling, or floor) with a ghost preview showing placement before confirming

![GhostPreview](https://i.imgur.com/hvHAB3R.gif)
- All switch types support waterlogging
- Use the Switch Texture Brush to open the Texture Customization GUI

### Texture Customization GUI

![TextureGUI](https://i.imgur.com/pvbWvOs.png)
- Place almost any solid block into the Base or Toggle texture slot
- Dropdown menu under each texture slot allows the choice of which face of the inserted block to use
- Dropdown menu next to the round arrow graphic changes the rotation of the texture
- Power indicator dropdown menu under the 3d preview allows the choice of which texture shows when powered/unpowered (Default, Alt, or None; the Slide style offers None (Toggle) and None (Base) instead of None)
- Real-time texture and 3D preview (note that textures with an tint/overlay may not render correctly in the previews)
- 95%+ vanilla block compatibility + compatibility with many modded solid blocks (including blocks with tinting and overlays)

### Switch Texture Brush

The Switch Texture Brush has three modes, and what it does on Sneak + Right-Click depends on which mode it is in:

- **Customize** - Opens the Texture Customization GUI
- **Copy** - Copies that switch's texture settings onto the brush
- **Paste** - Applies the brush's stored settings to that switch

Change mode with **Sneak + Scroll** while holding the brush. A **Cycle Brush Mode** keybind is also provided, unbound by default, which you can assign under Options > Controls.

The current mode is shown on screen while the brush is held and on the brush's tooltip, and the brush displays a distinctive active/loaded texture whenever texture settings have been copied to it, giving a clear visual indication of when settings are ready to paste.

The brush works in your main hand only.

| Action | Controls | Target |
|--------|----------|--------|
| Change mode | Sneak + Scroll | While holding the brush |
| Open Texture GUI (Customize mode) | Sneak + Right-Click | On a Customizable Switch block |
| Copy settings (Copy mode) | Sneak + Right-Click | On a Customizable Switch block |
| Paste settings (Paste mode) | Sneak + Right-Click | On a Customizable Switch block |
| Clear stored settings | Sneak + Right-Click | In the air |
| Instant-break | Left-Click | On any of the mod's blocks |

Sneak is the Shift key by default.

### Crafting

- **Basic Switch blocks** are crafted from a vanilla lever + a specific material per style
- **Customizable Switch blocks** are crafted from a Basic Switch + dye + terracotta + stick + stone
- Normal and inverted Basic variants convert freely via shapeless crafting
- **Switch Texture Brush** is crafted from a feather + iron ingot + wooden rod (diagonal pattern)
- All recipes are browsable in JEI

---

### Block Filtering (for Modpack Creators)

The mod automatically accepts most solid, full-cube blocks as texture sources. For finer control, two block tags let you override this:

- **`justsomeswitches:switches_allowed`** - Blocks in this tag bypass all checks and are always accepted
- **`justsomeswitches:switches_blocked`** - Blocks in this tag are always rejected (takes priority over allowed)

To customize, create a datapack with the tag file at:
`data/justsomeswitches/tags/blocks/switches_allowed.json` or `switches_blocked.json`

Tag references from other mods must use `"required": false` to avoid errors if that mod isn't installed.

### Config Options

All options below can also be edited in game from Mods > Just Some Switches > Config.

**Client** (`justsomeswitches-client.toml`)
- `showSwitchesPreview` - Show ghost preview during placement (default: `true`)
- `showBrushModeHud` - Show the brush mode indicator while the brush is held (default: `true`)
- `brushModeHudAnchor` - Indicator position: `BOTTOM_LEFT`, `ABOVE_HOTBAR`, `TOP_LEFT`, `TOP_RIGHT` or `BOTTOM_RIGHT` (default: `BOTTOM_LEFT`)
- `brushModeHudSize` - Indicator text size: `SMALL`, `NORMAL` or `LARGE` (default: `SMALL`) - sizes may look the same at a low GUI Scale
- `brushModeHudOffsetX` / `brushModeHudOffsetY` - Nudge the indicator by a few pixels to clear other overlays (default: `0`)
- `brushModeMessage` - When the action bar confirms a mode change: `SMART`, `ON` or `OFF` (default: `SMART`) - `SMART` hides it only while the indicator sits above the hotbar

**Common** (`justsomeswitches-common.toml`)
- `tightHitboxesBasic`  - Use tight-fitting hitboxes for Basic Switch blocks (default: `false`)
- `tightHitboxesSwitches` - Use tight-fitting hitboxes for Customizable Switch blocks (default: `true`)

**Server** (`justsomeswitches-server.toml`)
- `allowBlockEntities` - Allow blocks with BlockEntities as texture sources (default: `false`) - may cause crashes with certain modded blocks
- `disableBrushInstantBreak` - Disable brush instant breaking (default: `false`) - useful for multiplayer servers
- `respectBlockProtection` - Respect spawn protection and the world border when editing switches (default: `true`)

### Migration Notes (v1.33)

Players updating from a previous version:
- Any existing **Switches Wrench** items in your inventory or chests will automatically convert to **Switch Texture Brush** items on world load. All copied texture settings are preserved.
- The brush's **Alt** key combinations have been replaced by the three modes described above. `Shift + Alt + C` and `Shift + Alt` no longer do anything. Use **Sneak + Scroll** to change mode instead.
- The brush now works in the **main hand only**.
- If you customized the `disableWrenchInstantBreak` server config option, you'll need to set the new `disableBrushInstantBreak` option to your preferred value.
- All existing placed switch blocks work exactly as before - only their display names have changed.

### Translations

The mod ships with 20 languages besides English.

- Translations are machine-assisted and have not been checked by native speakers, so corrections are very welcome.
- German, Spanish, French, Italian, Dutch, Polish, Portuguese (Brazil and Portugal), Russian, Chinese Simplified and Japanese are translated throughout, with block and item names following Minecraft's own wording wherever the game already has a word for something.
- Arabic, Czech, Danish, Greek, Finnish, Norwegian, Romanian, Swedish and Ukrainian cover the menus and messages only. Block and item names stay in English in those languages until someone contributes them.
- To fix or add a language, edit the matching file in `src/main/resources/assets/justsomeswitches/lang/` and open a pull request. Any key you leave out falls back to English, so a partial file is perfectly fine.

---

### CurseForge

- For downloads and more info, check out the project on [CurseForge](https://www.curseforge.com/minecraft/mc-mods/just-some-switches).

---

### FAQ

**What blocks work as texture sources?**
- Almost any solid, full-cube block - including glazed terracotta, waxed copper, and animated textures.

**Can I use this mod in my modpack?**
- Yes, this mod is MIT licensed. Feel free to include it.

---

**Requires:** Minecraft 1.21.1 - NeoForge 21.1.219+ - Java 21+

**License:** [MIT](https://github.com/theJERMwarfare/JustSomeSwitches/blob/main/LICENSE)
