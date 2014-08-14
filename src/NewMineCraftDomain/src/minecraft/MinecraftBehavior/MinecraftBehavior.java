package minecraft.MinecraftBehavior;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import minecraft.MapIO;
import minecraft.MinecraftStateParser;
import minecraft.NameSpace;
import minecraft.MinecraftBehavior.Planners.AffordanceRTDPPlanner;
import minecraft.MinecraftBehavior.Planners.AffordanceVIPlanner;
import minecraft.MinecraftBehavior.Planners.BFSPlanner;
import minecraft.MinecraftBehavior.Planners.BFSRTDPPlanner;
import minecraft.MinecraftBehavior.Planners.MinecraftPlanner;
import minecraft.MinecraftBehavior.Planners.RTDPPlanner;
import minecraft.MinecraftBehavior.Planners.VIPlanner;
import minecraft.MinecraftDomain.MinecraftDomainGenerator;
import minecraft.MinecraftDomain.MacroActions.BuildTrenchMacroAction;
import minecraft.MinecraftDomain.MacroActions.LookDownAlotMacroAction;
import minecraft.MinecraftDomain.MacroActions.SprintMacroAction;
import minecraft.MinecraftDomain.MacroActions.TurnAroundMacroAction;
import minecraft.MinecraftDomain.Options.TrenchBuildOption;
import minecraft.MinecraftDomain.Options.WalkUntilCantOption;
import affordances.KnowledgeBase;
import burlap.behavior.affordances.AffordancesController;
import burlap.behavior.singleagent.*;
import burlap.behavior.singleagent.planning.OOMDPPlanner;
import burlap.behavior.singleagent.planning.QComputablePlanner;
import burlap.behavior.singleagent.planning.commonpolicies.AffordanceGreedyQPolicy;
import burlap.behavior.singleagent.planning.commonpolicies.GreedyDeterministicQPolicy;
import burlap.behavior.singleagent.planning.commonpolicies.GreedyQPolicy;
import burlap.behavior.singleagent.planning.deterministic.DeterministicPlanner;
import burlap.behavior.singleagent.planning.deterministic.SDPlannerPolicy;
import burlap.behavior.singleagent.planning.deterministic.TFGoalCondition;
import burlap.behavior.singleagent.planning.deterministic.uninformed.bfs.BFS;
import burlap.behavior.singleagent.planning.stochastic.rtdp.AffordanceRTDP;
import burlap.behavior.singleagent.planning.stochastic.rtdp.RTDP;
import burlap.behavior.singleagent.planning.stochastic.valueiteration.AffordanceValueIteration;
import burlap.behavior.singleagent.planning.stochastic.valueiteration.ValueIteration;
import burlap.oomdp.auxiliary.StateParser;
import burlap.oomdp.core.*;
import burlap.oomdp.logicalexpressions.LogicalExpression;
import burlap.oomdp.logicalexpressions.PFAtom;
import burlap.oomdp.singleagent.*;
import burlap.oomdp.singleagent.common.SingleGoalMultipleLERF;
import burlap.oomdp.singleagent.common.SingleLETF;
import burlap.behavior.statehashing.DiscreteStateHashFactory;
import minecraft.MinecraftStateGenerator.MinecraftStateGenerator;
import minecraft.MinecraftStateGenerator.Exceptions.StateCreationException;
import subgoals.*;

/**
 * The main behavior class for the minecraft domain
 * @author Dhershkowitz
 *
 */
public class MinecraftBehavior {
    // ----- CLASS variable -----
	public MinecraftDomainGenerator	MCDomainGenerator;
	private Domain						domain;
	private StateParser					MCStateParser;
	private RewardFunction				rewardFunction;
	private TerminalFunction			terminalFunction;
	private State						initialState;
	private DiscreteStateHashFactory	hashingFactory;
	public LogicalExpression 			currentGoal;
	
