package dev.octmc.profile;

import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import static org.junit.jupiter.api.Assertions.*;

class LayoutCodecTest {
    private String layout(String items) { return "{\"schemaVersion\":1,\"rows\":6,\"items\":"+items+"}"; }
    @Test void defaultTemplateAndExportCodeRoundTrip() throws Exception {
        String json;
        try(var input=Objects.requireNonNull(getClass().getResourceAsStream("/default-profile.json"))){json=new String(input.readAllBytes(),StandardCharsets.UTF_8);}
        Layout parsed=LayoutCodec.parse(json);
        assertEquals(parsed,LayoutCodec.decode(LayoutCodec.encode(parsed)));
        assertTrue(parsed.items().stream().anyMatch(e->e.item().equals("minecraft:emerald")&&e.lore().stream().anyMatch(l->l.contains("{money}"))));
    }
    @Test void rejectsDuplicateAndOutOfBoundsSlots() {
        assertThrows(IllegalArgumentException.class,()->LayoutCodec.parse(layout("[{\"slot\":54,\"item\":\"minecraft:emerald\"}]")));
        assertThrows(IllegalArgumentException.class,()->LayoutCodec.parse(layout("[{\"slot\":1,\"item\":\"minecraft:emerald\"},{\"slot\":1,\"item\":\"minecraft:barrier\"}]")));
    }
    @Test void rejectsFractionalNumbersCustomItemsAndPrivilegedActionTypes() {
        for(String entry:new String[]{"{\"slot\":1.5,\"item\":\"minecraft:emerald\"}","{\"slot\":1,\"item\":\"mod:emerald\"}","{\"slot\":1,\"item\":\"minecraft:air\"}","{\"slot\":1,\"item\":\"minecraft:emerald\",\"action\":{\"type\":\"console\"}}"})
            assertThrows(IllegalArgumentException.class,()->LayoutCodec.parse(layout("["+entry+"]")));
    }
    @Test void rejectsCommandInjectionLinesAndStripsLeadingSlash() {
        assertThrows(IllegalArgumentException.class,()->LayoutCodec.parse(layout("[{\"slot\":1,\"item\":\"minecraft:emerald\",\"action\":{\"type\":\"command\",\"value\":\"balance\\nop Steve\"}}]")));
        var parsed=LayoutCodec.parse(layout("[{\"slot\":1,\"item\":\"minecraft:emerald\",\"action\":{\"type\":\"command\",\"value\":\"/balance\"}}]"));
        assertEquals("balance",parsed.items().getFirst().action().value());
    }
    @Test void unicodeNamesSurviveImport() {
        var parsed=LayoutCodec.parse("{\"title\":\"Lyn ♥ 你好\",\"items\":[]}");
        assertEquals(parsed,LayoutCodec.decode(LayoutCodec.encode(parsed)));
    }
    @Test void enforcesSizeAndVersionLimits() {
        assertThrows(IllegalArgumentException.class,()->LayoutCodec.decode("ps1:"+"a".repeat(30001)));
        assertThrows(IllegalArgumentException.class,()->LayoutCodec.parse("{\"schemaVersion\":2,\"items\":[]}"));
        assertThrows(IllegalArgumentException.class,()->LayoutCodec.parse(" ".repeat(LayoutCodec.MAX_BYTES+1)));
    }
    @Test void emptyLayoutReallyHasNoFillerOrItems() {
        Layout parsed=LayoutCodec.parse("{\"rows\":1,\"filler\":null,\"items\":[]}");
        assertNull(parsed.filler());assertTrue(parsed.items().isEmpty());
    }
}
