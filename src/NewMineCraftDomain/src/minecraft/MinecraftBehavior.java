package minecraft;

import java.util.HashMap;
import java.util.List;

import minecraft.MinecraftDomain.MinecraftDomainGenerator;
import affordances.KnowledgeBase;
import burlap.behavior.affordances.AffordancesController;
import burlap.behavior.singleagent.*;
import burlap.behavior.singleagent.planning.OOMDPPlanner;
import burlap.behavior.singleagent.planning.QComputablePlanner;
import burlap.behavior.singleagent.planning.ValueFunctionPlanner;
import burlap.behavior.singleagent.planning.commonpolicies.AffordanceGreedyQPolicy;
import burlap.behavior.singleagent.planning.commonpolicies.GreedyDeterministicQPolicy;
import burlap.behavior.singleagent.planning.commonpolicies.GreedyQPolicy;
import burlap.behavior.singleagent.planning.deterministic.DeterministicPlanner;
import burlap.behavior.singleagent.planning.deterministic.SDPlannerPolicy;
import burlap.behavior.singleagent.planning.deterministic.TFGoalCondition;
import burlap.behavior.singleagent.planning.deterministic.uninformed.bfs.BFS;
import burlap.behavior.singleagent.planning.stochastic.rtdp.AffordanceRTDP;
import burlap.behavior.singleagent.planning.stochastic.rtdp.RTDP;
import burlap.behavior.singleagent.planning.stochastic.valueiteration.ValueIteration;
import burlap.oomdp.auxiliary.StateParser;
import burlap.oomdp.core.*;
import burlap.oomdp.logicalexpressions.LogicalExpression;
import burlap.oomdp.singleagent.*;
import burlap.oomdp.singleagent.common.SingleGoalPFRF;
import burlap.oomdp.singleagent.common.SinglePFTF;
import burlap.behavior.statehashing.DiscreteStateHashFactory;
import minecraft.MinecraftStateGenerator.MinecraftStateGenerator;
import minecraft.MinecraftStateGenerator.Exceptions.StateCreationException;

/**
 * The main behavior class for the minecraft domain
 * @author Dhershkowitz
 *
 */
public class MinecraftBehavior {
    // ----- CLASS variable -----
	private MinecraftDomainGenerator	MCDomainGenerator;
	private Domain						domain;
	private StateParser					MCStateParser;
	private RewardFunction				rewardFunction;
	private TerminalFunction			terminalFunction;
	private State						initialState;
	private DiscreteStateHashFactory	hashingFactory;
	
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
	
	
	//Params for Planners
	private double						gamma = 0.99;
	private double						minDelta = .01;
	private int							maxSteps = 200;
	private int 						numRollouts = 20000; // RTDP
	private int							maxDepth = 50; // RTDP
	private int 						vInit = -1; // RTDP

	
	// ----- CLASS METHODS -----
	/**
	 * Constructor to instantiate behavior
	 * @param filePath map filepath on which to perform the planning
	 */
	public MinecraftBehavior(String filePath) {
		this.updateMap(filePath);	
	}
	
