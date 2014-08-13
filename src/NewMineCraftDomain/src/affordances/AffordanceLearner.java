package affordances;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import minecraft.MapIO;
import minecraft.NameSpace;
import minecraft.MinecraftBehavior.MinecraftBehavior;
import minecraft.MinecraftBehavior.Planners.MinecraftPlanner;
import minecraft.MinecraftBehavior.Planners.VIPlanner;
import minecraft.WorldGenerator.LearningWorldGenerator;
import minecraft.WorldGenerator.MapFileGenerator;
import minecraft.WorldGenerator.WorldTypes.DeepTrenchWorld;
import minecraft.WorldGenerator.WorldTypes.PlaneGoalShelfWorld;
import minecraft.WorldGenerator.WorldTypes.PlaneGoldMineWorld;
import minecraft.WorldGenerator.WorldTypes.PlaneGoldSmeltWorld;
import minecraft.WorldGenerator.WorldTypes.PlaneTowerWorld;
import minecraft.WorldGenerator.WorldTypes.PlaneWallWorld;
import minecraft.WorldGenerator.WorldTypes.PlaneWorld;
import burlap.behavior.affordances.Affordance;
import burlap.behavior.affordances.AffordanceDelegate;
import burlap.behavior.affordances.HardAffordance;
import burlap.behavior.affordances.SoftAffordance;
import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.Policy.ActionProb;
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
	private Map<Integer,LogicalExpression> lgds;
	private MinecraftBehavior 		mcb;
	private int 					numWorldsPerLGD = 5;
	private boolean					countTotalActions = true;
	private Double 					lowInformationThreshold = 1.55; // Threshold for what is considered high entropy/low information
	private boolean					useOptions;
	private boolean					useMAs;