	//Propositional Functions
	public PropositionalFunction		pfAgentAtGoal;
	public PropositionalFunction		pfEmptySpace;
	public PropositionalFunction		pfBlockAt;
	public PropositionalFunction		pfAgentHasAtLeastXGoldOre;
	public PropositionalFunction		pfAgentHasAtLeastXGoldBar;
	public PropositionalFunction		pfBlockInFrontOfAgent;
	public PropositionalFunction		pfEndOfMapInFrontOfAgent;
	public PropositionalFunction		pfTrenchInFrontOfAgent;
	public PropositionalFunction		pfAgentInMidAir;
	public PropositionalFunction		pfTower;
	public PropositionalFunction		pfAgentInLava;
	public PropositionalFunction		pfAgentLookTowardGoal;
	public PropositionalFunction		pfAgentLookTowardGold;
	public PropositionalFunction		pfAgentLookTowardFurnace;
	public PropositionalFunction		pfAgentNotLookTowardGoal;
	public PropositionalFunction		pfAgentNotLookTowardGold;
	public PropositionalFunction		pfAgentNotLookTowardFurnace;
	public PropositionalFunction		pfAgentCanJump;
	
	// Dave's jenky hard coded prop funcs
	public PropositionalFunction		pfAgentLookForwardAndWalkable;
	public PropositionalFunction		pfTrenchBetweenAgentAndGoal;
	public PropositionalFunction		pfEmptyCellFrontAgentWalk;
	public PropositionalFunction		pfGoldBlockFrontOfAgent;
	public PropositionalFunction		pfFurnaceInFrontOfAgent;
	public PropositionalFunction		pfWallInFrontOfAgent;
	public PropositionalFunction		pfHurdleInFrontOfAgent;
	public PropositionalFunction 		pfLavaFrontAgent;
	public PropositionalFunction 		pfAgentLookBlock;
	public PropositionalFunction 		pfAgentLookWall;
	public PropositionalFunction 		pfAgentLookLava;
	
	
	//Params for Planners
	public double						gamma = 0.99;
	public double						minDelta = .01;
	public int							maxSteps = 200;

	public int 							numRollouts = 1500; // RTDP
	public int							maxDepth = 25; // RTDP
	public int 							vInit = 1; // RTDP
	public int 							numRolloutsWithSmallChangeToConverge = 50; // RTDP
	public double						boltzmannTemperature = 0.5;
	public double						lavaReward = -10.0;

	// ----- CLASS METHODS -----
	
	/**
	 * Blank constructor to instantiate behavior. Reads in the template map.
	 */
	public MinecraftBehavior() {
		MapIO mapIO = new MapIO(NameSpace.PATHTEMPLATEMAP);
		this.updateMap(mapIO);	
	}
	
	/**
	 * Constructor to instantiate behavior
	 * @param filePath map filepath on which to perform the planning
	 */
	public MinecraftBehavior(String filePath) {
		MapIO mapIO = new MapIO(filePath);
		this.updateMap(mapIO);	
	}
	
	public MinecraftBehavior(BufferedReader reader) {
		MapIO mapIO = new MapIO(reader);
		this.updateMap(mapIO);	
	}
	
	
	public MinecraftBehavior(MapIO mapIO) {
		this.updateMap(mapIO);	
	}
	
