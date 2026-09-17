package net.justsomeswitches.client;

import net.justsomeswitches.config.BrushHudAnchor;
import net.justsomeswitches.config.BrushHudSize;
import net.justsomeswitches.config.BrushModeMessage;
import net.justsomeswitches.config.SwitchesClientConfig;
import net.justsomeswitches.config.SwitchesCommonConfig;
import net.justsomeswitches.config.SwitchesServerConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.gui.widget.ExtendedButton;

import javax.annotation.Nonnull;

/** In-game configuration screen accessible from the Mods menu. */
public class SwitchesConfigScreen extends Screen {
    private final Screen parent;
    private ConfigOptionsList optionsList;
    /** Initial config values for change detection. */
    private boolean initialGhostPreview;
    private boolean initialBrushModeHud;
    private BrushHudAnchor initialHudAnchor;
    private BrushHudSize initialHudSize;
    private BrushModeMessage initialModeMessage;
    private int initialHudOffsetX;
    private int initialHudOffsetY;
    private Boolean initialAllowBlockEntities;
    private Boolean initialDisableBrushBreak;
    private Boolean initialRespectBlockProtection;
    private boolean initialTightHitboxesBasic;
    private boolean initialTightHitboxesSwitches;
    /** Current config values (nullable when server config unavailable). */
    private boolean ghostPreview;
    private boolean brushModeHud;
    private BrushHudAnchor hudAnchor;
    private BrushHudSize hudSize;
    private BrushModeMessage modeMessage;
    private int hudOffsetX;
    private int hudOffsetY;
    private Boolean allowBlockEntities;
    private Boolean disableBrushBreak;
    private Boolean respectBlockProtection;
    private boolean tightHitboxesBasic;
    private boolean tightHitboxesSwitches;
    /** Tracks if server config is available (requires world loaded). */
    private boolean serverConfigAvailable;

