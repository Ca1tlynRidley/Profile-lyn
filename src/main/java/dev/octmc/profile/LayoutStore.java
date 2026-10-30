package dev.octmc.profile;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Objects;

public final class LayoutStore {
    private final Path file;
    private Layout current;
    public LayoutStore(Path directory) { file = directory.resolve("profile.json"); }
    public Layout current() { return Objects.requireNonNull(current, "Profile layout is unavailable. Check the server log."); }
    public Path file() { return file; }
    public void load() throws IOException {
        Files.createDirectories(file.getParent());
        if (!Files.exists(file)) {
            try (var stream = Objects.requireNonNull(getClass().getResourceAsStream("/default-profile.json"))) {
                save(LayoutCodec.parse(new String(stream.readAllBytes(), StandardCharsets.UTF_8)));
            }
        } else {
            if (Files.size(file) > LayoutCodec.MAX_BYTES) throw new IllegalArgumentException("profile.json exceeds 128 KiB.");
            Layout candidate = LayoutCodec.parse(Files.readString(file));
            validateItems(candidate);
            current = candidate;
        }
    }
    public void save(Layout candidate) throws IOException {
        validateItems(candidate);
        Files.createDirectories(file.getParent());
        Path temporary = file.resolveSibling("profile.json.tmp");
        Files.writeString(temporary, LayoutCodec.GSON.toJson(candidate), StandardCharsets.UTF_8);
        if (Files.exists(file)) Files.copy(file, file.resolveSibling("profile.json.bak"), StandardCopyOption.REPLACE_EXISTING);
        try { Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
        catch (AtomicMoveNotSupportedException ex) { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING); }
        current = candidate;
    }
    private static void validateItems(Layout layout) {
        if (layout.filler() != null) validateItem(layout.filler());
        for (Layout.Entry entry : layout.items()) validateItem(entry.item());
    }
    private static void validateItem(String item) {
        if (!BuiltInRegistries.ITEM.containsKey(Identifier.parse(item))) throw new IllegalArgumentException("Unknown Minecraft 26.2 item: " + item);
    }
}
