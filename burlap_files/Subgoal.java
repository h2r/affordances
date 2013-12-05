package burlap.domain.singleagent.minecraft;

import java.util.HashMap;

import burlap.oomdp.core.PropositionalFunction;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.Action;

public class Subgoal{
	
	private String name;
	private PropositionalFunction pf;
	private HashMap<String,Action> actionList;
	private HashMap<String,Affordance> affordanceList;
	private boolean tryToSatisfy = true; // Determines if we should try and satisfy this, when false, or if we should just keep searching other subgoals.
	
	public Subgoal(String name, PropositionalFunction pf) {
		this.name = name;
		this.pf = pf;
		this.actionList = new HashMap<String,Action>();
		this.affordanceList = new HashMap<String,Affordance>();
	}
	
	public Subgoal(String name, PropositionalFunction pf, boolean tryToSatisfy) {
		this.tryToSatisfy = tryToSatisfy;
		this.name = name;
		this.pf = pf;
		this.actionList = new HashMap<String,Action>();
		this.affordanceList = new HashMap<String,Affordance>();
	}
	
	public boolean isTrue(State st, String[] params) {
		return pf.isTrue(st, params);
	}
	
	public String getName() {
		return name;
	}

	public boolean inActions(String name) {
		return (this.actionList.get(name) != null);	
	}
	
	public HashMap<String,Affordance> getAffordances() {
		return this.affordanceList;
	}
	
	public void addAction(Action a) {
		this.actionList.put(a.getName(), a);
	}
	
	public void addAffordance(Affordance a) {
		this.affordanceList.put(a.getName(), a);
	}
	
	public boolean isActionListEmpty() {
		return this.actionList.isEmpty();
	}
	
	public boolean shouldSatisfy() {
		return this.tryToSatisfy;
	}
	
}
//