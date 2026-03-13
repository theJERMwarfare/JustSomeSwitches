package net.justsomeswitches.client;

import net.justsomeswitches.config.SwitchesClientConfig;
import net.justsomeswitches.config.SwitchesCommonConfig;
import net.justsomeswitches.config.SwitchesServerConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.gui.widget.ExtendedButton;

import javax.annotation.Nonnull;

/** In-game configuration screen accessible from the Mods menu. */
public class SwitchesConfigScreen extends Screen {
    private final Screen parent;
    private ConfigOptionsList optionsList;
    /** Initial config values for change detection. */
    private boolean initialGhostPreview;
    private Boolean initialAllowBlockEntities;
    private Boolean initialDisableWrenchBreak;
    private boolean initialTightHitboxesBasic;
    private boolean initialTightHitboxesSwitches;
    /** Current config values (nullable when server config unavailable). */
    private boolean ghostPreview;
    private Boolean allowBlockEntities;
    private Boolean disableWrenchBreak;
    private boolean tightHitboxesBasic;
    private boolean tightHitboxesSwitches;
    /** Tracks if server config is available (requires world loaded). */
    private boolean serverConfigAvailable;

    public SwitchesConfigScreen(Screen parent) {
        super(Component.literal("Just Some Switches Configuration"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        try {
            initialGhostPreview = SwitchesClientConfig.SHOW_SWITCHES_PREVIEW.get();
            ghostPreview = initialGhostPreview;
        } catch (Exception e) {
            initialGhostPreview = true;
            ghostPreview = true;
        }
        try {
            initialAllowBlockEntities = SwitchesServerConfig.ALLOW_BLOCK_ENTITIES.get();
            allowBlockEntities = initialAllowBlockEntities;
            initialDisableWrenchBreak = SwitchesServerConfig.DISABLE_WRENCH_INSTANT_BREAK.get();
            disableWrenchBreak = initialDisableWrenchBreak;
            serverConfigAvailable = true;
        } catch (Exception e) {
            initialAllowBlockEntities = null;
            allowBlockEntities = null;
            initialDisableWrenchBreak = null;
            disableWrenchBreak = null;
            serverConfigAvailable = false;
        }
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
        int listTop = 32;
        int listBottom = this.height - 40;
        optionsList = new ConfigOptionsList(this.minecraft, this.width, this.height, listTop, listBottom, 22);
        // Client Settings
        optionsList.addEntry(new ConfigOptionsList.HeaderEntry("Client Settings"));
        optionsList.addEntry(new ConfigOptionsList.ButtonEntry(new ExtendedButton(
            0, 0, 200, 20, getGhostPreviewButtonText(),
            button -> {
                ghostPreview = !ghostPreview;
                button.setMessage(getGhostPreviewButtonText());
            }
        )));
        optionsList.addEntry(new ConfigOptionsList.TextEntry(
            "Shows a transparent preview before placement", 0xAAAAAA));
        // Server Settings
        optionsList.addEntry(new ConfigOptionsList.HeaderEntry("Server Settings"));
        if (serverConfigAvailable) {
            optionsList.addEntry(new ConfigOptionsList.ButtonEntry(new ExtendedButton(
                0, 0, 200, 20, getAllowBlockEntitiesButtonText(),
                button -> {
                    allowBlockEntities = !allowBlockEntities;
                    button.setMessage(getAllowBlockEntitiesButtonText());
                }
            )));
            optionsList.addEntry(new ConfigOptionsList.TextEntry(
                "WARNING: May crash with some modded blocks!", 0xFF5555));
            optionsList.addEntry(new ConfigOptionsList.ButtonEntry(new ExtendedButton(
                0, 0, 200, 20, getDisableWrenchBreakButtonText(),
                button -> {
                    disableWrenchBreak = !disableWrenchBreak;
                    button.setMessage(getDisableWrenchBreakButtonText());
                }
            )));
            optionsList.addEntry(new ConfigOptionsList.TextEntry(
                "Prevents instant wrench breaking on servers", 0xAAAAAA));
        } else {
            optionsList.addEntry(new ConfigOptionsList.TextEntry(
                "Server settings are only available when in a world", 0xFFAA00));
            optionsList.addEntry(new ConfigOptionsList.TextEntry(
                "Load into a world to configure server settings", 0xAAAAAA));
        }
        // Hitbox Settings
        optionsList.addEntry(new ConfigOptionsList.HeaderEntry("Hitbox Settings"));
        optionsList.addEntry(new ConfigOptionsList.ButtonEntry(new ExtendedButton(
            0, 0, 200, 20, getTightHitboxesBasicButtonText(),
            button -> {
                tightHitboxesBasic = !tightHitboxesBasic;
                button.setMessage(getTightHitboxesBasicButtonText());
            }
        )));
        optionsList.addEntry(new ConfigOptionsList.ButtonEntry(new ExtendedButton(
            0, 0, 200, 20, getTightHitboxesSwitchesButtonText(),
            button -> {
                tightHitboxesSwitches = !tightHitboxesSwitches;
                button.setMessage(getTightHitboxesSwitchesButtonText());
            }
        )));
        optionsList.addEntry(new ConfigOptionsList.TextEntry(
            "Hitboxes closely match each switch model's shape", 0xAAAAAA));
        this.addWidget(optionsList);
        // Save & Cancel buttons
        this.addRenderableWidget(new ExtendedButton(
            centerX - 100, this.height - 35, 95, 20,
            Component.literal("Save"),
            button -> this.saveAndClose()
        ));
        this.addRenderableWidget(new ExtendedButton(
            centerX + 5, this.height - 35, 95, 20,
            Component.literal("Cancel"),
            button -> this.onClose()
        ));
    }

    private Component getGhostPreviewButtonText() {
        return Component.literal("Show Ghost Preview: " + (ghostPreview ? "ON" : "OFF"));
    }
    private Component getAllowBlockEntitiesButtonText() {
        if (allowBlockEntities == null) {
            return Component.literal("Allow BlockEntities: UNAVAILABLE");
        }
        return Component.literal("Allow BlockEntities: " + (allowBlockEntities ? "ON" : "OFF"));
    }
    private Component getDisableWrenchBreakButtonText() {
        if (disableWrenchBreak == null) {
            return Component.literal("Disable Wrench Break: UNAVAILABLE");
        }
        return Component.literal("Disable Wrench Break: " + (disableWrenchBreak ? "ON" : "OFF"));
    }
    private Component getTightHitboxesBasicButtonText() {
        return Component.literal("Tight Hitboxes (Basic): " + (tightHitboxesBasic ? "ON" : "OFF"));
    }
    private Component getTightHitboxesSwitchesButtonText() {
        return Component.literal("Tight Hitboxes (Switches): " + (tightHitboxesSwitches ? "ON" : "OFF"));
    }

    @Override
    public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderDirtBackground(graphics);
        optionsList.render(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 15, 0xFFFFFF);
    }

    /** Save configuration changes and close the screen. */
    private void saveAndClose() {
        if (ghostPreview != initialGhostPreview) {
            SwitchesClientConfig.SHOW_SWITCHES_PREVIEW.set(ghostPreview);
            SwitchesClientConfig.SPEC.save();
        }
        if (serverConfigAvailable) {
            boolean serverChanged = false;
            if (allowBlockEntities != null && initialAllowBlockEntities != null
                    && !allowBlockEntities.equals(initialAllowBlockEntities)) {
                SwitchesServerConfig.ALLOW_BLOCK_ENTITIES.set(allowBlockEntities);
                serverChanged = true;
            }
            if (disableWrenchBreak != null && initialDisableWrenchBreak != null
                    && !disableWrenchBreak.equals(initialDisableWrenchBreak)) {
                SwitchesServerConfig.DISABLE_WRENCH_INSTANT_BREAK.set(disableWrenchBreak);
                serverChanged = true;
            }
            if (serverChanged) {
                SwitchesServerConfig.SPEC.save();
            }
        }
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
