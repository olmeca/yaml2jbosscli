package nl.olmeca;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

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
    boolean foundTag = false;
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
        foundTag = false;
        try {
            while ((letter = reader.read()) != -1) {
                switch (letter) {
                    case tagStart:
                        if (inTag) {
                            throw new IllegalStateException("Start tag inside tag in string '" + template + "'");
                        } else {
                            tagBuffer.setLength(0);
                            keyBuffer.setLength(0);
                            currentObject = values;
                            inTag = true;
                        }
                        break;
                    case tagEnd:
                        if (inTag) {
                            // resolve found tag
                            handleKey();
                            result.append(currentObjectString());
                            inTag = false;
                            foundTag = true;
                        } else {
                            throw new IllegalStateException("End tag without matching start tag in string '" + template + "'");
                        }
                        break;
                    case keyPathSeparator:
                        if (inTag) {
                            handleKey();
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
        if (!foundTag)
            throw new NoTagsFound();
        return result.toString();
    }

    private String currentObjectString() {
        return currentObject == null ? (tagStart + tagBuffer.toString() + tagEnd) : currentObject.toString();
    }

    private void handleKey() {
        if (currentObject != null) {
            if (currentObject instanceof Map) {
                String key = keyBuffer.toString();
                currentObject = ((Map<String, Object>) currentObject).get(key);
                keyBuffer.setLength(0);
            }
            else {
                throw new IllegalArgumentException("Context cannot resolve keypath longer than " + tagBuffer.toString());
            }
        }
    }

    public static class NoTagsFound extends RuntimeException {}
}
