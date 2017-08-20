package nl.olmeca;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

public class Context {

    private Map<String, Object> values;
    private final char tagStart = '<';
    private final char tagEnd = '>';
    private final char keyPathSeparator = '.';
    StringBuilder result;
    StringBuilder tagBuffer;
    StringBuilder keyBuffer;
    Object currentObject = null;
    boolean inTag = false;
    boolean foundKey = false;
    int letter;

    public Context() {
        this.values = new HashMap<>();
    }

    public Context(Map<String, Object> map) {
        this();
        if (map != null)
            this.values = map;
    }


    public String resolve(String template) {
        result = new StringBuilder(1000);
        tagBuffer = new StringBuilder(100);
        keyBuffer = new StringBuilder(20);
        StringReader reader = new StringReader(template);
        currentObject = values;
        inTag = false;
        foundKey = false;
        try {
            while ((letter = reader.read()) != -1) {
                switch (letter) {
                    case tagStart:
                        if (inTag) {
                            throw new IllegalStateException("Placeholder start marker inside placeholder in string '" + template + "'");
                        } else {
                            tagBuffer.setLength(0);
                            keyBuffer.setLength(0);
                            currentObject = values;
                            inTag = true;
                        }
                        break;
                    case tagEnd:
                        if (inTag) {
                            checkKeyBuffer(template);
                            if (tagBuffer.length() > 0) {
                                navigateObjectByKey();
                            }
                            // resolve found tag
                            result.append(currentValue());
                            inTag = false;
                        } else {
                            throw new IllegalStateException("Placeholder end marker without matching start marker in string '" + template + "'");
                        }
                        break;
                    case keyPathSeparator:
                        checkKeyBuffer(template);
                        if (inTag) {
                            navigateObjectByKey();
                            tagBuffer.append((char)letter);
                        } else {
                            result.append((char)letter);
                        }
                        break;
                    default:
                        if (inTag) {
                            tagBuffer.append((char)letter);
                            keyBuffer.append((char)letter);
                        }
                        else
                            result.append((char)letter);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Throwing this exception to signal that no tag was found
        if (!foundKey)
            throw new NoTagsFound();
        return result.toString();
    }

    private String tagString() {
        return tagStart + tagBuffer.toString() + tagEnd;
    }

    private void checkKeyBuffer(String template) {
        if (keyBuffer.length() == 0)
            throw new IllegalStateException("Illegal placeholder: '" + tagString() + "' in string: '" + template + "'");
    }

    private String currentValue() {
        return currentObject == null || !foundKey
                ? tagString()
                : currentObject.toString();
    }

    private void navigateObjectByKey() {
        if (currentObject != null) {
            if (currentObject instanceof Map) {
                Map<String, Object> currentMap = (Map<String, Object>) currentObject;
                if (keyBuffer.length() > 0) {
                    String key = keyBuffer.toString();
                    if(currentMap.containsKey(key))
                        foundKey = true;
                    currentObject = (currentMap.get(key));
                    keyBuffer.setLength(0);
                }
            }
            else {
                throw new IllegalArgumentException("Context cannot resolve keypath longer than " + tagBuffer.toString());
            }
        }
    }

    public static class NoTagsFound extends RuntimeException {}
}
