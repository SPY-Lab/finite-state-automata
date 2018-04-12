package it.univr.fsm.machine;

import java.util.HashSet;

import org.junit.Assert;
import org.junit.Test;


public class CharAtTest {


    @Test
    public void charAtTest1() {

        Automaton a = Automaton.makeAutomaton("abc");
        
        Automaton charAt = Automaton.charAt(a, 1);
        Automaton expectedResult = Automaton.makeAutomaton("b");
   
        Assert.assertTrue(charAt.equals(expectedResult));
    }
    
	@Test
	public void charAtTest2() throws Exception {
		State q = new State("q0", true, true);

		HashSet<State> states = new HashSet<>();
		HashSet<Transition> transition = new HashSet<>();

		states.add(q);
		transition.add(new Transition(q, q, "a"));

		// a^*
		Automaton a = new Automaton(transition, states);
		
		Automaton result = Automaton.charAt(a, 1);

		Automaton expectedResult = Automaton.union(Automaton.makeAutomaton("a"), Automaton.makeEmptyString());

		Assert.assertTrue(result.equals(expectedResult));
	}
    	
	@Test
	public void charAtTest3() throws Exception {
		Automaton a = Automaton.makeAutomaton("a");
		Automaton result = Automaton.charAt(a,0);
		Assert.assertTrue(result.equals(result));
	}
	
	@Test
	public void charAtTest4() throws Exception {
		Automaton a = Automaton.makeAutomaton("a");
		Automaton result = Automaton.charAt(a,5);
		Assert.assertTrue(result.equals(Automaton.makeEmptyString()));
	}
	
	@Test
	public void charAtTest5() throws Exception {
		State q0 = new State("q0", true, false);
		State q1 = new State("q1", false, true);

		HashSet<State> states = new HashSet<>();
		HashSet<Transition> transition = new HashSet<>();

		states.add(q0);
		states.add(q1);
		
		transition.add(new Transition(q0, q1, "a"));
		transition.add(new Transition(q1, q1, "a"));


		Automaton a = new Automaton(transition, states);

		Automaton result = Automaton.charAt(a,0);
		Automaton expectedResult = Automaton.makeAutomaton("a");

		Assert.assertTrue(result.equals(expectedResult));
	}
	
	@Test
	public void charAtTest6() throws Exception {
		Automaton a = Automaton.union(Automaton.makeAutomaton("b"), Automaton.makeAutomaton("c"));
		Automaton result = Automaton.charAt(a,0);
		Assert.assertTrue(result.equals(a));
	}
    
}
