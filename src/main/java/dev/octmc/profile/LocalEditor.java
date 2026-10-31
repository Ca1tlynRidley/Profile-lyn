package dev.octmc.profile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Extracts the bundled, standalone editor; no HTTP server or external hosting. */
final class LocalEditor {
    private static final String RESOURCES = "/profile-studio-editor/";
    private final Path directory;
    private final Function<String, InputStream> resources;
    private List<String> assets;
    private boolean prepared;

    LocalEditor(Path configDirectory) {
        this(configDirectory, LocalEditor.class::getResourceAsStream);
    }

    LocalEditor(Path configDirectory, Function<String, InputStream> resources) {
        this.directory = configDirectory.resolve("editor").toAbsolutePath().normalize();
        this.resources = resources;
    }

    Path export(Layout layout) throws IOException {
        if (!prepared || !Files.isRegularFile(directory.resolve("index.html"), LinkOption.NOFOLLOW_LINKS)) {
            extractAssets();
        }
        Path snapshot = destination("server-layout.js");
        Path temporary = destination("server-layout.js.tmp");
        Files.writeString(temporary, "globalThis.PROFILE_SERVER_LAYOUT = " + LayoutCodec.GSON.toJson(layout) + ";\n",
                StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS);
        try { Files.move(temporary, snapshot, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
        catch (AtomicMoveNotSupportedException ex) { Files.move(temporary, snapshot, StandardCopyOption.REPLACE_EXISTING); }
        writeArchive();
        return directory.resolve("index.html");
    }

    private void extractAssets() throws IOException {
        try (var manifest = open("editor-assets.txt")) {
            assets = new String(manifest.readAllBytes(), StandardCharsets.UTF_8).lines().filter(line -> !line.isBlank()).toList();
        }
        if (!assets.containsAll(List.of("index.html", "app.js", "core.js", "catalog.js", "default.js", "server-layout.js", "style.css"))) {
            throw new IOException("The bundled editor is incomplete. Reinstall the PROFILE-lyn JAR.");
        }
        for (String asset : assets) {
            Path target = destination(asset);
            try (var input = open(asset)) { Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING); }
        }
        prepared = true;
    }

    private void writeArchive() throws IOException {
        Path archive = directory.getParent().resolve("profile-editor.zip");
        Path temporary = archive.resolveSibling("profile-editor.zip.tmp");
        if (Files.isSymbolicLink(archive) || Files.isSymbolicLink(temporary)) {
            throw new IOException("The editor archive must not be a symbolic link.");
        }
        try (var output = new ZipOutputStream(Files.newOutputStream(temporary, StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS))) {
            for (String asset : assets) {
                output.putNextEntry(new ZipEntry("editor/" + asset));
                Files.copy(destination(asset), output);
                output.closeEntry();
            }
        }
        try { Files.move(temporary, archive, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
        catch (AtomicMoveNotSupportedException ex) { Files.move(temporary, archive, StandardCopyOption.REPLACE_EXISTING); }
    }

    private InputStream open(String asset) throws IOException {
        InputStream input = resources.apply(RESOURCES + asset);
        if (input == null) throw new IOException("Missing bundled editor file: " + asset);
        return input;
    }

    private Path destination(String asset) throws IOException {
        if (!asset.matches("(?:[A-Za-z0-9][A-Za-z0-9_.-]*/)*[A-Za-z0-9][A-Za-z0-9_.-]*")) {
            throw new IOException("Invalid bundled editor path: " + asset);
        }
        Path target = directory.resolve(asset).normalize();
        if (!target.startsWith(directory) || target.equals(directory)) throw new IOException("Editor path escapes its folder.");
        // Reject redirected output paths so extraction never follows links outside the editor folder.
        Path folder = directory;
        if (Files.isSymbolicLink(folder)) throw new IOException("The editor folder must not be a symbolic link.");
        Files.createDirectories(folder);
        for (Path part : directory.relativize(target.getParent())) {
            folder = folder.resolve(part);
            if (Files.isSymbolicLink(folder)) throw new IOException("Editor assets must not use symbolic links.");
            Files.createDirectories(folder);
        }
        if (Files.isSymbolicLink(target)) throw new IOException("Editor assets must not use symbolic links.");
        return target;
    }
}
