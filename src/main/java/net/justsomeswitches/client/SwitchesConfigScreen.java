package net.justsomeswitches.client;

import net.justsomeswitches.config.SwitchesClientConfig;
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
    /** Current config values (nullable when server config unavailable). */
    private boolean ghostPreview;
    private Boolean allowBlockEntities;
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
            // Fallback if client config somehow fails
            initialGhostPreview = true;
            ghostPreview = true;
        }
        
        // Server config is only available when a world is loaded
        try {
            initialAllowBlockEntities = SwitchesServerConfig.ALLOW_BLOCK_ENTITIES.get();
            allowBlockEntities = initialAllowBlockEntities;
            serverConfigAvailable = true;
        } catch (Exception e) {
            // Server config not available (not in a world)
            initialAllowBlockEntities = null;
            allowBlockEntities = null;
            serverConfigAvailable = false;
        }
        
        int centerX = this.width / 2;
        int startY = 35;
        
        // Client Settings Section
        
        // Ghost Preview Toggle Button
        this.addRenderableWidget(new ExtendedButton(
            centerX - 100, startY + 22, 200, 20,
            getGhostPreviewButtonText(),
            button -> {
                ghostPreview = !ghostPreview;
                button.setMessage(getGhostPreviewButtonText());
            }
        ));
        
        // Server Settings Section
        
        // Allow BlockEntities Toggle Button (only if server config is available)
        if (serverConfigAvailable) {
            this.addRenderableWidget(new ExtendedButton(
                centerX - 100, startY + 92, 200, 20,
                getAllowBlockEntitiesButtonText(),
                button -> {
                    allowBlockEntities = !allowBlockEntities;
                    button.setMessage(getAllowBlockEntitiesButtonText());
                }
            ));
        }
        
        // Save Button (full size text)
        this.addRenderableWidget(new ExtendedButton(
            centerX - 100, this.height - 50, 95, 20,
            Component.literal("Save"),
            button -> this.saveAndClose()
        ));
        
        // Cancel Button (full size text)
        this.addRenderableWidget(new ExtendedButton(
            centerX + 5, this.height - 50, 95, 20,
            Component.literal("Cancel"),
            button -> this.onClose()
        ));
    }
    
    /** Gets the text for the ghost preview button. */
    private Component getGhostPreviewButtonText() {
        return Component.literal("Show Ghost Preview: " + (ghostPreview ? "ON" : "OFF"));
    }
    
    /** Gets the text for the allow block entities button. */
    private Component getAllowBlockEntitiesButtonText() {
        if (allowBlockEntities == null) {
            return Component.literal("Allow BlockEntities: UNAVAILABLE");
        }
        return Component.literal("Allow BlockEntities: " + (allowBlockEntities ? "ON" : "OFF"));
    }
    
    @Override
    public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        
        int centerX = this.width / 2;
        int startY = 35;
        
        // Draw title (full size)
        graphics.drawCenteredString(this.font, this.title, centerX, 15, 0xFFFFFF);
        
        // === CLIENT SETTINGS SECTION ===
        
        // Draw Client Settings header (full size, bold, with underline)
        Component clientHeader = Component.literal("Client Settings").withStyle(style -> style.withBold(true));
        graphics.drawCenteredString(this.font, clientHeader, centerX, startY, 0xFFFFFF);
        
        // Draw underline for Client Settings header (measure actual bold text width)
        int clientHeaderWidth = this.font.width(clientHeader);
        graphics.fill(centerX - clientHeaderWidth / 2, startY + 10, centerX + clientHeaderWidth / 2, startY + 11, 0xFFFFFFFF);
        
        // Scale to 90% for help text
        graphics.pose().pushPose();
        graphics.pose().scale(0.9f, 0.9f, 1.0f);
        
        // Draw help text for Ghost Preview (centered, scaled to 90%)
        String ghostHelpText = "Displays a transparent preview of switches before placement";
        int scaledCenterX = (int)(centerX / 0.9f);
        int scaledY = (int)((startY + 47) / 0.9f);
        graphics.drawCenteredString(this.font, ghostHelpText, scaledCenterX, scaledY, 0xAAAAAA);
        
        graphics.pose().popPose();
        
        // === SERVER SETTINGS SECTION ===
        
        // Draw Server Settings header (full size, bold, with underline)
        Component serverHeader = Component.literal("Server Settings").withStyle(style -> style.withBold(true));
        graphics.drawCenteredString(this.font, serverHeader, centerX, startY + 70, 0xFFFFFF);
        
        // Draw underline for Server Settings header (measure actual bold text width)
        int serverHeaderWidth = this.font.width(serverHeader);
        graphics.fill(centerX - serverHeaderWidth / 2, startY + 80, centerX + serverHeaderWidth / 2, startY + 81, 0xFFFFFFFF);
        
        // Scale to 90% for warning and help text
        graphics.pose().pushPose();
        graphics.pose().scale(0.9f, 0.9f, 1.0f);
        
        if (serverConfigAvailable) {
            // Draw warning text (scaled to 90%)
            scaledCenterX = (int)(centerX / 0.9f);
            int scaledWarningY = (int)((startY + 117) / 0.9f);
            graphics.drawCenteredString(this.font, 
                "WARNING: May cause crashes with some modded blocks!",
                scaledCenterX, scaledWarningY, 0xFF5555);
            
            // Draw help text (scaled to 90%)
            int scaledHelpY = (int)((startY + 127) / 0.9f);
            graphics.drawCenteredString(this.font, 
                "Only enable if you need specific blocks with tile entities",
                scaledCenterX, scaledHelpY, 0xAAAAAA);
            
            // Draw restart reminder (scaled to 90%)
            int scaledRestartY = (int)((startY + 137) / 0.9f);
            graphics.drawCenteredString(this.font, 
                "Game must be restarted for changes to take effect",
                scaledCenterX, scaledRestartY, 0xFFAA00);
        } else {
            // Server config not available - show message
            scaledCenterX = (int)(centerX / 0.9f);
            int scaledUnavailableY = (int)((startY + 117) / 0.9f);
            graphics.drawCenteredString(this.font, 
                "Server settings are only available when in a world",
                scaledCenterX, scaledUnavailableY, 0xFFAA00);
            
            int scaledHelpY = (int)((startY + 127) / 0.9f);
            graphics.drawCenteredString(this.font, 
                "Load into a world to configure server settings",
                scaledCenterX, scaledHelpY, 0xAAAAAA);
        }
        
        graphics.pose().popPose();
    }
    
    /** Save configuration changes and close the screen. */
    private void saveAndClose() {
        boolean ghostPreviewChanged = ghostPreview != initialGhostPreview;
        
        // Save client config changes if any were made
        if (ghostPreviewChanged) {
            SwitchesClientConfig.SHOW_SWITCHES_PREVIEW.set(ghostPreview);
            SwitchesClientConfig.SPEC.save();
        }
        
        // Save server config changes if available and changed
        if (serverConfigAvailable && allowBlockEntities != null && initialAllowBlockEntities != null) {
            boolean blockEntitiesChanged = !allowBlockEntities.equals(initialAllowBlockEntities);
            if (blockEntitiesChanged) {
                SwitchesServerConfig.ALLOW_BLOCK_ENTITIES.set(allowBlockEntities);
                SwitchesServerConfig.SPEC.save();
            }
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
