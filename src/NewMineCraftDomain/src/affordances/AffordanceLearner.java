package affordances;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import minecraft.MapIO;
import minecraft.MinecraftBehavior;
import minecraft.NameSpace;
import minecraft.WorldGenerator.LearningWorldGenerator;
import minecraft.WorldGenerator.MapFileGenerator;
import burlap.behavior.affordances.Affordance;
import burlap.behavior.affordances.AffordanceDelegate;
import burlap.behavior.affordances.SoftAffordance;
import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.QValue;
import burlap.behavior.singleagent.planning.OOMDPPlanner;
import burlap.behavior.singleagent.planning.ValueFunctionPlanner;
import burlap.behavior.singleagent.planning.stochastic.valueiteration.ValueIteration;
import burlap.oomdp.core.AbstractGroundedAction;
import burlap.oomdp.core.GroundedProp;
import burlap.oomdp.core.PropositionalFunction;
import burlap.oomdp.core.State;
import burlap.oomdp.logicalexpressions.LogicalExpression;
import burlap.oomdp.logicalexpressions.PFAtom;
import burlap.oomdp.singleagent.Action;
import burlap.oomdp.singleagent.GroundedAction;

public class AffordanceLearner {
	
	private KnowledgeBase 			affordanceKB;
	private List<LogicalExpression> lgds;
	private MinecraftBehavior 		mcb;
	private int 					numWorldsPerLGD = 1;
	private boolean					countTotalActions = true;

	public AffordanceLearner(MinecraftBehavior mcb, KnowledgeBase kb, List<LogicalExpression> lgds, boolean countTotalActions) {
		this.lgds = lgds;
		this.mcb = mcb;
		this.affordanceKB = kb;
		this.countTotalActions = countTotalActions;
	}
	
	/**
	 * Runs the full learning algorithm
	 */
	public void learn() {
		
		List<MapIO> maps = new ArrayList<MapIO>();
		
		String learningMapDir = "src/minecraft/maps/learning/";
		createLearningMaps(learningMapDir);
		
		File testDir = new File(learningMapDir);
		String[] learningMaps = testDir.list();
		
		for(String map : learningMaps) {
			MapIO learningMap = new MapIO(learningMapDir + map);
			maps.add(learningMap);
		}

		// Run learning on all the generated maps
		for(MapIO map : maps) {
			System.out.println("\n\nLearning with map: " + map);
			learnMap(map);
		}
	}
	
	/**
	 * Creates some number of learning maps, indicated by the parameter
	 * @param learningMapDir: the number of maps to create for each goal type
	 */
	public void createLearningMaps(String learningMapDir) {
		
		MapFileGenerator mapMaker = new MapFileGenerator(1, 3, 4, learningMapDir);
		
		// Get rid of old maps
		mapMaker.clearMapsInDirectory();
		
		// Map parameters
		int floorDepth = 1;
		char floorOf = NameSpace.CHARINDBLOCK;
		int numTrenches = 0;
		boolean straightTrench = true;
		int numWalls = 0;
		char wallOf = NameSpace.CHARDIRTBLOCKNOTPICKUPABLE;
		boolean straightWall = true;
		int depthOfGoldOre = 1;
		String[] baseFileNames = {"DeepTrenchWorld", "WallPlaneWorld", "PlaneGoldMining", "PlaneGoldSmelting", "TowerPlaneWorld",};
		
		// Trench
		mapMaker.generateNMaps(this.numWorldsPerLGD, this.lgds.get(0), 2, floorOf, 1, straightTrench, numWalls, wallOf, straightWall, depthOfGoldOre, baseFileNames[0]);
		// Wall
		mapMaker.generateNMaps(this.numWorldsPerLGD, this.lgds.get(0), floorDepth, floorOf, numTrenches, straightTrench, 1, wallOf, straightWall, depthOfGoldOre, baseFileNames[1]);
		// Find gold ore
		mapMaker.generateNMaps(this.numWorldsPerLGD, this.lgds.get(1), floorDepth, floorOf, numTrenches, straightTrench, numWalls, wallOf, straightWall, depthOfGoldOre, baseFileNames[2]);
		// Smelt gold bar
		mapMaker.generateNMaps(this.numWorldsPerLGD, this.lgds.get(2), floorDepth, floorOf, numTrenches, straightTrench, numWalls, wallOf, straightWall, depthOfGoldOre, baseFileNames[3]);
//		// Build tower
//		mapMaker.generateNMaps(this.numWorldsPerLGD, this.lgds.get(3), floorDepth, floorOf, numTrenches, straightTrench, numWalls, wallOf, straightWall, depthOfGoldOre, baseFileNames[4]);
	}
	
	
	private void learnMap(MapIO map) {
		// Update behavior with new map
		this.mcb.updateMap(map);

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
		
		// Synthesize a policy on the given map
		Policy p = mcb.solve(planner);
		Map<AffordanceDelegate,List<AbstractGroundedAction>> seen = new HashMap<AffordanceDelegate,List<AbstractGroundedAction>>();  // Makes sure we don't count an action more than once per affordance (per map)
		
		// Updates the action counts (alpha)
		updateActionCounts(planner, p, seen, true);
		
		// Updates the action set size counts (beta)
		updateActionSetSizeCounts(seen);
		
		// Remove low information affordances
//		removeLowInfoAffordances();
	}
	
