package nl.olmeca;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommandLine {

    private List<String> indexedParams;
    private Map<Character, String> namedParams;
    private Map<String, Object> contextParams;

    public CommandLine(String[] arguments) {
        namedParams = new HashMap<>();
        contextParams = new HashMap<>();
        indexedParams = new ArrayList<>();
        Character paramName = null;
        for (String item: arguments) {
            if (paramName == null) {
                if (item.length() == 2 && item.charAt(0) == '-') {
                    paramName = item.charAt(1);
                }
                else if (item.length() > 3 && item.startsWith("--")) {
                    System.out.println("Command line context param: " + item);
                    int separatorIndex = item.indexOf('=');
                    String key = item.substring(2, separatorIndex);
                    // check if the '=' is at the end of the string
                    String value = separatorIndex == item.length() - 1 ? "" : item.substring(separatorIndex + 1);
                    System.out.println("key: " + key + ", value: " + value);
                    contextParams.put(key, value);
                }
                else {
                    indexedParams.add(item);
                }
            }
            else {
                namedParams.put(paramName, item);
                paramName = null;
            }
        }
    }

    public List<String> getIndexedParams() {
        return indexedParams;
    }

    public Map<Character, String> getNamedParams() {
        return namedParams;
    }

    public Map<String, Object> getContextParams() {
        return contextParams;
    }
}
