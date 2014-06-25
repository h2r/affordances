package affordances;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import minecraft.MapIO;
import minecraft.MinecraftBehavior;
import minecraft.MinecraftInitialStateGenerator;
import minecraft.NameSpace;
import minecraft.WorldGenerator.LearningWorldGenerator;
import burlap.behavior.affordances.Affordance;
import burlap.behavior.affordances.AffordanceDelegate;
import burlap.behavior.affordances.SoftAffordance;
import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.QValue;
import burlap.behavior.singleagent.planning.OOMDPPlanner;
import burlap.behavior.singleagent.planning.ValueFunctionPlanner;
import burlap.behavior.singleagent.planning.deterministic.TFGoalCondition;
import burlap.behavior.singleagent.planning.stochastic.valueiteration.ValueIteration;
import burlap.behavior.statehashing.StateHashTuple;
import burlap.oomdp.core.AbstractGroundedAction;
import burlap.oomdp.core.GroundedProp;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.PropositionalFunction;
import burlap.oomdp.core.State;
import burlap.oomdp.logicalexpressions.LogicalExpression;
import burlap.oomdp.logicalexpressions.PFAtom;
import burlap.oomdp.singleagent.Action;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.common.SinglePFTF;

public class AffordanceLearner {
	
	private KnowledgeBase affordanceKB;
	private List<LogicalExpression> lgds;
	private MinecraftBehavior mcb;
	private MinecraftInitialStateGenerator mcsg;
	private int 	numWorldsPerLGD 		= 10;
	private int 	numTrajectoriesPerWorld = 1;
	private Random	PRG 					= new Random();

	public AffordanceLearner(MinecraftBehavior mcb, KnowledgeBase kb, List<LogicalExpression> lgds) {
		this.lgds = lgds;
		this.mcb = mcb;
		this.affordanceKB = kb;
	}
	
	public void learn() {
		
		List<String> maps = new ArrayList<String>();
		
		LearningWorldGenerator worldGenerator = new LearningWorldGenerator(2,2,4);
		
		for(LogicalExpression goal : this.lgds){
			for (int i = 0; i < this.numWorldsPerLGD; i++) {
				// Make a new map w/ that goal, save it to a file in maps/learning/goal/<name>
				
				// Mapfile name information
				String mapname = "src/minecraft/maps/learning/" + goal.toString() + "/" + i + ".map";
				maps.add(mapname);
				
				// Build the map
				char[][][] charMap = worldGenerator.generateMap(goal);

				// Write header info (depends on goal specific information)
				// TODO: incorporate goal specific information
				HashMap<String,Integer> headerInfo = new HashMap<String,Integer>();
				headerInfo.put("B", 1);
				headerInfo.put("g", 0);
				headerInfo.put("b", 0);
				
				MapIO map = new MapIO(headerInfo, charMap);
				map.printHeaderAndMapToFile(mapname);
			}
		}
		
		for(String map : maps) {
			learnMap(map);
		}
	}
	
	private void learnMap(String map) {
		// Update behavior with new map
		this.mcb.updateMap(map);
		System.out.println("\n\nLearning with map: " + map);
		
		// Initialize behavior and planner
		OOMDPPlanner planner = new ValueIteration(mcb.getDomain(), mcb.getRewardFunction(), mcb.getTerminalFunction(), mcb.getGamma(), mcb.getHashFactory(), mcb.getMinDelta(), Integer.MAX_VALUE);
		/**
		 * We iterate through each state in the formed policy and get its "optimal" action. For each affordance,
		 * if that affordance is applicable in the state we increment its action count for the "optimal" action.
		 * 
		 * Note: We DO NOT want to increment an affordance's action count more than once for any given action. To
		 * avoid this we keep track of the actions that have been incremented so far for each affordance in the seen
		 * variable.
		 */
		
		// Form a policy on the given map
		Policy p = mcb.solve(planner);
		Map<AffordanceDelegate,List<AbstractGroundedAction>> seen = new HashMap<AffordanceDelegate,List<AbstractGroundedAction>>();  // Makes sure we don't count an action more than once per affordance (per map)
		List<State> allStates = ((ValueFunctionPlanner)planner).getAllStates();
		// Generate several trajectories from the world
		for (int i = 0; i < numTrajectoriesPerWorld ; i++) {

			State initialState = mcb.getInitialState();
			double initVal = ((ValueFunctionPlanner)planner).value(initialState);
			 
			for (State st: allStates) {
				
				// Check if we picked a bad state ("bad" = extremely low value)
				double stateVal = ((ValueFunctionPlanner)planner).value(st); 
				if (stateVal < 10 * initVal) {  // TODO: 10 is kind of randomly picked - make this better
					continue;
				}
				
				// Check for terminal function
				if (mcb.getTerminalFunction().isTerminal(st)) {
					continue;
				}

				// Get the optimal action for that state and update affordance counts
				GroundedAction ga = (GroundedAction) p.getAction(st);
				QValue qv = ((ValueFunctionPlanner)planner).getQ(st, ga);

				for (AffordanceDelegate affDelegate: affordanceKB.getAffordances()) {
					// Initialize key-value pair for this aff
					if (seen.get(affDelegate) == null) {
						seen.put(affDelegate, new ArrayList<AbstractGroundedAction>());
					}
					
					// If affordance is lit up
					if(affDelegate.primeAndCheckIfActiveInState(st)) {
						
						// If we haven't counted this action for this affordance yet
						if (!seen.get(affDelegate).contains(ga)) {
							// Update counts, and indicate we've seen this affordance/action pair
							((SoftAffordance)affDelegate.getAffordance()).updateActionCount(ga);
							
							List<AbstractGroundedAction> acts = seen.get(affDelegate);
							acts.add(ga);
							seen.put(affDelegate, acts);
						}
					}
				}
			}
		}
		
		for (AffordanceDelegate affDelegate: affordanceKB.getAffordances()) {
			if (seen.get(affDelegate).size() > 0) {
				((SoftAffordance)affDelegate.getAffordance()).updateActionSetSizeCount(seen.get(affDelegate).size());
			}
		}
	}
	
