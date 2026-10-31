package dev.octmc.profile;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.*;
import net.minecraft.network.chat.*;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.SimpleMenuProvider;
import org.slf4j.LoggerFactory;

public final class ProfileStudio implements ModInitializer {
    private final LayoutStore store = new LayoutStore(FabricLoader.getInstance().getConfigDir().resolve("profile-lyn"));
    private final LocalEditor editor = new LocalEditor(FabricLoader.getInstance().getConfigDir().resolve("profile-lyn"));
    private int ticks;
    @Override public void onInitialize() {
        ProfileEconomy.initialize(FabricLoader.getInstance().isModLoaded("eco_api_lyn"));
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            try { store.load(); }
            catch (Exception ex) { LoggerFactory.getLogger("PROFILE-lyn").error("Profile layout could not be loaded. Fix config/profile-lyn/profile.json and run /profile reload.", ex); }
            try { editor.export(store.current()); }
            catch (Exception ex) { LoggerFactory.getLogger("PROFILE-lyn").warn("Local editor could not be extracted. Check folder permissions and retry /profile editor.", ex); }
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (++ticks % 20 != 0) return;
            for (var player : server.getPlayerList().getPlayers()) if (player.containerMenu instanceof ProfileMenu menu) menu.refresh();
        });
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) -> {
            var command = Commands.literal("profile").executes(ctx -> guarded(ctx.getSource(), () -> {
                var player = ctx.getSource().getPlayerOrException();
                ProfileEconomy.ensureAccount(player.getUUID(), player.getScoreboardName());
                Layout layout = store.current();
                player.openMenu(new SimpleMenuProvider((id, inventory, viewer) -> new ProfileMenu(id, inventory, player, layout), ProfileText.component(ProfileText.replace(layout.title(), player, layout))));
                return 1;
            }));
            command.then(Commands.literal("reload").requires(ProfileStudio::admin).executes(ctx -> guarded(ctx.getSource(), () -> {
                store.load(); closeMenus(ctx.getSource());
                reply(ctx.getSource(), "PROFILE-lyn layout reloaded."); return 1;
            })));
            command.then(Commands.literal("import").requires(ProfileStudio::admin)
                .then(Commands.argument("code", StringArgumentType.greedyString()).executes(ctx -> guarded(ctx.getSource(), () -> {
                    store.save(LayoutCodec.decode(StringArgumentType.getString(ctx, "code").strip()));
                    closeMenus(ctx.getSource()); reply(ctx.getSource(), "Layout installed. Run /profile to open it. Previous file saved as profile.json.bak."); return 1;
                }))));
            command.then(Commands.literal("editor").requires(ProfileStudio::admin).executes(ctx -> guarded(ctx.getSource(), () -> {
                var file = editor.export(store.current());
                reply(ctx.getSource(), "Local editor ready: config/profile-lyn/editor/index.html");
                ctx.getSource().sendSuccess(() -> Component.literal("[Copy editor file path]").withStyle(style -> style.withUnderlined(true)
                    .withColor(net.minecraft.ChatFormatting.AQUA).withClickEvent(new ClickEvent.CopyToClipboard(file.toString()))), false);
                reply(ctx.getSource(), "Server on your PC: open that index.html in your browser. Hosted server: download config/profile-lyn/profile-editor.zip, extract it on your PC, then open editor/index.html.");
                reply(ctx.getSource(), "No website or sign-in needed. Edit, then Export to server > Copy import command. Use it in OP chat, or in the server console without the leading /."); return 1;
            })));
            dispatcher.register(command);
        });
    }
    private static boolean admin(CommandSourceStack source) { return source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER); }
    private static void closeMenus(CommandSourceStack source) {
        for (var player : source.getServer().getPlayerList().getPlayers()) if (player.containerMenu instanceof ProfileMenu) player.closeContainer();
    }
    private static void reply(CommandSourceStack source, String message) { source.sendSuccess(() -> Component.literal(message), false); }
    @FunctionalInterface private interface Command { int run() throws Exception; }
    private static int guarded(CommandSourceStack source, Command command) {
        try { return command.run(); }
        catch (Exception ex) { source.sendFailure(Component.literal("PROFILE-lyn: " + (ex.getMessage() == null ? "Action failed." : ex.getMessage()))); return 0; }
    }
}
