package com.github.wolray.seq;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author wolray
 */
public interface Splitter {
    Seq<String> split(String s);

    static Seq2<String, String> parsePairs(char[] chars, char entrySep, char kvSep) {
        return p -> {
            int len = chars.length, last = 0;
            String prev = null;
            for (int i = 0; i < len; i++) {
                if (chars[i] == entrySep) {
                    if (prev != null) {
                        if (p.test(prev, substring(chars, last, i))) {
                            return true;
                        }
                        prev = null;
                    }
                    last = i + 1;
                } else if (prev == null && chars[i] == kvSep) {
                    prev = substring(chars, last, i);
                    last = i + 1;
                }
            }
            return prev != null && p.test(prev, substring(chars, last, len));
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

    static Splitter ofEmpty() {
        return s -> Seq.empty();
    }

    static String substring(char[] chars, int start, int end) {
        return start < end ? new String(chars, start, end - start) : "";
    }
}
