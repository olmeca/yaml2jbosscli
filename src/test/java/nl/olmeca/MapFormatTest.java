package nl.olmeca;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MapFormatTest {

    private MapFormat context;
    private Map<String, Object> map;

    @BeforeEach
    public void initialize() {
        map = new HashMap<>();
        context = new MapFormat(map);
    }

    @Test
    public void test_context_finds_existing_key() {
        map.put("x", "one");
        String resolved = context.format("zo <x> test");
        assertEquals("zo one test", resolved);
    }

    @Test public void test_context_finds_existing_keys() {
        map.put("een", "one");
        map.put("twee", "two");
        String resolved = context.format("zo <een> en <twee> test");
        assertEquals("zo one en two test", resolved);
    }

    @Test public void test_context_returns_original_if_no_key_found() {
        map.put("twee", "two");
        String resolved = context.format("zo <een> en <twee> test");
        assertEquals("zo <een> en two test", resolved);
    }

    @Test public void test_context_leaves_empty_tag_untouched() {
        map.put("twee", "two");
        String resolved = context.format("zo <> en <twee> test");
        assertEquals("zo <> en two test", resolved);
    }

    @Test public void test_context_throws_exception_if_empty_tag() {
        assertThrows(MapFormat.NoTagsResolved.class, () -> {
            context.format("zo <> test");
        });
    }

    @Test public void test_context_throws_exception_if_empty_key_at_end() {
        assertThrows(IllegalStateException.class, () -> {
            context.format("zo <my.> test");
        });
    }

    @Test public void test_context_throws_exception_if_empty_key_at_start() {
        assertThrows(IllegalStateException.class, () -> {
            context.format("zo <.my> test");
        });
    }

    @Test public void test_context_returns_original_if_no_full_tag() {
        assertThrows(MapFormat.NoTagsResolved.class, () -> {
            context.format("zo <een test");
        });
    }

    @Test
    public void test_context_finds_existing_keypath() {
        map.put("x", "one");
        Map<String, Object> sub = new HashMap<>();
        sub.put("bar", "zet");
        map.put("foo", sub);
        String resolved = context.format("zo <foo.bar> test");
        assertEquals("zo zet test", resolved);
    }

    @Test public void test_read_param_replaces_key() {
        map.put("een", "one");
        map.put("twee", "two");
        String resolved = context.format("zo <een> en <twee> test");
        assertEquals("zo one en two test", resolved);
        context.readNamedParam("twee", "three");
        resolved = context.format("zo <een> en <twee> test");
        assertEquals("zo one en three test", resolved);
    }

    @Test
    public void test_read_param_replaces_keypath() {
        map.put("x", "one");
        Map<String, Object> sub = new HashMap<>();
        sub.put("bar", "zet");
        map.put("foo", sub);
        context.readNamedParam("foo.bar", "three");
        assertEquals("three", context.format("<foo.bar>"));
    }

    @Test
    public void test_read_param_adds_keypath_if_absent() {
        context.readNamedParam("foo.bar", "three");
        assertEquals("three", context.format("<foo.bar>"));
    }

    @Test public void test_read_param_replaces_recursively() {
        map.put("een", "one");
        map.put("twee", "<een>");
        map.put("drie", "<twee>");
        map.put("vier", 4);
        String resolved = context.format("zo <een> en <drie> <vier> test");
        assertEquals("zo one en one 4 test", resolved);
    }


}
