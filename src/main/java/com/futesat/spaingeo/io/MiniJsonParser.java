package com.futesat.spaingeo.io;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MiniJsonParser {
    private final String input;
    private int pos;

    private MiniJsonParser(String input) {
        this.input = input;
    }

    public static Object parse(String input) {
        MiniJsonParser parser = new MiniJsonParser(input);
        Object value = parser.parseValue();
        parser.skipWhitespace();
        if (!parser.isEnd()) {
            throw new IllegalArgumentException("Unexpected trailing JSON at position " + parser.pos);
        }
        return value;
    }

    private Object parseValue() {
        skipWhitespace();
        if (isEnd()) {
            throw new IllegalArgumentException("Unexpected end of JSON.");
        }
        char c = peek();
        switch (c) {
            case '{': return parseObject();
            case '[': return parseArray();
            case '"': return parseString();
            case 't': return parseTrue();
            case 'f': return parseFalse();
            case 'n': return parseNull();
            default: return parseNumber();
        }
    }

    private Map<String, Object> parseObject() {
        expect('{');
        skipWhitespace();
        Map<String, Object> object = new LinkedHashMap<>();
        if (peek() == '}') {
            pos++;
            return object;
        }
        while (true) {
            skipWhitespace();
            String key = parseString();
            skipWhitespace();
            expect(':');
            Object value = parseValue();
            object.put(key, value);
            skipWhitespace();
            char c = next();
            if (c == '}') {
                break;
            }
            if (c != ',') {
                throw new IllegalArgumentException("Expected ',' or '}' at position " + pos);
            }
        }
        return object;
    }

    private List<Object> parseArray() {
        expect('[');
        skipWhitespace();
        List<Object> array = new ArrayList<>();
        if (peek() == ']') {
            pos++;
            return array;
        }
        while (true) {
            array.add(parseValue());
            skipWhitespace();
            char c = next();
            if (c == ']') {
                break;
            }
            if (c != ',') {
                throw new IllegalArgumentException("Expected ',' or ']' at position " + pos);
            }
        }
        return array;
    }

    private String parseString() {
        expect('"');
        StringBuilder sb = new StringBuilder();
        while (!isEnd()) {
            char c = next();
            if (c == '"') {
                return sb.toString();
            }
            if (c == '\\') {
                if (isEnd()) {
                    throw new IllegalArgumentException("Unexpected end inside string escape.");
                }
                char e = next();
                switch (e) {
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    case '/': sb.append('/'); break;
                    case 'b': sb.append('\b'); break;
                    case 'f': sb.append('\f'); break;
                    case 'n': sb.append('\n'); break;
                    case 'r': sb.append('\r'); break;
                    case 't': sb.append('\t'); break;
                    case 'u': {
                        if (pos + 4 > input.length()) {
                            throw new IllegalArgumentException("Invalid unicode escape at position " + pos);
                        }
                        String hex = input.substring(pos, pos + 4);
                        sb.append((char) Integer.parseInt(hex, 16));
                        pos += 4;
                    } break;
                    default: throw new IllegalArgumentException("Invalid escape character: " + e);
                }
            } else {
                sb.append(c);
            }
        }
        throw new IllegalArgumentException("Unterminated string.");
    }

    private Boolean parseTrue() {
        expectLiteral("true");
        return Boolean.TRUE;
    }

    private Boolean parseFalse() {
        expectLiteral("false");
        return Boolean.FALSE;
    }

    private Object parseNull() {
        expectLiteral("null");
        return null;
    }

    private Number parseNumber() {
        int start = pos;
        if (peek() == '-') {
            pos++;
        }
        while (!isEnd() && Character.isDigit(peek())) {
            pos++;
        }
        if (!isEnd() && peek() == '.') {
            pos++;
            while (!isEnd() && Character.isDigit(peek())) {
                pos++;
            }
        }
        if (!isEnd() && (peek() == 'e' || peek() == 'E')) {
            pos++;
            if (!isEnd() && (peek() == '+' || peek() == '-')) {
                pos++;
            }
            while (!isEnd() && Character.isDigit(peek())) {
                pos++;
            }
        }
        String token = input.substring(start, pos);
        return Double.parseDouble(token);
    }

    private void skipWhitespace() {
        while (!isEnd() && Character.isWhitespace(peek())) {
            pos++;
        }
    }

    private void expect(char expected) {
        if (isEnd() || next() != expected) {
            throw new IllegalArgumentException("Expected '" + expected + "' at position " + pos);
        }
    }

    private void expectLiteral(String literal) {
        if (!input.startsWith(literal, pos)) {
            throw new IllegalArgumentException("Expected '" + literal + "' at position " + pos);
        }
        pos += literal.length();
    }

    private char peek() {
        return input.charAt(pos);
    }

    private char next() {
        return input.charAt(pos++);
    }

    private boolean isEnd() {
        return pos >= input.length();
    }
}
