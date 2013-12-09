package burlap.oomdp.singleagent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Stack;

import burlap.domain.singleagent.minecraft.Affordance;
import burlap.domain.singleagent.minecraft.MinecraftDomain;
import burlap.domain.singleagent.minecraft.Subgoal;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TransitionProbability;



public abstract class Action {

	protected String					name;									//name of the action
	protected Domain					domain;									//domain that hosts the action
	protected String []					parameterClasses = new String[0];		//list of class names for each parameter of the action
	protected String []					parameterOrderGroup = new String[0];	//setting two or more parameters to the same order group indicates that the action will be same regardless of which specific object is set to each parameter
	
	
	public Action(){
		//should not be called directly, but may be useful for subclasses of Action
	}
	
	
	//parameterClasses is expected to be comma delimited with no unnecessary spaces
	public Action(String name, Domain domain, String parameterClasses){
		
		String [] pClassArray;
		if(parameterClasses.equals("")){
			pClassArray = new String[0];
		}
		else{
			pClassArray = parameterClasses.split(",");
		}
		
		//without parameter order group specified, all parameters are assumed to be in a different group
		String [] pog = new String[pClassArray.length];
		for(int i = 0; i < pog.length; i++){
			pog[i] = name + ".P" + i;
		}
		
		this.init(name, domain, pClassArray, pog);
		
	}
	
	public Action(String name, Domain domain, String [] parameterClasses){
		
		String [] pog = new String[parameterClasses.length];
		//without parameter order group specified, all parameters are assumed to be in a different group
		for(int i = 0; i < pog.length; i++){
			pog[i] = name + ".P" + i;
		}
		this.init(name, domain, parameterClasses, pog);
		
	}
	
	public Action(String name, Domain domain, String [] parameterClasses, String [] replacedClassNames){
		this.init(name, domain, parameterClasses, replacedClassNames);
	}
	
	
	public void init(String name, Domain domain, String [] parameterClasses, String [] replacedClassNames){
		
		this.name = name;
		this.domain = domain;
		this.domain.addAction(this);
		this.parameterClasses = parameterClasses;
		this.parameterOrderGroup = replacedClassNames;
		
	}
	
	public final String getName(){
		return name;
	}
	
	
	public final String[] getParameterClasses(){
		return parameterClasses;
	}
	
	public final String[] getParameterOrderGroups(){
		return parameterOrderGroup;
	}
	
	/**
	 * 
	 * @return domain that hosts the action
	 */
	public final Domain getDomain(){
		return domain;
	}
	
