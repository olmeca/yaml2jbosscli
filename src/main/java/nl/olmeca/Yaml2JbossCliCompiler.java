package nl.olmeca;

import com.esotericsoftware.yamlbeans.YamlReader;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
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
    private static final char PARAM_PARAMFILE = 'p';

    File outputFile;
    Context context;


    public Yaml2JbossCliCompiler(File outputFile, Map<String, Object> contextMap) throws IOException {
        this.context = new Context(contextMap);
        this.outputFile = outputFile;
    }

    public void processFiles(List<String> files) throws IOException {
        try (PrintWriter out = new PrintWriter(outputFile, "UTF-8")) {
            files.stream().forEach(filePath -> processFile(new File(filePath), out));
        }
    }

    private void processFile(File file, PrintWriter writer) {
        System.out.println("Processing file: " + file);
        try {
            YamlReader reader = new YamlReader(new FileReader(file));
            List<Object> commands =  reader.read(List.class);
            commands.forEach(command ->
                    writeJbossCliCommand(writer, command, new ArrayList<>())
            );
            reader.close();
        } catch (IOException e) {
            System.out.println("Error reading file: " + file);
        }
    }

    private String resolve(String value) {
        return context == null ? value : context.resolve(value);
    }

    private List<String> plus(List<String> list, String item) {
        List<String> newPath = new ArrayList<>(list);
        newPath.add(item);
        return newPath;
    }

    private void writeJbossCliCommand(PrintWriter writer, Object object, List<String> path) {
        if (object instanceof List)
            writeJbossCliCommand(writer, (List) object, path);
        else if (object instanceof Map)
            writeJbossCliCommand(writer, (Map) object, path);
    }

    private void writeJbossCliCommand (PrintWriter writer, List<Object> commands, List<String> path) {
        commands.forEach(command -> {
            if (isCommand(command))
                writeCommand(writer, (Map)command, path);
            else
                writeJbossCliCommand(writer, command, path);
        });
    }

    private void writeCommand(PrintWriter writer, Map command, List<String> path) {
        path.forEach(item -> writer.write("/" + item));
        writer.write(serializeCommand(command));
    }

    private String serializeCommand(Map<String, Object> command) {
        Object args = command.get(ARGS_LABEL);
        String argString  = args == null ? "" : serializeArguments((Map) args);
        return ":" + command.get(CMD_LABEL) + argString;
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

    private String quoteIfRealString(String value) {
        if ("true".equals(value) ||
                "false".equals(value) ||
                INT_MATCHER.matcher(value).matches())
            return value;
        else
            return "\"" + value + "\"";
    }

    private String serializeList(List<Object> list) {
        List<String> itemStrings = list.stream().map(item -> serialize(item)).collect(Collectors.toList());
        return String.join(",", itemStrings);
    }

    private String serializeString(String string) {
        // Hack to only call myself on the resolved value if anything was resolved
        try {
            string = resolve(string);
            // recurse only if no exception was thrown
            return serialize(string);
        } catch (Context.NoTagsFound ntf) {
            return quoteIfRealString(string);
        }
    }

    private String serialize(Object object) {
        if (object instanceof String) {
            return serializeString((String) object);
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

    private void writeJbossCliCommand(PrintWriter writer, Map<String, Object> command, List<String> path) {
        command.entrySet().forEach(entry ->
            writeJbossCliCommand(writer, entry.getValue(), plus(path, cliName(resolve(entry.getKey()))))
        );
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
                System.out.println("File not found: " + contextFilePath);
                System.exit(1);
            }
            YamlReader reader = new YamlReader(new FileReader(contextFile));
            contextMap = reader.read(Map.class);
            reader.close();

        }
        List<String> params = commandLine.getIndexedParams();
        if (outputFilePath == null || params.size() == 0) {
            System.out.println("Usage: yaml2jbosscli -o <output file name> <input file name> [<input file name>]...");
            return;
        }
        System.out.println("Writing output to file: " + outputFilePath);
        Yaml2JbossCliCompiler compiler = new Yaml2JbossCliCompiler(new File(outputFilePath), contextMap);
        compiler.processFiles(commandLine.getIndexedParams());
    }
}
