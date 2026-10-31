package dev.octmc.profile;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.*;
import net.minecraft.server.level.ServerPlayer;
import java.util.regex.Pattern;

public final class ProfileText {
    private static final Pattern SCORE = Pattern.compile("\\{score:([A-Za-z0-9_.-]{1,64})}");
    private ProfileText() {}
    public static String replace(String text, ServerPlayer player, Layout layout) {
        String result = text.replace("{player}", player.getScoreboardName())
                .replace("{uuid}", player.getUUID().toString())
                .replace("{money}", ProfileEconomy.balance(player.getUUID()))
                .replace("{level}", Integer.toString(player.experienceLevel))
                .replace("{health}", Integer.toString((int) Math.ceil(player.getHealth())))
                .replace("{food}", Integer.toString(player.getFoodData().getFoodLevel()))
                .replace("{x}", Integer.toString(player.getBlockX()))
                .replace("{y}", Integer.toString(player.getBlockY()))
                .replace("{z}", Integer.toString(player.getBlockZ()))
                .replace("{online}", Integer.toString(player.level().getServer().getPlayerCount()));
        return SCORE.matcher(result).replaceAll(match -> java.util.regex.Matcher.quoteReplacement(score(player, match.group(1))));
    }
    private static String score(ServerPlayer player, String name) {
        var scoreboard = player.level().getServer().getScoreboard();
        var objective = scoreboard.getObjective(name);
        if (objective == null) return "Not configured";
        var score = scoreboard.getPlayerScoreInfo(player, objective);
        return Integer.toString(score == null ? 0 : score.value());
    }
    public static Component component(String raw) {
        MutableComponent result = Component.empty();
        Style style = Style.EMPTY.withItalic(false);
        StringBuilder segment = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) {
            if (raw.charAt(i) == '&' && i + 1 < raw.length()) {
                ChatFormatting format = ChatFormatting.getByCode(raw.charAt(i + 1));
                if (format != null) {
                    result.append(Component.literal(segment.toString()).setStyle(style)); segment.setLength(0);
                    style = format == ChatFormatting.RESET ? Style.EMPTY.withItalic(false)
                            : "0123456789abcdef".indexOf(Character.toLowerCase(raw.charAt(i + 1))) >= 0
                            ? Style.EMPTY.withItalic(false).applyFormat(format) : style.applyFormat(format);
                    i++; continue;
                }
            }
            segment.append(raw.charAt(i));
        }
        return result.append(Component.literal(segment.toString()).setStyle(style));
    }
}
