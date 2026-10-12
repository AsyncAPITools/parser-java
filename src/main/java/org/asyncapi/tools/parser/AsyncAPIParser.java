package org.asyncapi.tools.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.Map;

import com.github.erosb.jsonsKema.FormatValidationPolicy;
import com.github.erosb.jsonsKema.JsonParser;
import com.github.erosb.jsonsKema.JsonValue;
import com.github.erosb.jsonsKema.Schema;
import com.github.erosb.jsonsKema.SchemaLoader;
import com.github.erosb.jsonsKema.ValidationFailure;
import com.github.erosb.jsonsKema.Validator;
import com.github.erosb.jsonsKema.ValidatorConfig;
import org.yaml.snakeyaml.Yaml;

public class AsyncAPIParser {

    public static Object parse(String input) throws Exception {
        Object yamlValue = new Yaml().load(input);
        JsonValue instance = new JsonParser(toJson(yamlValue)).parse();
        String schemaVersion = getSchemaVersion(yamlValue);
        String schemaContent = read(AsyncAPIParser.class.getClassLoader().getResourceAsStream("schemas/" + schemaVersion + ".json"));

        JsonValue schemaJson = new JsonParser(schemaContent).parse();
        Schema schema = new SchemaLoader(schemaJson).load();
        Validator validator = Validator.create(schema, new ValidatorConfig(FormatValidationPolicy.ALWAYS));
        ValidationFailure failure = validator.validate(instance);
        System.out.println("failure: " + failure);
        return instance;
    }

    private static String getSchemaVersion(Object yamlValue) {
        String asyncApiVersion = yamlValue instanceof Map<?, ?> ? (String)((Map<?, ?>) yamlValue).get("asyncapi") : null;
        int majorVersionEnd = asyncApiVersion.indexOf('.');
        if (majorVersionEnd <= 0) {
            throw new IllegalArgumentException("Invalid AsyncAPI version: " + asyncApiVersion);
        }
        String schemaVersion = asyncApiVersion.substring(0, majorVersionEnd) + ".x.x";
        return schemaVersion;
    }

    public static Object parseFromUrl(URL input) throws Exception {
        return parse(read(input.openStream()));
    }

    public static Object parseFromStream(InputStream input) throws Exception {
        return parse(read(input));
    }

    public static Object parseFromReader(Reader input) throws Exception {
        return parse(read(input));
    }

    private static String read(InputStream input) throws IOException {
        return read(new InputStreamReader(input));
    }

    private static String read(Reader input) throws IOException {
        try (BufferedReader reader = new BufferedReader(input)) {
            StringBuilder content = new StringBuilder();
            reader.lines().forEach(line -> content.append(line).append(System.lineSeparator()));
            return content.toString();
        }
    }

    private static String toJson(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String || value instanceof Character) {
            return quoteJson(value.toString());
        }
        if (value instanceof Boolean || value instanceof Number) {
            return value.toString();
        }
        if (value instanceof Map<?, ?>) {
            StringBuilder json = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                if (!first) {
                    json.append(',');
                }
                json.append(quoteJson(String.valueOf(entry.getKey())))
                        .append(':')
                        .append(toJson(entry.getValue()));
                first = false;
            }
            return json.append('}').toString();
        }
        if (value instanceof Iterable<?>) {
            StringBuilder json = new StringBuilder("[");
            boolean first = true;
            for (Object item : (Iterable<?>) value) {
                if (!first) {
                    json.append(',');
                }
                json.append(toJson(item));
                first = false;
            }
            return json.append(']').toString();
        }
        return quoteJson(value.toString());
    }

    private static String quoteJson(String value) {
        StringBuilder json = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            switch (character) {
                case '"': json.append("\\\""); break;
                case '\\': json.append("\\\\"); break;
                case '\b': json.append("\\b"); break;
                case '\f': json.append("\\f"); break;
                case '\n': json.append("\\n"); break;
                case '\r': json.append("\\r"); break;
                case '\t': json.append("\\t"); break;
                default:
                    if (character < 0x20) {
                        json.append(String.format("\\u%04x", (int) character));
                    } else {
                        json.append(character);
                    }
            }
        }
        return json.append('"').toString();
    }

    public static void main(String[] args) throws Exception {
        Object result = AsyncAPIParser.parseFromStream(AsyncAPIParser.class.getClassLoader().getResourceAsStream("samples/2.x.x.yaml"));
        System.out.println("-------------------------");
        System.out.println("result : " + result);
        System.out.println("-------------------------");
    }

}