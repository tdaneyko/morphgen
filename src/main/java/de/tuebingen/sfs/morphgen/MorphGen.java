package de.tuebingen.sfs.morphgen;

import de.tuebingen.sfs.utils.StringUtils;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

/**
 * A morphology generator which can inflect words and create complete paradigms for them based on
 * paradigm and rule files.
 */
public class MorphGen {

    // A regex that can never match, default for sWithGloss with paradigms
    private static final String NEVER_MATCHING = "(?!x)x";
    // A regex that can never match, default for sWithGloss with paradigms
    private static final Pattern SEPARATORS = Pattern.compile("[|&<>]");
    // A regex matching word boundary symbols
    private static final Pattern WORD_BOUNDS = Pattern.compile("(\\A#)|(#\\z)");

    private static final Pattern PARADIGM_SPECIAL_CHARS = Pattern.compile("[()| _]+");

    // The rules to apply, in order
    private List<Rule> rules;
    // A map from POS to respective paradigm
    private Map<String, Paradigm> paradigms;
    // Regex matching strings that still contain glosses, will never match when not given paradigms
    private Pattern strWithGloss;

    /**
     * Create a morph gen with only rules, from a list of pre-created rules.
     * @param rules A list of rules, in order of application
     */
    public MorphGen(List<Rule> rules) {
        this.rules = new ArrayList<>(rules);
        this.paradigms = new HashMap<>();
        this.strWithGloss = Pattern.compile(NEVER_MATCHING);
    }

    /**
     * Create a morph gen with only rules, from a rule file.
     * @param ruleFile The path to the rule file
     */
    public MorphGen(String ruleFile) {
        this.rules = new ArrayList<>();
        readRules(ruleFile);
        this.paradigms = new HashMap<>();
        this.strWithGloss = Pattern.compile(NEVER_MATCHING);
    }

    /**
     * Create a morph gen with both rules and paradigms, from a rule file and a paradigm file.
     * @param ruleFile The path to the rule file
     * @param paradigmFile The path to the paradigm file
     */
    public MorphGen(String ruleFile, String paradigmFile) {
        this.rules = new ArrayList<>();
        readRules(ruleFile);
        this.paradigms = new HashMap<>();
        readParadigms(paradigmFile);
    }

