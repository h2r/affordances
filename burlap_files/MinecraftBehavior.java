package burlap.domain.singleagent.minecraft;

import java.util.ArrayList;
import java.util.List;

import burlap.oomdp.singleagent.SADomain;
import burlap.oomdp.core.Domain;

import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.singleagent.common.SingleGoalPFRF;
import burlap.oomdp.singleagent.common.SinglePFTF;
import burlap.oomdp.singleagent.common.UniformCostRF;
import burlap.oomdp.visualizer.Visualizer;
import burlap.oomdp.core.State;
import burlap.behavior.singleagent.EpisodeSequenceVisualizer;
import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.planning.OOMDPPlanner;
import burlap.behavior.singleagent.planning.QComputablePlanner;
import burlap.behavior.singleagent.planning.StateConditionTest;
import burlap.behavior.singleagent.planning.ValueFunctionPlanner;
import burlap.behavior.singleagent.planning.stochastic.valueiteration.*;
import burlap.oomdp.auxiliary.StateParser;
import burlap.oomdp.core.TerminalFunction;
import burlap.behavior.singleagent.planning.commonpolicies.GreedyQPolicy;
import burlap.behavior.singleagent.planning.deterministic.TFGoalCondition;
import burlap.behavior.statehashing.DiscreteStateHashFactory;



public class MinecraftBehavior {

	MinecraftDomain				mcdg;
	Domain						domain;
	StateParser					sp;
	RewardFunction				rf;
	TerminalFunction			tf;
	StateConditionTest			goalCondition;
	State						initialState;
	DiscreteStateHashFactory	hashingFactory;
	
	
	public MinecraftBehavior() {
		mcdg = new MinecraftDomain();
		domain = mcdg.generateDomain();
		
		sp = new MinecraftStateParser(domain); 	
		
		//define the task
		rf = new SingleGoalPFRF(domain.getPropFunction(MinecraftDomain.PFATGOAL), 10, -1); 
		tf = new SinglePFTF(domain.getPropFunction(MinecraftDomain.PFATGOAL)); 
		goalCondition = new TFGoalCondition(tf);
		
		// === Build Initial State=== //
		
		// -- Blocks --
		int MAXX = 10;
		int MAXY = 10;
		List <Integer> blockX = new ArrayList<Integer>();
		List <Integer> blockY = new ArrayList<Integer>();
		
		// Row i will have blocks in all 10 locations
		for (int i = 0; i < MAXX; i++){
//			 Place a width 2 trench @ x = 4 and x = 5
//			if (i == 5 || i == 6 || i == 4)
//			{
//				continue;
//			}
			blockX.add(i);
			blockY.add(MAXY);
		}
		
		initialState = MinecraftDomain.getCleanState(domain, blockX, blockY);

//		MinecraftDomain.addBlock(initialState, 4, 4, 1); // Adds a bridge over the trench
//		MinecraftDomain.addBlock(initialState, 5, 4, 1); // Adds a bridge over the trench
//		
		
		// -- Agent & Goal --
		MinecraftDomain.setAgent(initialState, 1, 1, 2, 10);
		MinecraftDomain.setGoal(initialState, 8, 8, 2);
		
		//set up the state hashing system
		hashingFactory = new DiscreteStateHashFactory();
		hashingFactory.setAttributesForClass(MinecraftDomain.CLASSAGENT, 
					domain.getObjectClass(MinecraftDomain.CLASSAGENT).attributeList); 
		
	}
	
	
	// Older working version if basic bad VI
	public void ValueIterationMC(String outputPath){
		
		if(!outputPath.endsWith("/")){
			outputPath = outputPath + "/";
		}
		
		OOMDPPlanner planner = new ValueIteration(domain, rf, tf, 0.99, hashingFactory, 1, Integer.MAX_VALUE);
		
		planner.planFromState(initialState);
//		System.out.println(((ValueFunctionPlanner) planner).value(initialState));
		//create a Q-greedy policy from the planner
		Policy p = new GreedyQPolicy((QComputablePlanner)planner);
		
		int maxIterations = 150;
		
		//record the plan results to a file
		p.evaluateBehaviorThreshold(initialState, rf, tf, maxIterations).writeToFile(outputPath + "planResult", sp);
		
	}
	
	
	public static void main(String[] args) {
	
		MinecraftBehavior mcb = new MinecraftBehavior();
		String outputPath = "output/"; //directory to record results
		
		// We will call planning and learning algorithms here
		mcb.ValueIterationMC(outputPath);

	}
	
	
}