	/**
	 * 
	 * @param filePathOfMap a filepath to the location of the ascii map to update the behavior to
	 */
	public void updateMap(String filePathOfMap) {
		//Perform IO on map�
		MapIO mapIO = new MapIO(filePathOfMap);
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
		
		//Set initial state
		try {
			this.initialState = MinecraftStateGenerator.createInitialState(mapAs3DArray, headerInfo, domain);
		} catch (StateCreationException e) {
			e.printStackTrace();
		}
		
		//Get propositional functions
		this.pfAgentAtGoal = domain.getPropFunction(NameSpace.PFATGOAL);
		this.pfEmptySpace = domain.getPropFunction(NameSpace.PFEMPSPACE);
		this.pfBlockAt = domain.getPropFunction(NameSpace.PFBLOCKAT);
		this.pfAgentHasAtLeastXGoldOre = domain.getPropFunction(NameSpace.PFATLEASTXGOLDORE);
		this.pfAgentHasAtLeastXGoldBar = domain.getPropFunction(NameSpace.PFATLEASTXGOLDBAR);
		this.pfBlockInFrontOfAgent = domain.getPropFunction(NameSpace.PFBLOCKINFRONT);
		this.pfEndOfMapInFrontOfAgent = domain.getPropFunction(NameSpace.PFENDOFMAPINFRONT);
		this.pfTrenchInFrontOfAgent = domain.getPropFunction(NameSpace.PFTRENCHINFRONT);
		this.pfAgentInMidAir = domain.getPropFunction(NameSpace.PFAGENTINMIDAIR);
		


		LogicalExpression relevantGoalExpression;
		List<GroundedProp> groundedGoals;
		
		if(filePathOfMap.contains("Gold")) {
			// If we're in a gold map, reset tf/rf for agent having gold
			groundedGoals = this.pfAgentHasAtLeastXGoldOre.getAllGroundedPropsForState(this.initialState);			
		} else{
			// Otherwise, in an AtGoal world, updated regularly.
			groundedGoals = this.pfAgentAtGoal.getAllGroundedPropsForState(this.initialState);
		}

		
		PropositionalFunction pfToUse = getPFFromHeader(headerInfo);
		
		//Set up reward function
		this.rewardFunction = new SingleGoalPFRF(pfToUse, 0, -1); 
		
		//Set up terminal function
		this.terminalFunction = new SinglePFTF(pfToUse);
//		
//		List<GroundedProp> groundedGoals = this.pfAgentAtGoal.getAllGroundedPropsForState(this.initialState);
//
//		
//		// Not parameterized goals, so take first (and only) grounding.
//		for(GroundedProp gp : groundedGoals) {
//			System.out.println("grounding: " + gp.pf.toString());
//		}
//		
//		GroundedProp groundedGoal = groundedGoals.get(0);
//		relevantGoalExpression = new PFAtom(groundedGoal);
//		
//	
//		// Set up reward function with new goal
////		this.rewardFunction = new SingleGoalLERF(relevantGoalExpression, 10, -1); 
//		
//		//Set up terminal function with new goal
////		this.terminalFunction = new SingleLETF(relevantGoalExpression);
//		
//		this.rewardFunction = new SingleGoalPFRF(this.pfAgentHasAtLeastXGoldOre);
//		this.terminalFunction = new SinglePFTF(this.pfAgentHasAtLeastXGoldOre);
	}
	