	/**
	 * 
	 * @param filePathOfMap a filepath to the location of the ascii map to update the behavior to
	 */
	public void updateMap(MapIO mapIO) {
		char[][][] mapAs3DArray = mapIO.getMapAs3DCharArray();
		HashMap<String, Integer> headerInfo = mapIO.getHeaderHashMap();
		
		//Update domain
		this.MCDomainGenerator = new MinecraftDomainGenerator(mapAs3DArray, headerInfo);
		this.domain = MCDomainGenerator.generateDomain();
		
		//Set state parser
		this.MCStateParser = new MinecraftStateParser(domain);
		
		// Set up the state hashing system
		this.hashingFactory = new DiscreteStateHashFactory();
		this.hashingFactory.setAttributesForClass(NameSpace.CLASSAGENT, domain.getObjectClass(NameSpace.CLASSAGENT).attributeList); 
		this.hashingFactory.setAttributesForClass(NameSpace.CLASSDIRTBLOCKNOTPICKUPABLE, domain.getObjectClass(NameSpace.CLASSDIRTBLOCKNOTPICKUPABLE).attributeList);
		this.hashingFactory.setAttributesForClass(NameSpace.CLASSDIRTBLOCKPICKUPABLE, domain.getObjectClass(NameSpace.CLASSDIRTBLOCKPICKUPABLE).attributeList);
		this.hashingFactory.setAttributesForClass(NameSpace.CLASSGOLDBLOCK, domain.getObjectClass(NameSpace.CLASSGOLDBLOCK).attributeList);

		//Set initial state
		try {
			this.initialState = MinecraftStateGenerator.createInitialState(mapAs3DArray, headerInfo, domain);
		} catch (StateCreationException e) {
			e.printStackTrace();
		}
		
		// Get propositional functions
		this.pfAgentAtGoal = domain.getPropFunction(NameSpace.PFATGOAL);
		this.pfEmptySpace = domain.getPropFunction(NameSpace.PFEMPSPACE);
		this.pfBlockAt = domain.getPropFunction(NameSpace.PFBLOCKAT);
		this.pfAgentHasAtLeastXGoldOre = domain.getPropFunction(NameSpace.PFATLEASTXGOLDORE);
		this.pfAgentHasAtLeastXGoldBar = domain.getPropFunction(NameSpace.PFATLEASTXGOLDBAR);
		this.pfBlockInFrontOfAgent = domain.getPropFunction(NameSpace.PFBLOCKINFRONT);
		this.pfEndOfMapInFrontOfAgent = domain.getPropFunction(NameSpace.PFENDOFMAPINFRONT);
		this.pfAgentInMidAir = domain.getPropFunction(NameSpace.PFAGENTINMIDAIR);
		this.pfAgentLookForwardAndWalkable = domain.getPropFunction(NameSpace.PFAGENTCANWALK);
		this.pfEmptyCellFrontAgentWalk = domain.getPropFunction(NameSpace.PFEMPTYCELLINWALK);
		this.pfTower = domain.getPropFunction(NameSpace.PFTOWER);
		this.pfGoldBlockFrontOfAgent = domain.getPropFunction(NameSpace.PFGOLDFRONTAGENTONE);
		this.pfFurnaceInFrontOfAgent = domain.getPropFunction(NameSpace.PFFURNACEINFRONT);
		this.pfWallInFrontOfAgent = domain.getPropFunction(NameSpace.PFWALLINFRONT);
		this.pfHurdleInFrontOfAgent = domain.getPropFunction(NameSpace.PFHURDLEINFRONTAGENT);
		this.pfAgentInLava = domain.getPropFunction(NameSpace.PFAGENTINLAVA);
		this.pfLavaFrontAgent = domain.getPropFunction(NameSpace.PFLAVAFRONTAGENT);
		this.pfAgentLookWall = domain.getPropFunction(NameSpace.PFAGENTLOOKWALL);
		this.pfAgentLookBlock = domain.getPropFunction(NameSpace.PFAGENTLOOKBLOCK);
		this.pfAgentLookLava = domain.getPropFunction(NameSpace.PFAGENTLOOKLAVA);
		this.pfAgentLookTowardGoal = domain.getPropFunction(NameSpace.PFAGENTLOOKTOWARDGOAL);
		this.pfAgentLookTowardGold = domain.getPropFunction(NameSpace.PFAGENTLOOKTOWARDGOLD);
		this.pfAgentLookTowardFurnace = domain.getPropFunction(NameSpace.PFAGENTLOOKTOWARDFURNACE);
		this.pfAgentNotLookTowardGoal = domain.getPropFunction(NameSpace.PFAGENTNOTLOOKTOWARDGOAL);
		this.pfAgentNotLookTowardGold = domain.getPropFunction(NameSpace.PFAGENTNOTLOOKTOWARDGOLD);
		this.pfAgentNotLookTowardFurnace = domain.getPropFunction(NameSpace.PFAGENTNOTLOOKTOWARDFURNACE);
		this.pfTrenchInFrontOfAgent = domain.getPropFunction(NameSpace.PFTRENCHINFRONTAGENT);
		this.pfAgentCanJump = domain.getPropFunction(NameSpace.PFAGENTCANJUMP);
		
		// Set up goal LE and lava LE for use in reward function
		PropositionalFunction pfToUse = getPFFromHeader(headerInfo);
		this.currentGoal = new PFAtom(pfToUse.getAllGroundedPropsForState(this.initialState).get(0)); 
		LogicalExpression lavaLE = new PFAtom(this.pfAgentInLava.getAllGroundedPropsForState(this.initialState).get(0));
		
		// Set up reward function
		HashMap<LogicalExpression, Double> rewardMap = new HashMap<LogicalExpression, Double>();
		rewardMap.put(this.currentGoal, 0.0);
		rewardMap.put(lavaLE, this.lavaReward);
		this.rewardFunction = new SingleGoalMultipleLERF(rewardMap, -1);
		
//		this.rewardFunction = new SingleGoalLERF(currentGoal, 0, -1); 
		
		//Set up terminal function
		this.terminalFunction = new SingleLETF(currentGoal);
				
//		//Set up reward function
//		this.rewardFunction = new SingleGoalPFRF(pfToUse, 0, -1); 
//		
//		//Set up terminal function
//		this.terminalFunction = new SinglePFTF(pfToUse);

	}
	
