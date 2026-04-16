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
 * Optimized for Java 21 using Records and Pattern Matching.
 */
public final class MiniJsonParser {
    private enum State { VALUE, KEY, COLON, COMMA_OR_END }

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
                read(); // '['
                skipWhitespace();
                if (peek() == ']') {
                    read(); // ']'
                } else {
                    while (true) {
                        consumer.accept(parseValueRecursive());
                        skipWhitespace();
                        int next = read();
                        if (next == ']') break;
                        if (next != ',') throw new IOException("Expected ',' or ']' in " + targetKey);
                    }
                }
            } else {
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

    private void skipValue() throws IOException {
        int first = peek();
        if (first == '{' || first == '[') {
            int depth = 0;
            while (true) {
                int c = read();
                if (c == -1) break;
                if (c == '{' || c == '[') depth++;
                else if (c == '}' || c == ']') depth--;
                else if (c == '"') {
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

    private Object parseValueRecursive() throws IOException {
        skipWhitespace();
        int c = peek();
        return switch (c) {
            case '{' -> {
                read();
                Map<String, Object> map = new LinkedHashMap<>();
                skipWhitespace();
                if (peek() == '}') { read(); yield map; }
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
                yield map;
            }
            case '[' -> {
                read();
                List<Object> list = new ArrayList<>();
                skipWhitespace();
                if (peek() == ']') { read(); yield list; }
                while (true) {
                    list.add(parseValueRecursive());
                    skipWhitespace();
                    int next = read();
                    if (next == ']') break;
                    if (next != ',') throw new IOException("Expected ',' or ']'");
                }
                yield list;
            }
            default -> parsePrimitive();
        };
    }

    private Object parseInternal() throws IOException {
        states.push(State.VALUE);

        while (!states.isEmpty()) {
            skipWhitespace();
            State state = states.pop();

            switch (state) {
                case VALUE -> handleValue();
                case KEY -> keys.push(parseString());
                case COLON -> { skipWhitespace(); if (read() != ':') throw new IOException("Expected ':'"); }
                case COMMA_OR_END -> handleCommaOrEnd();
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
        switch (c) {
            case '{' -> {
                read();
                Map<String, Object> map = new LinkedHashMap<>();
                skipWhitespace();
                if (peek() == '}') {
                    read();
                    emitValue(map);
                } else {
                    stack.push(map);
                    states.push(State.COMMA_OR_END);
                    states.push(State.VALUE);
                    states.push(State.COLON);
                    states.push(State.KEY);
                }
            }
            case '[' -> {
                read();
                List<Object> list = new ArrayList<>();
                skipWhitespace();
                if (peek() == ']') {
                    read();
                    emitValue(list);
                } else {
                    stack.push(list);
                    states.push(State.COMMA_OR_END);
                    states.push(State.VALUE);
                }
            }
            case -1 -> throw new IOException("Unexpected EOF");
            default -> emitValue(parsePrimitive());
        }
    }

    private void handleCommaOrEnd() throws IOException {
        skipWhitespace();
        int c = read();
        Object container = stack.peek();
        int endChar = (container instanceof Map) ? '}' : ']';

        if (c == endChar) {
            emitValue(stack.pop());
        } else if (c == ',') {
            states.push(State.COMMA_OR_END);
            states.push(State.VALUE);
            if (container instanceof Map) {
                states.push(State.COLON);
                states.push(State.KEY);
            }
        } else {
            throw new IOException("Expected ',' or '" + (char) endChar + "'");
        }
    }

    private void emitValue(Object value) {
        if (stack.isEmpty()) {
            root = value;
        } else {
            Object container = stack.peek();
            if (container instanceof Map map) {
                map.put(keys.pop(), value);
            } else if (container instanceof List list) {
                list.add(value);
            }
        }
    }

    private Object parsePrimitive() throws IOException {
        int c = peek();
        return switch (c) {
            case '"' -> parseString();
            case 't' -> { expectLiteral("true"); yield Boolean.TRUE; }
            case 'f' -> { expectLiteral("false"); yield Boolean.FALSE; }
            case 'n' -> { expectLiteral("null"); yield null; }
            default -> parseNumber();
        };
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
                    case '"' -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    case '/' -> sb.append('/');
                    case 'b' -> sb.append('\b');
                    case 'f' -> sb.append('\f');
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case 'u' -> {
                        char[] hex = new char[4];
                        for (int i = 0; i < 4; i++) {
                            int h = read();
                            if (h == -1) throw new IOException("Invalid unicode escape");
                            hex[i] = (char) h;
                        }
                        sb.append((char) Integer.parseInt(new String(hex), 16));
                    }
                    default -> throw new IOException("Invalid escape");
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
            if (s.contains(".") || s.toLowerCase().contains("e")) {
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

    private void unread(int c) throws IOException {
        if (c != -1) reader.unread(c);
    }
}
