import junit.framework.TestCase;
import de.tuebingen.sfs.morphgen.GlossedWord;
import de.tuebingen.sfs.morphgen.MorphGen;
import de.tuebingen.sfs.morphgen.MorphRule;

import static org.junit.Assert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.CoreMatchers.*;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class MorphGenTest extends TestCase {

    /**
     * Test simple Finnish rules.
     */
    public void testRulesFin() {
        String s = "joki{gensg=joen}{parsg=jokea}{parpl=jokia}{vh=a}";
        Map<String, String[]> finGroups = new HashMap<>();
        MorphRule finGEN = new MorphRule("[*]{gensg=[genstem]n}[*]|GEN", new String[]{"[genstem]|n"}, finGroups);
        MorphRule finINE = new MorphRule("[*]{gensg=[genstem]n}[*]{vh=[A]}|INE", new String[]{"[genstem]|ss[A]"}, finGroups);

        assertEquals("joe|n", finGEN.apply(s+"|GEN").getResults()[0]);
        assertEquals("joe|ssa", finINE.apply(s+"|INE").getResults()[0]);
    }

    /**
     * Test simple Arabic rules.
     */
    public void testRulesAra() {
        Map<String, String[]> araGroups = new HashMap<>();
        MorphRule ara = new MorphRule("[C1]a[C2]a[C3]a{th=[th]}&IPF&1SG", new String[]{"ja[C1][C2][th][C3]u"}, araGroups);

        assertEquals("jaktubu", ara.apply("kataba{th=u}&IPF&1SG").getResults()[0]);
    }

    /**
     * Test simple Malayalam rules.
     */
    public void testRulesMal() {
        Map<String, String[]> malGroups = new HashMap<>();
        MorphRule malPL = new MorphRule("[*];m|PL", new String[]{"[1];n|;na.l"}, malGroups);
        MorphRule malGEN1 = new MorphRule("[*][!l r]|GEN", new String[]{"[1][2]|in_re"}, malGroups);
        MorphRule malGEN2 = new MorphRule("[*]|GEN", new String[]{"[1]|u.te"}, malGroups);

        assertEquals("pa_la;n|;na.l", malPL.apply("pa_la;m|PL").getResults()[0]);
        assertEquals("pa_la;n|;na.l|u.te", malGEN2.apply("pa_la;n|;na.l|GEN").getResults()[0]);
        assertEquals("ka.tal|in_re", malGEN1.apply("ka.tal|GEN").getResults()[0]);
    }

    /**
     * Test simple Malayalam morph gen.
     */
    public void testSimpleGenMal() {
        MorphGen malGen = new MorphGen("/mal-rules-simple.tsv", "/mal-affixes.tsv");

        assertEquals("pa_la;n|;na.l", new ArrayList<>(malGen.generate("pa_la;m|PL")).get(0).getForm());
        assertEquals("pa_la;n|;na.l|u.te", new ArrayList<>(malGen.generate("pa_la;m|PL|GEN")).get(0).getForm());
        assertEquals("ka.tal|in_re", new ArrayList<>(malGen.generate("ka.tal|GEN")).get(0).getForm());

        assertThat(malGen.generate("pa_la;m PL"), hasSize(1));
        assertThat(malGen.generate("pa_la;m PL GEN"), hasSize(1));
        assertThat(malGen.generate("ka.tal GEN"), hasSize(1));

        assertEquals("pa_la;n|;na.l", new ArrayList<>(malGen.generate("pa_la;m PL")).get(0).getForm());
        assertEquals("pa_la;n|;na.l|u.te", new ArrayList<>(malGen.generate("pa_la;m PL GEN")).get(0).getForm());
        assertEquals("ka.tal|in_re", new ArrayList<>(malGen.generate("ka.tal GEN")).get(0).getForm());
    }

    /**
     * Test the complete Malayalam morph gen on a number of nouns.
     */
    public void testFullGenNounsMal() {
        MorphGen malGen = new MorphGen("/mal-rules.tsv", "/mal-affixes.tsv");
        try (BufferedReader read = new BufferedReader(new InputStreamReader(new FileInputStream(new File("src/test/resources/mal-test-nouns.tsv")), "UTF-8"))) {
            for (String line = read.readLine(); line != null; line = read.readLine()) {
                String[] fields = line.split("\t");
                if (fields.length == 2) {
                    List<GlossedWord> gens = new ArrayList<>(malGen.generate(fields[0]));
                    assertEquals(1, gens.size());
                    assertEquals(fields[1], gens.get(0).getForm());
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Test the complete Malayalam morph gen on the pronouns.
     */
    public void testFullGenPronounsMal() {
        MorphGen malGen = new MorphGen("/mal-rules.tsv", "/mal-affixes.tsv");
        try (BufferedReader read = new BufferedReader(new InputStreamReader(new FileInputStream(new File("src/test/resources/mal-test-pronouns.tsv")), "UTF-8"))) {
            for (String line = read.readLine(); line != null; line = read.readLine()) {
                String[] fields = line.split("\t");
                if (fields.length == 2) {
                    List<GlossedWord> gens = new ArrayList<>(malGen.generate(fields[0]));
                    GlossedWord[] expected = Arrays
                            .stream(fields[1].split(" \\|\\| "))
                            .map(str -> new GlossedWord(fields[0], str))
                            .toArray(GlossedWord[]::new);
                    assertThat(gens, hasSize(expected.length));
                    for (GlossedWord exp : expected)
                        assertThat(gens, hasItem(exp));
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Test the complete Malayalam morph gen on past tense verbs.
     */
    public void testFullGenVerbsPstMal() {
        MorphGen malGen = new MorphGen("/mal-rules.tsv", "/mal-affixes.tsv");
        try (BufferedReader read = new BufferedReader(new InputStreamReader(new FileInputStream(new File("src/test/resources/mal-test-verbs-pst.tsv")), "UTF-8"))) {
            for (String line = read.readLine(); line != null; line = read.readLine()) {
                String[] fields = line.split("\t");
                if (fields.length == 2) {
                    List<GlossedWord> gens = new ArrayList<>(malGen.generate(fields[0]));
                    GlossedWord[] expected = Arrays
                            .stream(fields[1].split(" \\|\\| "))
                            .map(str -> new GlossedWord(fields[0], str))
                            .toArray(GlossedWord[]::new);
                    assertThat(gens, hasSize(expected.length));
                    for (GlossedWord exp : expected)
                        assertThat(gens, hasItem(exp));
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Test the complete Malayalam morph gen on a number of verbs.
     */
    public void testFullGenVerbsMal() {
        MorphGen malGen = new MorphGen("/mal-rules.tsv", "/mal-affixes.tsv");
        try (BufferedReader read = new BufferedReader(new InputStreamReader(new FileInputStream(new File("src/test/resources/mal-test-verbs.tsv")), "UTF-8"))) {
            for (String line = read.readLine(); line != null; line = read.readLine()) {
                String[] fields = line.split("\t");
                if (fields.length == 2) {
                    List<GlossedWord> gens = new ArrayList<>(malGen.generate(fields[0]));
                    GlossedWord[] expected = Arrays
                            .stream(fields[1].split(" \\|\\| "))
                            .map(str -> new GlossedWord(fields[0], str))
                            .toArray(GlossedWord[]::new);
                    assertThat(gens, hasSize(expected.length));
                    for (GlossedWord exp : expected)
                        assertThat(gens, hasItem(exp));
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Test the paradigm generation of the Malayalam morph gen on a noun and a verb.
     */
    public void testParadigmMal() {
        MorphGen malGen = new MorphGen("/mal-rules.tsv", "/mal-affixes.tsv");

        Set<String> expectedNouns = new HashSet<>(Arrays.asList(
                "puucca","puucca NOM","puucca ACC","puucca DAT","puucca GEN","puucca SOC","puucca LOC","puucca INS",
                "puucca PL","puucca PL NOM","puucca PL ACC","puucca PL DAT","puucca PL GEN","puucca PL SOC",
                "puucca PL LOC","puucca PL INS"));
        assertEquals(expectedNouns, malGen.getParadigm("puucca", "ntest"));

        Set<String> expectedVerbs = new HashSet<>(Arrays.asList(
                "varuka","varuka PRS","varuka PST","varuka PST STAT","varuka HAB","varuka INT","varuka DES",
                "varuka HOR","varuka NEG","varuka PRS NEG","varuka PST NEG","varuka PST STAT NEG","varuka HAB NEG",
                "varuka INT NEG","varuka DES NEG","varuka HOR NEG","varuka A","varuka PRS A","varuka PST A",
                "varuka PST STAT A","varuka HAB A","varuka INT A","varuka DES A","varuka HOR A","varuka GER",
                "varuka IMP","varuka PAM","varuka NEG PAM"));
        assertEquals(expectedVerbs, malGen.getParadigm("varuka", "vtest"));
    }

    /**
     * Test the inflection paradigm generation of the Malayalam morph gen on the noun "puucca" (cat).
     */
    public void testInflectionsMal() {
        MorphGen malGen = new MorphGen("/mal-rules.tsv", "/mal-affixes.tsv");

        Set<String> puuccaExpected = new HashSet<>(Arrays.asList(
                "puucca","puucca|ye","puucca|kk^u","puucca|yu.te","puucca|yoo.t^u","puucca|yil","puucca|yaal",
                "puucca|ka.l","puucca|ka.l|e","puucca|ka.l|kk^u","puucca|ka.l|u.te","puucca|ka.l|oo.t^u","puucca|ka.l|il",
                "puucca|ka.l|aal","puucca|maar","puucca|maar|e","puucca|maar|in^u","puucca|maar|in_re",
                "puucca|maar|oo.t^u","puucca|maar|il","puucca|maar|aal"));
        assertEquals(puuccaExpected, malGen.getInflections("puucca", "ntest").stream().map(GlossedWord::getForm).collect(Collectors.toSet()));

        Set<String> puuccaExpected2 = new HashSet<>(Arrays.asList(
                "puucca","puucca|ye","puucca|kk^u","puucca|yu.te","puucca|yoo.t^u","puucca|yil","puucca|yaal","puucca|ka.l",
                "puucca|ka.l|e","puucca|ka.l|kk^u","puucca|ka.l|u.te","puucca|ka.l|oo.t^u","puucca|ka.l|il","puucca|ka.l|aal"));
        assertEquals(puuccaExpected2, malGen.getInflections("puucca{cl=nhum}", "ntest").stream().map(GlossedWord::getForm).collect(Collectors.toSet()));
    }

    /**
     * Test the Malayalam number generator.
     */
    public void testNumberGenerationMal() {
        MorphGen malGen = new MorphGen("/mal-rules.tsv", "/mal-affixes.tsv");

        Set<String> nums = new HashSet<>(Arrays.asList("+2 *10 onn^u", "+2 onn^u", "*10 onn^u", "onn^u"));
        assertEquals(nums, malGen.getParadigm("onn^u", "numtest"));

        assertEquals(8, malGen.generate("+3|*100|+5|*10|onn^u").size());
    }

    /**
     * Test simple Ryka rules (infixes!).
     */
    public void testRulesRyk() {
        Map<String, String[]> rykGroups = new HashMap<>();
        rykGroups.put("#C", new String[]{"kh","th","ph","sh","h","k","t","p","r","q","g","d","b","l"});
        rykGroups.put("#V", new String[]{"a","e","o","u","y","n"});
        rykGroups.put("#sep", new String[]{"|","&","<",">","<>"});
        MorphRule rykPC1 = new MorphRule("[stem][#C][#V][#C]<>PC", new String[]{"[stem][1][2]<[1][2]>[3]"}, rykGroups);
        MorphRule rykPC2 = new MorphRule("[stem][#C][#V]<>PC", new String[]{"[stem][1][2]<[1][2]>"}, rykGroups);
        MorphRule rykINE1 = new MorphRule("[stem][?#sep][#C]|INE", new String[]{"[stem][1][2]|er"}, rykGroups);
        MorphRule rykINE2 = new MorphRule("[stem][#V]|INE", new String[]{"[stem][1]|r"}, rykGroups);

        assertEquals("hethe<the>l", rykPC1.apply("hethel<>PC").getResults()[0]);
        assertEquals("hethel|er", rykINE1.apply("hethel|INE").getResults()[0]);
        assertEquals("hethe<the>l|er", rykINE1.apply("hethe<the>l|INE").getResults()[0]);
        assertEquals("daky<ky>", rykPC2.apply("daky<>PC").getResults()[0]);
        assertEquals("daky|r", rykINE2.apply("daky|INE").getResults()[0]);
        //assertEquals("daky<ky>|r", rykINE2.apply("daky<ky>-INE").getResults()[0]);
    }

    /**
     * Test simple Ryka morph gen (infixes!).
     */
    public void testSimpleGenRyk() {
        MorphGen rykGen = new MorphGen("/ryk-rules.tsv");

        assertEquals("hethe<the>l", new ArrayList<>(rykGen.generate("hethel<>PC")).get(0).getForm());
        assertEquals("hethel|er", new ArrayList<>(rykGen.generate("hethel|INE")).get(0).getForm());
        assertEquals("hethe<the>l|er", new ArrayList<>(rykGen.generate("hethel<>PC|INE")).get(0).getForm());
    }
}
