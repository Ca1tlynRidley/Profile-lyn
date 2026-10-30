package dev.octmc.profile;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import java.util.Map;
import java.util.stream.Collectors;

/** Only vanilla menu types and item stacks are sent to the client. No custom packets. */
public final class ProfileMenu extends ChestMenu {
    private final Layout layout;
    private final SimpleContainer display;
    private final ServerPlayer owner;
    private final Map<Integer, Layout.Entry> entries;
    private long nextClick;
    public ProfileMenu(int id, Inventory inventory, ServerPlayer owner, Layout layout) {
        this(id, inventory, owner, layout, new SimpleContainer(layout.rows() * 9));
    }
    private ProfileMenu(int id, Inventory inventory, ServerPlayer owner, Layout layout, SimpleContainer display) {
        super(type(layout.rows()), id, inventory, display, layout.rows());
        this.layout = layout; this.display = display; this.owner = owner;
        entries = layout.items().stream().collect(Collectors.toUnmodifiableMap(Layout.Entry::slot, e -> e));
        refresh();
    }
    private static MenuType<?> type(int rows) {
        return switch (rows) {
            case 1 -> MenuType.GENERIC_9x1; case 2 -> MenuType.GENERIC_9x2; case 3 -> MenuType.GENERIC_9x3;
            case 4 -> MenuType.GENERIC_9x4; case 5 -> MenuType.GENERIC_9x5; default -> MenuType.GENERIC_9x6;
        };
    }
    public void refresh() {
        for (int slot = 0; slot < display.getContainerSize(); slot++) {
            Layout.Entry e = entries.get(slot);
            if (e == null) {
                ItemStack stack = layout.filler() == null ? ItemStack.EMPTY : stack(layout.filler(), 1);
                if (!stack.isEmpty()) stack.set(DataComponents.CUSTOM_NAME, Component.literal(" "));
                display.setItem(slot, stack); continue;
            }
            ItemStack stack = stack(e.item(), e.count());
            if (!e.name().isEmpty()) stack.set(DataComponents.CUSTOM_NAME, ProfileText.component(ProfileText.replace(e.name(), owner, layout)));
            stack.set(DataComponents.LORE, new ItemLore(e.lore().stream().map(line -> ProfileText.component(ProfileText.replace(line, owner, layout))).toList()));
            stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, e.glint());
            display.setItem(slot, stack);
        }
        broadcastChanges();
    }
    private static ItemStack stack(String id, int count) { return new ItemStack(BuiltInRegistries.ITEM.getOptional(Identifier.parse(id)).orElseThrow(), count); }
    @Override public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }
    @Override public boolean stillValid(Player player) { return player == owner && owner.isAlive(); }
    @Override public void clicked(int slot, int button, ContainerInput input, Player player) {
        // Reject ALL inventory mutations: shift, number keys, drag, double-click, drop, clone and outside clicks.
        if (player != owner) return;
        sendAllDataToRemote();
        if (input != ContainerInput.PICKUP || button < 0 || button > 1) return;
        Layout.Entry entry = entries.get(slot);
        long now = System.nanoTime();
        if (entry == null || now < nextClick) return;
        nextClick = now + 250_000_000L;
        Layout.Action action = entry.action();
        switch (action.type()) {
            case "close" -> owner.closeContainer();
            case "refresh" -> refresh();
            case "message" -> owner.sendSystemMessage(ProfileText.component(ProfileText.replace(action.value(), owner, layout)));
            case "command" -> {
                String command = ProfileText.replace(action.value(), owner, layout);
                owner.closeContainer();
                // Preserve the player's permissions. Never run menu commands as the console or elevate OP.
                owner.level().getServer().getCommands().performPrefixedCommand(owner.createCommandSourceStack(), command);
            }
            default -> { }
        }
    }
}
