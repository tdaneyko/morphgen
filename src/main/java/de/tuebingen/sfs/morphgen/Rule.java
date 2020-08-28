package de.tuebingen.sfs.morphgen;

public abstract class Rule {

    String name;

    private Rule() {
        this("<?>");
    }

    protected Rule(String name) {
        this.name = name;
    }

    /**
     * Apply rule to a string input.
     * @param s Input string
     * @return Outputs generated by the rule, null if rule not applicable
     */
    public MorphRuleResult apply(String s) {
        return apply(s, s);
    }

    /**
     * Apply rule to a string input.
     * @param s Input string
     * @return Outputs generated by the rule, null if rule not applicable
     */
    public abstract MorphRuleResult apply(String orig, String s);
}
