package minecraft;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import burlap.behavior.singleagent.*;
import burlap.behavior.singleagent.planning.StateConditionTest;
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
import java.util.Scanner;

public class MinecraftBehavior {
	MinecraftDomain				MCDomainGenerator;
	Domain						domain;
	StateParser					MCStateParser;
	RewardFunction				rewardFunction;
	TerminalFunction			terminalFunction;
	StateConditionTest			goalCondition;
	State						initialState;
	DiscreteStateHashFactory	hashingFactory;
	

	
	// ------- Propositional Functions -------
	public PropositionalFunction		pfAgentAtGoal;
	
	
	// ------- Params for Planners ------- 
	static int 					numRollouts = 5000; // RTDP
	static int 					maxDepth = 50; // RTDP
	static double				vInit = 0;
	static double				goalReward = -1.0;
	static int					maxSteps = 100; // cutoff for VI
	static double				maxDelta = 0.01;
	static double				gamma = 0.99;

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
		//Perform IO on map§
		MapIO mapIO = new MapIO(filePathOfMap);
		char[][][] mapAs3DArray = mapIO.getMapAs3DCharArray();
		HashMap<String, Integer> headerInfo = mapIO.getHeaderHashMap();
		
		//Update domain
		this.MCDomainGenerator = new MinecraftDomain(mapAs3DArray, headerInfo);
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
		
		
		//Set up reward function
		this.rewardFunction = new SingleGoalPFRF(pfAgentAtGoal, 10, -1); 
		
		//Set up terminal function
		this.terminalFunction = new SinglePFTF(pfAgentAtGoal);
		

		
	}
	
	
	public void BFSExample(String outputPath) {
		TFGoalCondition goalCondition = new TFGoalCondition(this.terminalFunction);
		
		DeterministicPlanner planner = new BFS(this.domain, goalCondition, this.hashingFactory);
		planner.planFromState(initialState);
		
		Policy p = new SDPlannerPolicy(planner);
		
		p.evaluateBehavior(initialState, rewardFunction, terminalFunction).writeToFile(outputPath + "bfsPlanResult", MCStateParser);
		
		EpisodeAnalysis ea = p.evaluateBehavior(initialState, rewardFunction, terminalFunction);
		ea.writeToFile(outputPath, MCStateParser);
		
	}
	
	
	public static void main(String[] args) {
		String mapPath = "src/minecraft/maps/jumpworld.map";
		String outputPath = "src/minecraft/planningOutput/";
		MinecraftBehavior mcBeh = new MinecraftBehavior(mapPath);
		mcBeh.BFSExample(outputPath);
		
		
	}
	
}
