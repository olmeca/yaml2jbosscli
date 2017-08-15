package nl.olmeca;

import java.io.IOException;
import java.io.StringReader;
import java.util.Map;

public class Context {

    private Map<String, String> values;
    private final char tagStart = '<';
    private final char tagEnd = '>';

    public Context(Map<String, String> map) {
        this.values = map;
    }

    public String lookupValue(String keyPath) {
        return values.get(keyPath);
    }

    public String resolveTag (String tag) {
        // lookup the value in the values map
        // If not found we return the original <tag>
        String foundValue = lookupValue(tag);
        return foundValue == null ? tagStart + tag + tagEnd : foundValue;
    }

    public String resolve(String template) {
        StringBuilder result = new StringBuilder(100);
        StringBuilder tagBuffer = new StringBuilder(20);
        StringReader reader = new StringReader(template);
        boolean inTag = false;
        boolean foundTag = false;
        int letter;
        try {
            while ((letter = reader.read()) != -1) {
                switch (letter) {
                    case tagStart:
                        if (inTag) {
                            throw new IllegalStateException("Start tag inside tag in string '" + template + "'");
                        } else {
                            tagBuffer.setLength(0);
                            inTag = true;
                        }
                        break;
                    case tagEnd:
                        if (inTag) {
                            // resolve found tag
                            result.append(resolveTag(tagBuffer.toString()));
                            inTag = false;
                            foundTag = true;
                        } else {
                            throw new IllegalStateException("End tag without matching start tag in string '" + template + "'");
                        }
                        break;
                    default:
                        if (inTag)
                            tagBuffer.append((char)letter);
                        else
                            result.append((char)letter);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Throwing this exception to signal that no tag was found
        if (!foundTag)
            throw new IllegalArgumentException("No tag found.");
        return result.toString();
    }

}
