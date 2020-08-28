//package morphgen;
//
//import java.io.*;
//import java.util.ArrayList;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Set;
//
//public class ParadigmAutomaton {
//
//    private ParadigmState start;
//    private ParadigmState end;
//
//    public ParadigmAutomaton(String paradigmFile) {
//        start = new ParadigmState();
//        end = new ParadigmState();
//        try (BufferedReader read = new BufferedReader(new InputStreamReader(new FileInputStream(new File(paradigmFile)), "UTF-8"))) {
//            for (String line = read.readLine(); line != null; line = read.readLine()) {
//                if (!line.isEmpty()) {
//                    parseParadigm(line, start, end, true);
//                }
//            }
//        }
//        catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    private void parseParadigm(String regex, ParadigmState start, ParadigmState end, boolean prefix) {
//        if (!regex.isEmpty()) {
//            if (regex.startsWith("[")) {
//                if (!prefix) {
//                    System.err.println("More than one POS declaration in " + regex);
//                }
//                else {
//                    int x = regex.indexOf(']');
//                    if (x < 0) {
//                        System.err.println("No closing bracket ] in " + regex);
//                    }
//                    else {
//                        String rest = regex.substring(x + 1).trim();
//                        ParadigmState mid = (rest.isEmpty()) ? end : new ParadigmState();
//                        start.addTransition(new RootTransition(mid, regex.substring(1, x)));
//                        parseParadigm(rest, mid, end, false);
//                    }
//                }
//            }
//            else if (regex.startsWith("(")) {
//                int x = findClosingBracket(regex, 1);
//                if (x < 0) {
//                    System.err.println("No closing bracket ) in " + regex);
//                }
//                else {
//                    List<String> items = splitLowLevel(regex.substring(1, x), " || ");
//                    String rest = regex.substring(x + 1).trim();
//                    ParadigmState mid = (rest.isEmpty()) ? end : new ParadigmState();
//                    for (String item : items)
//                        parseParadigm(item, start, mid, prefix);
//                    parseParadigm(rest, mid, end, prefix);
//                }
//            }
//            else {
//                int x = regex.indexOf(' ');
//                String cur = regex;
//                String rest = "";
//                if (x >= 0) {
//                    cur = regex.substring(0, x);
//                    rest = regex.substring(x + 1);
//                }
//                ParadigmState mid = (rest.isEmpty()) ? end : new ParadigmState();
//                start.addTransition(new EpsilonTransition(mid));
//                ParadigmState prev = start;
//                String[] curs = cur.split("_");
//                for (int i = 0; i < curs.length-1; i++) {
//                    ParadigmState next = new ParadigmState();
//                    prev.addTransition(new AffixTransition(next, curs[i], prefix));
//                    prev = next;
//                }
//                prev.addTransition(new AffixTransition(mid, curs[curs.length-1], prefix));
//                parseParadigm(rest, mid, end, prefix);
//            }
//        }
//    }
//
//    /**
//     * Find the closing bracket for the previously parsed opening bracket.
//     * @param s The affix expression
//     * @param start The start index (must be after opening bracket in question and before next opening/closing bracket)
//     * @return Index of the closing bracket
//     */
//    private int findClosingBracket(String s, int start) {
//        int level = 0;
//        for (int c = start; c < s.length(); c++) {
//            if (s.charAt(c) == '(')
//                level++;
//            else if (s.charAt(c) == ')') {
//                if (level == 0)
//                    return c;
//                else
//                    level--;
//            }
//        }
//        return -1;
//    }
//
//    /**
//     * Split affix expression at a separator, but only on the lowest level, i.e. excluding separators occurring within
//     * brackets.
//     * @param s Affix expression
//     * @param sep Separator to split on
//     * @return Split affix expression
//     */
//    private List<String> splitLowLevel(String s, String sep) {
//        List<String> splits = new ArrayList<>();
//        int level = 0;
//        int prev = 0;
//        for (int c = 0; c < s.length(); c++) {
//            if (s.charAt(c) == '(')
//                level++;
//            else if (s.charAt(c) == ')')
//                level--;
//            else if (s.startsWith(sep, c) && level == 0) {
//                splits.add(s.substring(prev, c));
//                prev = c + 4;
//                c += 3;
//            }
//        }
//        splits.add(s.substring(prev));
//        return splits;
//    }
//
//    public void add(String word, String pos) {
//        if (!start.add(word, pos))
//            System.err.println("Unknown POS: " + pos);
//    }
//
//    public MorphAutomaton makeDictionary(List<MorphRule> rules) {
//        return null;
//    }
//
//
//    private class ParadigmState {
//
//        List<ParadigmTransition> transitions;
//
//        public ParadigmState() {
//            transitions = new ArrayList<>();
//        }
//
//        public void addTransition(ParadigmTransition t) {
//            transitions.add(t);
//        }
//
//        public boolean add(String word, String pos) {
//            for (ParadigmTransition trans : transitions) {
//                if (trans.add(word, pos))
//                    return true;
//            }
//            return false;
//        }
//    }
//
//    private interface ParadigmTransition {
//
//        boolean add(String word, String pos);
//
//    }
//
//    private class EpsilonTransition implements ParadigmTransition {
//
//        private ParadigmState to;
//
//        public EpsilonTransition(ParadigmState to) {
//            this.to = to;
//        }
//
//        public boolean add(String word, String pos) {
//            return to.add(word, pos);
//        }
//    }
//
//    private class RootTransition implements ParadigmTransition {
//
//        private ParadigmState to;
//        private String pos;
//        private Set<String> words;
//
//        public RootTransition(ParadigmState to, String pos) {
//            this.to = to;
//            this.pos = pos;
//            this.words = new HashSet<>();
//        }
//
//        public boolean add(String word, String pos) {
//            if (pos.equals(this.pos)) {
//                words.add(word);
//                return true;
//            }
//            return false;
//        }
//    }
//
//    private class AffixTransition implements ParadigmTransition {
//
//        private ParadigmState to;
//        private String affix;
//        private String sep;
//        private boolean prefix;
//
//        public AffixTransition(ParadigmState to, String affix, boolean prefix) {
//            this.to = to;
//            this.affix = affix;
//            this.prefix = prefix;
//        }
//
//        public boolean add(String word, String pos) {
//            return to.add(word, pos);
//        }
//    }
//
//}
