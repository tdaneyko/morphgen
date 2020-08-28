package de.tuebingen.sfs.morphgen;

/**
 * An inflected word with a gloss.
 */
public class GlossedWord {

    private String gloss;
    private String form;

    public GlossedWord(String gloss, String form) {
        this.gloss = gloss;
        this.form = form;
    }

    /**
     * Get the gloss of the word.
     * (e.g. "word|PL")
     * @return The word's gloss
     */
    public String getGloss() {
        return gloss;
    }

    /**
     * Get the inflected form of the word.
     * (e.g. "words")
     * @return The word's form
     */
    public String getForm() {
        return form;
    }

    @Override
    public String toString() {
        return gloss + "\t" + form;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof GlossedWord) {
            GlossedWord otherGl = (GlossedWord) other;
            return this.form.equals(otherGl.form) && this.gloss.equals(otherGl.gloss);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 13 + 7 * form.hashCode() + 11 * gloss.hashCode();
    }
}
