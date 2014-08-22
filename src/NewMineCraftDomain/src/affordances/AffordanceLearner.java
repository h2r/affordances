package affordances;

import java.io.File;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import minecraft.MapIO;
import minecraft.NameSpace;
import minecraft.MinecraftBehavior.MinecraftBehavior;
import minecraft.MinecraftBehavior.Planners.MinecraftPlanner;
import minecraft.MinecraftBehavior.Planners.VIPlanner;
import minecraft.MinecraftDomain.PropositionalFunctions.AlwaysTruePF;
import minecraft.WorldGenerator.MapFileGenerator;
import minecraft.WorldGenerator.WorldTypes.PlaneWorld;
import burlap.behavior.affordances.Affordance;
import burlap.behavior.affordances.AffordanceDelegate;
import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.QValue;
import burlap.behavior.singleagent.planning.OOMDPPlanner;
import burlap.behavior.singleagent.planning.ValueFunctionPlanner;
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
	private Map<AbstractGroundedAction,Integer> totalActionCounts = new HashMap<AbstractGroundedAction,Integer>();
	private static final Double 	lowVarianceThreshold = .004; // Threshold for what is considered high entropy/low information
	private static final double 	meanSquaredErrorThreshold = 0.004;
	private boolean					useOptions;
	private boolean					useMAs;
	private double 					fractOfStatesToUse;

	private KnowledgeBase			alwaysTrueKB;
	
	/**
	 * An object that is responsible for learning affordances.
	 * @param mcb: a MinecraftBehavior associated with the relevant domain and planners.
	 * @param kb: a KnowledgeBase of affordances containing the proper lgds/predicates.
	 * @param lgds: the list of goal descriptions to learn with
	 * @param countTotalActions: a boolean indicating if the learner should count the total number of action applications or number of worlds in which an action was optimal
	 * @param numWorlds: the number of worlds per task type to learn on
	 * @param useOptions: a boolean indicating if we should learn with options
	 * @param useMAs: a boolean indicating if we should learn with macroactions
	 * @param fractOfStatesToUse: the fraction of the state space to learn with
	 */
	public AffordanceLearner(MinecraftBehavior mcb, KnowledgeBase kb, Map<Integer,LogicalExpression> lgds, int numWorlds, boolean useOptions, boolean useMAs, double fractOfStatesToUse, List<AbstractGroundedAction> allActions) {
		this.lgds = lgds;
		this.mcb = mcb;
		this.affordanceKB = kb;
		this.useOptions = useOptions;
		this.useMAs = useMAs;
		this.fractOfStatesToUse = fractOfStatesToUse;
		this.numWorldsPerLGD = numWorlds;
		
		setupLearningDataStructures(allActions);
	}
	
	private void setupLearningDataStructures(List<AbstractGroundedAction> allActions) {
		PropositionalFunction alwaysTruePF = new AlwaysTruePF(NameSpace.PFALWAYSTRUE, mcb.getDomain(), new String[]{NameSpace.CLASSAGENT});
		LogicalExpression alwaysTrueLE = pfAtomFromPropFunc(alwaysTruePF); // For use in removing affordances that look too uniform
		this.alwaysTrueKB = new KnowledgeBase();
		for(LogicalExpression goal : this.lgds.values()) {
			Affordance aff = new Affordance(alwaysTrueLE, goal, allActions);
			AffordanceDelegate affD = new AffordanceDelegate(aff);
			this.alwaysTrueKB.add(affD);
		}
		
		// Count total action counts for each action for Naive Bayes.
		for(AbstractGroundedAction aga : allActions) {
			this.totalActionCounts.put(aga, 0);
		}
	}
	
	/**
	 * Runs the full learning algorithm
	 * @param createMaps: boolean to indicate if we should create new maps or use the existing ones
	 */
	public void learn(boolean createMaps) {
		
		// Setup map objects and create maps if we need to
		List<MapIO> maps = new ArrayList<MapIO>();
		String learningMapDir = NameSpace.PATHMAPS + "learning/";

		if(createMaps) {
			createLearningMaps(learningMapDir);
		}
		
		File testDir = new File(learningMapDir);
		String[] learningMaps = testDir.list();
				
		// Create the mapIO objects
		for(String map : learningMaps) {
			MapIO learningMap = new MapIO(learningMapDir + map);
			maps.add(learningMap);
		}

		// Run learning on all the generated maps
		int mapNum = 1;
		for(MapIO map : maps) {
			System.out.println("\n\nLearning with map" + mapNum + ".(frac=" + String.format(NameSpace.DOUBLEFORMAT, this.fractOfStatesToUse) + ") : " + map);
			mapNum++;
			int lgdInt = map.getHeaderHashMap().get("G");
			affordanceKB.getAffordancesController().setCurrentGoal(this.lgds.get(lgdInt));
			learnMap(map);
		}
		
		// Remove low information affordances
//		removeLowInfoAffordances();
		
		// Add total action counts to each aff delegate for naive bayes.
		attachTotalActionCountsToAffordances();
	}
	
	private void attachTotalActionCountsToAffordances() {
		for(AffordanceDelegate affDel : this.affordanceKB.getAffordances()) {
			affDel.getAffordance().setTotalActionCountMap(this.totalActionCounts);
		}
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
//		mapMaker.generateNMaps(this.numWorldsPerLGD, new DeepTrenchWorld(1, numLavaBlocks), 3, 3, 5);
//		mapMaker.generateNMaps(this.numWorldsPerLGD, new PlaneGoldMineWorld(numLavaBlocks), 1, 3, 4);
//		mapMaker.generateNMaps(this.numWorldsPerLGD, new PlaneGoldSmeltWorld(numLavaBlocks), 2, 2, 4);
//		mapMaker.generateNMaps(this.numWorldsPerLGD, new PlaneWallWorld(1, numLavaBlocks), 3, 1, 4);
		mapMaker.generateNMaps(this.numWorldsPerLGD, new PlaneWorld(numLavaBlocks), 3, 3, 4);

		// Not learning or testing with shelves right now
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
		
		// Updates the action counts (alpha)
		updateActionCounts(planner, p, true);
	}
	
	/**
	 * Updates the the hyperparameters for the Dirichlet Multinomial
	 * @param planner: a planner object that has already solved the given OO-MDP
	 * @param policy: a policy used to get sample trajectories
	 * @param seen: a map indicating which actions have been seen by each affordance
	 * @param countTotalActions: a boolean indicating to count total number of actions or worlds in which an action was used
	 */
	public void updateActionCounts(OOMDPPlanner planner, Policy policy, boolean countTotalActions) {
		
		// Get the fraction of states states from the policy that we're learning with
		List<State> allStates = ((ValueFunctionPlanner)planner).getAllStates();
		int numStates = allStates.size();
		int numStatesToCount = (int) Math.floor(this.fractOfStatesToUse*numStates);
		
		// Randomize the states so the fraction we get is sampled randomly 
		Collections.shuffle(allStates, new SecureRandom());
		
		// Generate several trajectories from the world
		State initialState = mcb.getInitialState();
		double initVal = ((ValueFunctionPlanner)planner).value(initialState);
		 
		// Loop over each state and count actions
		int numStatesCounted = 0;
		for (State st : allStates) {
			if (numStatesCounted >= numStatesToCount) {
				break;
			}
			
			// Check if we picked a bad state ("bad" = extremely low value)
			double stateVal = ((ValueFunctionPlanner)planner).value(st); 
			if (stateVal < 10 * initVal) {  // TODO: 10 is kind of randomly picked - make this better
				continue;
			}
			
			// Check for terminal function
			if (mcb.getTerminalFunction().isTerminal(st)) {
				continue;
			}
			
			// We're actually counting this state, so increment counter
			++numStatesCounted;
			
			// Get the optimal action for that state and update affordance counts
			GroundedAction ga = (GroundedAction) policy.getAction(st);
			QValue qv = ((ValueFunctionPlanner)planner).getQ(st, ga);

			for (AffordanceDelegate affDelegate: affordanceKB.getAffordances()) {
				// If affordance is lit up
				if(affDelegate.isActive(st)) {
					// Update counts, and indicate we've seen this affordance/action pair
					affDelegate.getAffordance().incrementActionCount(ga);
				}
			}
			
			// Increment total action counts
			int originalCount = this.totalActionCounts.get(ga);
			this.totalActionCounts.put(ga, originalCount + 1);
		}
	}
	
	/**
	 * Computes the entropy of each affordance and removes low-information (high entropy) affordances
	 */
	public void removeLowInfoAffordances() {
		List<AffordanceDelegate> toRemove = new ArrayList<AffordanceDelegate>();
		// Get counts for each affordance and queue zero count affs for removal
		for(AffordanceDelegate aff : affordanceKB.getAffordances()) {
			// Remove the always true predicate
			if(aff.getAffordance().preCondition.getClass().equals(AlwaysTruePF.class)) {
				toRemove.add(aff);
				continue;
			}
			List<Integer> counts = new ArrayList<Integer>(aff.getAffordance().getActionCounts().values());

			// Remove if alpha counts are all 0
			double total = 0.0;
			for(Integer d : counts) {
				total += d;
			}
			if (total == 0.0) {				
				toRemove.add(aff);
				continue;
			}
			
			// Remove if variance is similar to uniform (less than @field this.lowVarianceThreshold)
			double[] multinomial = normalizeCounts(counts);

			if(isCloseToUniformAffordance(multinomial)) {
				toRemove.add(aff);
				continue;
			}
			
			// Remove if too similar to the AlwaysTrue affordance distribution (the true action distribution for the world)
			if(isTooSimilarToTrueActionDistr(aff)) {
				toRemove.add(aff);
				continue;
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
	private boolean isCloseToUniformAffordance(double[] multinomial) {

		// If there is a low variance, low information.
		Double variance = computeVariance(multinomial);
		if(variance < this.lowVarianceThreshold) {
//			System.out.println("(AffordanceLearner) throwing out aff with variance: " + variance);
			return true;
		}
//		System.out.println("(AffordanceLearner) KEEEPING aff with variance: " + variance);
		return false;
	}
	
	/**
	 * Compares the given affordance to the always true affordance with the same goal (aff.g)
	 * and returns true if the mean square error between the two alpha counts are less than a
	 * particular threshold (defined in @field this.meanSquaredErrorThreshold
	 * @param aff
	 * @return
	 */
	private boolean isTooSimilarToTrueActionDistr(AffordanceDelegate aff) {
		// TODO: store always true KB in a way so we don't have to loop over and find goal matching aff
		AffordanceDelegate alwaysTrueAffToCompare = new AffordanceDelegate(null);
		for(AffordanceDelegate alwaysTrueAffDG : this.alwaysTrueKB.getAffordances()) {
			if(alwaysTrueAffDG.getAffordance().goalDescription.equals((aff.getAffordance().goalDescription))) {
				alwaysTrueAffToCompare = alwaysTrueAffDG;
			}
		}
		
		
		List<Integer> affordanceCounts = new ArrayList<Integer>(aff.getAffordance().getActionCounts().values());
		double[] affToCompareDistribution = normalizeCounts(affordanceCounts);
		List<Integer> trueActionDistrCounts = new ArrayList<Integer>(alwaysTrueAffToCompare.getAffordance().getActionCounts().values());
		double[] trueActionDistribution = normalizeCounts(trueActionDistrCounts);
		
		double sumSquared = 0;
		double meanSquaredError;
		
		for (int i = 0; i < affToCompareDistribution.length; ++i)
		{
	        sumSquared += Math.pow(affToCompareDistribution[i] - trueActionDistribution[i],2);
		}
		meanSquaredError = (double)sumSquared / affToCompareDistribution.length;
		
		if(meanSquaredError < this.meanSquaredErrorThreshold) {
			return true;
		}
		return false;
	}
	
	private static double computeVariance(double[] multinomial) {
		// Compute mean
		Double total = 0.0;
		for(int i = 0; i < multinomial.length; i++) {
			total += multinomial[i];
		}
		Double mean = total / multinomial.length;
		
		// Compute variance
		Double sumOfDifference = 0.0;
		for(int i = 0; i < multinomial.length; i++) {
			sumOfDifference += Math.pow((mean - multinomial[i]),2);
		}
		return sumOfDifference / (multinomial.length - 1);
	}
	
	/**
	 * Updates the hyperparameter for the dirichlet over action set size
	 * @param seen: map from affordances to actions
	 */
//	public void updateActionSetSizeCounts(Map<AffordanceDelegate,List<AbstractGroundedAction>> seen) {
//		// Count the action set size for each affordance for this world
//		double counted = 0.0;
//		for (AffordanceDelegate affDelegate: affordanceKB.getAffordances()) {
//			if (seen.get(affDelegate).size() > 0) {
//				++counted;
//				((SoftAffordance)affDelegate.getAffordance()).updateActionSetSizeCount(seen.get(affDelegate).size());
//			}
//			else{
//			}
//		}
////		System.out.println("(AffordanceLearner)Ratio of counted set sizes: " + (counted / affordanceKB.getAffordances().size()));
//	}
	
	/**
	 * Generates an affordance knowledge base object
	 * @param predicates: the list of predicates to use
	 * @param lgds: a list of goals to use
	 * @param allActions: the set of possible actions (OO-MDP action set)
	 * @return
	 */
	public static KnowledgeBase generateAffordanceKB(List<LogicalExpression> predicates, Map<Integer, LogicalExpression> lgds, List<AbstractGroundedAction> allActions) {
		KnowledgeBase affordanceKB = new KnowledgeBase();
		
		for (LogicalExpression pf : predicates) {
			for (LogicalExpression lgd : lgds.values()) {
				Affordance aff = new Affordance(pf, lgd, allActions);
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
			affDelegate.printCounts();
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
	 * Learns and returns a minecraft specific knowledge base of affordances
	 * @param mcBeh: The MinecraftBehavior object to plan with.
	 * @param numWorlds: The number of maps of each task type to learn on.
	 * @param learningRate: A boolean indicating if this KB is for learning rate purposes (changes location of KB).
	 * @param useOptions: A boolean indicating if we should learn with options
	 * @param useMAs: A boolean indicating if we should learn with macroactions
	 * @param fracOfStateSpace: The fraction of the state space to learn with.
	 * @return
	 */
	public static String generateMinecraftKB(MinecraftBehavior mcBeh, int numWorlds, boolean learningRate, boolean useOptions, boolean useMAs, double fracOfStateSpace) {
		
		// Get Actions
		List<AbstractGroundedAction> allGroundedActions = getAllActions(mcBeh, useOptions, useMAs);
		
		// Create lgd list, predicate list, and knowledge base template.
		Map<Integer, LogicalExpression> lgds = getMinecraftGoals(mcBeh);
		KnowledgeBase affKnowledgeBase = generateAffordanceKB(getMinecraftPredicates(mcBeh), lgds, allGroundedActions);
		
		// Initialize Learner
		AffordanceLearner affLearn = new AffordanceLearner(mcBeh, affKnowledgeBase, lgds, numWorlds, useOptions, useMAs, fracOfStateSpace, allGroundedActions);

		String kbName;
		String kbDir = "";
		if(learningRate) {
			kbName = "lr_" + String.format(NameSpace.DOUBLEFORMAT, affLearn.fractOfStatesToUse) + ".kb";
			kbDir = "learning_rate/";
		}
		else {
			kbName = "learned/learned" + affLearn.numWorldsPerLGD;
			if(useMAs) kbName += "_ma";
			else if(useOptions) kbName += "_op";
			else kbName += "_prim_acts";
			kbName += ".kb";
		}

		// Learn
		affLearn.learn(true);
		affKnowledgeBase.save(kbDir + kbName);

		return kbName;
	}
	
	private static List<AbstractGroundedAction> getAllActions(MinecraftBehavior mcBeh, boolean useOptions, boolean useMAs) {
		
		List<AbstractGroundedAction> allGroundedActions = new ArrayList<AbstractGroundedAction>();
		
		// Create Grounded Action instances for each primitive action
		List<Action> primitiveActions = mcBeh.getDomain().getActions();
		for(Action a : primitiveActions) {
			String[] freeParams = makeFreeVarListFromObjectClasses(a.getParameterClasses());
			GroundedAction ga = new GroundedAction(a, freeParams);
			allGroundedActions.add(ga);
		}
		
		// Create Grounded Action instances for each temporally extended action
		List<Action> temporallyExtendedActions = new ArrayList<Action>(MinecraftPlanner.getMapOfMAsAndOptions(mcBeh, useOptions, useMAs).values());
		for(Action a : temporallyExtendedActions) {
			String[] freeParams = makeFreeVarListFromObjectClasses(a.getParameterClasses());
			GroundedAction ga = new GroundedAction(a, freeParams);
			allGroundedActions.add(ga);
		}
		
		return allGroundedActions;
		
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
		
		// Add goals
		lgds.put(0,atGoalLE);
		lgds.put(1,goldOreLE);
		lgds.put(2,goldBlockLE);
		
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
		PropositionalFunction agentCanWalk = mcBeh.pfAgentCanWalk;
		LogicalExpression agentCanWAlkLE = pfAtomFromPropFunc(agentCanWalk);

		// Gold is in front of the agent
		PropositionalFunction goldFrontAgent = mcBeh.pfGoldBlockFrontOfAgent;
		LogicalExpression goldFrontAgentLE = pfAtomFromPropFunc(goldFrontAgent);
		
		// Wall (dest) front of agent
		PropositionalFunction wallFrontAgent = mcBeh.pfWallInFrontOfAgent;
		LogicalExpression wallFrontAgentLE = pfAtomFromPropFunc(wallFrontAgent);
		
		// Furnace front of agent
		PropositionalFunction furnaceFrontAgent = mcBeh.pfFurnaceInFrontOfAgent;
		LogicalExpression furnaceFrontAgentLE = pfAtomFromPropFunc(furnaceFrontAgent);
		
		// Hurdle front of agent
		PropositionalFunction hurdleFrontAgent = mcBeh.pfHurdleInFrontOfAgent;
		LogicalExpression hurdleFrontAgentLE = pfAtomFromPropFunc(hurdleFrontAgent);
		
		// Lava front of agent
		PropositionalFunction lavaFrontAgent = mcBeh.pfLavaFrontAgent;
		LogicalExpression lavaFrontAgentLE = pfAtomFromPropFunc(lavaFrontAgent);
		
		// Agent look at lava
		PropositionalFunction agentLookLava = mcBeh.pfAgentLookLava;
		LogicalExpression agentLookLavaLE = pfAtomFromPropFunc(agentLookLava);
		
		// Agent look at (dest) block
		PropositionalFunction agentLookDestWall = mcBeh.pfAgentLookDestWall;
		LogicalExpression agentLookDestWallLE = pfAtomFromPropFunc(agentLookDestWall);
		
		// Agent look at (ind) wall
		PropositionalFunction agentLookIndWall = mcBeh.pfAgentLookIndWall;
		LogicalExpression agentLookIndWallLE = pfAtomFromPropFunc(agentLookIndWall);
		
		// Agent in lava
		PropositionalFunction agentInLava = mcBeh.pfAgentInLava;
		LogicalExpression agentInLavaLE = pfAtomFromPropFunc(agentInLava);
		
		// Looking toward goal
		PropositionalFunction agentLookTowardGoal = mcBeh.pfAgentLookTowardGoal;
		LogicalExpression agentLookTowardGoalLE = pfAtomFromPropFunc(agentLookTowardGoal);
		
		// Looking toward gold
		PropositionalFunction agentLookTowardGold = mcBeh.pfAgentLookTowardGold;
		LogicalExpression agentLookTowardGoldLE = pfAtomFromPropFunc(agentLookTowardGold);
		
		// Looking toward furnace
		PropositionalFunction agentLookTowardFurnace = mcBeh.pfAgentLookTowardFurnace;
		LogicalExpression agentLookTowardFurnaceLE = pfAtomFromPropFunc(agentLookTowardFurnace);
		
		// Not looking toward goal
		PropositionalFunction notAgentLookTowardGoal = mcBeh.pfAgentNotLookTowardGoal;
		LogicalExpression notAgentLookTowardGoalLE = pfAtomFromPropFunc(notAgentLookTowardGoal);
		
		// Not looking toward gold
		PropositionalFunction notAgentLookTowardGold = mcBeh.pfAgentNotLookTowardGold;
		LogicalExpression notAgentLookTowardGoldLE = pfAtomFromPropFunc(notAgentLookTowardGold);
		
		// Not looking toward furnace
		PropositionalFunction notAgentLookTowardFurnace = mcBeh.pfAgentNotLookTowardFurnace;
		LogicalExpression notAgentLookTowardFurnaceLE = pfAtomFromPropFunc(notAgentLookTowardFurnace);
		
		// Agent can jump
		PropositionalFunction agentCanJump = mcBeh.pfAgentCanJump;
		LogicalExpression agentCanJumpLE = pfAtomFromPropFunc(agentCanJump);
		
		// Add LEs to list
		predicates.add(agentInAirLE);
		predicates.add(endOfMapLE);
		predicates.add(trenchLE);
		predicates.add(agentCanWAlkLE);
		predicates.add(goldFrontAgentLE);
		predicates.add(wallFrontAgentLE);
		predicates.add(hurdleFrontAgentLE);
		predicates.add(lavaFrontAgentLE);
		predicates.add(agentLookLavaLE);
		predicates.add(agentLookDestWallLE);
		predicates.add(agentLookIndWallLE);
		predicates.add(agentInLavaLE);
		predicates.add(agentLookTowardGoalLE);
		predicates.add(agentLookTowardGoldLE);
		predicates.add(agentLookTowardFurnaceLE);
		predicates.add(notAgentLookTowardGoalLE);
		predicates.add(notAgentLookTowardGoldLE);
		predicates.add(notAgentLookTowardFurnaceLE);
		predicates.add(agentCanJumpLE);
		predicates.add(furnaceFrontAgentLE);
		
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
		
		boolean addOptions = false;
		boolean addMAs = false;
		double fractionOfStateSpaceToLearnWith = 1.0;
		final int numWorldsToLearnWith = 3;
		MinecraftBehavior mcBeh = new MinecraftBehavior();
		generateMinecraftKB(mcBeh, numWorldsToLearnWith, false, addOptions, addMAs, fractionOfStateSpaceToLearnWith);
	}
	
}