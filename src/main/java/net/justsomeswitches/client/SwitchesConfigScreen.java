package net.justsomeswitches.client;

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
        // Get current config values with error handling
        try {
            initialGhostPreview = SwitchesClientConfig.SHOW_SWITCHES_PREVIEW.get();
            ghostPreview = initialGhostPreview;
        } catch (Exception e) {
            initialGhostPreview = true;
            ghostPreview = true;
        }
        // Server config is only available when a world is loaded
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
        // Layout: consistent spacing throughout
        // Title at Y=15
        // Spacing constants: header-to-button=14, button-to-helptext=5, helptext-to-header=18
        // --- Client Settings: header=33, button=47, help=72 ---
        this.addRenderableWidget(new ExtendedButton(
            centerX - 100, 47, 200, 20,
            getGhostPreviewButtonText(),
            button -> {
                ghostPreview = !ghostPreview;
                button.setMessage(getGhostPreviewButtonText());
            }
        ));
        // --- Server Settings: header=90, button=104, warn=129, button=141, help=166 ---
        if (serverConfigAvailable) {
            this.addRenderableWidget(new ExtendedButton(
                centerX - 100, 104, 200, 20,
                getAllowBlockEntitiesButtonText(),
                button -> {
                    allowBlockEntities = !allowBlockEntities;
                    button.setMessage(getAllowBlockEntitiesButtonText());
                }
            ));
            this.addRenderableWidget(new ExtendedButton(
                centerX - 100, 141, 200, 20,
                getDisableWrenchBreakButtonText(),
                button -> {
                    disableWrenchBreak = !disableWrenchBreak;
                    button.setMessage(getDisableWrenchBreakButtonText());
                }
            ));
        }
        // --- Hitbox Settings: header=184, button=198, button=223, help=248 ---
        int hitboxSectionY = serverConfigAvailable ? 184 : 140;
        this.addRenderableWidget(new ExtendedButton(
            centerX - 100, hitboxSectionY + 14, 200, 20,
            getTightHitboxesBasicButtonText(),
            button -> {
                tightHitboxesBasic = !tightHitboxesBasic;
                button.setMessage(getTightHitboxesBasicButtonText());
            }
        ));
        this.addRenderableWidget(new ExtendedButton(
            centerX - 100, hitboxSectionY + 39, 200, 20,
            getTightHitboxesSwitchesButtonText(),
            button -> {
                tightHitboxesSwitches = !tightHitboxesSwitches;
                button.setMessage(getTightHitboxesSwitchesButtonText());
            }
        ));
        // Save & Cancel buttons
        this.addRenderableWidget(new ExtendedButton(
            centerX - 100, this.height - 50, 95, 20,
            Component.literal("Save"),
            button -> this.saveAndClose()
        ));
        this.addRenderableWidget(new ExtendedButton(
            centerX + 5, this.height - 50, 95, 20,
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
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        int centerX = this.width / 2;
        // Title
        graphics.drawCenteredString(this.font, this.title, centerX, 15, 0xFFFFFF);
        // === CLIENT SETTINGS === header=33, button=47-67, help=72
        drawSectionHeader(graphics, centerX, 33, "Client Settings");
        drawScaledText(graphics, centerX, 72,
            "Displays a transparent preview of switches before placement", 0xAAAAAA);
        // === SERVER SETTINGS === header=90, button=104-124, warn=129, button=141-161, help=166
        drawSectionHeader(graphics, centerX, 90, "Server Settings");
        if (serverConfigAvailable) {
            drawScaledText(graphics, centerX, 129,
                "WARNING: May crash with some modded blocks!", 0xFF5555);
            drawScaledText(graphics, centerX, 166,
                "Prevents instant wrench breaking on servers", 0xAAAAAA);
        } else {
            drawScaledText(graphics, centerX, 104,
                "Server settings are only available when in a world", 0xFFAA00);
            drawScaledText(graphics, centerX, 114,
                "Load into a world to configure server settings", 0xAAAAAA);
        }
        // === HITBOX SETTINGS === header=184, buttons=198-218/223-243, help=248
        int hitboxSectionY = serverConfigAvailable ? 184 : 140;
        drawSectionHeader(graphics, centerX, hitboxSectionY, "Hitbox Settings");
        drawScaledText(graphics, centerX, hitboxSectionY + 64,
            "Tight hitboxes closely follow the shape of each switch model", 0xAAAAAA);
    }

    /** Draws a bold section header with underline. */
    private void drawSectionHeader(@Nonnull GuiGraphics graphics, int centerX, int y, String text) {
        Component header = Component.literal(text).withStyle(style -> style.withBold(true));
        graphics.drawCenteredString(this.font, header, centerX, y, 0xFFFFFF);
        int headerWidth = this.font.width(header);
        graphics.fill(centerX - headerWidth / 2, y + 10, centerX + headerWidth / 2, y + 11, 0xFFFFFFFF);
    }

    /** Draws centered text at 0.9x scale. */
    private void drawScaledText(@Nonnull GuiGraphics graphics, int centerX, int y, String text, int color) {
        graphics.pose().pushPose();
        graphics.pose().scale(0.9f, 0.9f, 1.0f);
        int scaledX = (int)(centerX / 0.9f);
        int scaledY = (int)(y / 0.9f);
        graphics.drawCenteredString(this.font, text, scaledX, scaledY, color);
        graphics.pose().popPose();
    }

    /** Save configuration changes and close the screen. */
    private void saveAndClose() {
        // Save client config changes
        if (ghostPreview != initialGhostPreview) {
            SwitchesClientConfig.SHOW_SWITCHES_PREVIEW.set(ghostPreview);
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
            if (disableWrenchBreak != null && initialDisableWrenchBreak != null
                    && !disableWrenchBreak.equals(initialDisableWrenchBreak)) {
                SwitchesServerConfig.DISABLE_WRENCH_INSTANT_BREAK.set(disableWrenchBreak);
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
