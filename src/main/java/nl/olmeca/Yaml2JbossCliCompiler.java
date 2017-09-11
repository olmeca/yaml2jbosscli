package nl.olmeca;

import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Yaml2JbossCliCompiler
 *
 */
public class Yaml2JbossCliCompiler
{
    private static final String CMD_LABEL = "cmd";
    private static final String ARGS_LABEL = "args";
    private static final Pattern INT_MATCHER =  Pattern.compile("[0-9]+[lL]?");
    private static final char PARAM_OUTPUTFILE = 'o';
    private static final char PARAM_PARAMFILE = 'c';

    MapFormat context;


    public Yaml2JbossCliCompiler(MapFormat context) throws IOException {
        this.context = context;
    }

    public void processFiles(List<String> files, String outputFile) throws IOException {
        OutputStream outputStream = outputFile == null ? System.out : new FileOutputStream(outputFile);
        processFiles(files, outputStream);
    }

    public void processFiles(List<String> files, OutputStream outputStream) throws IOException {
        try (OutputStreamWriter out = new OutputStreamWriter(outputStream, "UTF-8")) {
            files.stream().forEach(filePath -> processFile(new File(filePath), out));
        }
    }

    private void processFile(File file, OutputStreamWriter writer) {
        try {
            Yaml yaml = new Yaml();
            List<Object> commands =  yaml.load(new FileReader(file));
            processCommands(commands, writer);
        } catch (IOException e) {
            System.out.println("Error reading file: " + file);
        }
    }

    public void processCommands(String commandsString, OutputStreamWriter writer) {
        try {
            Yaml yaml = new Yaml();
            List<Object> commands =  yaml.load(commandsString);
            processCommands(commands, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void processCommands(List<Object> commands, OutputStreamWriter writer) throws IOException {
        for (Object command: commands)
            writeJbossCliCommand(writer, command, new ArrayList<>());
    }

    private String resolve(String value) {
        return context == null ? value : context.format(value);
    }

    private List<String> plus(List<String> list, String item) {
        List<String> newPath = new ArrayList<>(list);
        newPath.add(item);
        return newPath;
    }

    private void writeJbossCliCommand(OutputStreamWriter writer, Object object, List<String> path) throws IOException {
        if (object instanceof List)
            writeJbossCliCommand(writer, (List) object, path);
        else if (object instanceof Map)
            writeJbossCliCommand(writer, (Map) object, path);
    }

    private void writeJbossCliCommand (OutputStreamWriter writer, List<Object> commands, List<String> path) throws IOException {
        for (Object command: commands)
            if (isCommand(command))
                writeCommand(writer, (Map)command, path);
            else
                writeJbossCliCommand(writer, command, path);
    }

    private void writeCommand(OutputStreamWriter writer, Map command, List<String> path) throws IOException {
        for (String item: path) {
            writer.write("/" + item);
        }
        writer.write(serializeCommand(command));
    }

    private String serializeCommand(Map<String, Object> command) {
        Object args = command.get(ARGS_LABEL);
        String argString  = args == null ? "" : serializeArguments((Map) args);
        return ":" + command.get(CMD_LABEL) + argString + "\n";
    }

    private String serializeArguments(Map<String, Object> arguments) {
        return "(" + serializeMap(arguments) + ")";
    }

    private String serializeMap(Map<String, Object> theMap) {
        List<String> argStrings = theMap.entrySet().stream().map(entry ->
                entry.getKey() + "=" + serialize(entry.getValue())).collect(Collectors.toList()
        );
        return String.join(",", argStrings);
    }

    private String quoteIfRealString(String value, boolean quote) {
        if (    !quote ||
                "true".equals(value) ||
                "false".equals(value) ||
                INT_MATCHER.matcher(value).matches()
                )
            return value;
        else
            return "\"" + value + "\"";
    }

    private String serializeList(List<Object> list) {
        List<String> itemStrings = list.stream().map(item -> serialize(item)).collect(Collectors.toList());
        return String.join(",", itemStrings);
    }

    private String serializeString(String string, boolean quote) {
        // Hack to only call myself on the resolved value if anything was resolved
        String result = string;
        try {
            result = resolve(result);
            // recurse only if no exception was thrown
            return serialize(result);
        } catch (MapFormat.NoTagsResolved ntf) {
            return quoteIfRealString(result, quote);
        }
    }

    private String serializeKey(String key) {
        String resolvedKey = null;
        try {
            resolvedKey = resolve(key);
        } catch (MapFormat.NoTagsResolved ntf) {
            resolvedKey = key;
        }
        return cliName(resolvedKey);
    }

    private String serialize(Object object) {
        if (object instanceof String) {
            return serializeString((String) object, true);
        }
        else if (object instanceof Map) {
            return "{" + serializeMap((Map) object) + "}";
        }
        else if (object instanceof List) {
            return "[" + serializeList((List) object) + "]";
        }
        return object.toString();
    }

    private boolean isCommand(Object object) {
        return (object instanceof Map) && ((Map) object).get(CMD_LABEL) != null;
    }

    private void writeJbossCliCommand(OutputStreamWriter writer, Map<String, Object> command, List<String> path) throws IOException {
        for (Map.Entry<String, Object> entry: command.entrySet())
            writeJbossCliCommand(writer, entry.getValue(), plus(path, serializeKey(entry.getKey())));
    }

    private String cliName(String name) {
        return name.indexOf('=') == -1 ? name + "=" + name : name;
    }

    public static void main( String[] args ) throws IOException {
        CommandLine commandLine = new CommandLine(args);
        String outputFilePath = commandLine.getNamedParams().get(PARAM_OUTPUTFILE);
        String contextFilePath = commandLine.getNamedParams().get(PARAM_PARAMFILE);
        Map<String, Object> contextMap = null;
        if (contextFilePath != null) {
            File contextFile = new File(contextFilePath);
            if (!contextFile.exists()) {
                System.out.println("Resolver file not found. Exiting.");
                System.exit(1);
            }
            Yaml yaml = new Yaml();
            contextMap = yaml.load(new FileReader(contextFile));

        }
        MapFormat context;
        // TODO: parameter names should not contain '.'; would make them unreachable
        if (contextMap == null)
            context = new MapFormat(commandLine.getContextParams());
        else {
            context = new MapFormat(contextMap);
            commandLine.getContextParams().entrySet().stream().forEach(
                    entry -> context.readNamedParam(entry.getKey(), entry.getValue()));
        }
        List<String> params = commandLine.getIndexedParams();
        if (params.size() == 0) {
            System.out.println("Usage: yaml2jbosscli [-c <context file name>] [--keypath=value]... -o <output file name> <input file name> [<input file name>]...");
            return;
        }
        Yaml2JbossCliCompiler compiler = new Yaml2JbossCliCompiler(context);
        compiler.processFiles(commandLine.getIndexedParams(), outputFilePath);
    }
}
