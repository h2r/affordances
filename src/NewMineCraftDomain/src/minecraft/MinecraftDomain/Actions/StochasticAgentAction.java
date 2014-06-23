package minecraft.MinecraftDomain.Actions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import burlap.debugtools.RandomFactory;
import burlap.oomdp.core.Domain;

public abstract class StochasticAgentAction extends AgentAction {
	
	/**
	 * Random object for sampling distribution
	 */
	protected Random rand;
	
	/**
	 * hashmap from actions to probability of that action -- used to account for indeterminism
	 */
	protected HashMap<StochasticAgentAction, Double> actionToProb;
	
	public StochasticAgentAction(String name, Domain domain, int rows, int cols, int height, boolean causesAgentToFall){
		super(name, domain, rows, cols, height, causesAgentToFall);
		this.rand = RandomFactory.getMapped(0);
		
		this.actionToProb = new HashMap<StochasticAgentAction, Double>();
		this.actionToProb.put(this, 1.0);	
	}
	
	
	
	
	/**
	 * 
	 * @param possibleAction an action that might as a result of indeterminism in the space
	 * @param weight the relative weight (representing a likelihood) of that action
	 */
	private void addPossibleResultingAction(StochasticAgentAction possibleAction, Double weight) {
		this.actionToProb.put(possibleAction, weight);
	}
	
	/**
	 * turns all the weights in this.actionToProb into a probability distribution
	 */
	private void normalizeWeights () {
		StochasticAgentAction[] keys = (StochasticAgentAction[]) this.actionToProb.keySet().toArray();
		double totalProb = 0.0;
		//Get total weight
		for (int i = 0; i < keys.length; i++) {
			totalProb += this.actionToProb.get(keys[i]);
		}
		//normalize weights
		for (int i = 0; i < keys.length; i++) {
			double oldValue = this.actionToProb.get(keys[i]);
			this.actionToProb.put(keys[i], oldValue/totalProb);
		}
	}
	
	/**
	 * Used to cause an action to probabilistically cause other actions
	 * @param actions an array of all the actions that might occur from this action as a result of indeterminism
	 * @param weights an array of doubles of the respective likelihoods of the actions in actions
	 */
	public void addResultingActionsWithWeights(StochasticAgentAction [] actions, double [] weights) {
		assert(actions.length == weights.length);
		for (int i = 0; i < actions.length; i++) {
			addPossibleResultingAction(actions[i], weights[i]);
		}
		normalizeWeights();
	}
	
	
	/**
	 * 
	 * @return Get an randomly action as determined by input weights
	 */
	@Override
	protected StochasticAgentAction getAction() {
		ArrayList<StochasticAgentAction> keys = new ArrayList<StochasticAgentAction>();
		for(StochasticAgentAction key: this.actionToProb.keySet()) {
			keys.add(key);
		}
		
		//Sample actions until one is deemed probabilistic enough
		StochasticAgentAction currActionCandidate = keys.get(rand.nextInt(keys.toArray().length));
		double randProb = rand.nextDouble() ;
		while (actionToProb.get(currActionCandidate) < randProb) {
			currActionCandidate = keys.get(rand.nextInt(keys.toArray().length));
			randProb = rand.nextDouble();
		}
		
		//currActionCandidate is now the action to perform
		return currActionCandidate;

	}
	

}