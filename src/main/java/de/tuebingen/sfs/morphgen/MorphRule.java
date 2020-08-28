package de.tuebingen.sfs.morphgen;

import gnu.trove.list.TCharList;
import gnu.trove.list.array.TCharArrayList;
import de.tuebingen.sfs.utils.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * A morphological rule converting a glossed word into the represented form.
 */
public class MorphRule extends Rule {
    private RuleState matchStart;
    private String[][] produc;
    private boolean[][] lookup;

    /**
     * @param lhs Accepted input of the rule
     * @param rhs Produced outputs of the rule
     * @param groups Pre-defined variables that might occur in the rule
     * @param name Name of the rule for easier identification in error messages (optional)
     */
    public MorphRule(String lhs, String[] rhs, Map<String, String[]> groups, String name) {
        super(name);

        matchStart = parseRule(lhs, 0, 1, groups);

        int pLen = rhs.length;
        produc = new String[pLen][];
        lookup = new boolean[pLen][];
        for (int i = 0; i < pLen; i++) {
            produc[i] = rhs[i].split("(?=\\[)|(?<=\\])");
            lookup[i] = getLookup(produc[i]);
        }
    }

    /**
     * @param lhs Accepted input of the rule
     * @param rhs Produced outputs of the rule
     * @param groups Pre-defined variables that might occur in the rule
     */
    public MorphRule(String lhs, String[] rhs, Map<String, String[]> groups) {
        this(lhs, rhs, groups, "");
    }

    /**
     * Create the lookup array for a split rhs.
     * @param produc The rhs, split into sequences of literals and variables
     * @return A boolean array, containing true if the correspoding item in the produc array is a variable, else false
     */
    private boolean[] getLookup(String[] produc) {
        boolean[] lookup = new boolean[produc.length];
        for (int i = 0; i < produc.length; i++) {
            String pi = produc[i];
            if (pi.charAt(0) == '[') {
                produc[i] = pi.substring(1, pi.length() - 1);
                lookup[i] = true;
            }
        }
        return lookup;
    }

    /**
     * Recursively parse the string representation of the lhs of a rule into the automaton representation.
     * @param rule Input side of the rule as string
     * @param i Current index in the rule
     * @param varCount Number label of next unlabeled variable
     * @param groups Pre-defined variables that might occur in the rule
     * @return Start state of the rule automaton
     */
    private RuleState parseRule(String rule, int i, int varCount, Map<String, String[]> groups) {
        // Final state if rule has been parsed completely
        if (i >= rule.length())
            return new FinalState();

        // Encountering a variable reference or declaration
        if (rule.charAt(i) == '[') {
            // Get closing bracket
            int j = rule.indexOf(']', i);
            if (j < 0)
                System.err.println(name + ": No closing bracket for variable " + rule.substring(i));
            else {
                String varName = rule.substring(i+1, j);
                // Determine type of variable
                boolean optional = varName.charAt(0) == '?';
                boolean group = (optional && varName.charAt(1) == '#') || varName.charAt(0) == '#';
                boolean looseGroup = (optional && !group) || varName.charAt(0) == '!';
                // If variable with fixed value:
                if (optional || group || looseGroup) {
                    // Remove special characters '?' and '!'
                    if (optional || looseGroup)
                        varName = varName.substring(1);
                    // Check if valid group label
                    if (group && !groups.containsKey(varName))
                        System.err.println(name + ": Couldn't find group " + varName);
                    else {
                        // Create state
                        String[] alternatives = (group)
                                ? groups.get(varName).clone()
                                : StringUtils.split(varName, ' ');
                        if (optional)
                            return new OptionalDisjunctiveState(alternatives,
                                    parseRule(rule, j+1, varCount+1, groups),
                                    Integer.toString(varCount));
                        else
                            return new DisjunctiveState(alternatives.clone(),
                                    parseRule(rule, j+1, varCount+1, groups),
                                    Integer.toString(varCount));
                    }
                }
                // If variable of arbitrary content:
                else {
                    if (varName.equals("*")) {
                        varName = Integer.toString(varCount);
                        varCount++;
                    }
                    return new VariableState(parseRule(rule, j+1, varCount, groups), varName);
                }
            }
        }

        // If not a variable, create literal state
        return new LiteralState(rule.charAt(i), parseRule(rule, i+1, varCount, groups));
    }

    /**
     * Apply rule to a string input.
     * @param s Input string
     * @return Outputs generated by the rule, null if rule not applicable
     */
    @Override
    public MorphRuleResult apply(String orig, String s) {
        System.err.println(name + " " + s);
        Map<String, String> vars = new HashMap<>();
        TCharList seps = new TCharArrayList();
        if (matchStart.match(s, 0, vars, seps)) {
            String[] res = new String[produc.length];
            for (int i = 0; i < produc.length; i++) {
                StringBuilder r = new StringBuilder();
                for (int j = 0; j < produc[i].length; j++) {
                    if (lookup[i][j]) {
                        String var = vars.get(produc[i][j]);
                        if (var == null)
                            System.err.println(name + ": Couldn't find variable " + produc[i][j]);
                        else
                            r.append(var);
                    }
                    else
                        r.append(produc[i][j]);
                }
                res[i] = r.toString();
            }
            return new MorphRuleResult(fillSeps(orig, seps), s, res);
        }
        return null;
    }

