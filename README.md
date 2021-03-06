# MorphGen

MorphGen is a language-independent morphological generator, i.e. it can generate out all possible fully inflected forms of a lemma. It is based on user-provided rule sets and is thus not specialized on a single language, but can serve as a morphological generator for any language. Though its rules look similar to regular expressions and are interpreted by an automaton-like structure, MorphGen is not a finite state transducer. Its ability to store matches in memory and freely reinsert them later allows it to easily model morphological processes that are inherently difficult to express with a finite state machine, such as reduplication and metathesis.

For a description of the rule format, have a look at section 4.2 of [my term paper](https://github.com/tdaneyko/malayalam-glosser/blob/master/paper/ismla.pdf) on the [Malayalam Glosser](https://github.com/tdaneyko/malayalam-glosser), where MorphGen was first put to use. The files for Malayalam are also included in this repository under _src/main/resources_ as an example.

A .jar library version of MorphGen 1.0 is downloadable [here](https://github.com/tdaneyko/morphgen/releases/download/1.0/morphgen.jar).
