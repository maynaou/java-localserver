package com.javaserver.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonParser {
    private final String src; // le JSON complet
    private int pos;          // curseur : où on en est dans la lecture

    public JsonParser(String src) {
        this.src = src;
        this.pos = 0;
    }

    public Object parse() {
        skipWhitespace();
        return parseValue();
    }

    public Object parseValue() {
        skipWhitespace();
        char c = peek();

        if (c == '{') return parseObject();
        if (c == '[') return parseList();
        if (c == '"') return parseString();
        if (c == 't') return parseLiteral("true",  Boolean.TRUE);
        if (c == 'f') return parseLiteral("false", Boolean.FALSE);
        if (c == 'n') return parseLiteral("null",  null);
        if (c == '-' || Character.isDigit(c)) return parseNumber();
        throw new RuntimeException("[JsonParser] Caractère inattendu: '" + c + "' à pos=" + pos);
    }

    public Map<String, Object> parseObject() {
        Map<String, Object> obj = new HashMap<>();
        expect('{');
        skipWhitespace();
        if (peek() == '}') {
            pos++;
            return obj; 
        }

        while (true) {
            skipWhitespace();
            String key = parseString();
            System.out.println("Key: " + key);
            skipWhitespace();
            expect(':');
            skipWhitespace();
            Object value = parseValue();
            obj.put(key, value);
            skipWhitespace();
            char next = peek();
            if (next == '}') { pos++; break; } 
            if (next == ',') { pos++; continue; } 
        }

        return obj;
    }

    public String parseString() {
        expect('"');
        StringBuilder sb = new StringBuilder();
        while (pos < src.length()) {
            char c = src.charAt(pos++);

            if (c == '"') return sb.toString(); // guillemet fermant → fin

            if (c == '\\') {
                // Caractère échappé : \n \t \" \\ etc.
                char esc = src.charAt(pos++);
                switch (esc) {
                    case '"':  sb.append('"');  break;
                    case '\\': sb.append('\\'); break;
                    case '/':  sb.append('/');  break;
                    case 'n':  sb.append('\n'); break;
                    case 'r':  sb.append('\r'); break;
                    case 't':  sb.append('\t'); break;
                    default:   sb.append(esc);
                }
            } else {
                sb.append(c);
            }
        }

        throw new RuntimeException("[JsonParser] String non fermée dans le JSON");
    }

    public List<Object> parseList() {
        List<Object> list = new ArrayList<>();
        expect('[');
        skipWhitespace();
        if (peek() == ']') {
            pos++;
            return list; // TODO
        }
        while(true) {
            list.add(parseValue());
            skipWhitespace();
            char next = peek();
            if (next == ']') { pos++; break; }
            if (next == ',') { pos++; continue; }
        }
       return list; // TODO
    }

    Object parseLiteral(String word, Object value) {
        if (!src.startsWith(word, pos))
            throw new RuntimeException("[JsonParser] Attendu '" + word + "' à pos=" + pos);
        pos += word.length();
        return value;
    }

    Double parseNumber() {
        int start = pos;
        if (peek() == '-') pos++;
        while (pos < src.length()) {
            char c = src.charAt(pos);
            if (Character.isDigit(c) || c == '.' || c == 'e' || c == 'E') pos++;
            else break;
        }
        return Double.parseDouble(src.substring(start, pos));
    }

    private void skipWhitespace() {
        while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) {
            pos++;
        };
    }

    private char peek() {
        skipWhitespace();
        return pos < src.length() ? src.charAt(pos) : '\0';
    }

    private void expect(char c) {
        skipWhitespace();
        if (pos >= src.length() || src.charAt(pos) != c) {
           // System.out.println("Expected '" + c + "' at position " + pos);
            return;
        }
           
        pos++;
    }

    public String getSrc() {
        return src;
    }

    public int getPos() {
        return pos;
    }
}
