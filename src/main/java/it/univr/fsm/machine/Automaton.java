package it.univr.fsm.machine;

import it.univr.fsm.equations.*;

import it.univr.exception.*;

import it.univr.fsm.equations.Comp;
import it.univr.fsm.equations.Equation;
import it.univr.fsm.equations.GroundCoeff;
import it.univr.fsm.equations.Or;
import it.univr.fsm.equations.RegularExpression;
import it.univr.fsm.equations.Var;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Finite-state automaton class.
 * 
 * @author <a href="mailto:vincenzo.arceri@univr.it">Vincenzo Arceri</a>
 * @version 1.0
 * @since 24-10-2016
 */
public class Automaton {

	/**
	 * Starting symbol to name the states.
	 */
	public static char initChar = 'a';

	/**
	 * Set of transitions between states.
	 */
	private HashSet<Transition> delta;

	/**
	 * Set of states.
	 */
	private HashSet<State> states;

	/**
	 * Adjacency list Outgoing
	 */
	private HashMap<State, HashSet<Transition>> adjacencyListOutgoing;

	/**
	 * Constructs a new automaton.
	 * 
	 * @param initialState the initial state
	 * @param delta the set of transitions
	 * @param states the set of states
	 */

	public Automaton(HashSet<Transition> delta, HashSet<State> states)  {
		this.delta = delta;
		this.states = states;
		this.computeAdjacencyList();
	}

	public Automaton() {}


	private void computeAdjacencyList() {	
		adjacencyListOutgoing = new HashMap<State, HashSet<Transition>>();

		for (Transition t : getDelta()) {
			if (!adjacencyListOutgoing.containsKey(t.getFrom()))
				adjacencyListOutgoing.put(t.getFrom(), new HashSet<Transition>());

			adjacencyListOutgoing.get(t.getFrom()).add(t);	
		}
	}

	/**
	 * Check whether an automaton is deterministic
	 * 
	 * @param a the automaton
	 * @return a boolean
	 */
	public static boolean isDeterministic(Automaton a){
		for(State s: a.states){
			HashSet<Transition> outgoingTranisitions = a.getOutgoingTransitionsFrom(s);
			for(Transition t: outgoingTranisitions){
				if (t.getInput().isEmpty()) return false;

				for(Transition t2: outgoingTranisitions) {
					if (t2.getInput().isEmpty()) return false;
					if(!t.getTo().equals(t2.getTo()) && t.getInput().equals(t2.getInput())) return false; 
				}
			}
		}
		return true;
	}

	/**
	 * Check whether an automaton is contained in another
	 * 
	 * @param first the first automaton
	 * @param second the second automaton
	 * @return a boolean
	 * 
	 */
	public static boolean isContained(Automaton first, Automaton second){
		// first is contained in second if (first intersect !second) accepts empty language
		return Automaton.isEmptyLanguageAccepted(Automaton.intersection(first, Automaton.complement(second)));
	}

	public static Automaton chars(Automaton a) {

		a.minimize();

		HashSet<State> states = new HashSet<State>();
		HashSet<Transition> delta = new HashSet<Transition>();

		State q0 = new State("q0", true, false);
		State qf = new State("qf", false, true);

		states.add(q0);
		states.add(qf);

		for (Transition t : a.getDelta())
			delta.add(new Transition(q0, qf, t.getInput()));

		Automaton aut = new Automaton(delta, states);
		aut.minimize();
		return aut;
	}

	/**
	 * Check whether a state is reachable from initial state
	 * 
	 * @param f the state
	 * @param a the automaton
	 * @return a boolean
	 */
	public static boolean isReachable(State f, Automaton a ){
		HashSet<Transition> transitionSet;

		if (f.isInitialState()) {
			return true;
		} else {
			for(State s: a.states){
				transitionSet = a.getTransitionFrom(s, f);

				if( transitionSet != null)
					for(Transition t: transitionSet)
						if(isReachable(t.getFrom(),a)) return true;
			}

			return false;
		}
	}

	/**
	 * Check whether an automaton accepts the empty language or not
	 * 
	 * @param automaton the automaton
	 * @return a boolean
	 */
	public static boolean isEmptyLanguageAccepted(Automaton automaton) {
		automaton.minimize();
		return automaton.getFinalStates().isEmpty(); //&& !automaton.states.isEmpty();
	}

	/**
	 * Performs an intersection between multiple automatons
	 * 
	 * @param collection a collection of automatons
	 * @return the intersection
	 */
	public static Automaton intersection(Collection<Automaton> collection) {
		Automaton a = null;

		for (Automaton aut: collection)
			a = (a == null) ? aut : Automaton.intersection(a, aut);

		if (a != null)
			a.minimize();
		return a;
	}



	/**
	 * Performs a concatenation between multiple automatons
	 * 
	 * @param collection a collection of automatons
	 * @return the concatenation
	 * 
	 * Warning: the collection should consider the order between the automatons since the concatenation is not commutative
	 * 
	 */
	public static Automaton concat(Collection <Automaton> collection){
		Automaton result = null;

		for (Automaton aut: collection) 
			result = (result == null) ? aut : Automaton.concat(result, aut);

		return result;
	}

	/**
	 * Performs a subtraction between multiple automatons
	 * 
	 * @param collection a collection of automatons
	 * @return the subtraction
	 * 
	 */
	public static Automaton minus(Collection<Automaton> collection){
		Automaton a = null;

		for (Automaton aut: collection)
			a = (a == null) ? aut : Automaton.minus(a, aut);

		if (a != null)
			a.minimize();
		return a;
	}

	/**
	 * Performs the difference between two automatons
	 * 
	 * @param first the first automaton
	 * @param second the second automaton
	 * @return the difference
	 */

	public static Automaton minus(Automaton first, Automaton second){
		// first \ second = first intersect !second

		Automaton a = Automaton.intersection(first, Automaton.complement(second));
		a.minimize();

		return a;
	}

	/**
	 * Concats two automata
	 * 
	 * @param first the first automaton to merge
	 * @param second the second automaton to merge
	 * @return a new automaton, in which first is chained to second
	 * 
	 */
	public static Automaton concat(Automaton first, Automaton second){

		HashMap<State, State> mappingFirst = new HashMap<State,State>();
		HashMap<State, State> mappingSecond = new HashMap<State, State>();
		HashSet<Transition> newDelta = new HashSet<Transition>();
		HashSet<State> newStates = new HashSet<State>();
		HashSet<State> firstFinalStates = new HashSet<>();
		HashSet<State> secondInitialStates = new HashSet<>();

		int c = 0;

		// Add all the first automaton states
		for (State s: first.states) {

			// The first automaton states are not final, can be initial states
			mappingFirst.put(s, new State("q" + c++, s.isInitialState(), false));
			newStates.add(mappingFirst.get(s));

			if (s.isFinalState())
				firstFinalStates.add(s);
		}

		// Add all the second automaton states
		for (State s: second.states) {

			// the second automaton states are final, can't be initial states
			mappingSecond.put(s, new State("q" + c++, false, s.isFinalState()));
			newStates.add(mappingSecond.get(s));

			if(s.isInitialState())
				secondInitialStates.add(s);
		}

		// Add all the first automaton transitions
		for (Transition t: first.delta)
			newDelta.add(new Transition(mappingFirst.get(t.getFrom()), mappingFirst.get(t.getTo()), t.getInput()));

		// Add all the second automaton transitions
		for (Transition t: second.delta)
			newDelta.add(new Transition(mappingSecond.get(t.getFrom()), mappingSecond.get(t.getTo()), t.getInput()));

		// Add the links between the first automaton final states and the second automaton initial state
		for (State f: firstFinalStates)
			for(State s : secondInitialStates)
				newDelta.add(new Transition(mappingFirst.get(f), mappingSecond.get(s), ""));

		Automaton a = new Automaton(newDelta, newStates);
		a.minimize();

		return a;

	}

	/**
	 * Performs the automata complement operation
	 * 
	 * @param  automaton the automata input
	 * @return the complement of the automata
	 */
	public static Automaton complement(Automaton automaton) {
		HashMap<State, State> mapping = new HashMap<State,State>();
		HashSet<Transition> newDelta = new HashSet<Transition>();
		HashSet<State> newStates = new HashSet<State>();


		automaton = Automaton.totalize(automaton.clone());

		// Add states to the mapping, replacing accept states to reject
		for(State s: automaton.states) {
			mapping.put(s, new State(s.getState(), s.isInitialState(), !s.isFinalState()));
			newStates.add(mapping.get(s));
		}

		// Copying delta set
		for (Transition t:  automaton.delta)
			newDelta.add(new Transition(mapping.get(t.getFrom()), mapping.get(t.getTo()), t.getInput()));

		Automaton a = new Automaton(newDelta,newStates);
		a.minimize();
		return a;
	}

	public static Automaton totalize(Automaton automaton) {		
		HashSet<State> newState = new HashSet<>();
		HashSet<Transition> newDelta = new HashSet<>();

		for (State s : automaton.getStates())
			newState.add(s);

		State qbottom = new State("qbottom", false, false);

		newState.add(qbottom);

		for (Transition t : automaton.getDelta())
			newDelta.add(t);

		for (char alphabet = '!'; alphabet <= '~'; ++alphabet) 
			newDelta.add(new Transition(qbottom, qbottom, String.valueOf(alphabet)));


		Automaton result = new Automaton(newDelta, newState);

		for (State s : newState)
			for (char alphabet = '!'; alphabet <= '~'; ++alphabet) {
				HashSet<State> states = new HashSet<>();
				states.add(s);

				if (!result.readableCharFromState(states).contains(String.valueOf(alphabet)))
					newDelta.add(new Transition(s, qbottom, String.valueOf(alphabet)));

			}

		return new Automaton(newDelta, newState);
	}

