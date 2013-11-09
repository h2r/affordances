package burlap.domain.singleagent.minecraft;

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
		rf = new SingleGoalPFRF(domain.getPropFunction(MinecraftDomain.PFATGOAL), 10000, -1000); 
		tf = new SinglePFTF(domain.getPropFunction(MinecraftDomain.PFATGOAL)); 
		goalCondition = new TFGoalCondition(tf);
		
		
		//set up the initial state of the task
		initialState = MinecraftDomain.getState(domain);
		MinecraftDomain.setAgent(initialState, 1, 1, 2, 1);
		MinecraftDomain.setGoal(initialState, 6, 6, 2);
		
		//set up the state hashing system
		hashingFactory = new DiscreteStateHashFactory();
		hashingFactory.setAttributesForClass(MinecraftDomain.CLASSAGENT, 
					domain.getObjectClass(MinecraftDomain.CLASSAGENT).attributeList); 
		
	}
	
	public void ValueIterationMC(String outputPath){
		
		if(!outputPath.endsWith("/")){
			outputPath = outputPath + "/";
		}
		
		OOMDPPlanner planner = new ValueIteration(domain, rf, tf, 0.99, hashingFactory, 0.000000001, Integer.MAX_VALUE);
		
		planner.planFromState(initialState);
		
		//create a Q-greedy policy from the planner
		Policy p = new GreedyQPolicy((QComputablePlanner)planner);
		
		//record the plan results to a file
		p.evaluateBehavior(initialState, rf, tf).writeToFile(outputPath + "planResult", sp);
		
	}
	
	public void visualize(String outputPath){
		Visualizer v = MinecraftVisualizer.getVisualizer(domain, mcdg.getMapForVisualize(2));
		EpisodeSequenceVisualizer evis = new EpisodeSequenceVisualizer(v, domain, sp, outputPath);
	}
	
	public static void main(String[] args) {
	
		MinecraftBehavior mcb = new MinecraftBehavior();
		String outputPath = "output/"; //directory to record results
		
		// We will call planning and learning algorithms here
		mcb.ValueIterationMC(outputPath);
		
		// Run the visualizer
		mcb.visualize(outputPath);
	}
	
	
}