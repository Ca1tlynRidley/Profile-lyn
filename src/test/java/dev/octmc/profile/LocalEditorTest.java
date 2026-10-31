package dev.octmc.profile;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.*;

class LocalEditorTest {
    @TempDir Path temporary;

    private Layout layout(String title) {
        return new Layout(1, title, 1, null, List.of(new Layout.Entry(0, "minecraft:emerald", 1,
                "&aWallet", List.of("&7Balance: {money}"), false, new Layout.Action("none", ""))));
    }

    private Layout snapshot(Path editor) throws IOException {
        String script = Files.readString(editor.resolve("server-layout.js"));
        String prefix = "globalThis.PROFILE_SERVER_LAYOUT = ";
        assertTrue(script.startsWith(prefix));
        assertTrue(script.endsWith(";\n"));
        return LayoutCodec.parse(script.substring(prefix.length(), script.length() - 2));
    }

    @Test void extractsEveryBundledAssetAndTheCurrentLayoutAutomatically() throws Exception {
        Layout current = layout("Lyn ♥ 你好 \"quoted\"");
        Path index = new LocalEditor(temporary).export(current);
        assertEquals(temporary.resolve("editor/index.html"), index);
        assertEquals(current, snapshot(index.getParent()));
        List<String> assets;
        try (var manifest = Objects.requireNonNull(getClass().getResourceAsStream("/profile-studio-editor/editor-assets.txt"))) {
            assets = new String(manifest.readAllBytes(), StandardCharsets.UTF_8).lines().toList();
        }
        assertTrue(assets.size() > 1000);
        for (String asset : assets) {
            Path extracted = index.getParent().resolve(asset);
            assertTrue(Files.isRegularFile(extracted), asset);
            if (!asset.equals("server-layout.js")) {
                try (var original = Objects.requireNonNull(getClass().getResourceAsStream("/profile-studio-editor/" + asset))) {
                    assertArrayEquals(original.readAllBytes(), Files.readAllBytes(extracted), asset);
                }
            }
        }
        assertTrue(Files.isRegularFile(index.getParent().resolve("icons/emerald.png")));
        assertTrue(Files.isRegularFile(index.getParent().resolve("OPEN-EDITOR.txt")));
        try (var archive = new ZipFile(temporary.resolve("profile-editor.zip").toFile())) {
            assertEquals(assets.size(), archive.size());
            assertNotNull(archive.getEntry("editor/index.html"));
            assertNotNull(archive.getEntry("editor/icons/emerald.png"));
            try (var script = archive.getInputStream(archive.getEntry("editor/server-layout.js"))) {
                assertArrayEquals(Files.readAllBytes(index.getParent().resolve("server-layout.js")), script.readAllBytes());
            }
        }
    }

    @Test void aNewServerStartUpdatesAssetsAndPreservesServerConfigAndExtraFiles() throws Exception {
        Path index = new LocalEditor(temporary).export(layout("First layout"));
        Files.writeString(temporary.resolve("profile.json"), "keep the server config");
        Files.writeString(index.getParent().resolve("my-downloaded-layout.json"), "keep my download");
        Files.writeString(index.getParent().resolve("app.js"), "old editor version");
        Layout next = layout("Next layout");
        new LocalEditor(temporary).export(next);
        assertEquals(next, snapshot(index.getParent()));
        assertEquals("keep the server config", Files.readString(temporary.resolve("profile.json")));
        assertEquals("keep my download", Files.readString(index.getParent().resolve("my-downloaded-layout.json")));
        assertTrue(Files.readString(index.getParent().resolve("app.js")).contains("PROFILE_SERVER_LAYOUT"));
        try (var archive = new ZipFile(temporary.resolve("profile-editor.zip").toFile())) {
            assertNull(archive.getEntry("profile.json"));
            assertNull(archive.getEntry("editor/my-downloaded-layout.json"));
        }
    }

    @Test void missingBundledAssetsGiveAnActionableFailure() {
        var editor = new LocalEditor(temporary, name -> name.endsWith("/app.js") ? null : getClass().getResourceAsStream(name));
        var failure = assertThrows(IOException.class, () -> editor.export(layout("Test")));
        assertTrue(failure.getMessage().contains("Missing bundled editor file: app.js"));
    }

    @Test void invalidManifestCannotOverwriteFilesOutsideTheEditorFolder() throws Exception {
        Files.writeString(temporary.resolve("profile.json"), "do not change");
        String manifest = "index.html\napp.js\ncore.js\ncatalog.js\ndefault.js\nserver-layout.js\nstyle.css\n../profile.json\n";
        var editor = new LocalEditor(temporary, name -> name.endsWith("/editor-assets.txt")
                ? new ByteArrayInputStream(manifest.getBytes(StandardCharsets.UTF_8)) : getClass().getResourceAsStream(name));
        assertThrows(IOException.class, () -> editor.export(layout("Test")));
        assertEquals("do not change", Files.readString(temporary.resolve("profile.json")));
    }

    @Test void theCommandCanRecreateADeletedIndexAndRefreshTheLayout() throws Exception {
        var editor = new LocalEditor(temporary);
        Path index = editor.export(layout("First"));
        Files.delete(index);
        Layout next = layout("Second");
        assertEquals(index, editor.export(next));
        assertTrue(Files.isRegularFile(index));
        assertEquals(next, snapshot(index.getParent()));
    }

    @Test void aFailedSnapshotWriteKeepsThePreviousSnapshot() throws Exception {
        var editor = new LocalEditor(temporary);
        Layout first = layout("First");
        Path index = editor.export(first);
        Files.createDirectory(index.getParent().resolve("server-layout.js.tmp"));
        assertThrows(IOException.class, () -> editor.export(layout("Second")));
        assertEquals(first, snapshot(index.getParent()));
    }

    @Test void largeLayoutsOpenFromTheSnapshotWithoutAChatOrUrlLengthLimit() throws Exception {
        var entries = IntStream.range(0, 54).mapToObj(slot -> new Layout.Entry(slot, "minecraft:stone", 1,
                "Item " + slot, Collections.nCopies(16, "x".repeat(100)), false, new Layout.Action("none", ""))).toList();
        Layout large = new Layout(1, "Large profile", 6, null, entries);
        assertTrue(LayoutCodec.encode(large).length() > LayoutCodec.MAX_CODE);
        assertEquals(large, snapshot(new LocalEditor(temporary).export(large).getParent()));
    }
}