	private PropositionalFunction getPFFromHeader(HashMap<String, Integer> headerInfo) {
		switch(headerInfo.get(Character.toString(NameSpace.CHARGOALDESCRIPTOR))) {
		case NameSpace.INTXYZGOAL:
			return this.pfAgentAtGoal;
		
		case NameSpace.INTGOLDOREGOAL:
			return this.pfAgentHasAtLeastXGoldOre;
			
		case NameSpace.INTGOLDBARGOAL:
			return this.pfAgentHasAtLeastXGoldBar;
		case NameSpace.INTTOWERGOAL:
			return this.pfTower;
		default:
			break;
		}
		
		return null;

	}
	
	// --- ACCESSORS ---
	
	public Domain getDomain() {
		return this.domain;
	}
	
	public RewardFunction getRewardFunction() {
		return this.rewardFunction;
	}
	
	public TerminalFunction getTerminalFunction() {
		return this.terminalFunction;
	}
	
	public double getGamma() {
		return this.gamma;
	}
	
	public DiscreteStateHashFactory getHashFactory() {
		return this.hashingFactory;
	}

	public double getMinDelta() {
		return this.minDelta;
	}
	
	public State getInitialState() {
		return this.initialState;

	}
	
	public MinecraftDomainGenerator getDomainGenerator() {
		return this.MCDomainGenerator;
	}

	
	// ---------- PLANNERS ---------- 
	
