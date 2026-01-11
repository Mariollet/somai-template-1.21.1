package com.somapps.somai.menu;

import java.util.UUID;

import com.somapps.somai.SomAI;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public class WhistleControlMenu extends AbstractContainerMenu {
    private final UUID companionId;

    public WhistleControlMenu(int containerId, Inventory playerInventory, UUID companionId) {
        super(SomAI.WHISTLE_CONTROL_MENU.get(), containerId);
        this.companionId = companionId;
    }

    public WhistleControlMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInventory, buf.readBoolean() ? buf.readUUID() : null);
    }

    public UUID getCompanionId() {
        return this.companionId;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
