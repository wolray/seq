package com.github.wolray.seq;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author wolray
 */
public interface Splitter {
    Seq<String> split(String s);

    static Splitter of(String literal) {
        int len = literal.length();
        if (len == 0) {
            return ofEmpty();
        }
        if (len == 1) {
            return of(literal.charAt(0));
        }
        return s -> {
            char[] chars = s.toCharArray();
            return p -> {
                int beg = 0, index;
                while ((index = s.indexOf(literal, beg)) > 0) {
                    if (p.test(substring(chars, beg, index))) {
                        return true;
                    }
                    beg = index + len;
                }
                return p.test(substring(chars, beg, chars.length));
            };
        };
    }

    static Splitter of(char sep) {
        return s -> {
            char[] chars = s.toCharArray();
            int len = s.length();
            return p -> {
                int last = 0;
                for (int i = 0; i < len; i++) {
                    if (chars[i] == sep) {
                        if (p.test(substring(chars, last, i))) {
                            return true;
                        }
                        last = i + 1;
                    }
                }
                return p.test(substring(chars, last, len));
            };
        };
    }

    static Splitter of(Pattern sep) {
        return s -> {
            char[] chars = s.toCharArray();
            return p -> {
                Matcher matcher = sep.matcher(s);
                int beg = 0;
                while (matcher.find()) {
                    if (p.test(substring(chars, beg, matcher.start()))) {
                        return true;
                    }
                    beg = matcher.end();
                }
                return p.test(substring(chars, beg, chars.length));
            };
        };
    }

    static Splitter ofEmpty() {
        return s -> Seq.empty();
    }

    static String substring(char[] chars, int start, int end) {
        return start < end ? new String(chars, start, end - start) : "";
    }
}
