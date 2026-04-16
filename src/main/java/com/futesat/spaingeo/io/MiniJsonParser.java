package com.futesat.spaingeo.io;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.function.Consumer;

/**
 * A non-recursive JSON parser using a state machine and explicit stack.
 * Handles large nested structures without StackOverflowError and uses Reader for efficiency.
 */
public final class MiniJsonParser {
    private enum State {
        VALUE,          // Expecting any JSON value
        KEY,            // Expecting an object key (string)
        COLON,          // Expecting ':'
        COMMA_OR_END    // Expecting ',' or end of container ('}' or ']')
    }

    private final PushbackReader reader;
    private final Stack<Object> stack = new Stack<>();
    private final Stack<State> states = new Stack<>();
    private final Stack<String> keys = new Stack<>();
    private Object root = null;

    private MiniJsonParser(Reader reader) {
        this.reader = new PushbackReader(reader, 1);
    }

    public static Object parse(String json) {
        return parse(new StringReader(json));
    }

    public static Object parse(Reader reader) {
        try {
            return new MiniJsonParser(reader).parseInternal();
        } catch (IOException e) {
            throw new RuntimeException("JSON parse error: " + e.getMessage(), e);
        }
    }

    /**
     * Streams elements of a specific array within a root object.
     * Useful for large FeatureCollections where 'features' is the bottleneck.
     */
    public static void streamArray(Reader reader, String targetKey, Consumer<Object> consumer) {
        try {
            new MiniJsonParser(reader).parseStreaming(targetKey, consumer);
        } catch (IOException e) {
            throw new RuntimeException("JSON stream error: " + e.getMessage(), e);
        }
    }

    private void parseStreaming(String targetKey, Consumer<Object> consumer) throws IOException {
        skipWhitespace();
        if (read() != '{') throw new IOException("Expected '{' at start of GeoJSON");

        while (true) {
            skipWhitespace();
            int c = peek();
            if (c == '}') {
                read();
                break;
            }
            String key = parseString();
            skipWhitespace();
            if (read() != ':') throw new IOException("Expected ':' after key");
            skipWhitespace();

            if (targetKey.equals(key)) {
                if (peek() != '[') throw new IOException("Expected '[' for " + targetKey);
                read(); // consume '['
                skipWhitespace();
                if (peek() == ']') {
                    read(); // consume ']'
                } else {
                    while (true) {
                        // Use the non-recursive parseInternal logic but only for one value
                        Object element = parseOneValue();
                        consumer.accept(element);
                        skipWhitespace();
                        int next = read();
                        if (next == ']') break;
                        if (next != ',') throw new IOException("Expected ',' or ']' in " + targetKey);
                    }
                }
            } else {
                // Skip this value
                skipValue();
            }

            skipWhitespace();
            int next = peek();
            if (next == '}') {
                read();
                break;
            } else if (next == ',') {
                read();
            } else {
                throw new IOException("Expected ',' or '}' in root object");
            }
        }
    }

    private Object parseOneValue() throws IOException {
        // Reuse the state machine logic but limited to this value
        // To make it simple, we can use a temporary parser for this sub-tree
        // OR we can just use the existing doParse logic if we make it reusable.
        // Actually, the easiest is to just call parseValue() recursively BUT we know
        // that a SINGLE feature is small enough to not overflow! 
        // A single municipality feature has a few thousands points (8 levels of nesting).
        // So recursion is SAFE for a single feature.
        return parseValueRecursive();
    }

    private void skipValue() throws IOException {
        int first = peek();
        if (first == '{' || first == '[') {
            int depth = 0;
            while (true) {
                int c = read();
                if (c == -1) break;
                if (c == '{' || c == '[') depth++;
                else if (c == '}' || c == ']') {
                    depth--;
                } else if (c == '"') {
                    unread(c);
                    parseString();
                }
                if (depth == 0) break;
            }
        } else if (first == '"') {
            parseString();
        } else {
            parsePrimitive();
        }
    }

    private void unread(int c) throws IOException {
        if (c != -1) reader.unread(c);
    }

    private Object parseValueRecursive() throws IOException {
        skipWhitespace();
        int c = peek();
        if (c == '{') {
            read();
            Map<String, Object> map = new LinkedHashMap<>();
            skipWhitespace();
            if (peek() == '}') { read(); return map; }
            while (true) {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                if (read() != ':') throw new IOException("Expected ':'");
                map.put(key, parseValueRecursive());
                skipWhitespace();
                int next = read();
                if (next == '}') break;
                if (next != ',') throw new IOException("Expected ',' or '}'");
            }
            return map;
        } else if (c == '[') {
            read();
            List<Object> list = new ArrayList<>();
            skipWhitespace();
            if (peek() == ']') { read(); return list; }
            while (true) {
                list.add(parseValueRecursive());
                skipWhitespace();
                int next = read();
                if (next == ']') break;
                if (next != ',') throw new IOException("Expected ',' or ']'");
            }
            return list;
        } else {
            return parsePrimitive();
        }
    }