	/**
	 * Takes in an instance of an OOMDP planner and solves the OO-MDP
	 * @param planner
	 * @return p: The Policy from the solved OO-MDP
	 */
	public Policy solve(OOMDPPlanner planner) {
		// Solve the OO-MDP
		planner.planFromState(initialState);

		// Create a Q-greedy policy from the planner
		GreedyQPolicy p = new GreedyQPolicy((QComputablePlanner)planner);
		
		// Print out some infos
		EpisodeAnalysis ea = p.evaluateBehavior(initialState, this.rewardFunction, this.terminalFunction, maxSteps);
		
		System.out.println(ea.getActionSequenceString());

		return p;
	}
//	
//	public void BFSExample(boolean addOptions, boolean addMAs) {
//		TFGoalCondition goalCondition = new TFGoalCondition(this.terminalFunction);
//		
//		DeterministicPlanner planner = new BFS(this.domain, goalCondition, this.hashingFactory);
//				
//		planner.planFromState(initialState);
//		
//		Policy p = new SDPlannerPolicy(planner);
//		
//		p.evaluateBehavior(initialState, this.rewardFunction, this.terminalFunction);
//		
//		EpisodeAnalysis ea = p.evaluateBehavior(initialState, this.rewardFunction, this.terminalFunction);
//		System.out.println(ea.getActionSequenceString());
//	}
//	
//	public double[] ValueIterationPlanner(boolean addOptions, boolean addMAs){
//		TFGoalCondition goalCondition = new TFGoalCondition(this.terminalFunction);
//
//		ValueIteration planner = new ValueIteration(domain, rewardFunction, terminalFunction, gamma, hashingFactory, 0.01, Integer.MAX_VALUE);
//
//		long startTime = System.currentTimeMillis( );
//		
//		int bellmanUpdates = planner.planFromStateAndCount(initialState);
//		
//		// Create a Q-greedy policy from the planner
//		Policy p = new GreedyQPolicy((QComputablePlanner)planner);
//		
//		// Record the plan results to a file
//		EpisodeAnalysis ea = p.evaluateBehavior(initialState, rewardFunction, terminalFunction, maxSteps);
//
//		long totalPlanningTime  = System.currentTimeMillis( ) - startTime;
//		System.out.println(ea.getActionSequenceString());
//		// Count reward.
//		double totalReward = 0.;
//		for(Double d : ea.rewardSequence){
//			totalReward = totalReward + d;
//		}
//		
//		State finalState = ea.stateSequence.get(ea.stateSequence.size()-1);
//		double completed = goalCondition.satisfies(finalState) ? 1.0 : 0.0;
//		
//		double[] results = {bellmanUpdates, totalReward, completed, totalPlanningTime};
//		return results;
//	}
//	
//	public double[] AffordanceVI(KnowledgeBase affKB, boolean addOptions, boolean addMAs){
//		AffordancesController affController = affKB.getAffordancesController();
//		affController.setCurrentGoal(this.currentGoal); // Update goal to determine active affordances
//		
//		// Setup goal condition and planner
//		TFGoalCondition goalCondition = new TFGoalCondition(this.terminalFunction);
//		AffordanceValueIteration planner = new AffordanceValueIteration(domain, rewardFunction, terminalFunction, gamma, hashingFactory, 0.01, Integer.MAX_VALUE, affController);
//		
//		// Time
//		long startTime = System.currentTimeMillis( );
//		
//		// Plan and record bellmanUpdates
//		int bellmanUpdates = planner.planFromStateAndCount(initialState);
//		
//		// Create a Q-greedy policy from the planner
//		Policy p = new AffordanceGreedyQPolicy(affController, (QComputablePlanner)planner);
//		
//		// Record the plan results to a file
//		EpisodeAnalysis ea = p.evaluateBehavior(initialState, rewardFunction, terminalFunction, maxSteps);
//		
//		long totalPlanningTime  = System.currentTimeMillis( ) - startTime;
//		
//		// Count reward.
//		double totalReward = 0.;
//		for(Double d : ea.rewardSequence){
//			totalReward = totalReward + d;
//		}
//		
//		// Check to see if the planner found the goal
//		State finalState = ea.stateSequence.get(ea.stateSequence.size()-1);
//		double completed = goalCondition.satisfies(finalState) ? 1.0 : 0.0;
//		
//		
//		double[] results = {bellmanUpdates, totalReward, completed, totalPlanningTime};
//		return results;
//	}
//	
//	public double[] AffordanceRTDP(KnowledgeBase affKB, boolean addOptions, boolean addMAs){
//		AffordancesController affController = affKB.getAffordancesController();
//		affController.setCurrentGoal(this.currentGoal); // Update goal to determine active affordances
//		
//		AffordanceRTDP planner = new AffordanceRTDP(domain, rewardFunction, terminalFunction, gamma, hashingFactory, vInit, numRollouts, minDelta, maxDepth, affController, numRolloutsWithSmallChangeToConverge);
//		
//		long startTime = System.currentTimeMillis( );
//		
//		int bellmanUpdates = planner.planFromStateAndCount(initialState);
//
//		// Create a Policy from the planner
////		Policy p = new AffordanceBoltzmannQPolicy((QComputablePlanner)planner, boltzmannTemperature, affController);
//		Policy p = new AffordanceGreedyQPolicy(affController, (QComputablePlanner)planner);
//		EpisodeAnalysis ea = p.evaluateBehavior(initialState, rewardFunction, terminalFunction, maxSteps);
//		
//		// Compute CPU time
//		long totalPlanningTime  = System.currentTimeMillis( ) - startTime;
//		
//		// Count reward.
//		double totalReward = 0.;
//		for(Double d : ea.rewardSequence){
//			totalReward = totalReward + d;
//		}
//		
//		// Check if task completed
//		State finalState = ea.getState(ea.stateSequence.size() - 1);
//		double completed = terminalFunction.isTerminal(finalState) ? 1.0 : 0.0;
//		
////		System.out.println(ea.getActionSequenceString());
//
//		double[] results = {bellmanUpdates, totalReward, completed, totalPlanningTime};
//		
//		return results;
//	}
//	
//	/**
//	 * Solves the current OO-MDP using Real Time Dynamic Programming
//	 * @return: The number of bellman updates performed during planning
//	 */
//	public double[] RTDP(boolean addOptions, boolean addMAs) {
//
//		RTDP planner = new RTDP(domain, rewardFunction, terminalFunction, gamma, hashingFactory, vInit, numRollouts, minDelta, maxDepth);
//		addOptionsToOOMDPPlanner(planner, addOptions, addMAs);
//		planner.setMinNumRolloutsWithSmallValueChange(numRolloutsWithSmallChangeToConverge);
//		
//		long startTime = System.currentTimeMillis( );
//		
//		int bellmanUpdates = planner.planFromStateAndCount(initialState);
//		// Create a Q-greedy policy from the planner
//		Policy p = new GreedyQPolicy((QComputablePlanner)planner);
//		EpisodeAnalysis ea = p.evaluateBehavior(initialState, rewardFunction, terminalFunction, maxSteps);
//		
//		// Compute CPU time
//		long totalPlanningTime  = System.currentTimeMillis( ) - startTime;
//		
//		// Count reward
//		double totalReward = 0.;
//		for(Double d : ea.rewardSequence){
//			totalReward = totalReward + d;
//		}
//		
//		// Check if task completed
//		State finalState = ea.getState(ea.stateSequence.size() - 1);
//		double completed = terminalFunction.isTerminal(finalState) ? 1.0 : 0.0;
//		
//		System.out.println(ea.getActionSequenceString());
//
//		double[] results = {bellmanUpdates, totalReward, completed, totalPlanningTime};
//
//		return results;
//	}
//	
//	public double[] SubgoalPlanner(OOMDPPlanner planner) {
//		
//		List<Subgoal> subgoalKB = new ArrayList<Subgoal>();
//		
//		LogicalExpression hasOreLE = new PFAtom(this.pfAgentHasAtLeastXGoldOre.getAllGroundedPropsForState(this.initialState).get(0));
//		LogicalExpression hasGoldLE = new PFAtom(this.pfAgentHasAtLeastXGoldBar.getAllGroundedPropsForState(this.initialState).get(0));
//		Subgoal hasOreSG = new Subgoal(hasOreLE, hasGoldLE);
//		subgoalKB.add(hasOreSG);
//		
//		SubgoalPlanner sgPlanner = new SubgoalPlanner(domain, initialState, rewardFunction, terminalFunction, planner, subgoalKB);
//		sgPlanner.solve();
//		
//		return null;
//	}
//	
//	private void addOptionsToOOMDPPlanner(OOMDPPlanner toAddTo, boolean addOptions, boolean addMAs) {
//		//OPTIONS
//		if (addOptions) {
//			//Trench build option
//			toAddTo.addNonDomainReferencedAction(new TrenchBuildOption(NameSpace.OPTBUILDTRENCH, this.initialState, this.domain,
//					getRewardFunction(), this.gamma, this.hashingFactory));
//			//Walk until can't option
//			toAddTo.addNonDomainReferencedAction(new WalkUntilCantOption(NameSpace.OPTWALKUNTILCANT, this.initialState, this.domain,
//					this.rewardFunction, this.gamma, this.hashingFactory));
//		}
//
//		//MACROACTIONS
//		if (addMAs) {
//			//Sprint macro-action(2)
//			toAddTo.addNonDomainReferencedAction(new SprintMacroAction(NameSpace.MACROACTIONSPRINT, this.rewardFunction, 
//					this.gamma, this.hashingFactory, this.domain, this.initialState, 2));	
//			//Turn around macro-action
//			toAddTo.addNonDomainReferencedAction(new TurnAroundMacroAction(NameSpace.MACROACTIONTURNAROUND, this.rewardFunction, 
//					this.gamma, this.hashingFactory, this.domain, this.initialState));	
//			//Look down alot macro-action(2)
//			toAddTo.addNonDomainReferencedAction(new LookDownAlotMacroAction(NameSpace.MACROACTIONLOOKDOWNALOT, this.rewardFunction, 
//					this.gamma, this.hashingFactory, this.domain, this.initialState, 2));	
//			//Trench build macro-action
//			toAddTo.addNonDomainReferencedAction(new BuildTrenchMacroAction(NameSpace.MACROACTIONBUILDTRENCH, this.rewardFunction, 
//					this.gamma, this.hashingFactory, this.domain, this.initialState));
//		}	
//	}
	
