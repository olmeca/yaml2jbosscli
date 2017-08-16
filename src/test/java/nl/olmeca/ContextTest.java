package nl.olmeca;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ContextTest {

    private Context context;
    private Map<String, Object> map;

    @BeforeEach
    public void initialize() {
        map = new HashMap<>();
        context = new Context(map);
    }

    @Test
    public void test_context_finds_existing_key() {
        map.put("x", "one");
        String resolved = context.resolve("zo <x> test");
        assertEquals("zo one test", resolved);
    }

    @Test public void test_context_finds_existing_keys() {
        map.put("een", "one");
        map.put("twee", "two");
        String resolved = context.resolve("zo <een> en <twee> test");
        assertEquals("zo one en two test", resolved);
    }

    @Test public void test_context_returns_original_if_no_key_found() {
        String resolved = context.resolve("zo <een> test");
        assertEquals("zo <een> test", resolved);
    }

    @Test public void test_context_returns_original_if_no_full_tag() {
        assertThrows(Context.NoTagsFound.class, () -> {
            context.resolve("zo <een test");
        });
    }

    @Test
    public void test_context_finds_existing_keypath() {
        map.put("x", "one");
        Map<String, Object> sub = new HashMap<>();
        sub.put("bar", "zet");
        map.put("foo", sub);
        String resolved = context.resolve("zo <foo.bar> test");
        assertEquals("zo zet test", resolved);
    }


}