	/**
	 * Does the automata intersection
	 * 
	 * @param first the first automata
	 * @param second the first automata
	 * @return a new automata, the intersection of the first and the second
	 */

	public static Automaton intersection(Automaton first, Automaton second) {

		// !(!(first) u !(second))
		Automaton notFirst = Automaton.complement(first);
		Automaton notSecond = Automaton.complement(second);

		Automaton union = Automaton.union(notFirst, notSecond);
		//		union.minimize();

		Automaton result = Automaton.complement(union);
		//		result.minimize();
		return result;
	}

	/**
	 *
	 * @param path the path containing the automaton
	 * @return the automaton descripted in file
	 * @throws MalformedInputException whenever the file doesn't complain with the default pattern
	 *
	 * Read and returns an Automaton from file. It must be formatted in the following way:
	 *[state_name][reject] or [initial]
	 * <tab>[state_from] Sym -> [state_to]</tab>
	 */
	public static Automaton loadAutomataWithAlternatePattern(String path) {
		BufferedReader br = null;

		HashMap<String, State> mapStates = new HashMap<>();
		HashSet<Transition> delta = new HashSet<Transition>();
		HashSet<State> states = new HashSet<State>();

		try{
			String currentLine;
			br = new BufferedReader(new FileReader(path) );

			while((currentLine = br.readLine()) != null ){
				State current = null;
				State next = null;
				String sym = "";
				String stateName = "";

				// state
				if(currentLine.charAt(0) != '\t'){
					String[] pieces = currentLine.trim().split(" ");

					// sanity check
					if(pieces.length < 2) throw new MalformedInputException();

					for(int i = 0; i < pieces.length; i++){

						if(pieces[i].startsWith("[") && pieces[i].endsWith("]") && i == 0){
							stateName = pieces[i].substring(1, pieces[i].length()-1);
							// found state
							if(mapStates.containsKey(stateName)){
								current = mapStates.get(stateName);
							}else{
								mapStates.put(stateName, (current=new State(stateName,false,false)));
							}

						}else if(pieces[i].startsWith("[") && pieces[i].endsWith("]")
								&& pieces[i].contains("accept")){
							if(mapStates.containsKey(stateName)){
								current = mapStates.get(stateName);
								current.setFinalState(true);
							}else{
								throw new MalformedInputException();
							}

						}else if(pieces[i].startsWith("[") && pieces[i].endsWith("]")
								&& pieces[i].contains("initial")){
							if(mapStates.containsKey(stateName)){
								current = mapStates.get(stateName);
								current.setInitialState(true);
							}else{
								throw new MalformedInputException();
							}
						}
					}

					if(current == null) throw new MalformedInputException();

					if(!states.contains(current))
						states.add(current);
					else {
						states.remove(current);
						states.add(current);
					}


				}
				// transition
				else{
					String line = currentLine.substring(1, currentLine.length()).trim();
					String[] pieces = line.split(" ");

					// sanity check
					if (pieces.length > 4 || pieces.length < 3) throw new MalformedInputException();


					for(int i = 0; i < pieces.length; i++){
						if(pieces[i].startsWith("[") && pieces[i].endsWith("]") && i == 0){
							// state from
							stateName = pieces[i].substring(1, pieces[i].length()-1);

							if(mapStates.containsKey(stateName)){
								current = mapStates.get(stateName);
							}else{
								throw new MalformedInputException();
							}

						}else if(pieces[i].startsWith("[") && pieces[i].endsWith("]")){
							// next state
							stateName = pieces[i].substring(1, pieces[i].length()-1);
							if(mapStates.containsKey(stateName)){
								next = mapStates.get(stateName);
								states.add(next);
							}else{
								mapStates.put(stateName, (next = new State(stateName,false,false)));
							}

						}else if(!pieces[i].equals("->")){
							// transition symbol
							sym = pieces[i];

						}
					}

					delta.add(new Transition(current, next, sym));
				}
			}


		}catch(IOException | MalformedInputException e) {
			e.printStackTrace();
			return null;
		}finally{
			try{
				br.close();
			}catch(Exception c){
				System.err.println("Failed to close BufferedReader stream in loadAutomataWithAlternatePattern: " + c.getMessage() );
			}
		}

		Automaton a = new Automaton(delta, states);

		return a;
	}

	public static Automaton loadAutomataWithFSM2RegexPattern(String path){
		BufferedReader br = null;


		HashMap<String, State> mapStates = new HashMap<>();
		HashSet<Transition> delta = new HashSet<Transition>();
		HashSet<State> states = new HashSet<State>();

		try{
			String currentLine;
			br = new BufferedReader(new FileReader(path) );

			/**
			 * 0 : #states
			 * 1 : #initial
			 * 2 : #accepting
			 * 3 : #alphabet
			 * 4 : #transition
			 *
			 */
			int currentMode = -1;


			while((currentLine = br.readLine()) != null ){
				State current = null;
				String sym = "";

				switch(currentLine){
				case "#states":
					currentMode = 0;
					continue;
				case "#initial":
					currentMode = 1;
					continue;
				case "#accepting":
					currentMode = 2;
					continue;
				case "#alphabet":
					currentMode = 3;
					continue;
				case "#transitions":
					currentMode = 4;
					continue;
				}

				switch (currentMode){
				case 0:
					states.add((current = new State(currentLine,false,false)));
					mapStates.put(currentLine,current);
					break;
				case 1:
					if(mapStates.containsKey(currentLine)) {
						current = mapStates.get(currentLine);
						current.setInitialState(true);
					} else
						throw new MalformedInputException();
					break;
				case 2:
					if(mapStates.containsKey(currentLine)) {
						current = mapStates.get(currentLine);
						current.setFinalState(true);
					}else
						throw new MalformedInputException();
					break;
				case 3:
					break;
				case 4:
					current = mapStates.get(currentLine.split(">")[0].split(":")[0]);
					sym = (currentLine.split(">")[0]).split(":")[1];
					if(sym.equals("$")) sym = "";
					String[] to = currentLine.split(">")[1].split(",");

					if(current == null || to.length == 0) throw new MalformedInputException();

					for(String toS : to){
						if(mapStates.containsKey(toS)){
							delta.add(new Transition(current,mapStates.get(toS), sym));
						}else
							throw new MalformedInputException();
					}



					break;
				default:
					throw new MalformedInputException();
				}



			}


		}catch(IOException | MalformedInputException e) {
			e.printStackTrace();
			return null;
		}finally{
			try{
				br.close();
			}catch(Exception c){
				System.err.println("Failed to close BufferedReader stream in loadAutomataWithAlternatePattern: " + c.getMessage() );
			}
		}

		Automaton a = new Automaton(delta, states);

		return a;
	}

