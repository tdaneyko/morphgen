package de.tuebingen.sfs.morphgen;

public class MorphRuleResult {

    private String orig;
    private String split;
    private String[] results;

    public MorphRuleResult(String orig, String split, String[] results) {
        this.orig = orig;
        this.split = split;
        this.results = results;
    }

    public String getOrig() {
        return orig;
    }

    public String getSplit() {
        return split;
    }

    public String[] getResults() {
        return results;
    }
}
