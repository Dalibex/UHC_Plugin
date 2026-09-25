package me.dalibex.UHC_DBasic;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceIntegrityTest {
    private static final Pattern PLACEHOLDER = Pattern.compile("%[^%\\s]+%");
    private static final List<String> LANGUAGE_CODES = List.of("es", "en", "fr", "de", "it");

    @Test void languageLeafKeysTypesAndPlaceholdersMatch() throws IOException {
        Map<String, Object> baseline = flatten(loadFile("src/main/resources/lang/messages_es.yml"));
        for (String languageCode : LANGUAGE_CODES) {
            Map<String, Object> current = flatten(loadFile("src/main/resources/lang/messages_" + languageCode + ".yml"));
            assertEquals(baseline.keySet(), current.keySet(), languageCode);
            for (String key : baseline.keySet()) {
                assertEquals(baseline.get(key).getClass(), current.get(key).getClass(), languageCode + ":" + key);
                assertEquals(placeholders(baseline.get(key)), placeholders(current.get(key)), languageCode + ":" + key);
            }
        }
    }

    @Test void pluginDescriptorDeclaresCommandsDependenciesAndExpandedVersion() throws IOException {
        Map<String, Object> source = loadFile("src/main/resources/plugin.yml");
        assertEquals("${version}", source.get("version"));
        assertTrue(source.get("depend") instanceof List<?>);
        assertEquals(Set.of("TAB", "SkinsRestorer"), new LinkedHashSet<>((List<?>) source.get("depend")));
        assertEquals(Set.of("uhcadmin", "uhccommands", "start", "confirmstart", "cancelstart",
                        "reset", "team", "settime", "lang", "assignteam", "abandon", "setteamepisode", "setpvpepisode"),
                ((Map<?, ?>) source.get("commands")).keySet());

        try (InputStream stream = getClass().getClassLoader().getResourceAsStream("plugin.yml")) {
            assertNotNull(stream);
            Map<String, Object> processed = new Yaml().load(stream);
            String version = String.valueOf(processed.get("version"));
            assertFalse(version.contains("${"));
            assertTrue(version.matches("\\d+\\.\\d+\\.\\d+(?:[-+].+)?"), version);
        }
    }

    private static Map<String, Object> loadFile(String path) throws IOException {
        try (InputStream stream = Files.newInputStream(Path.of(path))) {
            return new Yaml().load(stream);
        }
    }

    private static Map<String, Object> flatten(Map<String, Object> root) {
        Map<String, Object> leaves = new LinkedHashMap<>();
        flattenInto("", root, leaves);
        return leaves;
    }

    private static void flattenInto(String prefix, Map<?, ?> values, Map<String, Object> leaves) {
        for (Map.Entry<?, ?> entry : values.entrySet()) {
            String key = prefix.isEmpty() ? String.valueOf(entry.getKey()) : prefix + "." + entry.getKey();
            if (entry.getValue() instanceof Map<?, ?> nested) flattenInto(key, nested, leaves);
            else leaves.put(key, entry.getValue());
        }
    }

    private static List<Set<String>> placeholders(Object value) {
        List<?> values = value instanceof List<?> list ? list : List.of(value);
        List<Set<String>> result = new ArrayList<>();
        for (Object item : values) {
            Set<String> found = new LinkedHashSet<>();
            Matcher matcher = PLACEHOLDER.matcher(String.valueOf(item));
            while (matcher.find()) found.add(matcher.group());
            result.add(found);
        }
        return result;
    }
}
