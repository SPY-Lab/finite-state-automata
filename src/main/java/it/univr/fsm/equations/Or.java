package it.univr.fsm.equations;

import java.util.Vector;

import it.univr.fsm.machine.State;

public class Or extends RegularExpression {
	public RegularExpression first;
	public RegularExpression second;

	public Or(RegularExpression first, RegularExpression second) {
		this.first = first;
		this.second = second;
	}

	public RegularExpression getSecond() {
		return second;
	}

	public RegularExpression getFirst() {
		return first;
	}

	@Override
	public String toString() {
		return "(" + first.toString() + " + " + second.toString() + ")";
	}

	@Override
	public RegularExpression replace(State s, RegularExpression e) {
		return new Or(first.replace(s, e), second.replace(s, e));
	}


	@Override
	public RegularExpression syntetize(State s) {
		return new Or(first.syntetize(s), second.syntetize(s));
	}

	@Override
	public int hashCode() {
		return first.hashCode() + second.hashCode();
	}

	@Override
	public boolean equals(Object other) {
		if(other instanceof Or) {
			return (first.equals(((Or) other).first) && second.equals(((Or) other).second))
					|| (first.equals(((Or) other).second) && second.equals(((Or) other).first));
		}
		return false;

	}

	@Override
	public boolean containsOnly(State s) {
		return first.containsOnly(s) && second.containsOnly(s);
	}

	@Override
	public boolean contains(State s) {
		return first.contains(s) || second.contains(s);
	}

	@Override
	public Vector<RegularExpression> getTermsWithState(State s) {
		Vector<RegularExpression> v = new Vector<RegularExpression>();

		for (int i = 0; i < first.getTermsWithState(s).size(); ++i) {
			if (first.getTermsWithState(s).get(i).isGround())
				continue;
			v.add(first.getTermsWithState(s).get(i));
		}

		for (int i = 0; i < second.getTermsWithState(s).size(); ++i) {
			if (second.getTermsWithState(s).get(i).isGround())
				continue;
			v.addElement(second.getTermsWithState(s).get(i));
		}

		return v;
	}

	@Override
	public Vector<RegularExpression> getGroundTerms() {
		Vector<RegularExpression> v = new Vector<RegularExpression>();

		for (int i = 0; i < first.getGroundTerms().size(); ++i)
			v.add(first.getGroundTerms().get(i));

		for (int i = 0; i < second.getGroundTerms().size(); ++i)
			v.add(second.getGroundTerms().get(i));

		return v;
	}

	@Override
	public boolean isGround() {
		return first.isGround() && second.isGround();
	}

	@Override
	public Vector<RegularExpression> inSinglePart() {
		Vector<RegularExpression> v = new Vector<RegularExpression>();

		for (int i = 0; i < first.inSinglePart().size(); ++i)
			v.add(first.inSinglePart().get(i));

		for (int i = 0; i < second.inSinglePart().size(); ++i)
			v.add(second.inSinglePart().get(i));

		return v;
	}

	@Override
	public Vector<RegularExpression> inBlockPart(){
		Vector<RegularExpression> v = new Vector<>();
		v.add(first);
		v.add(second);
		return v;
	}

	@Override
	public RegularExpression remove(RegularExpression e) {
		first = first.remove(e);
		second = second.remove(e);


		if(second instanceof GroundCoeff && ((GroundCoeff) second).getString().equals("")){
			return first;
		}else if(first instanceof GroundCoeff && ((GroundCoeff) first).getString().equals("")){
			return second;
		}

		return this;
	}

	@Override
	public RegularExpression factorize(RegularExpression e) {
		return first.factorize(e) != null && second.factorize(e) != null ? first.factorize(e) :
			e.factorize(first) != null && e.factorize(second) != null ? e.factorize(first) : null;
	}

	@Override
	public RegularExpression simplify() {

		if (first.equals(new GroundCoeff("")) && second.equals(new GroundCoeff("")) )
			return new GroundCoeff("");
		else if (first.equals(new GroundCoeff("")) && second instanceof Star)
			return second;
		else if (second.equals(new GroundCoeff("")) && first instanceof Star)
			return first;
		return new Or(first.simplify(), second.simplify());
	}
}