//	private OOMDPPlanner planner;
	
	public AffordanceLearner(MinecraftBehavior mcb, KnowledgeBase kb, Map<Integer,LogicalExpression> lgds, boolean countTotalActions) {
		this.lgds = lgds;
		this.mcb = mcb;
		this.affordanceKB = kb;
		this.countTotalActions = countTotalActions;
//		this.planner = planner;
	}
	
	public AffordanceLearner(MinecraftBehavior mcb, KnowledgeBase kb, Map<Integer,LogicalExpression> lgds, boolean countTotalActions, int numWorldsPerLGD) {
		this.lgds = lgds;
		this.mcb = mcb;
		this.affordanceKB = kb;
		this.countTotalActions = countTotalActions;
		this.numWorldsPerLGD = numWorldsPerLGD;
//		this.planner = planner;
	}
	
	/**
	 * Runs the full learning algorithm
	 */
	public void learn() {
		
		List<MapIO> maps = new ArrayList<MapIO>();
		
		String learningMapDir = NameSpace.PATHMAPS + "learning/";
		
		System.out.println("(affLearner) learningMapDir: " + learningMapDir);
//		createLearningMaps(learningMapDir);
		
		// For grid
//		List<String> mapNames = new ArrayList<String>();
//		for(int i = 0; i < this.numWorldsPerLGD; i++) {
//			mapNames.add("DeepTrenchWorld" + i + ".map");
//		}
//		for(String map : mapNames) {
////	InputStream testDir = getClass().getResourceAsStream("/" + learningMapDir + map);
//	MapIO learningMap = new MapIO(learningMapDir + map);
//	maps.add(learningMap);
//}
		
		File testDir = new File(learningMapDir);
		String[] learningMaps = testDir.list();
				
		for(String map : learningMaps) {
			MapIO learningMap = new MapIO(learningMapDir + map);
			maps.add(learningMap);
		}

		// Run learning on all the generated maps
		int mapNum = 1;
		for(MapIO map : maps) {
			System.out.println("\n\nLearning with map" + mapNum + ": " + map);
			mapNum++;
			int lgdInt = map.getHeaderHashMap().get("G");
			affordanceKB.getAffordancesController().setCurrentGoal(this.lgds.get(lgdInt));
			learnMap(map);
		}
		
		// Remove low information affordances
		removeLowInfoAffordances();
	}

	/**
	 * Creates some number of learning maps, indicated by the parameter
	 * @param learningMapDir: the number of maps to create for each goal type
	 */
	public void createLearningMaps(String learningMapDir) {
		
		MapFileGenerator mapMaker = new MapFileGenerator(2, 3, 4, learningMapDir);
		
		// Get rid of old maps
		mapMaker.clearMapsInDirectory();
		
		int numLavaBlocks = 1;
		
		System.out.println("Generating maps..." + this.numWorldsPerLGD);
		mapMaker.generateNMaps(this.numWorldsPerLGD, new DeepTrenchWorld(1, numLavaBlocks), 4, 4, 5);
		
//		mapMaker.generateNMaps(this.numWorldsPerLGD, new PlaneGoldMineWorld(numLavaBlocks), 2, 2, 4);
//		mapMaker.generateNMaps(this.numWorldsPerLGD, new PlaneGoldSmeltWorld(numLavaBlocks), 3, 3, 4);
//		mapMaker.generateNMaps(this.numWorldsPerLGD, new PlaneWallWorld(1, numLavaBlocks), 3, 2, 4);
//		mapMaker.generateNMaps(this.numWorldsPerLGD, new PlaneWorld(numLavaBlocks + 1), 3, 3, 4);
//		mapMaker.generateNMaps(this.numWorldsPerLGD, new PlaneGoalShelfWorld(2,1, numLavaBlocks), 2, 2, 5);
		
	}
	
	/**
	 * Run the learning algorithm on a given map
	 * @param map: the MapIO object to learn on
	 */
	private void learnMap(MapIO map) {
		// Update behavior with new map
		this.mcb.updateMap(map);
		
		/**
		 * We iterate through each state in the formed policy and get its "optimal" action. For each affordance,
		 * if that affordance is applicable in the state we increment its action count for the "optimal" action.
		 */
		
		MinecraftPlanner mcPlanner = new VIPlanner(mcb, this.useOptions, this.useMAs);
		OOMDPPlanner planner = mcPlanner.retrievePlanner();
		
		// Synthesize a policy on the given map
		Policy p = mcb.solve(planner);
		Map<AffordanceDelegate,List<AbstractGroundedAction>> seen = new HashMap<AffordanceDelegate,List<AbstractGroundedAction>>();  // Makes sure we don't count an action more than once per affordance (per map)
		
		// Updates the action counts (alpha)
		updateActionCounts(planner, p, seen, true);
		
		// Updates the action set size counts (beta)
		updateActionSetSizeCounts(seen);
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
			System.out.println("optimal GA:" + ga.toString());
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
	
	/**
	 * Computes the entropy of each affordance and removes low-information (high entropy) affordances
	 */
	public void removeLowInfoAffordances() {
		List<AffordanceDelegate> toRemove = new ArrayList<AffordanceDelegate>();
		// Get counts for each affordance and queue zero count affs for removal
		for(AffordanceDelegate aff : affordanceKB.getAffordances()) {
			Collection<Integer> countsCol = ((SoftAffordance)aff.getAffordance()).getActionCounts().values();
			List<Integer> counts = new ArrayList<Integer>(countsCol);
			
			// Remove if alpha counts are all 0
			double total = 0.0;
			for(Integer d : counts) {
				total += d;
			}
			if (total == 0.0) {				
				toRemove.add(aff);
				continue;
			}
			
			// Remove if entropy is close to as high as uniform
			double[] multinomial = normalizeCounts(counts);
			if(isLowInformationAffordance(multinomial)) {
				toRemove.add(aff);
			}
		}
		
		// Actually remove (done separately to avoid modifying the iterable while looping)
		for(AffordanceDelegate affToRemove : toRemove) {
			affordanceKB.remove(affToRemove);
		}
		
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
	
	/**
	 * Determines if a given multinomial is statistically indistinguishable from the uniform distribution.
	 * @param multinomial
	 * @return
	 */
	private boolean isLowInformationAffordance(double[] multinomial) {
		
		double[] uniform = new double[multinomial.length];
		Arrays.fill(uniform, 1.0/multinomial.length);
		Double result = 0.0;
		for(int i = 0; i < multinomial.length; i++) {
			if (multinomial[i] != 0.0) {
				result -= multinomial[i] * Math.log(multinomial[i]);
			}
		}
		
		// High entropy, return false.
		if(result > this.lowInformationThreshold) {
			return true;
		}
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
	public static KnowledgeBase generateAffordanceKB(List<LogicalExpression> predicates, Map<Integer, LogicalExpression> lgds, List<AbstractGroundedAction> allActions, boolean softFlag) {
		KnowledgeBase affordanceKB = new KnowledgeBase();
		
		for (LogicalExpression pf : predicates) {
			for (LogicalExpression lgd : lgds.values()) {
				Affordance aff;
				if(softFlag) {
					aff = new SoftAffordance(pf, lgd, allActions);
				}
				else {
					aff = new HardAffordance(pf, lgd, allActions);
				}
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
	 * Learns a Minecraft specific KnowledgeBase
	 * @param mb: MinecraftBehavior instance
	 * @return
	 */
	public static String generateMinecraftKB(MinecraftBehavior mcBeh, int numWorlds, boolean learningRate, boolean useOptions, boolean useMAs) {
		
//		MinecraftPlanner planner = new VIPlanner(mcBeh, useOptions, useMAs);
//		OOMDPPlanner planner = new ValueIteration(mcBeh.getDomain(), mcBeh.getRewardFunction(), mcBeh.getTerminalFunction(), mcBeh.getGamma(), mcBeh.getHashFactory(), mcBeh.getMinDelta(), Integer.MAX_VALUE);
		
		// Create actions
		List<Action> primitiveActions = mcBeh.getDomain().getActions();
		
//		List<Action> allActions = planner.retrievePlanner().actions;
		List<AbstractGroundedAction> allGroundedActions = new ArrayList<AbstractGroundedAction>();
		
		// Create Grounded Action instances for each action
		for(Action a : primitiveActions) {
			String[] freeParams = makeFreeVarListFromObjectClasses(a.getParameterClasses());
			GroundedAction ga = new GroundedAction(a, freeParams);
			allGroundedActions.add(ga);
		}
		
		// Create lgd list, predicate list, and knowledge base template.
		Map<Integer, LogicalExpression> lgds = getMinecraftGoals(mcBeh);
		KnowledgeBase affKnowledgeBase = generateAffordanceKB(getMinecraftPredicates(mcBeh), lgds, allGroundedActions, true);
		
		// Initialize Learner
		boolean countTotalActions = true;
		AffordanceLearner affLearn = new AffordanceLearner(mcBeh, affKnowledgeBase, lgds, countTotalActions, numWorlds);
		
		String kbName;
		if(learningRate) {
			kbName = "learning_rate/lr_" + affLearn.numWorldsPerLGD + ".kb";
		}
		else {
			kbName = "learned" + affLearn.numWorldsPerLGD + ".kb";
		}
		
		// Learn
		affLearn.learn();
		affKnowledgeBase.save(kbName);

		return kbName;
	}
	
	/**
	 * Retrieves the list of minecraft lifted goal descriptions
	 * @param mcBeh
	 * @return a Map<Integer, LogicalExpression>
	 */
	private static Map<Integer,LogicalExpression> getMinecraftGoals(MinecraftBehavior mcBeh) {
		// Set up goal description list
		Map<Integer,LogicalExpression> lgds = new HashMap<Integer,LogicalExpression>();
		
		PropositionalFunction atGoal = mcBeh.pfAgentAtGoal;
		LogicalExpression atGoalLE = pfAtomFromPropFunc(atGoal);
		
		PropositionalFunction hasGoldOre = mcBeh.pfAgentHasAtLeastXGoldOre;
		LogicalExpression goldOreLE = pfAtomFromPropFunc(hasGoldOre);
		
		PropositionalFunction hasGoldBlock = mcBeh.pfAgentHasAtLeastXGoldBar;
		LogicalExpression goldBlockLE = pfAtomFromPropFunc(hasGoldBlock);
		
		PropositionalFunction towerBuilt = mcBeh.pfTower;
		LogicalExpression towerBuiltLE = pfAtomFromPropFunc(towerBuilt);
		
		// Add goals
		lgds.put(0,atGoalLE);
		lgds.put(1,goldOreLE);
		lgds.put(2,goldBlockLE);
		lgds.put(3,towerBuiltLE);
		
		return lgds;
	}
	
	private static List<LogicalExpression> getMinecraftPredicates(MinecraftBehavior mcBeh) {
		// Set up precondition list
		List<LogicalExpression> predicates = new ArrayList<LogicalExpression>();
		
		// AgentInAir PFAtom
		PropositionalFunction agentInAir = mcBeh.pfAgentInMidAir;
		LogicalExpression agentInAirLE = pfAtomFromPropFunc(agentInAir);
		
		// EndOfMapInFrontOfAgent PFAtom
		PropositionalFunction endOfMapInFrontOfAgent = mcBeh.pfEndOfMapInFrontOfAgent;
		LogicalExpression endOfMapLE = pfAtomFromPropFunc(endOfMapInFrontOfAgent);
		
		// Trench in front of the agent PFAtom
		PropositionalFunction trenchInFrontOf = mcBeh.pfTrenchInFrontOfAgent;
		LogicalExpression trenchLE = pfAtomFromPropFunc(trenchInFrontOf);
		
		// Area in front of agent is clear PFAtom
		PropositionalFunction forwardWalkable = mcBeh.pfAgentLookForwardAndWalkable;
		LogicalExpression forwardWalkableLE = pfAtomFromPropFunc(forwardWalkable);

		// Gold is in front of the agent
		PropositionalFunction goldFrontAgent = mcBeh.pfGoldBlockFrontOfAgent;
		LogicalExpression goldFrontAgentLE = pfAtomFromPropFunc(goldFrontAgent);
		
		PropositionalFunction wallFrontAgent = mcBeh.pfWallInFrontOfAgent;
		LogicalExpression wallFrontAgentLE = pfAtomFromPropFunc(wallFrontAgent);
		
		PropositionalFunction feetBlockHeadClear = mcBeh.pfFeetBlockedHeadClear;
		LogicalExpression feetBlockHeadClearLE = pfAtomFromPropFunc(feetBlockHeadClear);
		
		PropositionalFunction lavaFrontAgent = mcBeh.pfLavaFrontAgent;
		LogicalExpression lavaFrontAgentLE = pfAtomFromPropFunc(lavaFrontAgent);
		
		PropositionalFunction agentLookLava = mcBeh.pfAgentLookLava;
		LogicalExpression agentLookLavaLE = pfAtomFromPropFunc(agentLookLava);
		
		PropositionalFunction agentLookBlock = mcBeh.pfAgentLookBlock;
		LogicalExpression agentLookBlockLE = pfAtomFromPropFunc(agentLookBlock);
		
		PropositionalFunction agentLookWall = mcBeh.pfAgentLookWall;
		LogicalExpression agentLookWallLE = pfAtomFromPropFunc(agentLookWall);
		
		PropositionalFunction agentInLava = mcBeh.pfAgentInLava;
		LogicalExpression agentInLavaLE = pfAtomFromPropFunc(agentInLava);
		
		PropositionalFunction agentLookTowardGoal = mcBeh.pfAgentLookTowardGoal;
		LogicalExpression agentLookTowardGoalLE = pfAtomFromPropFunc(agentLookTowardGoal);
		
		PropositionalFunction agentLookTowardGold = mcBeh.pfAgentLookTowardGold;
		LogicalExpression agentLookTowardGoldLE = pfAtomFromPropFunc(agentLookTowardGold);
		
		PropositionalFunction agentLookTowardFurnace = mcBeh.pfAgentLookTowardFurnace;
		LogicalExpression agentLookTowardFurnaceLE = pfAtomFromPropFunc(agentLookTowardFurnace);
		
		PropositionalFunction notAgentLookTowardGoal = mcBeh.pfAgentNotLookTowardGoal;
		LogicalExpression notAgentLookTowardGoalLE = pfAtomFromPropFunc(notAgentLookTowardGoal);
		
		PropositionalFunction notAgentLookTowardGold = mcBeh.pfAgentNotLookTowardGold;
		LogicalExpression notAgentLookTowardGoldLE = pfAtomFromPropFunc(notAgentLookTowardGold);
		
		PropositionalFunction notAgentLookTowardFurnace = mcBeh.pfAgentNotLookTowardFurnace;
		LogicalExpression notAgentLookTowardFurnaceLE = pfAtomFromPropFunc(notAgentLookTowardFurnace);
		
		// Add LEs to list
		predicates.add(agentInAirLE);
		predicates.add(endOfMapLE);
		predicates.add(trenchLE);
		predicates.add(forwardWalkableLE);
		predicates.add(goldFrontAgentLE);
		predicates.add(wallFrontAgentLE);
		predicates.add(feetBlockHeadClearLE);
		predicates.add(lavaFrontAgentLE);
		predicates.add(agentLookLavaLE);
		predicates.add(agentLookBlockLE);
		predicates.add(agentLookWallLE);
		predicates.add(agentInLavaLE);
		predicates.add(agentLookTowardGoalLE);
		predicates.add(agentLookTowardGoldLE);
		predicates.add(agentLookTowardFurnaceLE);
		predicates.add(notAgentLookTowardGoalLE);
		predicates.add(notAgentLookTowardGoldLE);
		predicates.add(notAgentLookTowardFurnaceLE);
		
		return predicates;
	}

	public static void main(String[] args) {
//		String templatePath = Test.class.getProtectionDomain().getCodeSource().getLocation().getPath();
//		System.out.println(new File(".").getAbsolutePath());
//		List<Integer> l = new ArrayList<Integer>();
//		
//		File input;
//		try {
//			input = new File(l.getClass().getResource("/src/minecraft/maps/learning/DeepTrenchWorld0.map").toURI());
//			System.out.println("INPUT: " + input);
//		} catch (URISyntaxException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
//		InputStream is = o.getClass.getResourceAsStream("/DeepTrenchWorld0.map");
//		InputStreamReader isr = new InputStreamReader(input);
//		BufferedReader br = new BufferedReader(isr);
		
//		MinecraftBehavior mb = new MinecraftBehavior(br);
		
		// Non-Grid
		boolean addOptions = false;
		boolean addMAs = false;
		MinecraftBehavior mcBeh = new MinecraftBehavior();
		generateMinecraftKB(mcBeh, 10, false, addOptions, addMAs);
		
	}
	
}