	private PropositionalFunction getPFFromHeader(HashMap<String, Integer> headerInfo) {
		switch(headerInfo.get(Character.toString(NameSpace.CHARGOALDESCRIPTOR))) {
		case NameSpace.INTXYZGOAL:
			return this.pfAgentAtGoal;
		
		case NameSpace.INTGOLDBARGOAL:
			return this.pfAgentHasAtLeastXGoldBar;
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
		GreedyDeterministicQPolicy p = new GreedyDeterministicQPolicy((QComputablePlanner)planner);
		
		// Print out some infos
		EpisodeAnalysis ea = p.evaluateBehavior(initialState, this.rewardFunction, this.terminalFunction, maxSteps);
		
		System.out.println(ea.getActionSequenceString());

		return p;
	}
	
	public void BFSExample(String outputPath) {
		TFGoalCondition goalCondition = new TFGoalCondition(this.terminalFunction);
		
		DeterministicPlanner planner = new BFS(this.domain, goalCondition, this.hashingFactory);
		planner.planFromState(initialState);
		
		Policy p = new SDPlannerPolicy(planner);
		
		p.evaluateBehavior(initialState, this.rewardFunction, this.terminalFunction).writeToFile(outputPath + "bfsPlanResult", MCStateParser);
		
		EpisodeAnalysis ea = p.evaluateBehavior(initialState, this.rewardFunction, this.terminalFunction);
		ea.writeToFile(outputPath, MCStateParser);
	}
	
	private double sumReward(List<Double> rewardSeq) {
		double total = 0;
		for (double d : rewardSeq) {
			total += d;
		}
		return total;
	}
	
	public void ValueIterationPlanner(){
		TFGoalCondition goalCondition = new TFGoalCondition(this.terminalFunction);
		OOMDPPlanner planner = new ValueIteration(domain, rewardFunction, terminalFunction, gamma, hashingFactory, 0.01, Integer.MAX_VALUE);
		
		planner.planFromState(initialState);
		
		// Create a Q-greedy policy from the planner
		Policy p = new GreedyQPolicy((QComputablePlanner)planner);
		
		// Record the plan results to a file
		EpisodeAnalysis ea = p.evaluateBehavior(initialState, rewardFunction, terminalFunction, maxSteps);
		
		System.out.println(ea.getActionSequenceString());
		double totalReward = sumReward(ea.rewardSequence);
		
		State finalState = ea.stateSequence.get(ea.stateSequence.size()-1);
		
		double completed = goalCondition.satisfies(finalState) ? 1.0 : 0.0;
		if (completed == 1.0) {
			System.out.println("VI completed with " + totalReward + " reward!");
		}
		else {
			System.out.println("VI failed!");
		}
	}
	
	public void AffordanceRTDP(KnowledgeBase affKB){
		AffordancesController affController = affKB.getAffordancesController();

		ValueFunctionPlanner planner = new AffordanceRTDP(domain, this.rewardFunction, this.terminalFunction, this.gamma, this.hashingFactory, this.vInit, this.numRollouts, this.minDelta, this.maxDepth, affController);
		
		planner.planFromState(initialState);
		
		// Create a Q-greedy policy from the planner
		Policy p = new AffordanceGreedyQPolicy(affController, (QComputablePlanner)planner);
		EpisodeAnalysis ea = p.evaluateBehavior(initialState, rewardFunction, terminalFunction, maxSteps);
		System.out.println(ea.getActionSequenceString());

	}
	
	public void RTDP() {

		ValueFunctionPlanner planner = new RTDP(domain, this.rewardFunction, this.terminalFunction, this.gamma, this.hashingFactory, this.vInit, this.numRollouts, this.minDelta, this.maxDepth);
		
		planner.planFromState(initialState);
		
		// Create a Q-greedy policy from the planner
		Policy p = new GreedyQPolicy((QComputablePlanner)planner);
		EpisodeAnalysis ea = p.evaluateBehavior(initialState, rewardFunction, terminalFunction, maxSteps);
		System.out.println(ea.getActionSequenceString());

	}
	
	public static void main(String[] args) {

//		String mapName = "0.map";
//		String mapPath = "src/minecraft/maps/learning/AgentHasXGoldOre/" + mapName;
		
		String mapName = "testingGoldOre.map";
		String mapPath = "src/minecraft/maps/" + mapName;

		String outputPath = "src/minecraft/planningOutput/";
		MinecraftBehavior mcBeh = new MinecraftBehavior(mapPath);
		
		// BFS
//		mcBeh.BFSExample(outputPath);
		
		// VI
		mcBeh.ValueIterationPlanner();
		
//		// Affordance RTDP
//		KnowledgeBase affKB = new KnowledgeBase();
//		affKB.load(mcBeh.getDomain(), "trenches50.kb");
//		mcBeh.AffordanceRTDP(affKB);
		
		// Subgoal Planner
//		OOMDPPlanner lowLevelPlanner = new RTDP(mcBeh.domain, mcBeh.rewardFunction, mcBeh.terminalFunction, mcBeh.gamma, mcBeh.hashingFactory, mcBeh.vInit, mcBeh.numRollouts, mcBeh.minDelta, mcBeh.maxDepth);
//		SubgoalKnowledgeBase subgoalKB = new SubgoalKnowledgeBase(mapName, mcBeh.domain);
//		List<Subgoal> highLevelPlan = subgoalKB.generateSubgoalKB(mapName);
//		SubgoalPlanner sgp = new SubgoalPlanner(mcBeh.domain, mcBeh.getInitialState(), mcBeh.rewardFunction, mcBeh.terminalFunction, lowLevelPlanner, highLevelPlan);
//		sgp.solve();
		
		// RTDP
//		mcBeh.RTDP();
	}
	
	
}
