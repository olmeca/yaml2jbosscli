package nl.olmeca;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommandLine {

    private List<String> indexedParams;
    private Map<Character, String> namedParams;

    public CommandLine(String[] arguments) {
        namedParams = new HashMap<>();
        indexedParams = new ArrayList<>();
        Character paramName = null;
        for (String item: arguments) {
            if (paramName == null) {
                if (item.length() == 2 && item.charAt(0) == '-') {
                    paramName = item.charAt(1);
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
}
