package burlap.domain.singleagent.minecraft;

import java.util.HashMap;

import burlap.oomdp.core.PropositionalFunction;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.Action;

public class Subgoal{
	
	private String name;
	private String[] params;
	private PropositionalFunction pf;
//	private HashMap<String,Action> actionMap;
//	private HashMap<String,Affordance> affordanceMap;
	private Action action;
	private Affordance affordance;
	private Subgoal subgoal;
	private boolean tryToSatisfy = true; // Determines if we should try and satisfy this, when false, or if we should just keep searching other subgoals.
	
	
	public Subgoal(String name, PropositionalFunction pf, String[] params, boolean tryToSatisfy) {
		this.tryToSatisfy = tryToSatisfy;
		this.name = name;
		this.pf = pf;
		this.action = action;
		this.affordance = affordance;
		this.params = params;
//		this.actionMap = new HashMap<String,Action>();
//		this.affordanceMap = new HashMap<String,Affordance>();
	}
	
	public Subgoal(String name, PropositionalFunction pf) {
		this.name = name;
		this.pf = pf;
		this.action = action;
		this.affordance = affordance;
//		this.actionMap = new HashMap<String,Action>();
//		this.affordanceMap = new HashMap<String,Affordance>();
	}
	
	public Subgoal(String name, PropositionalFunction pf, boolean tryToSatisfy) {
		this.tryToSatisfy = tryToSatisfy;
		this.name = name;
		this.pf = pf;
		this.action = action;
		this.affordance = affordance;
//		this.actionMap = new HashMap<String,Action>();
//		this.affordanceMap = new HashMap<String,Affordance>();
	}

	
	public boolean isTrue(State st) {
		return pf.isTrue(st, this.params);
	}
	
	public int[] delta(State s) {
		return this.pf.delta(s, this.params);
	}
	
	public String getName() {
		return name;
	}

	public boolean inActions(String name) {
//		return (this.actionMap.get(name) != null);
		return (this.action != null && this.action.getName().equals(name));
	}
	
	public Affordance getAffordance() {
//		return this.affordanceMap;
		return this.affordance;
	}
	
	public Subgoal getSubgoal() {
		return this.subgoal;
	}
	
	public String[] getParams() {
		return this.params;
	}
	
	public PropositionalFunction getPropFunc() {
		return this.pf;
	}
	
	public void setAction(Action a) {
//		this.actionMap.put(a.getName(), a);
		this.action = a;
	}
	
	public void setAffordance(Affordance a) {
//		this.affordanceMap.put(a.getName(), a);
		this.affordance = a;
	}
	
	public void setSubgoal(Subgoal sg) {
		this.subgoal = sg;
	}
	
	public void setParams(String[] newParams) {
		this.params = newParams;
	}
	
	public boolean isActionListEmpty() {
//		return this.actionMap.isEmpty();
		return this.action == null;
	}
	

	public boolean hasSubGoal() {
		return this.subgoal != null;
	}
	
	public boolean hasAffordance() {
		return this.affordance != null;
	}
	
	public boolean shouldSatisfy() {
		return this.tryToSatisfy;
	}

	
}
//