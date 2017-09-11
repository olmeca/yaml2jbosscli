package nl.olmeca;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

public class MapFormat {

    private Map<String, Object> context;
    private final char tagStart = '<';
    private final char tagEnd = '>';
    private final char keyPathSeparator = '.';
    StringBuilder result;
    StringBuilder tagBuffer;
    StringBuilder keyBuffer;
    Object currentObject = null;
    boolean inTag = false;
    boolean foundKey = false;

    public MapFormat() {
        this.context = new HashMap<>();
    }

    public MapFormat(Map<String, Object> map) {
        this();
        if (map != null)
            this.context = map;
        result = new StringBuilder(1000);
        tagBuffer = new StringBuilder(100);
        keyBuffer = new StringBuilder(20);
    }

    private void reset() {
        result.setLength(0);
        inTag = false;
        foundKey = false;
        resetPlaceholderState();
    }

    private void resetPlaceholderState() {
        tagBuffer.setLength(0);
        keyBuffer.setLength(0);
        currentObject = context;
    }

    public void readNamedParam(String keyPath, Object value) {
        String[] keys = keyPath.split("\\.");
        if (keys.length == 0) {
            context.put(keyPath, value);
        }
        else {
            // navigate down the contextParam map, using the keys
            Map<String, Object> cursor = context;
            for (int i = 0; i < keys.length-1; i++) {
                Map<String, Object> subContext = (Map) cursor.get(keys[i]);
                // if not yet there, then create it
                if (subContext == null) {
                    subContext = new HashMap<>();
                    cursor.put(keys[i], subContext);
                }
                cursor = subContext;
            }
            cursor.put(keys[keys.length-1], value);
        }
    }



    public String format(String template) {
        reset();
        StringReader reader = new StringReader(template);
        try {
            int lastReadChar;
            while ((lastReadChar = reader.read()) != -1) {
                switch (lastReadChar) {
                    case tagStart:
                        if (inTag) {
                            throw new IllegalStateException("Placeholder start marker inside placeholder in string '" + template + "'");
                        } else {
                            resetPlaceholderState();
                            inTag = true;
                        }
                        break;
                    case tagEnd:
                        if (inTag) {
                            if (tagBuffer.length() > 0) {
                                checkKeyBuffer(template);
                                navigateObjectByKey();
                                // format found tag
                                appendContent(currentValue());
                            }
                            else {
                                // Found '<>', keeping it in result
                                appendContent(tagString());
                            }
                            inTag = false;
                        } else {
                            throw new IllegalStateException("Placeholder end marker without matching start marker in string '" + template + "'");
                        }
                        break;
                    case keyPathSeparator:
                        if (inTag) {
                            checkKeyBuffer(template);
                            navigateObjectByKey();
                            tagBuffer.append((char)lastReadChar);
                        } else {
                            appendContent((char)lastReadChar);
                        }
                        break;
                    default:
                        appendContent((char) lastReadChar);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Throwing this exception to signal that no tag was found
        if (!foundKey)
            throw new NoTagsResolved();
        return content();
    }

    private String content() {
        return result.toString();
    }

    private void appendContent(String value) {
        result.append(value);
    }

    private void appendContent(char theChar) {
        if (inTag) {
            tagBuffer.append(theChar);
            keyBuffer.append(theChar);
        }
        else
            result.append(theChar);
    }

    private String tagString() {
        return tagStart + tagBuffer.toString() + tagEnd;
    }

    private void checkKeyBuffer(String template) {
        if (keyBuffer.length() == 0)
            throw new IllegalStateException("Illegal placeholder: '" + tagString() + "' in string: '" + template + "'");
    }

    private String currentValue() {
        if (currentObject == null || !foundKey)
            return tagString();

        // If the current value is an string containing placeholder then recurse
        if (currentObject instanceof String && ((String) currentObject).indexOf(tagStart) != -1) {
            return new MapFormat(context).format((String) currentObject);
        }
        else {
            return currentObject.toString();
        }
    }

    private void navigateObjectByKey() {
        if (currentObject != null) {
            if (currentObject instanceof Map) {
                Map<String, Object> currentMap = (Map<String, Object>) currentObject;
                if (keyBuffer.length() > 0) {
                    String key = keyBuffer.toString();
                    if(currentMap.containsKey(key)) {
                        foundKey = true;
                    }
                    currentObject = (currentMap.get(key));
                    keyBuffer.setLength(0);
                }
            }
            else {
                throw new IllegalArgumentException("Context cannot format keypath longer than " + tagBuffer.toString());
            }
        }
    }

    public static class NoTagsResolved extends RuntimeException {}
}