    private String fillSeps(String word, TCharList seps) {
        StringBuilder filled = new StringBuilder();
        int i = 0;
        for (int s = 0; s < seps.size(); s++) {
            int j = word.indexOf(' ', i);
            if (j >= 0) {
                filled.append(word, i, j).append(seps.get(s));
                if (seps.get(s) == '<') {
                    filled.append('>');
                    s++;
                }
                i = j + 1;
            }
        }
        return filled.append(word, i, word.length()).toString();
    }

    /**
     * @param c A character
     * @return True if c is a morphological separator
     */
    private static boolean isSeparator(char c) {
        return c == '|' || c == '&' || c == '<' || c == '>';
    }


    /**
     * Interface for a state in a morphological rule automaton.
     */
    private interface RuleState {
        /**
         * Determine whether the string at its current position is accepted by this state and create ad-hoc variables.
         * @param s Input string
         * @param i Current index in string
         * @param vars Accumulator for values of variables declared by rule (that have to be extracted from the input
         *             string), will be inserted by this method.
         * @return True if string is matched, false if not
         */
        boolean match(String s, int i, Map<String, String> vars, TCharList seps);
    }

    /**
     * Final (accepting) state of a morphological rule automaton.
     */
    private class FinalState implements RuleState {

        FinalState() {}

        @Override
        public boolean match(String s, int i, Map<String, String> vars, TCharList seps) {
            return s.length() <= i || (isSeparator(s.charAt(i)) && this.match(s, i+1, vars, seps));
        }
    }

    /**
     * State accepting a single literal character (ignoring intervening separators).
     */
    private class LiteralState implements RuleState {
        char transition;
        private RuleState toState;

        LiteralState(char transition, RuleState toState) {
            this.transition = transition;
            this.toState = toState;
        }

        @Override
        public boolean match(String s, int i, Map<String, String> vars, TCharList seps) {
            if (s.length() <= i)
                return false;
            char c = s.charAt(i);
            if (c == ' ' && isSeparator(transition) && toState.match(s, (transition == '<') ? i : i+1, vars, seps)) {
                seps.insert(0, transition);
                return true;
            }
            return (c == transition && toState.match(s, i+1, vars, seps)
                    || isSeparator(c) && this.match(s, i+1, vars, seps));
        }
    }

    /**
     * State accepting one of multiple literal strings, storing the matching one in a variable.
     */
    private class DisjunctiveState implements RuleState {
        private String[] transitions;
        private RuleState toState;
        private String varName;

        DisjunctiveState(String[] transitions, RuleState toState, String varName) {
            this.transitions = transitions;
            this.toState = toState;
            this.varName = varName;
        }

        @Override
        public boolean match(String s, int i, Map<String, String> vars, TCharList seps) {
            if (s.length() <= i)
                return false;
            char c = s.charAt(i);
            for (String sub : transitions) {
                if (s.startsWith(sub, i) && toState.match(s, i + sub.length(), vars, seps)) {
                    vars.put(varName, sub);
                    return true;
                }
            }
            return isSeparator(c) && this.match(s, i+1, vars, seps);
        }
    }

    /**
     * State optionally accepting one of multiple literal strings, storing the matching one (or none if nothing matches)
     * in a variable.
     */
    private class OptionalDisjunctiveState extends DisjunctiveState {

        OptionalDisjunctiveState(String[] transitions, RuleState toState, String varName) {
            super(transitions, toState, varName);
        }

        @Override
        public boolean match(String s, int i, Map<String, String> vars, TCharList seps) {
            if (super.match(s, i, vars, seps))
                return true;
            if (super.toState.match(s, i, vars, seps)) {
                vars.put(super.varName, "");
                return true;
            }
            return false;
        }
    }

    /**
     * State accepting any number of arbitrary characters (or none) until the next state matches, storing the
     * accepted string in a variable.
     */
    private class VariableState implements RuleState {
        private RuleState toState;
        private String varName;

        VariableState(RuleState toState, String varName) {
            this.toState = toState;
            this.varName = varName;
        }

        @Override
        public boolean match(String s, int i, Map<String, String> vars, TCharList seps) {
            if (s.length() <= i) {
                if (toState.match(s, i, vars, seps)) {
                    vars.put(varName, "");
                    return true;
                }
                else
                    return false;
            }
            for (int j = i; j <= s.length(); j++) {
                if (toState.match(s, j, vars, seps)) {
                    vars.put(varName, s.substring(i, j));
                    return true;
                }
            }
            return false;
        }
    }

}
