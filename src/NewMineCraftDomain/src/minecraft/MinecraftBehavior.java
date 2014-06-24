package minecraft;

import burlap.behavior.singleagent.*;
import burlap.behavior.singleagent.planning.OOMDPPlanner;
import burlap.behavior.singleagent.planning.QComputablePlanner;
import burlap.behavior.singleagent.planning.commonpolicies.GreedyQPolicy;
import burlap.behavior.singleagent.planning.deterministic.DeterministicPlanner;
import burlap.behavior.singleagent.planning.deterministic.SDPlannerPolicy;
import burlap.behavior.singleagent.planning.deterministic.TFGoalCondition;
import burlap.behavior.singleagent.planning.deterministic.uninformed.bfs.BFS;
import burlap.oomdp.auxiliary.StateParser;
import burlap.oomdp.core.*;
import burlap.oomdp.singleagent.*;
import burlap.oomdp.singleagent.common.*;
import burlap.behavior.statehashing.DiscreteStateHashFactory;

import java.util.HashMap;

import minecraft.MinecraftDomain.MinecraftDomainGenerator;

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
	//Params for Planners
	private double						gamma = 0.99;
	private double						minDelta = 0.01;
	private int							maxSteps = 200;

	
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
		this.initialState = MinecraftInitialStateGenerator.createInitialState(mapAs3DArray, headerInfo, domain);
		
		//Get propositional functions
		this.pfAgentAtGoal = domain.getPropFunction(NameSpace.PFATGOAL);
		this.pfEmptySpace = domain.getPropFunction(NameSpace.PFEMPSPACE);
		this.pfBlockAt = domain.getPropFunction(NameSpace.PFBLOCKAT);
		
		PropositionalFunction propFunToUse = this.pfAgentAtGoal;
		
		//Set up reward function
		this.rewardFunction = new SingleGoalPFRF(propFunToUse, 10, -1); 
		
		//Set up terminal function
		this.terminalFunction = new SinglePFTF(propFunToUse);
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
		Policy p = new GreedyQPolicy((QComputablePlanner)planner);
		
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
	
	
	public static void main(String[] args) {
		String mapPath = "src/minecraft/maps/jumpworld.map";
		String outputPath = "src/minecraft/planningOutput/";
		MinecraftBehavior mcBeh = new MinecraftBehavior(mapPath);
		mcBeh.BFSExample(outputPath);
		
		
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
	
}
