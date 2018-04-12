# Finite state automata library
Implementation of a the finite state automata abstract domain library for abstract interpretation.

## Build an automaton

To build the automaton corresponding to a given string, you can use the static method `makeAutomaton`.
```
Automaton a = Automaton.makeAutomaton("hello");
```

To build the automaton corresponding to the union of two automata, you can use the static method `union`.
```
Automaton u = Automaton.union(Automaton.makeAutomaton("yes"), Automaton.makeAutomaton("no"));
```
Similarly, you can perform intersection, minus, concatenation operations.

## Contributors
- Vincenzo Arceri vincenzo.arceri@univr.it
- Isabella Mastroeni isabella.mastroeni@univr.it
