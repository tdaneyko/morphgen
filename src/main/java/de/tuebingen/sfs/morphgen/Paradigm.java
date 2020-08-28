package de.tuebingen.sfs.morphgen;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A paradigm with all the possible inflections (on gloss level) for some type of word.
 */
public class Paradigm {
    private Set<String> prefixes;
    private Set<String> suffixes;

    /**
     * Create a paradigm from sets containing all possible prefixes and suffixes.
     * @param prefixes The prefixes
     * @param suffixes The suffixes
     */
    public Paradigm(Set<String> prefixes, Set<String> suffixes) {
        this.prefixes = new HashSet<>(prefixes);
        this.suffixes = new HashSet<>(suffixes);
    }

    /**
     * Create a paradigm from string expressions representing the possible prefixes and suffixes.
     * @param prefix The prefix expression
     * @param suffix The suffix expression
     */
    public Paradigm(String prefix, String suffix) {
        this.prefixes = parseAffix(prefix);
        this.suffixes = parseAffix(suffix);
    }

    /**
     * Unfolds a prefix or suffix expression into a set of all possible affixes.
     * @param affix The prefix or suffix expression
     * @return The actual affixes described by the expression
     */
    private Set<String> parseAffix(String affix) {
        Set<String> out = new HashSet<>();
        out.add("");
        if (!affix.isEmpty()) {
            if (affix.startsWith("(")) {
                int x = findClosingBracket(affix, 1);
                if (x < 0) {
                    System.err.println("No closing bracket in " + affix);
                    return out;
                }
                List<String> items = splitLowLevel(affix.substring(1, x), " || ");
                Set<String> next = parseAffix(affix.substring(x+1));
                for (String item : items) {
                    for (String parse : parseAffix(item)) {
                        for (String other : next) {
                            out.add((parse + " " + other).trim());
                        }
                    }
                }
            }
            else {
                int x = affix.indexOf(' ');
                String cur = affix;
                String rest = "";
                if (x >= 0) {
                    cur = affix.substring(0, x);
                    rest = affix.substring(x + 1);
                }
                cur = cur.replace('_', ' ');
                Set<String> next = parseAffix(rest);
                out.addAll(next);
                for (String other : next)
                    out.add((cur + " " + other).trim());
            }
        }
        return out;
    }

    /**
     * Find the closing bracket for the previously parsed opening bracket.
     * @param s The affix expression
     * @param start The start index (must be after opening bracket in question and before next opening/closing bracket)
     * @return Index of the closing bracket
     */
    private int findClosingBracket(String s, int start) {
        int level = 0;
        for (int c = start; c < s.length(); c++) {
            if (s.charAt(c) == '(')
                level++;
            else if (s.charAt(c) == ')') {
                if (level == 0)
                    return c;
                else
                    level--;
            }
        }
        return -1;
    }

    /**
     * Split affix expression at a separator, but only on the lowest level, i.e. excluding separators occurring within
     * brackets.
     * @param s Affix expression
     * @param sep Separator to split on
     * @return Split affix expression
     */
    private List<String> splitLowLevel(String s, String sep) {
        List<String> splits = new ArrayList<>();
        int level = 0;
        int prev = 0;
        for (int c = 0; c < s.length(); c++) {
            if (s.charAt(c) == '(')
                level++;
            else if (s.charAt(c) == ')')
                level--;
            else if (s.startsWith(sep, c) && level == 0) {
                splits.add(s.substring(prev, c));
                prev = c + 4;
                c += 3;
            }
        }
        splits.add(s.substring(prev));
        return splits;
    }

    /**
     * Get paradigm for a given word.
     * @param word The word to get the paradigm for
     * @return All possible realizations of the word
     */
    public Set<String> getParadigm(String word) {
        Set<String> paradigms = new HashSet<>();
        for (String prefix : prefixes)
            for (String suffix : suffixes)
                paradigms.add((prefix + " " + word + " " + suffix).trim());
        return paradigms;
    }
}