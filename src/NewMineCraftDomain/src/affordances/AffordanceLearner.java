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
	private int numWorldsPerLGD = 100;
	private MinecraftBehavior mcb;
	private MinecraftInitialStateGenerator mcsg;
	private int numTrajectoriesPerWorld = 1;
	private Random				PRG = new Random();

	public AffordanceLearner(MinecraftBehavior mcb, KnowledgeBase kb, List<LogicalExpression> lgds) {
		double[] numUpdates = {0.0};
		int numRollouts = 1000;
		int maxDepth = 250;
		this.lgds = lgds;
		this.mcb = mcb;
//		this.mcsg = mcsg;
		this.affordanceKB = kb;
		
	}
	
	
	public void learn() {
		
		List<String> maps = new ArrayList<String>();
		
		LearningWorldGenerator worldGenerator = new LearningWorldGenerator(3,3,4);
		
		for(LogicalExpression goal : this.lgds){
			for (int i = 0; i < this.numWorldsPerLGD; i++) {
				// Make a new map w/ that goal, save it to a file in maps/learning/goal/<name>
				
				// Mapfile name information
				String mapname = "src/minecraft/maps/learning/" + goal.toString() + i + ".map";
				maps.add(mapname);
				
				System.out.println(goal.toString());
				
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
				System.out.println("Action: " + ga.actionName() + " QValue: " + qv.q);
				for (AffordanceDelegate affDelegate: affordanceKB.getAll()) {
					// Initialize key-value pair for this aff
					if (seen.get(affDelegate) == null) {
						seen.put(affDelegate, new ArrayList<AbstractGroundedAction>());
					}
					
					// If affordance is lit up
					affDelegate.primeAndCheckIfActiveInState(st);
					if (affDelegate.actionIsRelevant(ga)){
						
						// If we haven't counted this action for this affordance yet
						if (!seen.get(affDelegate).contains(ga)) {
							// Update counts, seen hashmap (indicate we've seen this affordance)
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
			if (seen == null || seen.get(affDelegate) == null) {
				int x = 1 ;
			}
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
	public static String[] makeFreeVarListFromOrderGroups(String[] orderGroups, String[] objectClasses){
		List<String> groundedPropFreeVariablesList = new ArrayList<String>();
		
		// TODO: improve variable binding stuff
		// Make variables free
		for(String orderGroup : orderGroups){
			String freeVar = "?" + orderGroup;
			groundedPropFreeVariablesList.add(freeVar);
		}
		String[] groundedPropFreeVars = new String[groundedPropFreeVariablesList.size()];
		groundedPropFreeVars = groundedPropFreeVariablesList.toArray(groundedPropFreeVars);
		
		return groundedPropFreeVars;
	}
	
	public void printCounts() {
		for (AffordanceDelegate affDelegate: this.affordanceKB.getAll()) {
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
			String[] freeParams = makeFreeVarListFromOrderGroups(a.getParameterOrderGroups(), a.getParameterClasses());
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
		
		// Block PFAtom
		PropositionalFunction blockAt = mb.pfBlockAt;
		LogicalExpression blockLE = pfAtomFromPropFunc(blockAt);
		
		// EmptySpace PFAtom
		PropositionalFunction emptyAt = mb.pfEmptySpace;
		LogicalExpression emptyLE = pfAtomFromPropFunc(emptyAt);
		
		KnowledgeBase affKnowledgeBase = generateAffordanceKB(predicates, lgds, allGroundedActions);
		
		// Initialize Learner
		AffordanceLearner affLearn = new AffordanceLearner(mb, affKnowledgeBase, lgds);
		
		affLearn.learn();
		affLearn.printCounts();
		
		affKnowledgeBase.save("trenches" + affLearn.numWorldsPerLGD + ".kb");
	}
	
	private static LogicalExpression pfAtomFromPropFunc(PropositionalFunction pf) {
		String[] pfFreeParams = makeFreeVarListFromOrderGroups(pf.getParameterOrderGroups(), pf.getParameterClasses());
		GroundedProp blockGP = new GroundedProp(pf, pfFreeParams);
		return new PFAtom(blockGP);
	}

}