    /**
     * Parse the rule file into a list of rules.
     * @param ruleFile The path to the rule file
     */
    private void readRules(String ruleFile) {
        try (BufferedReader read = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(ruleFile), StandardCharsets.UTF_8))) {
            Map<String, String[]> groups = new HashMap<>();
            for (String line = read.readLine(); line != null; line = read.readLine()) {
                // Group definition
                if (line.startsWith("#def")) {
                    String[] fields = StringUtils.split(line, '\t');
                    if (fields.length == 3)
                        groups.put(fields[1], StringUtils.split(fields[2].substring(1, fields[2].length()-1),' '));
                    else
                        System.err.println("Unknown group definition format: " + line);
                }
                // Rule
                else if (!line.isEmpty() && !line.startsWith("//")) {
                    String[] fields = StringUtils.split(line, '\t');
                    if (fields.length == 2 || fields.length == 1) {
                        String lhs = fields[0];
                        String[] rhs = (fields.length == 2) ? StringUtils.split(fields[1], " || ") : new String[]{""};

                        if (lhs.charAt(0) == '*') {
                            rules.add(new ReplaceRule(lhs.substring(1), rhs, groups, line));
                        }
                        else {
                            // Insert start and end wildcards, if needed, to make sure that previously appended prefixes
                            // and suffixes are not deleted and can be matched.
                            String start = getStartWildcard(lhs);
                            if (start.equals("[§start]"))
                                lhs = start + lhs;
                            for (int r = 0; r < rhs.length; r++) {
                                if (!rhs[r].contains(start))
                                    rhs[r] = start + rhs[r];
                                if (!lhs.endsWith("#"))
                                    rhs[r] = rhs[r] + "[§end]";
                            }
                            if (!lhs.endsWith("#"))
                                lhs = lhs + "[§end]";
                            rules.add(new MorphRule(WORD_BOUNDS.matcher(lhs).replaceAll(""), rhs, groups, line));
                        }
                    }
                    else
                        System.err.println("Unknown rule format: " + line);
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Get the rhs label of an initial wildcard on the lhs, return label [§start] if there is none.
     * @param lhs Input side of a rule
     * @return Initial wildcard label on output side
     */
    private static String getStartWildcard(String lhs) {
        if (lhs.charAt(0) == '#')
            return "";
        if (lhs.startsWith("[*]"))
            return "[1]";
        if (lhs.charAt(0) == '[' && lhs.charAt(1) != '#' && lhs.charAt(1) != '!' && lhs.charAt(1) != '?') {
            int x = lhs.indexOf(']');
            if (x < 0)
                return "[§start]";
            return lhs.substring(0, x);
        }
        return "[§start]";
    }

    /**
     * Parse the paradigm file into a map from POS label to paradigm and create the gloss regex.
     * @param paradigmFile The path to the paradigm file
     */
    private void readParadigms(String paradigmFile) {
        try (BufferedReader read = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(paradigmFile), StandardCharsets.UTF_8))) {
            Set<String> glosses = new HashSet<>();
            for (String line = read.readLine(); line != null; line = read.readLine()) {
                if (!line.isEmpty()) {
                    int p = line.indexOf('[');
                    int s = line.indexOf(']');
                    if (p >= 0 && s >= 0) {
                        String prefix = line.substring(0, p);
                        String suffix = line.substring(s+1);
                        String pos = line.substring(p+1, s);
                        Paradigm par = new Paradigm(prefix, suffix);
                        paradigms.put(pos, par);
                        for (String gloss : PARADIGM_SPECIAL_CHARS.split(prefix))
                            glosses.add(gloss);
                        for (String gloss : PARADIGM_SPECIAL_CHARS.split(suffix))
                            glosses.add(gloss);
                    }
                    else
                        System.err.println("Wrong format: " + line);
                }
            }
            glosses.remove("");
            StringBuilder regex = new StringBuilder(".*((");
            for (String gloss : glosses)
                regex.append(gloss
                        .replace('_', ' ')
                        .replace("+", "\\+")
                        .replace("*", "\\*"))
                        .append(")|(");
            regex.append("[|&<>][|&<>]+)).*");
            strWithGloss = Pattern.compile(regex.toString());
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Generate realizations for input gloss.
     * @param in A glossed word
     * @return Realizations of that word
     */
    public Set<GlossedWord> generate(String in) {
        Set<GlossedWord> ins = new HashSet<>();
        ins.add(new GlossedWord(in, in));
//        for (String realization : allPossibleRealizations(in))
//            ins.add(new GlossedWord(realization, realization));
        return generate(ins);
    }

    /**
     * Generate realizations for input glosses.
     * @param ins A set of glossed words
     * @return Realizations of these words
     */
    public Set<GlossedWord> generate(Set<GlossedWord> ins) {
        Set<GlossedWord> outs = ins;
        for (Rule rule : rules) {
            ins = outs;
            outs = new HashSet<>();
            for (GlossedWord in : ins) {
                MorphRuleResult res = rule.apply(in.getGloss(), in.getForm());
                if (res != null) {
                    String[] outz = res.getResults();
                    for (String out : outz)
                        outs.add(new GlossedWord(res.getOrig(), out));
                }
                else
                    outs.add(in);
            }
        }
        outs.removeIf(out -> strWithGloss.matcher(out.getForm()).matches());
        return outs;
    }

    /**
     * Get the paradigm of possible glosses for a raw word.
     * @param word A word
     * @param pos The POS of that word
     * @return The complete paradigm for that word
     */
    public Set<String> getParadigm(String word, String pos) {
        Paradigm par = paradigms.get(pos);
        if (par != null)
            return par.getParadigm(word);
        System.err.println("Unknown POS: " + pos);
        Set<String> s = new HashSet<>();
        s.add(word);
        return s;
    }

    /**
     * Get the paradigm of possible inflections for a raw word.
     * @param word A word
     * @param pos The POS of that word
     * @return The complete inflected paradigm for that word
     */
    public Set<GlossedWord> getInflections(String word, String pos) {
        Set<GlossedWord> inflections = new HashSet<>();
        Set<String> paradigm = getParadigm(word, pos);
        for (String template : paradigm)
            inflections.addAll(generate(template));
        return inflections;
    }

    /**
     * Get all possible forms of the words in a list and print them to a file.
     * @param infile A list with tab-separated lemma, pos and translations in each line
     * @param outfile A list of all possible inflections of the vocabulary, with translations and glosses
     */
    public void unfoldVocabulary(String infile, String outfile) {
        unfoldVocabulary(infile, outfile, false);
    }

    /**
     * Get all possible forms of the words in a list and print them to a file.
     * @param infile A list with tab-separated lemma, pos and translations in each line
     * @param outfile A list of all possible inflections of the vocabulary, with translations and glosses
     * @param append Append to outfile instead of overwriting it
     */
    public void unfoldVocabulary(String infile, String outfile, boolean append) {
        List<String> previous = new ArrayList<>();
        Set<String> forms = new HashSet<>();
        if (append) {
            try (BufferedReader read = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(outfile), StandardCharsets.UTF_8))) {
                for (String line = read.readLine(); line != null; line = read.readLine()) {
                    if (!line.isEmpty()) {
                        previous.add(line);
                        int i = line.indexOf('\t');
                        if (i >= 0 && (line.contains("|") || line.contains("<>")))
                            forms.add(line.substring(0, i));
                    }
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        try (BufferedReader read = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(infile), StandardCharsets.UTF_8));
             PrintWriter writ = new PrintWriter(new OutputStreamWriter(new FileOutputStream(new File(outfile)), Charset.forName("UTF-8")))) {
            for (String oldLine : previous)
                writ.println(oldLine);
            for (String line = read.readLine(); line != null; line = read.readLine()) {
                if (!line.isEmpty()) {
                    String[] fields = StringUtils.split(line, '\t');
                    if (fields.length == 3) {
                        if (!forms.contains(fields[0])) {
                            Set<GlossedWord> infl = getInflections(fields[0], fields[1]);
                            Map<String, List<GlossedWord>> splits = new HashMap<>();
                            for (GlossedWord gw : infl) {
                                if (!splits.containsKey(gw.getForm()))
                                    splits.put(gw.getForm(), new ArrayList<>());
                                splits.get(gw.getForm()).add(gw);
                            }
                            for (String form : splits.keySet()) {
                                String phon = SEPARATORS.matcher(form).replaceAll("");
                                if ((form.contains("|") || form.contains("<>")))
                                    forms.add(phon);
                                String prefixes = "";
                                String suffixes = "";
                                for (GlossedWord gw : splits.get(form)) {
                                    String gloss = gw.getGloss();
                                    int s = gloss.indexOf(fields[0]);
                                    prefixes += gloss.substring(0, s) + "/";
                                    suffixes += gloss.substring(s + fields[0].length()) + "/";
                                }
                                writ.println(phon + "\t" + form + "\t"
                                        + prefixes.substring(0, prefixes.length()-1) + "\t" + fields[2] + "\t"
                                        + suffixes.substring(0, suffixes.length()-1));
                            }
//                            for (GlossedWord gw : infl) {
//                                String form = gw.getForm();
//                                String phon = SEPARATORS.matcher(form).replaceAll("");
//                                String gloss = gw.getGloss();
//                                if ((form.contains("|") || form.contains("<>")))
//                                    forms.add(phon);
//                                int s = gloss.indexOf(fields[0]);
//                                writ.println(phon + "\t" + form + "\t"
//                                        + gloss.substring(0, s) + "\t" + fields[2] + "\t"
//                                        + gloss.substring(s + fields[0].length()));
////                            for (String transl : fields[2].split("/")) {
////                                writ.println(phon + "\t" + form + "\t" + gloss.replace(fields[0], transl));
////                            }
//                            }
                        }
                        else
                            System.err.println("Entry exists as inflected form: " + fields[0]);
                    }
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {
        /// FINNISH ///
        Map<String, String[]> finGroups = new HashMap<>();
        MorphRule finGEN = new MorphRule("[*]{gensg=[genstem]n}[*]|GEN", new String[]{"[genstem]|n"}, finGroups);
        MorphRule finINE = new MorphRule("[*]{gensg=[genstem]n}[*]{vh=[A]}|INE", new String[]{"[genstem]|ss[A]"}, finGroups);

        System.out.println("FINNISH:");
        String s = "joki{gensg=joen}{parsg=jokea}{parpl=jokia}{vh=a}";
        System.out.println("s = " + s);
        System.out.println("finGEN = [*]{gensg=[genstem]n}[*]|GEN => [genstem]|n");
        System.out.println("finINE = [*]{gensg=[genstem]n}[*]{vh=[A]}|INE => [genstem]|ss[A]");
        System.out.println("finGEN.apply(s+\"|GEN\") = " + finGEN.apply(s+"|GEN").getResults()[0]); // Should be joe|n
        System.out.println("finINE.apply(s+\"|INE\") = " + finINE.apply(s+"|INE").getResults()[0]); // Should be joe|ssa
        System.out.println();

        /// ARABIC ///
        Map<String, String[]> araGroups = new HashMap<>();
        MorphRule ara = new MorphRule("[C1]a[C2]a[C3]a{th=[th]}&IPF&1SG", new String[]{"ja[C1][C2][th][C3]u"}, araGroups);

        System.out.println("ARABIC:");
        System.out.println("ara = [C1]a[C2]a[C3]a{th=[th]}&IPF&1SG => ja[C1][C2][th][C3]u");
        System.out.println("ara.apply(\"kataba{th=u}&IPF&1SG\") = " + ara.apply("kataba{th=u}&IPF&1SG").getResults()[0]); // Should be jaktubu
        System.out.println();

        /// MALAYALAM ///
        MorphGen malGen = new MorphGen("/mal-rules.tsv", "/mal-affixes.tsv");

        System.out.println("MALAYALAM:");

        System.out.println("generate(\"pustaka;m|GEN\"): ");
        for (GlossedWord gw : malGen.generate("pustaka;m|GEN"))
            System.out.println("\t" + gw.getGloss() + " = " + gw.getForm());
        System.out.println("generate(\"keeral.a;m LOC\"): ");
        for (GlossedWord gw : malGen.generate("keeral.a;m LOC"))
            System.out.println("\t" + gw.getGloss() + " = " + gw.getForm());
        System.out.println("generate(\"kaa.nuka{pst=tu}|PST|STAT|NEG\"): ");
        for (GlossedWord gw : malGen.generate("kaa.nuka{pst=tu}|PST|STAT|NEG"))
            System.out.println("\t" + gw.getGloss() + " = " + gw.getForm());

        System.out.println("getParadigm(\"puucca\", \"n\"): ");
        for (String infl : malGen.getParadigm("puucca", "n"))
            System.out.println("\t" + infl);

        System.out.println("getInflections(\"puucca\", \"n\"): ");
        for (GlossedWord gw : malGen.getInflections("puucca", "n"))
            System.out.println("\t" + gw.getGloss() + " = " + gw.getForm());
    }
}