	// State.java passes in a string as well apparently
	public final boolean applicableInState(State st, String s, Domain domain){
		return applicableInState(st, domain);
	}
	
	
	public final boolean applicableInState(State st, Domain domain){
		if(!domain.affordanceMode) {
			return true;
		}
		System.out.println(st.toString() + " -- " + this.name);
		
		// Get relevant Affordance based on subgoal.
		
		Affordance curAfford = getRelevAffordance(st, domain);
		List<Subgoal> subgoals = curAfford.getSubgoals();
		
		// Breadth first search through affordance space
		
		LinkedList<Subgoal> bfsQ = new LinkedList<Subgoal>();
		bfsQ.addAll(subgoals);
		
//		Stack<Subgoal> tmpGoalStack = new Stack<Subgoal>();
		
//		Iterator itr = subgoals.keySet().iterator();
		while(!bfsQ.isEmpty()) {
			Subgoal sg = bfsQ.remove();
			System.out.println(sg.getName());
			if (sg.isTrue(st)) {
				if (sg.inActions(this.name)) {
					// This action is associated with a relevant subgoal, return true.
					return true;
				}
				else if (sg.hasAffordance()) {
					// Subgoal's action isn't correct but it has an affordance
					// so let's try to follow it (later)
					Affordance af = sg.getAffordance();
					for (Subgoal afSG: af.getSubgoals()) {
						if (afSG.isTrue(st) || !afSG.shouldSatisfy()) {
							// Either Subgoal is true or isn't a big deal so we take care of it now
							// Consider adding: if subGoal.inActions(this.name), return true
//							if (afSG.inActions(this.name)) {
//								return true;
//							}
//							else {
							bfsQ.add(afSG);
//							}
						}
						else if (afSG.shouldSatisfy()) {

							// Can't walk right, so we want to find a new y coord that lets us walk right
							Integer dx = Integer.parseInt(afSG.getParams()[0]);
							Integer dy = Integer.parseInt(afSG.getParams()[1]);
							Integer dz = Integer.parseInt(afSG.getParams()[2]);
							
							// Change Y positively
							while(!afSG.isTrue(st) && dy < MinecraftDomain.MAXY) {

//								Integer dx = Integer.parseInt(afSG.getParams()[0]);
//								Integer dy = Integer.parseInt(afSG.getParams()[1]) + 1;
//								Integer dz = Integer.parseInt(afSG.getParams()[2]);
								dy++;
								String[] newParams = {dx.toString(), dy.toString(), dz.toString()};
								afSG.setParams(newParams);							}

							while (!afSG.isTrue(st) && dy > -MinecraftDomain.MAXY) {
								dy--;
								String[] newParams = {dx.toString(), dy.toString(), dz.toString()};
								afSG.setParams(newParams);
							}
							
							if (afSG.isTrue(st) && afSG.hasSubGoal()) {
								String[] globParams = MinecraftDomain.locCoordsToGlobal(st, afSG.getParams());
								afSG.getSubgoal().setParams(globParams);
								
								// For now only isWalkablePX - should loop and find the place were X is 
								// walkable and make isAtLocation of that walkable X the new subgoal
								domain.goalStack.add(afSG.getSubgoal());
								curAfford = getRelevAffordance(st, domain);
								curAfford.setSubGoalParams(globParams);
								subgoals = curAfford.getSubgoals();
								bfsQ.clear();
								bfsQ.addAll(subgoals);								
							}
							


						}
					}
				}
				else if (sg.hasSubGoal()) {
					if (sg.getSubgoal().inActions(this.name)) {
						return true;
					}
				}
				
			}
			else {
//				System.out.println(curAfford.getName());
			}
		}

		// Action was not found in relevant affordances/subgoals
		return false;
	}
	

	
	/**
	 * Default behavior is that an action can be applied in any state
	 * , but this might need be overridden if that is not the case.
	 * @param st the state to perform the action on
	 * @param params list of parameters to be passed into the action
	 * @return whether the action can be performed on the given state
	 */
	public Affordance getRelevAffordance(State st, Domain domain){

		// pop stack, search affordance list for string of thing popped, perform that action.
		
		Subgoal goal = domain.goalStack.peek();

		while (goal.isTrue(st)) {
			domain.goalStack.pop();
			goal = domain.goalStack.peek();
		}
		
		HashMap<String,Affordance> affordances = domain.affordances;
		String goalName = goal.getName();
		Affordance curAfford = affordances.get("d" + goalName);
//		int[] delta = goal.delta(st);
//
//		String[] locGoalCoords = {"" + delta[0], "" + delta[1], "" + delta[2]};
//		
//		String[] globalCoords = MinecraftDomain.locCoordsToGlobal(st, locGoalCoords);
		
//		curAfford.setSubGoalParams(globalCoords);
		
		curAfford.setSubGoalParams(goal.getParams());
		
		return curAfford;
	}
	
	
	public final boolean applicableInState(State st, String params){
		return applicableInState(st, params.split(","));
	}
	
	/**
	 * Default behavior is that an action can be applied in any state
	 * , but this might need be overridden if that is not the case.
	 * @param st the state to perform the action on
	 * @param params list of parameters to be passed into the action
	 * @return whether the action can be performed on the given state
	 */
	public boolean applicableInState(State st, String [] params){

		return true; 
	}
	
	
	//params are expected to be comma delimited with no unnecessary spaces
	public final State performAction(State st, String params){
		
		return performAction(st, params.split(","));
		
	}
	/**This is a wrapper for performActionHelper that first performs a check to see whether the action is applicable to the current state.
	 * @param st the state to perform the action on
	 * @param params list of parameters to be passed into the action
	 * @return the modified State st
	 */
	public final State performAction(State st, String [] params){
		
		State resultState = st.copy();
		if(params.length == 0) {
			// Affordance case
			if(!this.applicableInState(st, domain)){
				return resultState; //can't do anything if it's not applicable in the state so return the current state
			}
		}
		else if(!this.applicableInState(st, params)){
			return resultState; //can't do anything if it's not applicable in the state so return the current state
		}
		
		return performActionHelper(resultState, params);
		
	}
	
	
	/**Naturally, this should be overridden if it's not a primitive.
	"Primitive" here means that execution is longer than one time step
	and a result of executing other actions.
	 * 
	 * @return whether the action is primitive (longer than one time step)
	 */
	public boolean isPrimitive(){
		return true;
	}
	
	
	public List<TransitionProbability> getTransitions(State st, String params){
		return this.getTransitions(st, params.split(","));
	}
	
	///this method should only be defined for finite MDPs
	//the default behavior assumes that the MDP is deterministic and will need to be
	//overridden for stochastic MDPs for each action
	public List<TransitionProbability> getTransitions(State st, String [] params){
		
		List <TransitionProbability> transition = new ArrayList<TransitionProbability>();
		State res = this.performAction(st, params);
		transition.add(new TransitionProbability(res, 1.0));
		
		return transition;
	}
	
	/**parameterClasses is expected to be comma delimited with no unnecessary spaces
	 * @param st the state to perform the action on
	 * @param params list of parameters to be passed into the action
	 * @return the modified State st
	 */
	protected abstract State performActionHelper(State st, String [] params);
	
	
	
	
	public boolean equals(Object obj){
		Action op = (Action)obj;
		if(op.name.equals(name))
			return true;
		return false;
	}
	
	public int hashCode(){
		return name.hashCode();
	}
	
	
}
