//package morphgen;
//
//import java.util.List;
//
//public class MorphAutomaton {
//
//
//    private class MorphState {
//
//        private List<MorphTransition> transitions;
//
//        public Word get(String s, int i) {
//            if (!transitions.isEmpty()) {
//                for (MorphTransition transition : transitions) {
//                    Word w = transition.get(s, i);
//                    if (w != null)
//                        return w;
//                }
//            }
//            return new Word("", "");
//        }
//
//    }
//
//    private class Word {
//        private StringBuilder split;
//        private StringBuilder gloss;
//
//        public Word(String split, String gloss) {
//            this.split = new StringBuilder(split);
//            this.gloss = new StringBuilder(gloss);
//        }
//
//        public void prepend(String split, String gloss) {
//            this.split.insert(0, split);
//            this.gloss.insert(0, gloss);
//        }
//    }
//
//    private interface MorphTransition {
//
//    }
//
//    private class RootPlaceholder implements MorphTransition {
//
//        private String pos;
//
//        public RootTransition fill(String word, String pos) {
//            return null;
//        }
//
//    }
//
//    private class UnfilledTransition implements MorphTransition {
//
//        String feature;
//
//        public FilledTransition fill(String rule, String out) {
//            return null;
//        }
//
//    }
//
//    private interface FilledTransition extends MorphTransition {
//
//        Word get(String s, int i);
//
//    }
//
//    private class EpsilonTransition implements FilledTransition {
//
//        private MorphState to;
//    }
//
//    private class AffixTransition implements FilledTransition {
//
//        private MorphState to;
//        private String form;
//        private String feature;
//        private boolean prefix;
//
//    }
//
//    private class RootTransition implements FilledTransition {
//
//        private MorphState to;
//        private String root;
//        private String features;
//        private List<String> infixes;
//        private List<Range> infixRanges;
//
//
//        private class Range {
//            private int from;
//            private int to;
//        }
//    }
//
//}