    public SwitchesConfigScreen(Screen parent) {
        super(Component.translatable("gui.justsomeswitches.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        // Get current config values with error handling
        try {
            initialGhostPreview = SwitchesClientConfig.SHOW_SWITCHES_PREVIEW.get();
            ghostPreview = initialGhostPreview;
            initialBrushModeHud = SwitchesClientConfig.SHOW_BRUSH_MODE_HUD.get();
            brushModeHud = initialBrushModeHud;
            initialHudAnchor = SwitchesClientConfig.BRUSH_MODE_HUD_ANCHOR.get();
            hudAnchor = initialHudAnchor;
            initialHudSize = SwitchesClientConfig.BRUSH_MODE_HUD_SIZE.get();
            hudSize = initialHudSize;
            initialModeMessage = SwitchesClientConfig.BRUSH_MODE_MESSAGE.get();
            modeMessage = initialModeMessage;
            initialHudOffsetX = SwitchesClientConfig.BRUSH_MODE_HUD_OFFSET_X.get();
            hudOffsetX = initialHudOffsetX;
            initialHudOffsetY = SwitchesClientConfig.BRUSH_MODE_HUD_OFFSET_Y.get();
            hudOffsetY = initialHudOffsetY;
        } catch (Exception e) {
            initialGhostPreview = true;
            ghostPreview = true;
            initialBrushModeHud = true;
            brushModeHud = true;
            initialHudAnchor = BrushHudAnchor.BOTTOM_LEFT;
            hudAnchor = initialHudAnchor;
            initialHudSize = BrushHudSize.SMALL;
            hudSize = initialHudSize;
            initialModeMessage = BrushModeMessage.SMART;
            modeMessage = initialModeMessage;
            initialHudOffsetX = 0;
            hudOffsetX = 0;
            initialHudOffsetY = 0;
            hudOffsetY = 0;
        }
        // Server config is only available when a world is loaded
        try {
            initialAllowBlockEntities = SwitchesServerConfig.ALLOW_BLOCK_ENTITIES.get();
            allowBlockEntities = initialAllowBlockEntities;
            initialDisableBrushBreak = SwitchesServerConfig.DISABLE_BRUSH_INSTANT_BREAK.get();
            disableBrushBreak = initialDisableBrushBreak;
            initialRespectBlockProtection = SwitchesServerConfig.RESPECT_BLOCK_PROTECTION.get();
            respectBlockProtection = initialRespectBlockProtection;
            serverConfigAvailable = true;
        } catch (Exception e) {
            initialAllowBlockEntities = null;
            allowBlockEntities = null;
            initialDisableBrushBreak = null;
            disableBrushBreak = null;
            initialRespectBlockProtection = null;
            respectBlockProtection = null;
            serverConfigAvailable = false;
        }
        // Common config (always available)
        try {
            initialTightHitboxesBasic = SwitchesCommonConfig.TIGHT_HITBOXES_BASIC.get();
            tightHitboxesBasic = initialTightHitboxesBasic;
            initialTightHitboxesSwitches = SwitchesCommonConfig.TIGHT_HITBOXES_SWITCHES.get();
            tightHitboxesSwitches = initialTightHitboxesSwitches;
        } catch (Exception e) {
            initialTightHitboxesBasic = false;
            tightHitboxesBasic = false;
            initialTightHitboxesSwitches = true;
            tightHitboxesSwitches = true;
        }
        int centerX = this.width / 2;
        // Scrollable options list between title and Save/Cancel buttons
        int listTop = 32;
        int listHeight = this.height - 40 - listTop;
        optionsList = new ConfigOptionsList(this.minecraft, this.width, listHeight, listTop, 22);
        // --- Client Settings ---
        optionsList.addEntry(new ConfigOptionsList.HeaderEntry(Component.translatable("gui.justsomeswitches.config.section.client")));
        optionsList.addEntry(new ConfigOptionsList.ButtonEntry(new ExtendedButton(
            0, 0, 200, 20,
            getGhostPreviewButtonText(),
            button -> {
                ghostPreview = !ghostPreview;
                button.setMessage(getGhostPreviewButtonText());
            }
        )));
        optionsList.addEntry(new ConfigOptionsList.TextEntry(
            Component.translatable("gui.justsomeswitches.config.ghost_preview_hint"), 0xAAAAAA));
        optionsList.addEntry(new ConfigOptionsList.ButtonEntry(new ExtendedButton(
            0, 0, 200, 20,
            getBrushModeHudButtonText(),
            button -> {
                brushModeHud = !brushModeHud;
                button.setMessage(getBrushModeHudButtonText());
            }
        )));
        optionsList.addEntry(new ConfigOptionsList.TextEntry(
            Component.translatable("gui.justsomeswitches.config.brush_mode_hud_hint"), 0xAAAAAA));
        optionsList.addEntry(new ConfigOptionsList.ButtonEntry(new ExtendedButton(
            0, 0, 200, 20,
            getHudAnchorButtonText(),
            button -> {
                hudAnchor = hudAnchor.next();
                button.setMessage(getHudAnchorButtonText());
            }
        )));
        optionsList.addEntry(new ConfigOptionsList.ButtonEntry(new ExtendedButton(
            0, 0, 200, 20,
            getHudSizeButtonText(),
            button -> {
                hudSize = hudSize.next();
                button.setMessage(getHudSizeButtonText());
            }
        )));
        optionsList.addEntry(new ConfigOptionsList.TextEntry(
            Component.translatable("gui.justsomeswitches.config.hud_size_hint"), 0xAAAAAA));
        optionsList.addEntry(new ConfigOptionsList.ButtonEntry(new ExtendedButton(
            0, 0, 200, 20,
            getModeMessageButtonText(),
            button -> {
                modeMessage = modeMessage.next();
                button.setMessage(getModeMessageButtonText());
            }
        )));
        optionsList.addEntry(new ConfigOptionsList.TextEntry(
            Component.translatable("gui.justsomeswitches.config.mode_message_hint"), 0xAAAAAA));
        optionsList.addEntry(new ConfigOptionsList.SliderEntry(
            "gui.justsomeswitches.config.hud_offset_x",
            -SwitchesClientConfig.HUD_OFFSET_LIMIT, SwitchesClientConfig.HUD_OFFSET_LIMIT,
            hudOffsetX, value -> hudOffsetX = value));
        optionsList.addEntry(new ConfigOptionsList.SliderEntry(
            "gui.justsomeswitches.config.hud_offset_y",
            -SwitchesClientConfig.HUD_OFFSET_LIMIT, SwitchesClientConfig.HUD_OFFSET_LIMIT,
            hudOffsetY, value -> hudOffsetY = value));
        optionsList.addEntry(new ConfigOptionsList.TextEntry(
            Component.translatable("gui.justsomeswitches.config.hud_offset_hint"), 0xAAAAAA));
        // --- Server Settings ---
        optionsList.addEntry(new ConfigOptionsList.HeaderEntry(Component.translatable("gui.justsomeswitches.config.section.server")));
        if (serverConfigAvailable) {
            optionsList.addEntry(new ConfigOptionsList.ButtonEntry(new ExtendedButton(
                0, 0, 200, 20,
                getAllowBlockEntitiesButtonText(),
                button -> {
                    allowBlockEntities = !allowBlockEntities;
                    button.setMessage(getAllowBlockEntitiesButtonText());
                }
            )));
            optionsList.addEntry(new ConfigOptionsList.TextEntry(
                Component.translatable("gui.justsomeswitches.config.allow_block_entities_hint"), 0xFF5555));
            optionsList.addEntry(new ConfigOptionsList.ButtonEntry(new ExtendedButton(
                0, 0, 200, 20,
                getBrushInstantBreakButtonText(),
                button -> {
                    disableBrushBreak = !disableBrushBreak;
                    button.setMessage(getBrushInstantBreakButtonText());
                }
            )));
            optionsList.addEntry(new ConfigOptionsList.TextEntry(
                Component.translatable("gui.justsomeswitches.config.brush_instant_break_hint"), 0xAAAAAA));
            optionsList.addEntry(new ConfigOptionsList.ButtonEntry(new ExtendedButton(
                0, 0, 200, 20,
                getRespectBlockProtectionButtonText(),
                button -> {
                    respectBlockProtection = !respectBlockProtection;
                    button.setMessage(getRespectBlockProtectionButtonText());
                }
            )));
            optionsList.addEntry(new ConfigOptionsList.TextEntry(
                Component.translatable("gui.justsomeswitches.config.respect_block_protection_hint"), 0xAAAAAA));
        } else {
            optionsList.addEntry(new ConfigOptionsList.TextEntry(
                Component.translatable("gui.justsomeswitches.config.server_unavailable"), 0xFFAA00));
            optionsList.addEntry(new ConfigOptionsList.TextEntry(
                Component.translatable("gui.justsomeswitches.config.server_unavailable_hint"), 0xAAAAAA));
        }
        // --- Hitbox Settings ---
        optionsList.addEntry(new ConfigOptionsList.HeaderEntry(Component.translatable("gui.justsomeswitches.config.section.hitbox")));
        optionsList.addEntry(new ConfigOptionsList.ButtonEntry(new ExtendedButton(
            0, 0, 200, 20,
            getTightHitboxesBasicButtonText(),
            button -> {
                tightHitboxesBasic = !tightHitboxesBasic;
                button.setMessage(getTightHitboxesBasicButtonText());
            }
        )));
        optionsList.addEntry(new ConfigOptionsList.ButtonEntry(new ExtendedButton(
            0, 0, 200, 20,
            getTightHitboxesSwitchesButtonText(),
            button -> {
                tightHitboxesSwitches = !tightHitboxesSwitches;
                button.setMessage(getTightHitboxesSwitchesButtonText());
            }
        )));
        optionsList.addEntry(new ConfigOptionsList.TextEntry(
            Component.translatable("gui.justsomeswitches.config.tight_hitboxes_hint"), 0xAAAAAA));
        this.addWidget(optionsList);
        this.addRenderableWidget(optionsList);
        // Save & Cancel buttons
        this.addRenderableWidget(new ExtendedButton(
            centerX - 100, this.height - 35, 95, 20,
            Component.translatable("gui.justsomeswitches.config.save"),
            button -> this.saveAndClose()
        ));
        this.addRenderableWidget(new ExtendedButton(
            centerX + 5, this.height - 35, 95, 20,
            Component.translatable("gui.cancel"),
            button -> this.onClose()
        ));
    }

    /** ON/OFF state shared by every toggle button, so the words translate once. */
    private static Component onOff(boolean value) {
        return Component.translatable(value ? "gui.justsomeswitches.config.on" : "gui.justsomeswitches.config.off");
    }

    /** Shown for a server option while no world is loaded. */
    private static Component unavailable() {
        return Component.translatable("gui.justsomeswitches.config.unavailable");
    }

    private Component getGhostPreviewButtonText() {
        return Component.translatable("gui.justsomeswitches.config.ghost_preview", onOff(ghostPreview));
    }

    private Component getBrushModeHudButtonText() {
        return Component.translatable("gui.justsomeswitches.config.brush_mode_hud", onOff(brushModeHud));
    }

    private Component getHudAnchorButtonText() {
        return Component.translatable("gui.justsomeswitches.config.hud_anchor", hudAnchor.getDisplayName());
    }

    private Component getHudSizeButtonText() {
        return Component.translatable("gui.justsomeswitches.config.hud_size", hudSize.getDisplayName());
    }

    private Component getModeMessageButtonText() {
        return Component.translatable("gui.justsomeswitches.config.mode_message", modeMessage.getDisplayName());
    }

    private Component getAllowBlockEntitiesButtonText() {
        if (allowBlockEntities == null) {
            return Component.translatable("gui.justsomeswitches.config.allow_block_entities", unavailable());
        }
        return Component.translatable("gui.justsomeswitches.config.allow_block_entities", onOff(allowBlockEntities));
    }

    private Component getBrushInstantBreakButtonText() {
        if (disableBrushBreak == null) {
            return Component.translatable("gui.justsomeswitches.config.brush_instant_break", unavailable());
        }
        // Inverted on purpose: the config key disables the feature, the label reports it enabled.
        return Component.translatable("gui.justsomeswitches.config.brush_instant_break", onOff(!disableBrushBreak));
    }

    private Component getRespectBlockProtectionButtonText() {
        if (respectBlockProtection == null) {
            return Component.translatable("gui.justsomeswitches.config.respect_block_protection", unavailable());
        }
        return Component.translatable("gui.justsomeswitches.config.respect_block_protection", onOff(respectBlockProtection));
    }

    private Component getTightHitboxesBasicButtonText() {
        return Component.translatable("gui.justsomeswitches.config.tight_hitboxes_basic", onOff(tightHitboxesBasic));
    }

    private Component getTightHitboxesSwitchesButtonText() {
        return Component.translatable("gui.justsomeswitches.config.tight_hitboxes_customizable", onOff(tightHitboxesSwitches));
    }

    @Override
    public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        optionsList.render(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 15, 0xFFFFFF);
    }

    /** Save configuration changes and close the screen. */
    private void saveAndClose() {
        // Save client config changes
        boolean clientChanged = false;
        if (ghostPreview != initialGhostPreview) {
            SwitchesClientConfig.SHOW_SWITCHES_PREVIEW.set(ghostPreview);
            clientChanged = true;
        }
        if (brushModeHud != initialBrushModeHud) {
            SwitchesClientConfig.SHOW_BRUSH_MODE_HUD.set(brushModeHud);
            clientChanged = true;
        }
        if (hudAnchor != initialHudAnchor) {
            SwitchesClientConfig.BRUSH_MODE_HUD_ANCHOR.set(hudAnchor);
            clientChanged = true;
        }
        if (hudSize != initialHudSize) {
            SwitchesClientConfig.BRUSH_MODE_HUD_SIZE.set(hudSize);
            clientChanged = true;
        }
        if (modeMessage != initialModeMessage) {
            SwitchesClientConfig.BRUSH_MODE_MESSAGE.set(modeMessage);
            clientChanged = true;
        }
        if (hudOffsetX != initialHudOffsetX) {
            SwitchesClientConfig.BRUSH_MODE_HUD_OFFSET_X.set(hudOffsetX);
            clientChanged = true;
        }
        if (hudOffsetY != initialHudOffsetY) {
            SwitchesClientConfig.BRUSH_MODE_HUD_OFFSET_Y.set(hudOffsetY);
            clientChanged = true;
        }
        if (clientChanged) {
            SwitchesClientConfig.SPEC.save();
        }
        // Save server config changes if available
        if (serverConfigAvailable) {
            boolean serverChanged = false;
            if (allowBlockEntities != null && initialAllowBlockEntities != null
                    && !allowBlockEntities.equals(initialAllowBlockEntities)) {
                SwitchesServerConfig.ALLOW_BLOCK_ENTITIES.set(allowBlockEntities);
                serverChanged = true;
            }
            if (disableBrushBreak != null && initialDisableBrushBreak != null
                    && !disableBrushBreak.equals(initialDisableBrushBreak)) {
                SwitchesServerConfig.DISABLE_BRUSH_INSTANT_BREAK.set(disableBrushBreak);
                serverChanged = true;
            }
            if (respectBlockProtection != null && initialRespectBlockProtection != null
                    && !respectBlockProtection.equals(initialRespectBlockProtection)) {
                SwitchesServerConfig.RESPECT_BLOCK_PROTECTION.set(respectBlockProtection);
                serverChanged = true;
            }
            if (serverChanged) {
                SwitchesServerConfig.SPEC.save();
            }
        }
        // Save common config changes
        boolean commonChanged = (tightHitboxesBasic != initialTightHitboxesBasic)
                || (tightHitboxesSwitches != initialTightHitboxesSwitches);
        if (commonChanged) {
            SwitchesCommonConfig.TIGHT_HITBOXES_BASIC.set(tightHitboxesBasic);
            SwitchesCommonConfig.TIGHT_HITBOXES_SWITCHES.set(tightHitboxesSwitches);
            SwitchesCommonConfig.SPEC.save();
        }
        this.onClose();
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(parent);
        }
    }
}