	public static Automaton loadAutomataWithJFLAPPattern(String path){
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		HashSet<State> newStates = new HashSet<>();
		HashSet<Transition> newDelta = new HashSet<>();

		HashMap<String, State> statesMap = new HashMap<>();

		try {
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document document = builder.parse(new File(path));
			document.getDocumentElement().normalize();

			NodeList nListStates = document.getElementsByTagName("state");
			NodeList nListTransitions = document.getElementsByTagName("transition");

			// analyze all the states
			for(int i = 0 ; i< nListStates.getLength(); i++){
				Node actual = nListStates.item(i);

				String stateName = actual.getAttributes().getNamedItem("name").getNodeValue();
				boolean isInitialState = false;
				boolean isFinalState = false;

				Node property = actual.getFirstChild();

				// analyze state properties
				while (property != null){
					switch (property.getNodeName()){
					case "initial":
						isInitialState = true;
						break;
					case "final":
						isFinalState = true;
						break;
					}

					property = property.getNextSibling();
				}

				State s = new State(stateName,isInitialState,isFinalState);
				statesMap.put(actual.getAttributes().getNamedItem("id").getNodeValue(),s);
				newStates.add(statesMap.get(actual.getAttributes().getNamedItem("id").getNodeValue()));
			}

			// analyze all the transitions
			for(int i = 0 ; i< nListTransitions.getLength(); i++){
				Node actual = nListTransitions.item(i);
				State from = null;
				State to = null;
				String sym = "";

				Node property = actual.getFirstChild();

				// analyze transitions properties
				while (property != null){
					switch (property.getNodeName()){
					case "from":
						if(!statesMap.containsKey(property.getFirstChild().getNodeValue())) throw new MalformedInputException();
						from = statesMap.get(property.getFirstChild().getNodeValue());
						break;
					case "to":
						if(!statesMap.containsKey(property.getFirstChild().getNodeValue())) throw new MalformedInputException();
						to = statesMap.get(property.getFirstChild().getNodeValue());
						break;
					case "read":
						if(property.getFirstChild() != null)
							sym = property.getFirstChild().getNodeValue();
						break;
					}

					property = property.getNextSibling();
				}

				newDelta.add(new Transition(from, to, sym));


			}

			return new Automaton(newDelta, newStates);

		} catch (ParserConfigurationException e) {
			e.printStackTrace();
			return null;
		} catch (SAXException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}


	/**
	 * 
	 * @param path the path containing the automaton
	 * @return the automaton descripted in file
	 * @throws MalformedInputException whenever the file doesn't complain with the default pattern
	 * 
	 * Read and returns an Automaton from file. It must be formatted in the following way:
	 * 1st line: all the states
	 * 2nd line: initial states
	 * 3rd line: final states
	 * Transitions: From To Sym
	 */

	public static Automaton loadAutomata(String path){
		/*	
		 * This method follows this pattern in the file
		 * 	q0 q1 a
		 * 	q1 q2 b
		 * 	q2 q3 c 
		 */

		BufferedReader br = null;

		HashMap<String, State> mapStates = new HashMap<String, State>();
		HashSet<Transition> delta = new HashSet<Transition>();
		HashSet<State> states = new HashSet<State>();
		HashSet<State> initialStates = new HashSet<State>();
		State currentState;
		int lineNum;


		try{
			String currentLine;			
			br = new BufferedReader(new FileReader(path) );


			for(lineNum = 0; (currentLine = br.readLine()) != null ; lineNum++){
				String[] pieces;

				pieces=currentLine.split(" ");

				switch(lineNum){
				// here i will find all the states
				case 0:
					for(String s: pieces){
						mapStates.put(s, currentState = new State(s,false,false));
						states.add(mapStates.get(s));
					}
					break;

					// initial states
				case 1:
					for(String s: pieces){
						currentState = mapStates.get(s);

						if(currentState==null) throw new MalformedInputException();

						currentState.setInitialState(true);
						initialStates.add(currentState);
					}

					break;

					// final states
				case 2:
					for(String s: pieces){
						currentState=mapStates.get(s);

						if(currentState==null) throw new MalformedInputException();

						currentState.setFinalState(true);
					}
					break;

					// transitions
				default:
					if(pieces.length!=3) throw new MalformedInputException();

					if(mapStates.get(pieces[0])==null || mapStates.get(pieces[1])==null) throw new MalformedInputException();

					delta.add(new Transition(mapStates.get(pieces[0]),mapStates.get(pieces[1]),pieces[2]));

					break;

				}

			}


		}catch(IOException | MalformedInputException e) {
			e.printStackTrace();
			return null;
		}finally{
			try{
				br.close();
			}catch(Exception c){
				System.err.println("Failed to close BufferedReader stream in loadAutomata: " + c.getMessage() );
			}
		}

		Automaton a = new Automaton(delta, states);

		return a;
	}


	/**
	 * Runs a string on the automaton.
	 * 
	 * @param s the string
	 * @return true if the string is accepted by the automaton, false otherwise
	 */
	public boolean run(String s) {
		return run(s, getInitialState());
	}

	public boolean run(String s, State state){
		ArrayList<String> input = (ArrayList<String>) toList(s);

		return _run(input, state);

	}

	private boolean _run(ArrayList<String> s, State state){
		boolean found = false;

		if(state.isFinalState() && s.isEmpty())
			return true;
		else if(!state.isFinalState() && s.isEmpty()) //string is empty and I'm not in a final state
			return false;
		else{
			ArrayList<String> scopy = new ArrayList<>(s);
			String ch = scopy.get(0);

			for(Transition t : getOutgoingTransitionsFrom(state)){
				if(t.isFirable(state, ch) && !t.getInput().equals("")) {
					scopy.remove(0);
					found = found || _run(scopy, t.fire(ch));
				}else if(t.getInput().equals("")){
					found = found || _run(scopy, t.fire(ch));
				}
			}
		}

		return found;
	}

	/**
	 * Runs a string on the automaton starting from a given state.
	 * 
	 * @param s the string
	 * @param state the starting state
	 * @return true if the string is accepted by the automaton, false otherwise
	 */
	/*
	public boolean run(String s, State state) {
		ArrayList<String> input = (ArrayList<String>) toList(s);
		State currentState = state;
		boolean found = false;
		while (!input.isEmpty() || found) {
			found = false;
			for (Transition t: delta) {
				if (!input.isEmpty() && t.isFirable(currentState, input.get(0))) {
					currentState = t.fire(input.get(0));
					input.remove(0);
					found = true;
				} else if (t.getInput().equals("") && t.getFrom().equals(currentState)) { // Empty transition
					currentState = t.fire("");
					found = true;
					break;
				}
			}
			if (found)
				continue;
		}
		if (currentState.isFinalState())
			return true;
		return false;
	} */

	/**
	 * Returns the set of transitions.
	 */
	public HashSet<Transition> getDelta() {
		return delta;
	}

	/**
	 * Sets the set of transition
	 */
	public void setDelta(HashSet<Transition> delta) {
		this.delta = delta;
	}

	/**
	 * Returns the string as an array of chars.
	 */
	public static List<String> toList(String s) {
		ArrayList<String> result = new ArrayList<String>();

		for (int i = 0; i < s.length(); ++i)
			result.add(s.substring(i, i+1));

		return result;
	}

	public void removeTransition(Transition t) {
		this.delta.remove(t);
	}


	/**
	 * Returns the state with the given name.
	 * 
	 * @param name the name of the state.
	 * @return the state with the given name.
	 */
	public State getState(String name) {

		for (State s : this.states) 
			if (s.getState().equals(name))
				return s;
		return null;
	}

	/**
	 * Builds an automaton from a given string.
	 * 
	 * @param s the string.
	 * @return a new automaton recognize the given string.
	 */
	public static Automaton makeAutomaton(String s) {


		if (s.equals(""))
			return Automaton.makeEmptyString();

		HashSet<State> states = new HashSet<State>();
		HashSet<Transition> delta = new HashSet<Transition>();

		State initialState = new State("q0", true, false);
		states.add(initialState);



		State state = initialState;


		ArrayList<String> input = (ArrayList<String>) toList(s);

		for (int i = 0; i < input.size(); ++i) {
			State next = new State("q" + (i+1), false, i == input.size() -1 ? true : false );
			states.add(next);		

			/*if (input.get(i).equals(" "))
				gamma.add(new Transition(state, next, "", ""));
			else	*/
			delta.add(new Transition(state, next, input.get(i)));

			state = next;
		}

		return new Automaton(delta, states);
	}

	public HashSet<Transition> getOutgoingTransitionsFrom(State s) {
		HashSet<Transition> result = adjacencyListOutgoing.get(s);

		if (result == null) {
			adjacencyListOutgoing.put(s,  new HashSet<Transition>());
			return new HashSet<Transition>();
		}

		return result;

	}

	public void setAdjacencyListOutgoing(HashMap<State, HashSet<Transition>> adjacencyListOutgoing) {
		this.adjacencyListOutgoing = adjacencyListOutgoing;
	}


	public static Automaton union(HashSet<Automaton> automata) {
		Automaton result = Automaton.makeEmptyLanguage();


		for (Automaton a : automata)
			result = Automaton.union(result, a);

		return result;
	}

	/**
	 * Union operation between two automata.
	 * 
	 * @param a1 first automaton.
	 * @param a2 second automaton.
	 * @return the union of the two automata.
	 */
	public static Automaton union(Automaton a1, Automaton a2) {
		State newInitialState = new State("initialState", true, false);
		HashSet<Transition> newGamma = new HashSet<Transition>();
		HashSet<State> newStates = new HashSet<State>();

		int c = 1;
		HashMap<State, State> mappingA1 = new HashMap<State, State>(); 
		HashMap<State, State> mappingA2 = new HashMap<State, State>(); 

		newStates.add(newInitialState);

		State initialA1 = null; 
		State initialA2 = null;

		for (State s : a1.states) {

			mappingA1.put(s, new State("q" + c++, false, s.isFinalState()));

			newStates.add(mappingA1.get(s));



			if (s.isInitialState())
				initialA1 = mappingA1.get(s);
		}

		for (State s : a2.states) {
			mappingA2.put(s, new State("q" + c++, false, s.isFinalState()));
			newStates.add(mappingA2.get(s));



			if (s.isInitialState())
				initialA2 = mappingA2.get(s);
		}

		for (Transition t : a1.delta)
			newGamma.add(new Transition(mappingA1.get(t.getFrom()), mappingA1.get(t.getTo()), t.getInput()));

		for (Transition t : a2.delta)
			newGamma.add(new Transition(mappingA2.get(t.getFrom()), mappingA2.get(t.getTo()), t.getInput()));

		newGamma.add(new Transition(newInitialState, initialA1, ""));
		newGamma.add(new Transition(newInitialState, initialA2, ""));

		Automaton a =  new Automaton(newGamma, newStates);
		a.minimize();
		return a;
	}

	/**
	 * Returns an automaton recognize any string.
	 */
	public static Automaton makeTopLanguage() {
		HashSet<State> newStates = new HashSet<State>();
		HashSet<Transition> newGamma = new HashSet<Transition>();
		State initialState = new State("q0", true, true);

		newStates.add(initialState);

		for (char alphabet = '!'; alphabet <= '~'; ++alphabet) 
			newGamma.add(new Transition(initialState, initialState, String.valueOf(alphabet)));

		return new Automaton(newGamma, newStates);
	}

	/**
	 * Returns an automaton recognize the empty language.
	 */
	public static Automaton makeEmptyLanguage() {

		HashSet<State> newStates = new HashSet<State>();
		HashSet<Transition> newGamma = new HashSet<Transition>();
		State initialState = new State("q0", true, false);

		newStates.add(initialState);

		for (char alphabet = '!'; alphabet <= '~'; ++alphabet) 
			newGamma.add(new Transition(initialState, initialState, String.valueOf(alphabet)));

		return new Automaton(newGamma, newStates);
	}

	/**
	 * Returns an automaton recognize the empty string.
	 */
	public static Automaton makeEmptyString() {

		HashSet<State> newStates = new HashSet<State>();
		HashSet<Transition> newDelta = new HashSet<Transition>();

		State q0 = new State("q0", true, true);

		newStates.add(q0);

		return new Automaton(newDelta, newStates);
	}

	/**
	 * Epsilon closure operation of a state.
	 * 
	 * @param s the state
	 * @return an HashSet of states reachable from the states by using only epsilon transition.
	 */
	public HashSet<State> epsilonClosure(State s) {
		HashSet<State> paths = new HashSet<State>();
		HashSet<State> previous = new HashSet<State>();
		HashSet<State> partial;
		paths.add(s);

		while (!paths.equals(previous)) {
			previous = (HashSet<State>) paths.clone();
			partial = new HashSet<State>();
			// partial.add(s);

			for (State reached : paths) 
				for (Transition t : this.getOutgoingTransitionsFrom(reached)) 
					if (t.isEpsilonTransition())
						partial.add(t.getTo());

			paths.addAll(partial);
		}

		return paths;
	}

	/**
	 * Epsilon closure of a Set of states
	 * @param set the set
	 * @return an HashSet of states reachable from the states by using only epsilon transition.
	 *
	 */
	public HashSet<State> epsilonClosure(HashSet<State> set){
		HashSet<State> solution = new HashSet<>();

		for(State s : set)
			solution.addAll(epsilonClosure(s));

		return solution;
	}

	private HashSet<State> moveNFA(HashSet<State> set, String sym){
		HashSet<State> solution = new HashSet<>();

		for(State s : set) {
			HashSet<Transition> outgoing = getOutgoingTransitionsFrom(s);
			for(Transition t : outgoing) {
				if(t.getInput().equals(sym)) {
					solution.add(t.getTo());
				}
			}

		}

		return solution;
	}

	/**
	 * Determinization automata operation.
	 *  
	 * @return a new determinized automaton. 
	 */

	//	public Automaton determinize() {
	//
	//		HashMap<HashSet<State>, Boolean> dStates = new HashMap<HashSet<State>, Boolean>();
	//		HashSet<Transition> dGamma = new HashSet<Transition>();
	//		HashSet<State> newStates = new HashSet<State>();
	//
	//		dStates.put(epsilonClosure(this.getInitialState()), false);
	//		HashSet<State> T;
	//
	//		State newInitialState = new State(createName(epsilonClosure(this.getInitialState())), true, isPartitionFinalState(epsilonClosure(this.getInitialState())));
	//
	//
	//
	//		newStates.add(newInitialState);
	//
	//		while ((T = notMarked(dStates)) != null) {
	//			dStates.put(T, true);
	//
	//
	//			for (String alphabet: readableCharFromState(T)) {
	//
	//				HashSet<State> newStateWithEpsilonTransition = new HashSet<State>();
	//
	//				// reachable states after epsilon transition
	//				for (State s : T)
	//					for (Transition t : this.getOutgoingTransitionsFrom(s))
	//						if (t.getInput().equals(String.valueOf(alphabet)))
	//							newStateWithEpsilonTransition.add(t.getTo());
	//
	//				HashSet<HashSet<State>> newStateWithNoEpsilon = new HashSet<HashSet<State>>();
	//
	//				for (State s : newStateWithEpsilonTransition)
	//					newStateWithNoEpsilon.add(this.epsilonClosure(s));
	//
	//
	//				HashSet<State> U = new HashSet<State>();
	//
	//				for (HashSet<State> ps : newStateWithNoEpsilon) // Flatting hashsets
	//					for (State s : ps)
	//						U.add(s);
	//
	//				// TODO:I think that lacks a control whether a set is contained in another one
	//	/*			HashMap<HashSet<State>, Boolean> tempDStates = new HashMap<HashSet<State>, Boolean>();
	//
	//				for (HashSet<State> tst : dStates.keySet()) {
	//					HashSet<State> newTst = (HashSet<State>) tst.clone();
	//					newTst.remove(new State("init", true, false));
	//					tempDStates.put(newTst, dStates.get(tst));
	//				}
	//	*/
	//				if (!dStates.containsKey(U) )
	//					dStates.put(U, false);
	//				else {
	//					for (HashSet<State> s : dStates.keySet())
	//						if (s.equals(U))
	//							U = s;
	//				}
	//
	//
	//				State from = new State(createName(T), false, isPartitionFinalState(T));
	//				State to = new State(createName(U), false, isPartitionFinalState(U));
	//
	//				newStates.add(from);
	//				newStates.add(to);
	//
	//				dGamma.add(new Transition(from, to, String.valueOf(alphabet), ""));
	//
	//			}
	//
	//		}
	//
	//		Automaton a = (new Automaton(newInitialState, dGamma, newStates)).deMerge(++initChar);
	//		return a;
	//	}

	public Automaton determinize() {
		HashSet<State> newStates = new HashSet<>();
		HashSet<Transition> newDelta = new HashSet<>();

		HashMap<HashSet<State>, Boolean> statesMarked = new HashMap<>();
		HashMap<HashSet<State>,State> statesName = new HashMap<>();
		int num = 0;
		LinkedList<HashSet<State>> unMarkedStates = new LinkedList<>();

		HashSet<State> temp;

		temp = epsilonClosure(this.getInitialState());
		statesName.put(temp,new State("q" + String.valueOf(num++) , true, isPartitionFinalState(temp)));

		newStates.add(statesName.get(temp));
		statesMarked.put(temp, false);
		unMarkedStates.add(temp);

		while(!unMarkedStates.isEmpty()){
			HashSet<State> T = unMarkedStates.getFirst();
			newStates.add(statesName.get(T));

			// mark T
			unMarkedStates.removeFirst();
			statesMarked.put(T, true);

			for (String alphabet: readableCharFromState(T)) {
				temp = epsilonClosure(moveNFA(T, alphabet));

				if(!statesName.containsKey(temp))
					statesName.put(temp,new State("q" + String.valueOf(num++) ,false,isPartitionFinalState(temp)));

				newStates.add(statesName.get(temp));

				if (!statesMarked.containsKey(temp)) {
					statesMarked.put(temp, false);
					unMarkedStates.addLast(temp);
				}

				newDelta.add(new Transition(statesName.get(T), statesName.get(temp), alphabet));
			}
		}

		Automaton a = new Automaton(newDelta, newStates);
		return a;

	}


	/**
	 * Returns true if at least one state of the partition states is a final state, false otherwise.
	 * 
	 * @param states state partition.
	 */
	private boolean isPartitionFinalState(HashSet<State> states) {

		for (State s : states)
			if (s.isFinalState())
				return true;
		return false;

	}

	/**
	 * Returns true if at least one state of the partition states is an initial state, false otherwise.
	 * 
	 * @param states state partition.
	 */
	private boolean isPartitionInitialState(HashSet<State> states) {

		for (State s : states)
			if (s.isInitialState())
				return true;
		return false;

	}

	/**
	 * RegEx printing.
	 */
	public String prettyPrint() {
		return this.toRegex().toString();
	}

	//	private String createName(HashSet<State> states) {
	//		String result = "";
	//
	//		if (!states.isEmpty()) {
	//
	//			for (State s : states)
	//				result += s.getState() + "x";
	//
	//			result = result.substring(0, result.length() -1);
	//		}
	//		return result;
	//	}

	/**
	 * Returns the set of strings readable from the states state partition.
	 * 
	 * @param states state partition.
	 */
	private HashSet<String> readableCharFromState(HashSet<State> states) {

		HashSet<String> result = new HashSet<String>();

		for (State s : states)
			for (Transition t : this.getOutgoingTransitionsFrom(s)) 
				if (!t.getInput().equals(""))
					result.add(t.getInput());
		return result;

	}

	//	/**
	//	 * Returns the set of strings readable from the state s.
	//	 * 
	//	 * @param s state of this automaton.
	//	 */
	//	private HashSet<String> readableCharFromState(State s) {
	//
	//		HashSet<String> result = new HashSet<String>();
	//
	//		for (Transition t : this.getOutgoingTransitionsFrom(s)) {
	//			if (!t.getInput().equals(""))
	//				result.add(t.getInput());
	//		}
	//
	//		return result;
	//
	//	}

	/**
	 * Removes the unreachable states of an automaton.
	 */
	public void removeUnreachableStates() {
		HashSet<State> reachableStates = new HashSet<State>();
		reachableStates.add(this.getInitialState());

		HashSet<State> newStates = new HashSet<State>();
		newStates.add(this.getInitialState());		

		do {
			HashSet<State> temp = new HashSet<State>();
			for (State s : newStates) {
				//				for (String alphabet : this.readableCharFromState(s))
				for (Transition t : this.getOutgoingTransitionsFrom(s))
					//						if (t.getFrom().equals(s)/* && t.getInput().equals(alphabet)*/)
					temp.add(t.getTo());
			}

			temp.removeAll(reachableStates);
			newStates = temp;

			reachableStates.addAll(newStates);

		} while (!newStates.isEmpty());


		//		int oldSize;
		//		do {
		//			oldSize = newStates.size();
		//
		//			for (State s : newStates) 
		//				for (Transition t : this.getOutgoingTransitionsFrom(s))
		//					newStates.add(t.getTo());
		//
		//		} while (newStates.size() != oldSize);

		states.removeIf(s -> !reachableStates.contains(s));
		delta.removeIf(t -> !reachableStates.contains(t.getFrom()));
	}

	/**
	 * Brzozowski's minimization algorithm.
	 */
	public void minimize() {

		//		if (!isDeterministic(this)) {
		//			Automaton a = this.determinize();
		//			this.delta = a.delta;
		//			this.states = a.states;
		//			this.adjacencyListOutgoing = a.getAdjacencyListOutgoing();
		//		}

		this.reverse();
		Automaton a = this.determinize();
		a.reverse();
		a = a.determinize();

		//		this.initialState = a.initialState;
		this.delta = a.delta;
		this.states = a.states;
		this.adjacencyListOutgoing = a.getAdjacencyListOutgoing();


		//				this.minimizeHopcroft();
		//		
		//				Automaton a = this.deMerge(++initChar); 
		//				this.initialState = a.initialState;
		//				this.states = a.states;
		//				this.delta = a.delta;
		//				this.adjacencyListOutgoing = a.getAdjacencyListOutgoing();


	}

	//	public void minimizeBrowozozwi() {
	//
	//		if (!isDeterministic(this)) {
	//			Automaton a = this.determinize();
	//			this.initialState = a.initialState;
	//			this.delta = a.delta;
	//			this.states = a.states;
	//			this.adjacencyListOutgoing = a.getAdjacencyListOutgoing();
	//			this.adjacencyListIncoming = a.getAdjacencyListIncoming();
	//		}
	//
	//
	//		this.reverse();
	//		Automaton a = this.determinize();
	//		a.reverse();
	//		a = a.determinize();
	//
	//		this.initialState = a.initialState;
	//		this.delta = a.delta;
	//		this.states = a.states;
	//		this.adjacencyListIncoming = a.getAdjacencyListIncoming();
	//		this.adjacencyListOutgoing = a.getAdjacencyListOutgoing();
	//	}

	public static HashSet<String> getAlphabet(Automaton a){
		HashSet<String> alphabet = new HashSet<String>();

		for (Transition t : a.delta)
			//			if (!alphabet.contains(t.getInput()))
			alphabet.add(t.getInput());

		return alphabet;
	}

	//	private State getOutgoingStatefromTransitionSymbol(State s, String symbol){
	//		for(Transition t : delta){
	//			if(t.getInput().equals(symbol) && t.getFrom().equals(s)){
	//				return t.getTo();
	//			}
	//		}
	//		return null;
	//	}

	/*public void hopcroftremoveUnreachableStates(){
		HashSet<State> unreachableStates = new HashSet<>();
			HashSet<State> reachableStates = (HashSet<State>) this.getInitialStates().clone();
		HashSet<State> newStates = (HashSet<State>) this.getInitialStates().clone();
		HashSet<State> reachableStates = this.getInitialStates();
		HashSet<State> newStates = this.getInitialStates();
		HashSet<Transition> transitionstoRemove = new HashSet<>(); 
		HashSet<State> temp;
		do{
			temp = new HashSet<>(Collections.<State>emptySet());
			for(State s : newStates){
				for(String a : getAlphabet(this)){
					State to = getOutgoingStatefromTransitionSymbol(s, a);
					if(to != null) temp.add(to);
				}
			}
			newStates = new HashSet<>();
			newStates.addAll(temp);
			newStates.removeAll(reachableStates);
			reachableStates.addAll(newStates);
		}while(!newStates.equals(Collections.<State>emptySet()));
		// Opt
		unreachableStates.addAll(states);
		unreachableStates.removeAll(reachableStates);
		states.removeAll(unreachableStates);
		for(Transition t: delta)
			if(!states.contains(t.getFrom()))
				transitionstoRemove.add(t);
		delta.removeAll(transitionstoRemove);
		// Opt
		//this.adjacencyList = this.computeAdjacencyList();
	}*/

	private HashSet<Transition> getIncomingTransitionsTo(State s) {
		HashSet<Transition> result = new HashSet<Transition>();

		for (Transition t : this.delta)
			if (t.getTo().equals(s))
				result.add(t);

		return result;
	}	

	private HashSet<State> getXSet(HashSet<State> A, String c){
		HashSet<State> xSet = new HashSet<State>();

		for(State s : A){
			HashSet<Transition> transitionsIncoming = getIncomingTransitionsTo(s);

			for(Transition t : transitionsIncoming){
				if(t.getInput().equals(c))
					xSet.add(t.getFrom());
			}

		}

		return xSet;
	}

	private LinkedList<HashSet<State>> getYList(HashSet<HashSet<State>> P, HashSet<State> X){
		LinkedList<HashSet<State>> Ys = new LinkedList<>();
		HashSet<State> Ytemp ;

		for(HashSet<State> s : P){

			//try to select a set, see if condition is respected
			Ytemp = s;

			if(!setIntersection(X,Ytemp).isEmpty() && !setSubtraction(Ytemp,X).isEmpty())
				Ys.add(Ytemp);


		}

		return Ys;
	}

	private HashSet<State> setIntersection(HashSet<State> first, HashSet<State> second){
		HashSet<State> intersection = (HashSet<State>) first.clone();
		intersection.retainAll(second);
		return intersection;
	}

	private HashSet<State> setSubtraction(HashSet<State> first, HashSet<State> second){
		HashSet<State> firstCopy = (HashSet<State>) first.clone();

		firstCopy.removeAll(second);
		return firstCopy;

	}

	private HashSet<State> getSet(State s1, HashSet<HashSet<State>> P){
		for(HashSet<State> S : P){
			if (S.contains(s1)) 
				return S;
		}

		return null;
	}


	private HashSet<HashSet<State>> moorePartition( HashSet<HashSet<State>> P, HashSet<State> S){
		// 2 map nested : State -> (Input symbol -> next Set)
		HashMap<State,
		HashMap<String, HashSet<State>>> setMap = new HashMap<>();

		HashSet<HashSet<State>> partitionS = new HashSet<>();

		// classify states
		for(State s : S){
			HashSet<Transition> transitionsOutgoings = getOutgoingTransitionsFrom(s);

			// for all the transition, retrieve the next state set and update the map
			for(Transition t : transitionsOutgoings){
				if(!setMap.containsKey(s)){
					HashMap<String, HashSet<State>> inputstoSet = new HashMap<>();
					inputstoSet.put(t.getInput(),getSet(t.getTo(),P));
					setMap.put(s, inputstoSet);
				}else{
					HashMap<String, HashSet<State>> inputstoSet = setMap.get(s);
					if(!inputstoSet.containsKey(t.getInput())){
						inputstoSet.put(t.getInput(),getSet(t.getTo(),P));
					}
				}
			}
		}

		// create partition
		for(State s1 : S){
			HashMap<String, HashSet<State>> transitions_s1 = setMap.get(s1);
			HashSet<State> candidateSet = new HashSet<>();
			candidateSet.add(s1);


			for(State s2 : S){
				if(!s1.equals(s2) ){
					HashMap<String, HashSet<State>> transitions_s2 = setMap.get(s2);


					if ( (transitions_s1 == null && transitions_s2 == null) || transitions_s1.equals(transitions_s2)) {

						candidateSet.add(s2);
					}

				}
			}

			partitionS.add(candidateSet);

		}

		return partitionS;

	}



	public void minimizeMoore(){
		if (!isDeterministic(this)) {
			Automaton a = this.determinize();
			this.delta = a.delta;
			this.states = a.states;
			this.adjacencyListOutgoing = a.getAdjacencyListOutgoing();
		}

		this.removeUnreachableStates();

		HashSet<HashSet<State>> Pnew = new HashSet<>();
		Pnew.add(setSubtraction(states, getFinalStates()));
		Pnew.add(getFinalStates());

		HashSet<HashSet<State>> P ;

		do{
			P = new HashSet<>(Pnew);
			Pnew = new HashSet<>(Collections.<HashSet<State>>emptySet());

			for(HashSet<State> S : P) {
				Pnew.addAll(moorePartition(P, S));
			}


		}while(!P.equals(Pnew));


		constructMinimumAutomatonFromPartition(P);
	}


	public void minimizeHopcroft(){
		if (!isDeterministic(this)) {
			Automaton a = this.determinize();
			this.delta = a.delta;
			this.states = a.states;
			this.adjacencyListOutgoing = a.getAdjacencyListOutgoing();
		}

		this.removeUnreachableStates();


		// the partition P
		HashSet<HashSet<State>> P = new HashSet<>();
		P.add(this.getFinalStates());
		P.add(setSubtraction(this.states, this.getFinalStates()) );

		//the partition W
		HashSet<HashSet<State>> W = new HashSet<>();
		W.add(this.getFinalStates());

		HashSet<State> A = new HashSet<>();
		HashSet<State> X;
		List<HashSet<State>> listYs;
		//Random r = new Random();

		while(!W.isEmpty()){
			//choose and remove a set A from W

			for(HashSet<State> s : W){
				A = s;
				//	if(r.nextInt(2) == 0)
				break;
			}
			W.remove(A);

			for(String c : getAlphabet(this)){
				// select a X set for which a transition in c leads to a state in A
				X = getXSet(A,c);

				// list of set Y in P such that X intersect Y != empty and Y \ X != empty
				listYs = getYList(P, X);

				for(HashSet<State> Y : listYs){
					HashSet<State> xyintersection = setIntersection(X,Y);
					HashSet<State> yxsubtraction = setSubtraction(Y,X);

					P.remove(Y);
					P.add(xyintersection);
					P.add(yxsubtraction);

					if(W.contains(Y)){
						W.remove(Y);
						W.add(xyintersection);
						W.add(yxsubtraction);

					}else{
						if(xyintersection.size() <= yxsubtraction.size()){
							W.add(xyintersection);
						}else
							W.add(yxsubtraction);
					}
				}

			}
		}

		// construct the minimum automata
		constructMinimumAutomatonFromPartition(P);

	}

	private void constructMinimumAutomatonFromPartition(HashSet<HashSet<State>> P) {
		HashMap<State, State> automatonStateBinding = new HashMap<>();

		int num = 0;
		initChar++;

		this.states = new HashSet<State>();

		for(HashSet<State> macroState : P){
			boolean isInitialState = isPartitionInitialState(macroState);
			boolean isFinalState = isPartitionFinalState(macroState);

			String macroStatename = (initChar) + String.valueOf(num++);


			State mergedMacroState = new State(macroStatename, isInitialState, isFinalState);

			this.states.add(mergedMacroState);

			for(State s : macroState)
				automatonStateBinding.put(s, mergedMacroState);
		}

		HashSet<Transition> newDelta = new HashSet<>();

		for(Transition t : this.delta)
			newDelta.add(new Transition(automatonStateBinding.get(t.getFrom()), automatonStateBinding.get(t.getTo()), t.getInput()));

		this.delta = newDelta;
		this.computeAdjacencyList();
	}

	/**
	 * Gets the adjacency list of the automaton.
	 */
	public HashMap<State, HashSet<Transition>> getAdjacencyListOutgoing() {
		return adjacencyListOutgoing;
	}


	/**
	 * Sets the adjacency list of the automaton.
	 */
	public void setAdjacencyList(HashMap<State, HashSet<Transition>> adjacencyList) {
		this.adjacencyListOutgoing = adjacencyList;
	}


	/**
	 * Reverse automata operation.
	 */
	public void reverse() {

		HashSet<State> newStates = new HashSet<State>();
		HashSet<Transition> newDelta = new HashSet<Transition>();
		HashMap<State, State> mapping = new HashMap<State, State>();

		final State newInitialState = new State("init", true, false);
		newStates.add(newInitialState);

		// reversing edges
		for (Transition t : this.delta) {
			mapping.put(t.getFrom(),t.getFrom());
			mapping.put(t.getTo(),t.getTo());
			newDelta.add(new Transition(mapping.get(t.getTo()) , mapping.get(t.getFrom()), t.getInput()));
		}

		for (State s : this.states) {
			State newState = mapping.containsKey(s) ? mapping.get(s) : new State(s.getState(), false, false);

			if (s.isFinalState()) {
				newState.setFinalState(false);
				newDelta.add(new Transition(newInitialState, newState, ""));
				//newInitialState = newState;
			}

			if (s.isInitialState()) {
				newState.setFinalState(true);
				newState.setInitialState(false);
			}

			newStates.add(newState);
		}

		this.delta = newDelta;
		this.states = newStates;
		this.computeAdjacencyList();

	}

	/**
	 * Returns the regular expressions associated to this automaton
	 * using the Brzozowski algebraic method.
	 */
	public RegularExpression toRegex() {

		Vector<Equation> equations = new Vector<Equation>();

		HashMap<State, Equation> toSubstitute = new HashMap<>();
		boolean equationReplaced = true;
		boolean toSubstituteUpdated = false;

		for (State s : this.getStates()) {
			RegularExpression result = null;
			RegularExpression resultToSameState = null;

			HashSet<Transition> out = this.getOutgoingTransitionsFrom(s);

			if (out.size() > 0) {
				for (Transition t : out) {
					if(!t.getTo().equals(s))
						if (result == null)
							result = new Comp(new GroundCoeff(t.getInput()), new Var(t.getTo()));
						else
							result = new Or(result, new Comp(new GroundCoeff(t.getInput()), new Var(t.getTo())));
					else{
						if(resultToSameState == null)
							resultToSameState = new GroundCoeff(t.getInput());
						else
							resultToSameState = new Or(resultToSameState, new GroundCoeff(t.getInput()));

					}
				}

				if(resultToSameState != null && result != null){
					resultToSameState = new Star(resultToSameState);
					result = new Comp(resultToSameState, result);
				}else if(resultToSameState != null){
					resultToSameState = new Star(resultToSameState);
					result = resultToSameState;
				}

				equations.add(new Equation(s, result));
			} else
				equations.add(new Equation(s, new GroundCoeff("")));
		}

		int indexOfInitialState = 0;


		// search for initial state index and minimize equations first, then add ground formulas
		for (int i = 0; i < equations.size(); ++i) {
			Equation e;

			if (equations.get(i).getLeftSide().isInitialState()) {
				indexOfInitialState = i;
				//break;
			}

			/**
			 * This fixes the "unsoundness problem"
			 */
			if (equations.get(i).getLeftSide().isFinalState()) {				
				equations.get(i).setE(new Or(equations.get(i).getE(), new GroundCoeff("")));
			}

			equations.set(i, (e = new Equation(equations.get(i).getLeftSide(), equations.get(i).getE().simplify())));

			if (equations.get(i).isIndipendent()) {
				equations.set(i, equations.get(i).syntetize());
				equations.set(i, new Equation(equations.get(i).getLeftSide(), equations.get(i).getE().simplify()));
			}

			if(e.getE().isGround()) toSubstitute.put(e.getLeftSide(),e);
		}


		// Fix-point
		while (!equations.get(indexOfInitialState).getE().isGround()) {


			// syntetize all the equations
			for(int i = 0; i < equations.size(); i++){

				//System.out.println("Simplifying 1" + equations.get(i).getLeftSide());
				equations.set(i, new Equation(equations.get(i).getLeftSide(),
						equations.get(i).getE().simplify()));

				if (equations.get(i).isIndipendent()) {
					equations.set(i, equations.get(i).syntetize());
					equations.set(i, new Equation(equations.get(i).getLeftSide(), equations.get(i).getE().simplify()));

					//System.out.println("Syntetized 1" + equations.get(i).getLeftSide());

					// replacing in toSubstitute
					if( toSubstitute.containsKey(equations.get(i).getLeftSide())){
						toSubstitute.replace(equations.get(i).getLeftSide(), equations.get(i));
					}else {
						// add to toSubstitute if the formula is ground
						if (equations.get(i).getE().isGround()) {
							toSubstitute.put(equations.get(i).getLeftSide(), equations.get(i));
						}
					}
				}
			}

			// heuristic: if no equation has been replaced, pick one and add to toSubstitute set
			if(!equationReplaced){
				for(int l = 0 ; l < equations.size(); l++){
					if(l != indexOfInitialState && !toSubstitute.containsKey(equations.get(l).getLeftSide())){
						toSubstitute.put(equations.get(l).getLeftSide(), equations.get(l));
						break;
					}
				}
			}


			equationReplaced = false;
			toSubstituteUpdated = false;

			for (State s : toSubstitute.keySet()) {

				// search for equations to replace
				for(int i = 0 ; i < equations.size(); i++){

					// Synthetize the indipendent equations
					if (!equations.get(i).isIndipendent()) {

						equations.set(i, equations.get(i).syntetize());
						//System.out.println("Simplifying 2" + equations.get(i).getLeftSide());
						equations.set(i, new Equation(equations.get(i).getLeftSide(), equations.get(i).getE().simplify()));

						//System.out.println("Syntetized 2" + equations.get(i).getLeftSide());

						// replacing in toSubstitute
						if( toSubstitute.containsKey(equations.get(i).getLeftSide())){
							toSubstitute.replace(equations.get(i).getLeftSide(), equations.get(i));
						}else {
							// add to toSubstitute if the formula is ground
							if (equations.get(i).getE().isGround()) {
								toSubstitute.put(equations.get(i).getLeftSide(), equations.get(i));
							}
						}

					}
					if(!equations.get(i).getE().isGround()) {
						if(equations.get(i).getE().contains(s)) {
							Equation getFromSubstituteMap = toSubstitute.get( s );

							// substitute
							equations.set(i, new Equation(equations.get(i).getLeftSide(),
									equations.get(i).getE().replace(s, getFromSubstituteMap.getE() )));
							equationReplaced = true;
							//System.out.println("Replaced: " + equations.get(i).getLeftSide());

							// replacing in toSubstitute
							if( toSubstitute.containsKey(equations.get(i).getLeftSide())){
								toSubstitute.replace(equations.get(i).getLeftSide(), equations.get(i));
								break;
							}else {
								// add to toSubstitute if the formula is ground
								if (equations.get(i).getE().isGround()) {
									toSubstitute.put(equations.get(i).getLeftSide(), equations.get(i));
									toSubstituteUpdated = true;
									break;
								}
							}

							if (equations.get(i).getE().isGround()) break;

						}

					}
				}
				if(toSubstituteUpdated) break;  // to avoid ConcurrentModificationException

			}

		}

		return equations.get(indexOfInitialState).getE().simplify();

	}

	/**
	 * Checks if there exists a transition between two states.
	 * 
	 * @param s1 first state.
	 * @param s2 second state.
	 * @return the transition from s1 to s2 if exists, null otherwise.
	 */
	public Transition hasTransitionFrom(State s1, State s2) {

		for (Transition t : this.delta)
			if (t.getFrom().equals(s1) && t.getTo().equals(s2))
				return t;
		return null;
	}

	public HashSet<Transition> getTransitionFrom(State s1, State s2) {
		HashSet<Transition> result = new HashSet<Transition>();

		for (Transition t : this.delta)
			if (t.getFrom().equals(s1) && t.getTo().equals(s2))
				result.add(t);

		return result;
	}

	public boolean hasCycle() {
		Set<State> whiteSet = new HashSet<>();
		Set<State> graySet = new HashSet<>();
		Set<State> blackSet = new HashSet<>();

		for (State vertex : getStates()) 
			whiteSet.add(vertex);

		while (whiteSet.size() > 0) {
			State current = whiteSet.iterator().next();
			if(dfs(current, whiteSet, graySet, blackSet)) {
				return true;
			}
		}
		return false;
	}

	private boolean dfs(State current, Set<State> whiteSet, Set<State> graySet, Set<State> blackSet ) {
		//move current to gray set from white set and then explore it.
		moveVertex(current, whiteSet, graySet);
		for(Transition t : getOutgoingTransitionsFrom(current)) {
			State neighbor = t.getTo();

			//if in black set means already explored so continue.
			if (blackSet.contains(neighbor)) {
				continue;
			}
			//if in gray set then cycle found.
			if (graySet.contains(neighbor)) {
				return true;
			}
			if(dfs(neighbor, whiteSet, graySet, blackSet)) {
				return true;
			}
		}

		//move vertex from gray set to black set when done exploring.
		moveVertex(current, graySet, blackSet);
		return false;
	}

	private void moveVertex(State vertex, Set<State> sourceSet, Set<State> destinationSet) {
		sourceSet.remove(vertex);
		destinationSet.add(vertex);
	}

	public HashMap<State, String> bfs() {
		return bfsAux(this.getInitialState());
	}

	public HashMap<State, String> bfsAux(State root) {
		HashMap<State, String> distance = new HashMap<State, String>();
		HashMap<State, State> parent = new HashMap<State, State>();

		for (State s : this.getStates()) { 
			distance.put(s, null); 
			parent.put(s, null);
		}

		Queue<State> q = new LinkedList<State>();
		distance.put(root, "");
		q.add(root);

		while (!q.isEmpty()) {
			System.out.println(q);
			State current = q.remove();

			for (Transition t : this.getOutgoingTransitionsFrom(current)) {
				if (distance.get(t.getTo()) == null) {
					distance.put(t.getTo(), distance.get(current) +  t.getInput());
					parent.put(t.getTo(), current);
					q.add(t.getTo());
				}
			}	
		}

		return distance;
	}

	/**
	 * Returns the sets of final states.
	 * 
	 * @return an HashSet of final states.
	 */
	public HashSet<State> getFinalStates() {
		HashSet<State> result = new HashSet<State>();

		for (State s : this.states) 
			if (s.isFinalState())
				result.add(s);

		return result;
	}

	public HashSet<String> getMaximalPrefixNumber(State s, Vector<State> visited) {
		HashSet<String> result = new HashSet<String>();


		if (visited.contains(s)) 
			return result;

		visited.add(s);

		for (Transition t : this.getOutgoingTransitionsFrom(s)) {

			if (isNumeric(t.getInput()) || ((t.getInput().equals("-") || t.getInput().equals("+")) && visited.size() == 1)) { 
				HashSet<String> nexts = getMaximalPrefixNumber(t.getTo(), visited);

				if (nexts.isEmpty()) 
					result.add(t.getInput());
				else				
					for (String next : nexts) 
						result.add(t.getInput() + next);
			}
		}

		return result;
	}

	private boolean isNumeric(String input) {
		return input.matches("[0-9]");
	}

	/**
	 * Performs the set of strings of size at most n recognized from the state s.
	 * 
	 * @param s state
	 * @param n string size
	 * @return the language of size at most n recognized from the state s.
	 */
	public HashSet<String> getStringsAtMost(State s, int n) {
		HashSet<String> result = new HashSet<String>();

		if (n == 0)
			return result;

		for (Transition t : this.getOutgoingTransitionsFrom(s)) {
			String partial = t.getInput();

			if (getStringsAtMost(t.getTo(), n - 1).isEmpty())
				result.add(partial);
			else
				for (String next : getStringsAtMost(t.getTo(), n - 1))
					result.add(partial + next);
		}

		return result;
	}

	/**
	 * Check if the automaton recognizes exactly one string
	 * @return true if the automaton recognizes exactly one string, false otherwise.
	 */
	public boolean recognizesExactlyOneString() {
		for (State f : getStates())
			if (getAdjacencyListOutgoing().get(f) != null && getAdjacencyListOutgoing().get(f).size() > 1)
				return false;

		return true;
	}

	public Automaton widening(int n) {		
		HashMap<State, HashSet<String>> languages = new HashMap<State, HashSet<String>>();
		HashSet<HashSet<State>> powerStates = new HashSet<HashSet<State>>();

		for (State s : this.getStates()) 
			languages.put(s, this.getStringsAtMost(s, n));

		for (State s1 : this.getStates()) 
			for (State s2 : this.getStates())
				if (languages.get(s1).equals(languages.get(s2))) {
					boolean found = false;
					for (HashSet<State> singlePowerState : powerStates) 
						if (singlePowerState.contains(s1) || singlePowerState.contains(s2)) {
							singlePowerState.add(s1);
							singlePowerState.add(s2);
							found = true;
							break;
						}
					if (!found) {
						HashSet<State> newPowerState = new HashSet<State>();
						newPowerState.add(s1);
						newPowerState.add(s2);
						powerStates.add(newPowerState);
					}
				}

		HashSet<State> newStates = new HashSet<State>();
		HashMap<HashSet<State>, State> mapping = new HashMap<HashSet<State>, State>();


		int i = 0;

		for (HashSet<State> ps : powerStates) {
			State ns = new State("q" + i++, isPartitionInitialState(ps), isPartitionFinalState(ps));
			newStates.add(ns);
			mapping.put(ps, ns);
		}

		HashSet<Transition> newDelta = new HashSet<Transition>();

		HashSet<State> fromPartition = null;
		HashSet<State> toPartition = null;


		for (Transition t : this.getDelta()) {
			for (HashSet<State> ps : powerStates) {
				if (ps.contains(t.getFrom()))
					fromPartition = ps;
				if (ps.contains(t.getTo()))
					toPartition = ps;			
			}

			newDelta.add(new Transition(mapping.get(fromPartition), mapping.get(toPartition), t.getInput()));

		}
		return new Automaton(newDelta, newStates);
	}

	/**
	 * Returns the initial state.
	 */
	public State getInitialState() {
		for (State s : this.getStates())
			if (s.isInitialState())
				return s;

		return null;
	}

	public HashSet<State> getInitialStates() {
		HashSet<State> initialStates = new HashSet<State>();

		for (State s: this.states) 
			if (s.isInitialState()) 
				initialStates.add(s);

		return initialStates;
	}

	/**
	 * Sets the initial state.
	 */
	public void setInitialState(State initialState) {
		for (State s : this.getStates())
			if (s.getState().equals(initialState.getState()))
				s.setInitialState(true);
	}

	/**
	 * Gets the states of the automaton.
	 */
	public HashSet<State> getStates() {
		return states;
	}

	/**
	 * Sets the states of the automaton.
	 */
	public void setStates(HashSet<State> states) {
		this.states = states;
	}

	/**
	 * Returns a string representing the automaton.
	 */
	public String automatonPrint() {
		String result = "";

		for (State st: this.getStates()) {
			if (!this.getOutgoingTransitionsFrom(st).isEmpty() || st.isFinalState() || st.isInitialState()) {
				result += "[" +  st.getState() + "] " + (st.isFinalState() ? "[accept]" + (st.isInitialState() ? "[initial]\n" : "\n") : "[reject]" + (st.isInitialState() ? "[initial]\n" : "\n"));
				for (Transition t : this.getOutgoingTransitionsFrom(st))
					result += "\t" + st + " " + t.getInput() + " -> " + t.getTo() + "\n";  
			}
		}

		return result;
	}

	@Override
	public String toString() {
		return this.prettyPrint();
	}

	@Override
	public Automaton clone() {

		HashSet<State> newStates = new HashSet<State>();
		HashSet<Transition> newDelta = new HashSet<Transition>();
		HashMap<String, State> nameToStates = new HashMap<String, State>();

		for (State s: this.states) {
			State newState = new State(s.getState(), s.isInitialState(), s.isFinalState());
			newStates.add(newState);
			nameToStates.put(newState.getState(), newState);
		}

		for (Transition t : this.delta)
			newDelta.add(new Transition(nameToStates.get(t.getFrom().getState()), nameToStates.get(t.getTo().getState()), t.getInput()));

		return new Automaton(newDelta, newStates);
	}

	public boolean approxEquals(Object other) {
		if (other instanceof Automaton) 
			return (this.getDelta().size() == ((Automaton) other).getDelta().size() && this.getStates().size() == ((Automaton) other).getStates().size());

		return false;
	}

	/**
	 * Equal operator between automata.
	 */
	@Override
	public boolean equals(Object other) {
		if (other instanceof Automaton) {

			Automaton first = Automaton.intersection(this, Automaton.complement((Automaton) other));

			first.removeUnreachableStates();
			if (!first.getFinalStates().isEmpty()) 
				return false;

			Automaton second = Automaton.intersection(Automaton.complement(this), (Automaton) other);

			second.removeUnreachableStates();
			if (!second.getFinalStates().isEmpty()) 
				return false;

			return true;
		}

		return false;
	}

	@Override
	public int hashCode() {
		return getStates().size() + getDelta().size();
	}

	public int maxLengthString() {
		int max = Integer.MIN_VALUE;

		for (Vector<State> v : this.pahtsFrom(this.getInitialState(), new Vector<State>()))
			if (v.size() > max)
				max = v.size();

		return max; 
	}

	public HashSet<Vector<State>> pahtsFrom(State init, Vector<State> visited) {
		HashSet<Vector<State>> result = new HashSet<Vector<State>>();

		if (init.isFinalState() || visited.contains(init)) {
			Vector<State> v = new Vector<State>();
			result.add(v);
			return result;			
		}

		visited.add(init);

		for (Transition t : this.getOutgoingTransitionsFrom(init)) {
			Vector<State> partial = new Vector<State>();
			partial.add(init);

			for (Vector<State> v : this.pahtsFrom(t.getTo(), visited)) {
				Vector<State> p = (Vector<State>) partial.clone();
				p.addAll(v);
				result.add(p);
			}
		}
		return result;
	}

	public static Automaton leftQuotient(Automaton L1, Automaton L2) {
		Automaton result = L1.clone();
		Automaton L1copy = L1.clone();

		// Remove the initial state
		for (State q : result.getStates())
			if (q.isInitialState())
				q.setInitialState(false);


		// Remove the initial state
		for (State q : L1copy.getStates())
			if (q.isFinalState())
				q.setFinalState(false);

		for (State q: L1copy.getStates()) {
			q.setFinalState(true);

			Automaton copy = L1copy.clone();
			copy.minimize();

			if (!Automaton.isEmptyLanguageAccepted(Automaton.intersection(copy, L2))) {
				for (State rS: result.getStates())
					if (rS.getState().equals(q.getState()))
						rS.setInitialState(true);
			}

			q.setFinalState(false);
		}

		result.minimize();
		return result;
	}

	public static Automaton rightQuotient(Automaton L1, Automaton L2) {
		Automaton result = L1.clone();
		Automaton L1copy = L1.clone();


		// Remove the initial state
		for (State q : result.getStates())
			if (q.isFinalState())
				q.setFinalState(false);


		// Remove the initial state
		for (State q : L1copy.getStates())
			if (q.isInitialState())
				q.setInitialState(false);

		for (State q: L1copy.getStates()) {
			q.setInitialState(true);

			Automaton copy = L1copy.clone();
			copy.minimize();


			if (!Automaton.isEmptyLanguageAccepted(Automaton.intersection(copy, L2))) {
				for (State rS: result.getStates())
					if (rS.getState().equals(q.getState()))
						rS.setFinalState(true);
			}

			q.setInitialState(false);
		}

		result.minimize();
		return result;
	}

	public static Automaton prefix(Automaton automaton) {
		Automaton result = automaton.clone();

		for (State s : result.getStates())
			s.setFinalState(true);

		result.minimize();

		return result;
	}

	public static Automaton suffix(Automaton automaton) {
		Automaton result = automaton.clone();

		for (State s : result.getStates())
			s.setInitialState(true);

		result.minimize();

		return result;
	}

	public static Automaton prefixAtMost(long i, Automaton automaton) {
		return Automaton.intersection(Automaton.prefix(automaton), Automaton.exactLengthAutomaton(i));
	}

	public static Automaton suffixAtMost(long i, Automaton automaton) {
		return Automaton.intersection(Automaton.suffix(automaton), Automaton.exactLengthAutomaton(i));
	}

	public static Automaton suffixesAt(long i, Automaton automaton) {
		Automaton result = Automaton.leftQuotient(automaton, Automaton.prefixAtMost(i, automaton));	
		return Automaton.isEmptyLanguageAccepted(result) ? Automaton.makeEmptyString() : result;
	}

	public static Automaton substring(Automaton a, long i, long j) {	


		long initPoint = Long.min(i, j) < 0 ? 0 : Long.min(i, j);
		long endPoint = Long.max(i, j) < 0 ? 0 : Long.max(i, j);

		Automaton left = Automaton.suffixesAt(initPoint, a);	

		Automaton noProperSubs = Automaton.intersection(left, Automaton.atMostLengthAutomaton(endPoint-initPoint));
		return Automaton.union(Automaton.intersection(Automaton.rightQuotient(left,  Automaton.suffixesAt(endPoint, a)), Automaton.exactLengthAutomaton(endPoint-initPoint)), noProperSubs);	
	}

	public static Automaton substringWithUnknownEndPoint(Automaton a, long i, long j) {	 
		return Automaton.rightQuotient(Automaton.suffixesAt(i,a),  Automaton.suffix(Automaton.suffixesAt(j, a)));
	}

	public static Automaton factorsStartingAt(Automaton a, long i) {
		Automaton left = Automaton.leftQuotient(a, Automaton.prefixAtMost(i, a));	
		return Automaton.suffix(Automaton.prefix(left));
	}

	public static Automaton exactLengthAutomaton(long max) {
		HashSet<State> states = new HashSet<>();
		HashSet<Transition> delta = new HashSet<>();

		State q0 = new State("q0", true, false);
		states.add(q0);

		State prev = q0;

		for (int i = 0; i < max; ++i) {
			State next = new State("q" + i + 1, false, false);
			states.add(next);

			for (char alphabet = '!'; alphabet <= '~'; ++alphabet) 
				delta.add(new Transition(prev, next, String.valueOf(alphabet)));

			prev = next;
		}

		prev.setFinalState(true);

		return new Automaton(delta, states);
	}

	public static Automaton atMostLengthAutomaton(long max) {
		HashSet<State> states = new HashSet<>();
		HashSet<Transition> delta = new HashSet<>();

		State q0 = new State("q0", true, true);
		states.add(q0);

		State prev = q0;

		for (int i = 0; i < max; ++i) {
			State next = new State("q" + i + 1, false, true);
			states.add(next);

			for (char alphabet = '!'; alphabet <= '~'; ++alphabet) 
				delta.add(new Transition(prev, next, String.valueOf(alphabet)));

			prev = next;
		}

		prev.setFinalState(true);

		return new Automaton(delta, states);
	}

	public static Automaton charAt(Automaton a, long i) {
		return Automaton.substring(a, i, i + 1);
	}

	public static Automaton factors(Automaton a) {
		return Automaton.suffix(Automaton.prefix(a));
	}


	public LinkedList<State> minimumDijkstra(State target) {
		Set<State>  settledNodes = new HashSet<State>();
		Set<State> unSettledNodes = new HashSet<State>();
		Map<State, Integer> distance = new HashMap<State, Integer>();
		Map<State, State> predecessors = new HashMap<State, State>();

		distance.put(getInitialState(), 0);
		unSettledNodes.add(getInitialState());

		while (unSettledNodes.size() > 0) {
			State node = getMinimum(unSettledNodes, distance);
			settledNodes.add(node);
			unSettledNodes.remove(node);
			findMinimalDistances(node, distance, predecessors, unSettledNodes, settledNodes);
		}

		return getPath(target, predecessors);
	}

	public LinkedList<State> maximumDijkstra(State target) {
		Set<State>  settledNodes = new HashSet<State>();
		Set<State> unSettledNodes = new HashSet<State>();
		Map<State, Integer> distance = new HashMap<State, Integer>();
		Map<State, State> predecessors = new HashMap<State, State>();

		distance.put(getInitialState(), 0);
		unSettledNodes.add(getInitialState());

		while (unSettledNodes.size() > 0) {
			State node = getMaximum(unSettledNodes, distance);
			settledNodes.add(node);
			unSettledNodes.remove(node);
			findMaximalDistances(node, distance, predecessors, unSettledNodes, settledNodes);
		}

		return getPath(target, predecessors);
	}

	private void findMinimalDistances(State node, Map<State, Integer> distance, Map<State, State> predecessors, Set<State> unSettledNodes, Set<State> settledNodes) {
		List<State> adjacentNodes = getNeighbors(node, settledNodes);
		for (State target : adjacentNodes) {
			if (getShortestDistance(target, distance) > getShortestDistance(node, distance)
					+ getDistance(node, target)) {
				distance.put(target, getShortestDistance(node, distance)
						+ getDistance(node, target));
				predecessors.put(target, node);
				unSettledNodes.add(target);
			}
		}
	}

	private void findMaximalDistances(State node, Map<State, Integer> distance, Map<State, State> predecessors, Set<State> unSettledNodes, Set<State> settledNodes) {
		List<State> adjacentNodes = getNeighbors(node, settledNodes);
		for (State target : adjacentNodes) {
			if (getLongestDistance(target, distance) < getLongestDistance(node, distance)
					+ getDistance(node, target)) {
				distance.put(target, getShortestDistance(node, distance)
						+ getDistance(node, target));
				predecessors.put(target, node);
				unSettledNodes.add(target);
			}
		}
	}

	private int getDistance(State node, State target) {
		for (Transition edge : getDelta()) {
			if (edge.getFrom().equals(node)
					&& edge.getTo().equals(target)) {
				return 1;
			}
		}
		throw new RuntimeException("Should not happen");
	}

	private List<State> getNeighbors(State node, Set<State> settledNodes) {
		List<State> neighbors = new ArrayList<State>();
		for (Transition edge : getDelta()) {
			if (edge.getFrom().equals(node)
					&& !isSettled(edge.getTo(), settledNodes)) {
				neighbors.add(edge.getTo());
			}
		}
		return neighbors;
	}

	private State getMinimum(Set<State> vertexes, Map<State, Integer> distance) {
		State minimum = null;
		for (State vertex : vertexes) {
			if (minimum == null) {
				minimum = vertex;
			} else {
				if (getShortestDistance(vertex, distance) < getShortestDistance(minimum, distance)) {
					minimum = vertex;
				}
			}
		}
		return minimum;
	}

	private State getMaximum(Set<State> vertexes, Map<State, Integer> distance) {
		State maximum = null;
		for (State vertex : vertexes) {
			if (maximum == null) {
				maximum = vertex;
			} else {
				if (getShortestDistance(vertex, distance) > getLongestDistance(maximum, distance)) {
					maximum = vertex;
				}
			}
		}

		return maximum;
	}

	private boolean isSettled(State vertex, Set<State> settledNodes) {
		return settledNodes.contains(vertex);
	}

	private int getShortestDistance(State destination, Map<State, Integer> distance) {
		Integer d = distance.get(destination);
		if (d == null) {
			return Integer.MAX_VALUE;
		} else {
			return d;
		}
	}

	private int getLongestDistance(State destination, Map<State, Integer> distance) {
		Integer d = distance.get(destination);

		if (d == null) {
			return Integer.MIN_VALUE;
		} else {
			return d;
		}
	}

	public LinkedList<State> getPath(State target, Map<State, State> predecessors) {
		LinkedList<State> path = new LinkedList<State>();
		State step = target;

		// check if a path exists
		if (predecessors.get(step) == null) {
			path.add(target);
			return path;
		}

		path.add(step);
		while (predecessors.get(step) != null) {
			step = predecessors.get(step);
			path.add(step);
		}
		// Put it into the correct order
		Collections.reverse(path);
		return path;
	}
}