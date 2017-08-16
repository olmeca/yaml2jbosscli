package nl.olmeca;


import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ContextTest {

    private Context context;

    @Test
    public void test_context_finds_existing_key() {
        Map<String, String> map = new HashMap<>();
        map.put("een", "one");
        context = new Context(map);
        String resolved = context.resolve("zo <een> test");
        assertEquals("zo one test", resolved);
    }

    @Test public void test_context_finds_existing_keys() {
        Map<String, String> map = new HashMap<>();
        map.put("een", "one");
        map.put("twee", "two");
        context = new Context(map);
        String resolved = context.resolve("zo <een> en <twee> test");
        assertEquals("zo one en two test", resolved);
    }
}
