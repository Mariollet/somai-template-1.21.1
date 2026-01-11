package com.somapps.somai.client;

import com.somapps.somai.PlayerEngineCommands;
import com.somapps.somai.menu.WhistleControlMenu;
import com.somapps.somai.net.CompanionStatusRequestPayload;
import com.somapps.somai.net.SomAINetwork;
import com.somapps.somai.net.WhistleActionPayload;
import com.somapps.somai.net.WhistleRunCommandPayload;
import com.somapps.somai.net.WhistleClearLinkPayload;
import com.somapps.somai.net.WhistleSummonPayload;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class WhistleControlScreen extends AbstractContainerScreen<WhistleControlMenu> {
    private EditBox commandBox;
    private int statusTick;

    private PlayerEngineCommands.Category category = PlayerEngineCommands.Category.ALL;
    private int page;

    public WhistleControlScreen(WhistleControlMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 360;
        this.imageHeight = 290;
    }

    @Override
    protected void init() {
        super.init();

        int left = this.leftPos;
        int top = this.topPos;

        boolean hasCompanion = this.menu.getCompanionId() != null;

        int marginX = 10;
        int buttonHeight = 20;
        int buttonW = 110;
        int gapX = 8;

        // Category selector row
        int catY = top + 56;
        int catH = 18;
        int catW = 48;
        this.addRenderableWidget(Button.builder(Component.literal("All"), b -> setCategory(PlayerEngineCommands.Category.ALL))
            .bounds(left + marginX + 0 * (catW + 4), catY, catW, catH).build());
        this.addRenderableWidget(Button.builder(Component.literal("Ctl"), b -> setCategory(PlayerEngineCommands.Category.CONTROL))
            .bounds(left + marginX + 1 * (catW + 4), catY, catW, catH).build());
        this.addRenderableWidget(Button.builder(Component.literal("Mov"), b -> setCategory(PlayerEngineCommands.Category.MOVEMENT))
            .bounds(left + marginX + 2 * (catW + 4), catY, catW, catH).build());
        this.addRenderableWidget(Button.builder(Component.literal("Inv"), b -> setCategory(PlayerEngineCommands.Category.INVENTORY))
            .bounds(left + marginX + 3 * (catW + 4), catY, catW, catH).build());
        this.addRenderableWidget(Button.builder(Component.literal("Cbt"), b -> setCategory(PlayerEngineCommands.Category.COMBAT))
            .bounds(left + marginX + 4 * (catW + 4), catY, catW, catH).build());
        this.addRenderableWidget(Button.builder(Component.literal("Misc"), b -> setCategory(PlayerEngineCommands.Category.MISC))
            .bounds(left + marginX + 5 * (catW + 4), catY, catW + 6, catH).build());

        int y0 = top + 80;
        var recallBtn = Button.builder(Component.literal("Recall"), b -> sendAction(WhistleActionPayload.Action.RECALL))
            .bounds(left + marginX, y0, buttonW, buttonHeight).build();
        recallBtn.active = hasCompanion;
        this.addRenderableWidget(recallBtn);

        var stopBtn = Button.builder(Component.literal("Stop"), b -> sendAction(WhistleActionPayload.Action.STOP))
            .bounds(left + marginX + (buttonW + gapX), y0, buttonW, buttonHeight).build();
        stopBtn.active = hasCompanion;
        this.addRenderableWidget(stopBtn);

        var waitBtn = Button.builder(Component.literal("Wait/Follow"), b -> sendAction(WhistleActionPayload.Action.TOGGLE_WAIT))
            .bounds(left + marginX + 2 * (buttonW + gapX), y0, buttonW, buttonHeight).build();
        waitBtn.active = hasCompanion;
        this.addRenderableWidget(waitBtn);

        var summonBtn = Button.builder(Component.literal("Summon"), b -> sendSummon())
            .bounds(left + marginX, y0 + 24, buttonW, buttonHeight).build();
        summonBtn.active = !hasCompanion;
        this.addRenderableWidget(summonBtn);

        var resetBtn = Button.builder(Component.literal("Reset link"), b -> sendClearLink())
            .bounds(left + marginX + (buttonW + gapX), y0 + 24, buttonW, buttonHeight).build();
        resetBtn.active = hasCompanion;
        this.addRenderableWidget(resetBtn);

        // Extra spacing row (keeps presets from colliding with action buttons)
        int presetsY0 = y0 + 24 + buttonHeight + 12;

        // Presets grid (paged)
        int presetTop = presetsY0;
        int presetW = 110;
        int presetH = 20;
        int presetGapX = 8;
        int presetGapY = 4;
        int presetRows = 4;
        int presetCols = 3;
        int perPage = presetRows * presetCols;

        var specs = PlayerEngineCommands.specs(this.category);
        int totalPages = Math.max(1, (int) Math.ceil(specs.size() / (double) perPage));
        if (this.page >= totalPages) {
            this.page = 0;
        }

        int start = this.page * perPage;
        for (int i = 0; i < perPage; i++) {
            int idx = start + i;
            if (idx >= specs.size()) {
                break;
            }

            var spec = specs.get(idx);
            int row = i / presetCols;
            int col = i % presetCols;
            int x = left + marginX + col * (presetW + presetGapX);
            int y = presetTop + row * (presetH + presetGapY);

            String tipText = spec.template();
            if (!spec.description().isBlank()) {
                tipText = spec.template() + "\n" + spec.description();
            }

            var presetBtn = Button.builder(Component.literal(spec.label()), b -> presetOrRun(spec.template()))
                .bounds(x, y, presetW, presetH)
                .tooltip(Tooltip.create(Component.literal(tipText)))
                .build();
            presetBtn.active = hasCompanion;
            this.addRenderableWidget(presetBtn);
        }

        // Paging controls
        int pagerY = presetTop + presetRows * (presetH + presetGapY) + 6;
        this.addRenderableWidget(Button.builder(Component.literal("<"), b -> prevPage(totalPages))
            .bounds(left + marginX, pagerY, 20, 18).build());
        this.addRenderableWidget(Button.builder(Component.literal(">"), b -> nextPage(totalPages))
            .bounds(left + marginX + 25, pagerY, 20, 18).build());

        int cmdY = top + this.imageHeight - 26;
        int runW = 60;
        int cmdW = this.imageWidth - (marginX * 2) - runW - 6;
        this.commandBox = new EditBox(this.font, left + marginX, cmdY, cmdW, 18, Component.literal("PlayerEngine command"));
        this.commandBox.setMaxLength(256);
        this.commandBox.setEditable(hasCompanion);
        this.addRenderableWidget(this.commandBox);

        var runBtn = Button.builder(Component.literal("Run"), b -> runCommand())
            .bounds(left + marginX + cmdW + 6, cmdY - 1, runW, 20).build();
        runBtn.active = hasCompanion;
        this.addRenderableWidget(runBtn);

        if (hasCompanion) {
            this.setInitialFocus(this.commandBox);
        }
    }

    private void setCategory(PlayerEngineCommands.Category category) {
        if (category == null) {
            category = PlayerEngineCommands.Category.ALL;
        }
        this.category = category;
        this.page = 0;
        rebuild();
    }

    private void prevPage(int totalPages) {
        if (totalPages <= 1) {
            return;
        }
        this.page = (this.page - 1 + totalPages) % totalPages;
        rebuild();
    }

    private void nextPage(int totalPages) {
        if (totalPages <= 1) {
            return;
        }
        this.page = (this.page + 1) % totalPages;
        rebuild();
    }

    private void rebuild() {
        // Rebuild widgets to reflect category/page changes.
        this.clearWidgets();
        this.init();
    }

    private void sendAction(WhistleActionPayload.Action action) {
        if (this.menu.getCompanionId() == null) {
            return;
        }
        SomAINetwork.sendToServer(new WhistleActionPayload(this.menu.getCompanionId(), action));
    }

    private void sendSummon() {
        SomAINetwork.sendToServer(new WhistleSummonPayload(true));
    }

    private void sendClearLink() {
        SomAINetwork.sendToServer(new WhistleClearLinkPayload());
    }

    private void presetOrRun(String commandTemplate) {
        if (commandTemplate == null || commandTemplate.isBlank()) {
            return;
        }
        if (this.commandBox == null) {
            return;
        }

        // Shift-click runs immediately, otherwise it just pre-fills.
        if (hasShiftDown()) {
            if (this.menu.getCompanionId() == null) {
                return;
            }
            SomAINetwork.sendToServer(new WhistleRunCommandPayload(this.menu.getCompanionId(), commandTemplate));
            return;
        }

        this.commandBox.setValue(commandTemplate + " ");
        this.commandBox.moveCursorToEnd(false);
        this.setFocused(this.commandBox);
    }

    private void runCommand() {
        if (this.commandBox == null) {
            return;
        }

        String cmd = this.commandBox.getValue();
        if (cmd == null) {
            return;
        }

        cmd = cmd.trim();
        if (cmd.isEmpty()) {
            return;
        }

        if (this.menu.getCompanionId() == null) {
            return;
        }
        SomAINetwork.sendToServer(new WhistleRunCommandPayload(this.menu.getCompanionId(), cmd));
        this.commandBox.setValue("");
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.commandBox != null && this.commandBox.isFocused()) {
            if (keyCode == 257 || keyCode == 335) { // Enter
                runCommand();
                return true;
            }
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, 0xAA000000);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        statusTick++;
        if (this.menu.getCompanionId() != null && (statusTick % 10) == 0) {
            SomAINetwork.sendToServer(new CompanionStatusRequestPayload(this.menu.getCompanionId()));
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, Component.literal("SomAI - Companion Control"), 10, 6, 0xFFFFFF);

        // status block is intentionally compact; controls below are larger

        if (this.menu.getCompanionId() == null) {
            guiGraphics.drawString(this.font, Component.literal("No companion linked."), 10, 18, 0xBBBBBB);
            guiGraphics.drawString(this.font, Component.literal("Press Summon to create one."), 10, 30, 0xBBBBBB);
            guiGraphics.drawString(this.font, Component.literal("Then use the whistle on it to relink if needed."), 10, 42, 0x888888);
            guiGraphics.drawString(this.font, Component.literal("Category: " + this.category.name() + " | page " + (this.page + 1)), 10, 54, 0x888888);
            guiGraphics.drawString(this.font, Component.literal("Tip: click preset to fill; Shift+click runs"), 10, this.imageHeight - 40, 0x888888);
            return;
        }

        ClientCompanionStatusCache.Status status = ClientCompanionStatusCache.get(this.menu.getCompanionId());
        if (status != null) {
            guiGraphics.drawString(this.font, Component.literal("HP: " + status.health() + "/" + status.maxHealth() + (status.waiting() ? " (WAIT)" : "")), 10, 18, 0xDDDDDD);
            guiGraphics.drawString(this.font, Component.literal("Dim: " + status.dimension()), 10, 30, 0xBBBBBB);
            guiGraphics.drawString(this.font, Component.literal("Auto: " + (status.pathing() ? "pathing" : "idle") + (status.goal().isEmpty() ? "" : " - " + status.goal())), 10, 42, 0xBBBBBB);
        } else {
            guiGraphics.drawString(this.font, Component.literal("Status: (waiting for server...)"), 10, 18, 0xBBBBBB);
        }

        guiGraphics.drawString(this.font, Component.literal("Category: " + this.category.name() + " | page " + (this.page + 1)), 10, 54, 0x888888);
        guiGraphics.drawString(this.font, Component.literal("Tip: click preset to fill; Shift+click runs"), 10, this.imageHeight - 40, 0x888888);
    }
}
