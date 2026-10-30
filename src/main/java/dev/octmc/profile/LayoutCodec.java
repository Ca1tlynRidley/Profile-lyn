package dev.octmc.profile;

import com.google.gson.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

public final class LayoutCodec {
    public static final int MAX_BYTES = 131072;
    public static final int MAX_CODE = 30000;
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Pattern ITEM = Pattern.compile("minecraft:[a-z0-9_./-]+");
    private static final Set<String> ACTIONS = Set.of("none", "close", "message", "command", "refresh");
    private LayoutCodec() {}

    public static Layout parse(String json) {
        if (json.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) throw invalid("Layout is too large (maximum 128 KiB).");
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            int version = integer(root, "schemaVersion", 1, 1, 1);
            String title = string(root, "title", "{player}'s Profile", 80);
            int rows = integer(root, "rows", 6, 1, 6);
            String filler = root.has("filler") && !root.get("filler").isJsonNull() ? item(string(root, "filler", "", 100)) : null;
            if (!root.has("items") || !root.get("items").isJsonArray()) throw invalid("items must be an array.");
            JsonArray entries = root.getAsJsonArray("items");
            if (entries.size() > rows * 9) throw invalid("Too many items for this inventory.");
            Set<Integer> slots = new HashSet<>();
            List<Layout.Entry> result = new ArrayList<>();
            for (JsonElement element : entries) {
                JsonObject e = element.getAsJsonObject();
                int slot = integer(e, "slot", -1, 0, rows * 9 - 1);
                if (!slots.add(slot)) throw invalid("Duplicate slot: " + slot);
                String id = item(string(e, "item", "", 100));
                int count = integer(e, "count", 1, 1, 64);
                String name = string(e, "name", "", 160);
                List<String> lore = new ArrayList<>();
                if (e.has("lore")) {
                    JsonArray lines = e.getAsJsonArray("lore");
                    if (lines.size() > 16) throw invalid("Use at most 16 lore lines per item.");
                    for (JsonElement line : lines) lore.add(text(line, "lore line", 256));
                }
                boolean glint = false;
                if (e.has("glint")) {
                    if (!e.get("glint").isJsonPrimitive() || !e.getAsJsonPrimitive("glint").isBoolean()) throw invalid("glint must be true or false.");
                    glint = e.get("glint").getAsBoolean();
                }
                JsonObject action = e.has("action") ? e.getAsJsonObject("action") : new JsonObject();
                String type = string(action, "type", "none", 16);
                if (!ACTIONS.contains(type)) throw invalid("Unknown action: " + type);
                String value = string(action, "value", "", 512);
                if (type.equals("command")) {
                    value = value.strip();
                    if (value.startsWith("/")) value = value.substring(1);
                    if (value.isBlank() || value.contains("\n") || value.contains("\r")) throw invalid("A command must be one non-empty line.");
                }
                result.add(new Layout.Entry(slot, id, count, name, lore, glint, new Layout.Action(type, value)));
            }
            return new Layout(version, title, rows, filler, result);
        } catch (IllegalArgumentException ex) { throw ex; }
        catch (RuntimeException ex) { throw invalid("Invalid layout JSON: " + ex.getMessage()); }
    }

    public static Layout decode(String code) {
        if (!code.startsWith("ps1:") || code.length() > MAX_CODE) throw invalid("Use a Profile Studio ps1: code (maximum 30,000 characters), or import profile.json.");
        try { return parse(new String(Base64.getUrlDecoder().decode(code.substring(4)), StandardCharsets.UTF_8)); }
        catch (IllegalArgumentException ex) { throw invalid("Invalid import code: " + ex.getMessage()); }
    }
    public static String encode(Layout layout) {
        return "ps1:" + Base64.getUrlEncoder().withoutPadding().encodeToString(new Gson().toJson(layout).getBytes(StandardCharsets.UTF_8));
    }
    private static String item(String id) {
        if (!ITEM.matcher(id).matches() || id.equals("minecraft:air")) throw invalid("Choose a vanilla Minecraft item: " + id);
        return id;
    }
    private static int integer(JsonObject object, String key, int fallback, int min, int max) {
        int value = fallback;
        if (object.has(key)) {
            JsonElement element = object.get(key);
            if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) throw invalid(key + " must be a whole number.");
            try { value = element.getAsBigDecimal().intValueExact(); }
            catch (ArithmeticException ex) { throw invalid(key + " must be a whole number."); }
        }
        if (value < min || value > max) throw invalid(key + " must be between " + min + " and " + max + ".");
        return value;
    }
    private static String string(JsonObject object, String key, String fallback, int max) {
        return object.has(key) ? text(object.get(key), key, max) : fallback;
    }
    private static String text(JsonElement value, String label, int max) {
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) throw invalid(label + " must be text.");
        String result = value.getAsString();
        if (result.length() > max || result.indexOf('\0') >= 0) throw invalid(label + " is too long or contains a null character.");
        return result;
    }
    private static IllegalArgumentException invalid(String message) { return new IllegalArgumentException(message); }
}