    private Object parseInternal() throws IOException {
        states.push(State.VALUE);

        while (!states.isEmpty()) {
            skipWhitespace();
            State state = states.pop();

            switch (state) {
                case VALUE:
                    handleValue();
                    break;
                case KEY:
                    handleKey();
                    break;
                case COLON:
                    handleColon();
                    break;
                case COMMA_OR_END:
                    handleCommaOrEnd();
                    break;
            }
        }

        skipWhitespace();
        if (peek() != -1) {
            throw new IOException("Unexpected trailing characters");
        }
        return root;
    }

    private void handleValue() throws IOException {
        int c = peek();
        if (c == -1) throw new IOException("Unexpected EOF");

        if (c == '{') {
            read(); // consume '{'
            Map<String, Object> map = new LinkedHashMap<>();
            skipWhitespace();
            if (peek() == '}') {
                read(); // consume '}'
                emitValue(map);
            } else {
                stack.push(map);
                states.push(State.COMMA_OR_END);
                states.push(State.VALUE);
                states.push(State.COLON);
                states.push(State.KEY);
            }
        } else if (c == '[') {
            read(); // consume '['
            List<Object> list = new ArrayList<>();
            skipWhitespace();
            if (peek() == ']') {
                read(); // consume ']'
                emitValue(list);
            } else {
                stack.push(list);
                states.push(State.COMMA_OR_END);
                states.push(State.VALUE);
            }
        } else {
            emitValue(parsePrimitive());
        }
    }

    private void handleKey() throws IOException {
        if (peek() != '"') throw new IOException("Expected object key (string)");
        keys.push(parseString());
    }

    private void handleColon() throws IOException {
        skipWhitespace();
        if (read() != ':') throw new IOException("Expected ':'");
    }

    private void handleCommaOrEnd() throws IOException {
        skipWhitespace();
        int c = read();
        Object container = stack.peek();
        boolean isMap = container instanceof Map;

        int endChar = isMap ? '}' : ']';

        if (c == endChar) {
            emitValue(stack.pop());
        } else if (c == ',') {
            states.push(State.COMMA_OR_END);
            states.push(State.VALUE);
            if (isMap) {
                states.push(State.COLON);
                states.push(State.KEY);
            }
        } else {
            throw new IOException("Expected ',' or '" + (char) endChar + "' at pos " + c);
        }
    }

    private void emitValue(Object value) {
        if (stack.isEmpty()) {
            root = value;
        } else {
            Object container = stack.peek();
            if (container instanceof Map) {
                ((Map<String, Object>) container).put(keys.pop(), value);
            } else {
                ((List<Object>) container).add(value);
            }
        }
    }

    private Object parsePrimitive() throws IOException {
        int c = peek();
        if (c == '"') return parseString();
        if (c == 't') { expectLiteral("true"); return Boolean.TRUE; }
        if (c == 'f') { expectLiteral("false"); return Boolean.FALSE; }
        if (c == 'n') { expectLiteral("null"); return null; }
        return parseNumber();
    }

    private String parseString() throws IOException {
        if (read() != '"') throw new IOException("Expected '\"'");
        StringBuilder sb = new StringBuilder();
        while (true) {
            int c = read();
            if (c == -1) throw new IOException("Unterminated string");
            if (c == '"') return sb.toString();
            if (c == '\\') {
                int next = read();
                switch (next) {
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    case '/': sb.append('/'); break;
                    case 'b': sb.append('\b'); break;
                    case 'f': sb.append('\f'); break;
                    case 'n': sb.append('\n'); break;
                    case 'r': sb.append('\r'); break;
                    case 't': sb.append('\t'); break;
                    case 'u':
                        char[] hex = new char[4];
                        for (int i = 0; i < 4; i++) {
                            int h = read();
                            if (h == -1) throw new IOException("Invalid unicode escape");
                            hex[i] = (char) h;
                        }
                        sb.append((char) Integer.parseInt(new String(hex), 16));
                        break;
                    default: throw new IOException("Invalid escape: \\" + (char) next);
                }
            } else {
                sb.append((char) c);
            }
        }
    }

    private Number parseNumber() throws IOException {
        StringBuilder sb = new StringBuilder();
        while (true) {
            int c = peek();
            if (c == -1 || (!Character.isDigit(c) && c != '.' && c != '-' && c != '+' && c != 'e' && c != 'E')) break;
            sb.append((char) read());
        }
        String s = sb.toString();
        try {
            if (s.contains(".") || s.contains("e") || s.contains("E")) {
                return Double.parseDouble(s);
            } else {
                return Long.parseLong(s);
            }
        } catch (NumberFormatException e) {
            throw new IOException("Invalid number: " + s);
        }
    }

    private void expectLiteral(String literal) throws IOException {
        for (int i = 0; i < literal.length(); i++) {
            if (read() != literal.charAt(i)) throw new IOException("Expected '" + literal + "'");
        }
    }

    private void skipWhitespace() throws IOException {
        while (true) {
            int c = peek();
            if (c == -1 || !Character.isWhitespace(c)) break;
            read();
        }
    }

    private int peek() throws IOException {
        int c = reader.read();
        if (c != -1) reader.unread(c);
        return c;
    }

    private int read() throws IOException {
        return reader.read();
    }
}