	/**
	 * Updates the the hyperparameters for the Dirichlet Multinomial
	 * @param planner: a planner object that has already solved the given OO-MDP
	 * @param policy: a policy used to get sample trajectories
	 * @param seen: a map indicating which actions have been seen by each affordance
	 * @param countTotalActions: a boolean indicating to count total number of actions or worlds in which an action was used
	 */
	public void updateActionCounts(OOMDPPlanner planner, Policy policy, Map<AffordanceDelegate,List<AbstractGroundedAction>> seen, boolean countTotalActions) {
		
		// Get all states from the policy
		List<State> allStates = ((ValueFunctionPlanner)planner).getAllStates();
		
		// Generate several trajectories from the world
		State initialState = mcb.getInitialState();
		double initVal = ((ValueFunctionPlanner)planner).value(initialState);
		 
		// Loop over each state and count actions
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
			GroundedAction ga = (GroundedAction) policy.getAction(st);
			QValue qv = ((ValueFunctionPlanner)planner).getQ(st, ga);

			for (AffordanceDelegate affDelegate: affordanceKB.getAffordances()) {
				// Initialize key-value pair for this aff
				if (seen.get(affDelegate) == null) {
					seen.put(affDelegate, new ArrayList<AbstractGroundedAction>());
				}
				
				// If affordance is lit up
				if(affDelegate.primeAndCheckIfActiveInState(st, affordanceKB.getAffordancesController().currentGoal)) {
					
					// If we're counting total number of actions OR we haven't counted this action for this affordance yet
					if (this.countTotalActions || !seen.get(affDelegate).contains(ga)) {
						// Update counts, and indicate we've seen this affordance/action pair
						((SoftAffordance)affDelegate.getAffordance()).updateActionCount(ga);
						
						if (!seen.get(affDelegate).contains(ga)) {
							List<AbstractGroundedAction> acts = seen.get(affDelegate);
							acts.add(ga);
							seen.put(affDelegate, acts);
						}
					}
				}
			}
		}
	}
	
	public void removeLowInfoAffordances() {
		List<AffordanceDelegate> toRemove = new ArrayList<AffordanceDelegate>();
		
		for(AffordanceDelegate aff : affordanceKB.getAffordances()) {
			Collection<Integer> countsCol = ((SoftAffordance)aff.getAffordance()).getActionCounts().values();
			List<Integer> counts = new ArrayList<Integer>(countsCol);
			
			double total = 0.0;
			for(Integer d : counts) {
				
				total += d;
			}
			if (total == 0.0) {
				toRemove.add(aff);
			}
			
//			double[] multinomial = normalizeCounts(counts);
//			// If the distribution is indistinguishable from random, get rid of the affordance.
//			if (!isIndistinguishableFromUniform(multinomial)) {
//				affordanceKB.remove(aff);
//			}
		}
		
//		for(AffordanceDelegate affToRemove : toRemove) {
//			affordanceKB.remove(affToRemove);
//		}
		
	}
	
	/**
	 * Converts a list of counts to a multinomial distribution
	 * @param counts: the counts to inform the multinomial
	 * @return result (double[]): represents the multinomial distribution resulting from the counts given
	 */
	private double[] normalizeCounts(List<Integer> counts) {
		double[] result = new double[counts.size()];
		double total = 0.0;
		for(Integer i : counts) {
			total = total + i;
		}
		
		for(int i = 0; i < result.length; i++) {
			result[i] = counts.get(i) / total;
		}
		return result;
	}
	
	private boolean isIndistinguishableFromUniform(double[] multinomial) {
		double[] uniform = new double[multinomial.length];
		Arrays.fill(uniform, 1.0/multinomial.length);
		Double result = 0.0;
		for(int i = 0; i < multinomial.length; i++) {
			if (multinomial[i] != 0.0) {
				System.out.println("affLearner: multi, log(mult): " + multinomial[i] + "," + Math.log(multinomial[i]));
				result -= multinomial[i] * Math.log(multinomial[i]);
			}
		}
		
		System.out.println("(AffordanceLearner) entropy: " + result);
		
		return false;
	}
	
	/**
	 * Updates the hyperparameter for the dirichlet over action set size
	 * @param seen: map from affordances to actions
	 */
	public void updateActionSetSizeCounts(Map<AffordanceDelegate,List<AbstractGroundedAction>> seen) {
		// Count the action set size for each affordance for this world
		double counted = 0.0;
		for (AffordanceDelegate affDelegate: affordanceKB.getAffordances()) {
			if (seen.get(affDelegate).size() > 0) {
				++counted;
				((SoftAffordance)affDelegate.getAffordance()).updateActionSetSizeCount(seen.get(affDelegate).size());
			}
			else{
			}
		}
//		System.out.println("(AffordanceLearner)Ratio of counted set sizes: " + (counted / affordanceKB.getAffordances().size()));
	}
	
	/**
	 * Generates an affordance knowledge base object
	 * @param predicates: the list of predicates to use
	 * @param lgds: a list of goals to use
	 * @param allActions: the set of possible actions (OO-MDP action set)
	 * @return
	 */
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
	 * Helper method that prints out the counts for each affordance
	 */
	public void printCounts() {
		for (AffordanceDelegate affDelegate: this.affordanceKB.getAffordances()) {
			((SoftAffordance)affDelegate.getAffordance()).printCounts();
			System.out.println("");
		}
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

	/**
	 * Helper method that creates a PFAtom from a propositional function
	 * @param pf
	 * @return
	 */
	public static LogicalExpression pfAtomFromPropFunc(PropositionalFunction pf) {
		String[] pfFreeParams = makeFreeVarListFromObjectClasses(pf.getParameterClasses());
		GroundedProp blockGP = new GroundedProp(pf, pfFreeParams);
		return new PFAtom(blockGP);
	}
	
	/**
	 * Creates a Minecraft specific KnowledgeBase
	 * @param mb: MinecraftBehavior instance
	 * @return
	 */
	public static String generateMinecraftKB(MinecraftBehavior mb) {
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
		PropositionalFunction hasGold = mb.pfAgentHasAtLeastXGoldOre;
		LogicalExpression goldLE = pfAtomFromPropFunc(hasGold);
		
		PropositionalFunction atGoal = mb.pfAgentAtGoal;
		LogicalExpression goalLE = pfAtomFromPropFunc(atGoal);
		
		lgds.add(goalLE);
//		lgds.add(goldLE);
		
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
		
		// AgentLookForwardAndWalkable PFAtom
		PropositionalFunction forwardWalkable = mb.pfAgentLookForwardAndWalkable;
		LogicalExpression forwardWalkableLE = pfAtomFromPropFunc(forwardWalkable);
		
		// Empty Cell front of agent PFAtom
		PropositionalFunction forwardWalkableTrench = mb.pfEmptyCellFrontAgentWalk;
		LogicalExpression forwardWalkableTrenchLE = pfAtomFromPropFunc(forwardWalkableTrench);
		
		// Add LEs to list
		predicates.add(agentInAirLE);
		predicates.add(endOfMapLE);
		predicates.add(trenchLE);
		predicates.add(forwardWalkableLE);
		predicates.add(forwardWalkableTrenchLE);
		
		KnowledgeBase affKnowledgeBase = generateAffordanceKB(predicates, lgds, allGroundedActions);

		// Initialize Learner
		boolean countTotalActions = true;
		AffordanceLearner affLearn = new AffordanceLearner(mb, affKnowledgeBase, lgds, countTotalActions);
		
		String kbName = "tests" + affLearn.numWorldsPerLGD + ".kb";
		
		affLearn.learn();
		
		affKnowledgeBase.save(kbName);
		
		return kbName;
	}

	
	public static void main(String[] args) {
		MinecraftBehavior mb = new MinecraftBehavior("src/minecraft/maps/template.map");
		
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
		LogicalExpression atGoalLE = pfAtomFromPropFunc(atGoal);
		
		PropositionalFunction hasGoldOre = mb.pfAgentHasAtLeastXGoldOre;
		LogicalExpression goldOreLE = pfAtomFromPropFunc(hasGoldOre);
		
		PropositionalFunction hasGoldBlock = mb.pfAgentHasAtLeastXGoldBar;
		LogicalExpression goldBlockLE = pfAtomFromPropFunc(hasGoldBlock);
		
		PropositionalFunction towerBuilt = mb.pfTower;
		LogicalExpression towerBuiltLE = pfAtomFromPropFunc(towerBuilt);
		
		// Add goals
		lgds.add(atGoalLE);
		lgds.add(goldOreLE);
		lgds.add(goldBlockLE);
//		lgds.add(towerBuiltLE);
		
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
		
		// AgentLookForwardAndWalkable PFAtom
		PropositionalFunction forwardWalkable = mb.pfAgentLookForwardAndWalkable;
		LogicalExpression forwardWalkableLE = pfAtomFromPropFunc(forwardWalkable);

		PropositionalFunction goldFrontAgent = mb.pfGoldBlockFrontOfAgent;
		LogicalExpression goldFrontAgentLE = pfAtomFromPropFunc(goldFrontAgent);
		
		PropositionalFunction furnaceFrontAgent = mb.pfFurnaceInFrontOfAgent;
		LogicalExpression furnaceFrontAgentLE = pfAtomFromPropFunc(furnaceFrontAgent);
		
		PropositionalFunction wallFrontAgent = mb.pfWallInFrontOfAgent;
		LogicalExpression wallFrontAgentLE = pfAtomFromPropFunc(wallFrontAgent);
		
		// Add LEs to list
		predicates.add(agentInAirLE);
		predicates.add(endOfMapLE);
		predicates.add(trenchLE);
		predicates.add(forwardWalkableLE);
		predicates.add(goldFrontAgentLE);
		predicates.add(furnaceFrontAgentLE);
		predicates.add(wallFrontAgentLE);
		
		KnowledgeBase affKnowledgeBase = generateAffordanceKB(predicates, lgds, allGroundedActions);

		// Initialize Learner
		boolean countTotalActions = true;
		AffordanceLearner affLearn = new AffordanceLearner(mb, affKnowledgeBase, lgds, countTotalActions);
		
		affLearn.learn();
//		affLearn.printCounts();
		
		affKnowledgeBase.save("testremove" + affLearn.numWorldsPerLGD + ".kb");
	}
	
}