	public int countReachableStates() {
		OOMDPPlanner vi = new ValueIteration(domain, rewardFunction, terminalFunction, gamma, hashingFactory, boltzmannTemperature, maxDepth);
		
		((ValueIteration)vi).performReachabilityFrom(initialState);
		return vi.getMapToStateIndex().size();
	}
	
	public static void main(String[] args) {
//		String mapsPath = System.getProperty("user.dir") + "/maps/";
		
		String mapName = "src/minecraft/maps/test/DeepTrenchWorld0.map";
		
		MinecraftBehavior mcBeh = new MinecraftBehavior(mapName);
		double [] results;
		
		//BFS
//		BFSPlanner bfsPlanner = new BFSPlanner(mcBeh, true, true);
//		results = bfsPlanner.runPlanner();
//		System.out.println("(minecraftBehavior) results: " + results[0] + "," + results[1] + "," + results[2] + "," + results[3]);

		//RTDP
//		RTDPPlanner rtdpPlanner = new RTDPPlanner(mcBeh, false, true);
//		results = rtdpPlanner.runPlanner();
//		System.out.println("(minecraftBehavior) results: " + results[0] + "," + results[1] + "," + results[2] + "," + results[3]);
		
		//BFSRTDP
//		BFSRTDPPlanner BFSRTDPPlanner = new BFSRTDPPlanner(mcBeh, false, false);
//		results = BFSRTDPPlanner.runPlanner();
//		System.out.println("(minecraftBehavior) results: " + results[0] + "," + results[1] + "," + results[2] + "," + results[3]);
		
		//AFFRTDP
		boolean useOptions = true;
		boolean useMAs = true;
		boolean softFlag = true;
		// Load knowledge base
		KnowledgeBase affKB = new KnowledgeBase();
		affKB.load(mcBeh.getDomain(), MinecraftPlanner.getMapOfMAsAndOptions(mcBeh, useOptions, useMAs), "3_learned_opt_ma.kb", false);

		AffordanceRTDPPlanner affRTDPPlanner = new AffordanceRTDPPlanner(mcBeh, true, true, affKB);
		results = affRTDPPlanner.runPlanner();
		System.out.println("(minecraftBehavior) results [expertRTDP]: " + results[0] + "," + results[1] + "," + results[2] + "," + results[3]);

		//VI
//		VIPlanner viPlan = new VIPlanner(mcBeh, false, false);
//		results = viPlan.runPlanner();
//		System.out.println("(minecraftBehavior) results [VI]: " + results[0] + "," + results[1] + "," + results[2] + "," + results[3]);

		// Affordance VI
//		AffordanceVIPlanner affVIPlan = new AffordanceVIPlanner(mcBeh, true, true, "somekb.kb");
//		results = affVIPlan.runPlanner();
//		System.out.println("(minecraftBehavior) results: " + results[0] + "," + results[1] + "," + results[2] + "," + results[3]);

		
		// Subgoal Planner
//		OOMDPPlanner lowLevelPlanner = new RTDP(mcBeh.domain, mcBeh.rewardFunction, mcBeh.terminalFunction, mcBeh.gamma, mcBeh.hashingFactory, mcBeh.vInit, mcBeh.numRollouts, mcBeh.minDelta, mcBeh.maxDepth);
//		mcBeh.SubgoalPlanner(lowLevelPlanner);
		
		//		SubgoalKnowledgeBase subgoalKB = new SubgoalKnowledgeBase(mapName, mcBeh.domain);
//		List<Subgoal> highLevelPlan = subgoalKB.generateSubgoalKB(mapName);
//		SubgoalPlanner sgp = new SubgoalPlanner(mcBeh.domain, mcBeh.getInitialState(), mcBeh.rewardFunction, mcBeh.terminalFunction, lowLevelPlanner, highLevelPlan);
//		sgp.solve();
		
		// Collect results and write to file
//		File resultsFile = new File("tests/results/mcBeh_results.result");
//		BufferedWriter bw;
//		FileWriter fw;
//		try {
//			fw = new FileWriter(resultsFile.getAbsoluteFile());
//			bw = new BufferedWriter(fw);
//			bw.write("(minecraftBehavior) results: RTDP " + results[0] + "," + results[1] + "," + results[2] + "," + String.format("%.2f", results[3] / 1000) + "s");
//			bw.flush();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
	}
	
	
}