	public static KnowledgeBase generateAffordanceKB(List<LogicalExpression> predicates, List<LogicalExpression> lgds, List<AbstractGroundedAction> allActions) {
		KnowledgeBase affordanceKB = new KnowledgeBase();
		
		for (LogicalExpression pf : predicates) {
			for (LogicalExpression lgd : lgds) {
				Affordance aff = new SoftAffordance(pf, lgd, allActions);
				AffordanceDelegate affDelegate = new AffordanceDelegate(aff);	
				affordanceKB.add(affDelegate);
			}
		}
		
		return affordanceKB;
		
	}
	
	/**
	 * Gets a list of free variables given an OOMDP object's parameter object classes and order groups
	 * @param orderGroups
	 * @param objectClasses
	 * @return: String[] - a list of free variables
	 */
	public static String[] makeFreeVarListFromObjectClasses(String[] objectClasses){
		List<String> groundedPropFreeVariablesList = new ArrayList<String>();
		
		// TODO: improve variable binding stuff
		// Make variables free
		for(String objectClass : objectClasses){
			String freeVar = "?" + objectClass.charAt(0);
			groundedPropFreeVariablesList.add(freeVar);
		}
		String[] groundedPropFreeVars = new String[groundedPropFreeVariablesList.size()];
		groundedPropFreeVars = groundedPropFreeVariablesList.toArray(groundedPropFreeVars);
		
		return groundedPropFreeVars;
	}
	
	public void printCounts() {
		for (AffordanceDelegate affDelegate: this.affordanceKB.getAffordances()) {
			((SoftAffordance)affDelegate.getAffordance()).printCounts();
			System.out.println("");
		}
	}
	
	public static void main(String[] args) {
		MinecraftBehavior mb = new MinecraftBehavior("src/minecraft/maps/learning/template.map");
		
		List<Action> allActions = mb.getDomain().getActions();
		List<AbstractGroundedAction> allGroundedActions = new ArrayList<AbstractGroundedAction>();

		// Create Grounded Action instances for each action
		for(Action a : allActions) {
			String[] freeParams = makeFreeVarListFromObjectClasses(a.getParameterClasses());
			GroundedAction ga = new GroundedAction(a, freeParams);
			allGroundedActions.add(ga);
		}
		
		
		// Set up goal description list
		List<LogicalExpression> lgds = new ArrayList<LogicalExpression>();
		PropositionalFunction atGoal = mb.pfAgentAtGoal;
		LogicalExpression goalLE = pfAtomFromPropFunc(atGoal);
		goalLE.setName("crosstrench");
		lgds.add(goalLE);
		
		// Set up precondition list
		List<LogicalExpression> predicates = new ArrayList<LogicalExpression>();
		
		// AgentInAir PFAtom
		PropositionalFunction agentInAir = mb.pfAgentInMidAir;
		LogicalExpression agentInAirLE = pfAtomFromPropFunc(agentInAir);
		
		// EndOfMapInFrontOfAgent PFAtom
		PropositionalFunction endOfMapInFrontOfAgent = mb.pfEndOfMapInFrontOfAgent;
		LogicalExpression endOfMapLE = pfAtomFromPropFunc(endOfMapInFrontOfAgent);
		
		// TrenchInFrontOfAgent PFAtom
		PropositionalFunction trenchInFrontOf = mb.pfTrenchInFrontOfAgent;
		LogicalExpression trenchLE = pfAtomFromPropFunc(trenchInFrontOf);
		
		// Add LEs to list
		predicates.add(agentInAirLE);
		predicates.add(endOfMapLE);
		predicates.add(trenchLE);
		
		KnowledgeBase affKnowledgeBase = generateAffordanceKB(predicates, lgds, allGroundedActions);

		// Initialize Learner
		AffordanceLearner affLearn = new AffordanceLearner(mb, affKnowledgeBase, lgds);
		
		affLearn.learn();
		affLearn.printCounts();
		
		affKnowledgeBase.save("trenches" + affLearn.numWorldsPerLGD + ".kb");
		
	}
	
	private static LogicalExpression pfAtomFromPropFunc(PropositionalFunction pf) {
		String[] pfFreeParams = makeFreeVarListFromObjectClasses(pf.getParameterClasses());
		GroundedProp blockGP = new GroundedProp(pf, pfFreeParams);
		return new PFAtom(blockGP);
	}

}