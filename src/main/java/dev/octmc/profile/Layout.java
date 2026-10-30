package dev.octmc.profile;

import java.util.List;

public record Layout(int schemaVersion, String title, int rows, String filler, List<Entry> items) {
    public Layout { items = List.copyOf(items); }
    public record Entry(int slot, String item, int count, String name, List<String> lore,
                        boolean glint, Action action) {
        public Entry { lore = List.copyOf(lore); }
    }
    public record Action(String type, String value) {}
}
