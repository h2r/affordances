package burlap.domain.singleagent.minecraft;

import burlap.oomdp.core.PropositionalFunction;
import burlap.oomdp.core.State;

public class Subgoal extends Node {
	
	private String name;
	private PropositionalFunction pf;
	
	public Subgoal(String name, PropositionalFunction pf) {
		this.name = name;
		this.pf = pf;
	}
	
	public boolean isTrue(State st, String[] params) {
		pf.delta(st, params);
		return false;
	}
}
//