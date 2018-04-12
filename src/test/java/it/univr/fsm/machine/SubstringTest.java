package it.univr.fsm.machine;

import java.util.HashSet;

import org.junit.Assert;
import org.junit.Test;

public class SubstringTest {


	@Test
	public void substringTest1() { 
		Automaton a = Automaton.makeAutomaton("a");
		
		Automaton result = Automaton.substring(a, 0, 1);
		System.out.println(result);
		Assert.assertTrue(result.equals(Automaton.makeAutomaton("a")));
	}
	
	@Test
	public void substringTest2() { 
		Automaton a = Automaton.makeAutomaton("a");
		
		Automaton result = Automaton.substring(a, 0, 0);

		Assert.assertTrue(result.equals(Automaton.makeEmptyString()));
	}
	
	@Test
	public void substringTest3() { 
		Automaton a = Automaton.makeAutomaton("a");
		Automaton b = Automaton.makeAutomaton("b");
		
		Automaton aorb = Automaton.union(a, b);
		
		Automaton result = Automaton.substring(aorb, 0, 1);
		Assert.assertTrue(result.equals(aorb));
	}
	
	@Test
	public void substringTest4() { 
		Automaton a = Automaton.makeAutomaton("abc");
		Automaton b = Automaton.makeAutomaton("def");
		
		Automaton aorb = Automaton.union(a, b);

		Automaton result = Automaton.substring(aorb, 1, 2);
		Automaton expectedResult = Automaton.union(Automaton.makeAutomaton("b"), Automaton.makeAutomaton("e"));
		
		Assert.assertTrue(result.equals(expectedResult));
	}
	
	
	@Test
	public void substringTest5() {

		HashSet<State> states = new HashSet<State>();
		HashSet<Transition> delta = new HashSet<Transition>();
		
		State q0 = new State("q0", true, true);

		states.add(q0);
		
		delta.add(new Transition(q0, q0, "a"));
		
		// a^*
		Automaton aStar = new Automaton(delta, states);
		
		
		Automaton result = Automaton.substring(aStar, 0, 3);
		
		HashSet<Automaton> automata = new HashSet<>();
		automata.add(Automaton.makeEmptyString());
		automata.add(Automaton.makeAutomaton("a"));
		automata.add(Automaton.makeAutomaton("aa"));
		automata.add(Automaton.makeAutomaton("aaa"));
		
		// "" | a | aa | aaa
		Automaton expectedResult = Automaton.union(automata);
		
		Assert.assertTrue(result.equals(expectedResult));	
	}
	
	
	
	@Test
	public void substringTest6() {

		HashSet<State> states = new HashSet<State>();
		HashSet<Transition> delta = new HashSet<Transition>();
		
		State q0 = new State("q0", true, true);

		states.add(q0);
		
		delta.add(new Transition(q0, q0, "a"));
		
		// a^* u hello
		Automaton automaton = Automaton.union(new Automaton(delta, states), Automaton.makeAutomaton("hello"));
		
		
		Automaton result = Automaton.substring(automaton, 1, 3);
		
				
		HashSet<Automaton> automata = new HashSet<>();
		automata.add(Automaton.makeEmptyString());
		automata.add(Automaton.makeAutomaton("a"));
		automata.add(Automaton.makeAutomaton("aa"));
		automata.add(Automaton.makeAutomaton("el"));
		
		// "" | a | aa  | hel
		Automaton expectedResult = Automaton.union(automata);
		
		Assert.assertTrue(result.equals(expectedResult));	
	}
	
	
	@Test
	public void  substringTest7() { 
		Automaton a = Automaton.makeAutomaton("hello");
		Automaton b = Automaton.makeAutomaton("papers");
		Automaton c = Automaton.makeAutomaton("lang");
		
		Automaton automaton = Automaton.union(Automaton.union(a, b), c);
		
		Automaton result = Automaton.substring(automaton, 1, 3);
		Automaton expectedResult = Automaton.union(Automaton.makeAutomaton("el"), Automaton.union(Automaton.makeAutomaton("ap"), Automaton.makeAutomaton("an")));
		
		Assert.assertTrue(result.equals(expectedResult));
	}
	
	/**
	 * Example used in SAS 2018
	 */
	@Test
	public void  substringTest8() { 
		Automaton a = Automaton.makeAutomaton("hello");
		Automaton b = Automaton.makeAutomaton("abc");
		
		Automaton automaton = Automaton.union(a, b);
		
		Automaton result = Automaton.substring(automaton, 1, 4);
		Automaton expectedResult = Automaton.union(Automaton.makeAutomaton("bc"), Automaton.makeAutomaton("ell"));
		
		Assert.assertTrue(result.equals(expectedResult));
	}
	
	/**
	 * Example used in SAS 2018
	 */
	@Test
	public void  substringTest9() { 
		HashSet<State> states = new HashSet<State>();
		HashSet<Transition> delta = new HashSet<Transition>();
		
		State q0 = new State("q0", true, true);

		states.add(q0);
		
		delta.add(new Transition(q0, q0, "a"));
		
		// a^* u hello
		Automaton automaton = Automaton.union(new Automaton(delta, states), Automaton.makeAutomaton("hello"));
		
		
		Automaton result = Automaton.substring(automaton, 1, 4);
		
				
		HashSet<Automaton> automata = new HashSet<>();
		automata.add(Automaton.makeEmptyString());
		automata.add(Automaton.makeAutomaton("a"));
		automata.add(Automaton.makeAutomaton("aa"));
		automata.add(Automaton.makeAutomaton("aaa"));
		automata.add(Automaton.makeAutomaton("ell"));
		
		Automaton expectedResult = Automaton.union(automata);
		
		Assert.assertTrue(result.equals(expectedResult));	
	}
	
	@Test
	public void  substringTest10() { 
		Automaton a = Automaton.makeAutomaton("hello");
		Automaton b = Automaton.makeAutomaton("papers");
		Automaton c = Automaton.makeAutomaton("lang");
		
		Automaton automaton = Automaton.union(Automaton.union(a, b), c);
		
		Automaton result = Automaton.substring(automaton, 1, 5);
		Automaton expectedResult = Automaton.union(Automaton.makeAutomaton("ello"), Automaton.union(Automaton.makeAutomaton("aper"), Automaton.makeAutomaton("ang")));
		
		Assert.assertTrue(result.equals(expectedResult));
	}
	
	@Test
	public void substringTest11() {

		HashSet<State> states = new HashSet<State>();
		HashSet<Transition> delta = new HashSet<Transition>();
		
		State q0 = new State("q0", true, true);

		states.add(q0);
		
		delta.add(new Transition(q0, q0, "a"));
		
		// a^* u hello u bc
		Automaton automaton = Automaton.union(Automaton.union(new Automaton(delta, states), Automaton.makeAutomaton("hello")), Automaton.makeAutomaton("bc"));
		
		
		Automaton result = Automaton.substring(automaton, 1, 3);
		
				
		HashSet<Automaton> automata = new HashSet<>();
		automata.add(Automaton.makeEmptyString());
		automata.add(Automaton.makeAutomaton("a"));
		automata.add(Automaton.makeAutomaton("aa"));
		automata.add(Automaton.makeAutomaton("el"));
		automata.add(Automaton.makeAutomaton("c"));
		automata.add(Automaton.makeAutomaton(""));
		
		// "" | a | aa  | hel
		Automaton expectedResult = Automaton.union(automata);
		
		Assert.assertTrue(result.equals(expectedResult));	
	}
}
