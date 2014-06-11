package minecraft.MinecraftDomain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import minecraft.NameSpace;
import burlap.debugtools.RandomFactory;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.Action;

public class MinecraftActions {
	//------------MOVEMENT ACTION------------	
	/**
	 * Action to move the agent in space
	 */
	public static class MovementAction extends Action{
			
		/**
		 * Random object for sampling distribution
		 */
		protected Random rand;
		
		/**
		 * hashmap from actions to probability of that action -- used to account for indeterminism
		 */
		HashMap<MovementAction, Double> actionToProb;
		
		//Location changes that result from the action
		private int xChange;
		private int yChange;
		private int zChange;
		
		//Stores dimensions of map
		private int rows;
		private int cols;
		private int height;
		
		
		public MovementAction(String name, Domain domain, int xChange, int yChange, int zChange, int rows, int cols, int height){
			super(name, domain, "");
			this.rand = RandomFactory.getMapped(0);
			
			this.xChange = xChange;
			this.yChange = yChange;
			this.zChange = zChange;
			
			this.rows = rows;
			this.cols = cols;
			this.height = height;
			
			this.actionToProb = new HashMap<MovementAction, Double>();
			this.actionToProb.put(this, 1.0);	
		}
		
		/**
		 * Determines if block in agents way at x,y,z locations (including z-1 since agent has feet!)
		 * @param x x location to check
		 * @param y y location to check
		 * @param z z location to check
		 * @param state the burlap state to check for collisions in
		 * @returns Boolean of whether there are blocks in the agents way at input location
		 */
		private Boolean emptySpaceForAgentAt(int x, int y, int z, State state) {
			List<ObjectInstance> allObjects = state.getAllObjects();
			//Loop over all objects that collide with the agent and perform collision detection
			for (ObjectInstance object: allObjects) {
				if (object.getObjectClass().hasAttribute(NameSpace.ATTCOLLIDESWITHAGENT) && object.getDiscValForAttribute(NameSpace.ATTCOLLIDESWITHAGENT) == 1) {
					if (object.getDiscValForAttribute(NameSpace.ATTX) == x && 
							object.getDiscValForAttribute(NameSpace.ATTY) == y &&
							(object.getDiscValForAttribute(NameSpace.ATTZ) == z ||
							object.getDiscValForAttribute(NameSpace.ATTZ) == z-1)) {
						return false;
					}
				}
			}
			return true;
		}
		
		/**
		 * Determines if a location is in bounds
		 * @param x x location to check
		 * @param y y location to check
		 * @param z z location to check
		 * @returns Boolean of whether input is in-bounds
		 */
		private Boolean agentWithinMapAt(int x, int y, int z) {
			int zFeet = z -1;
			return (x >= 0 && x < this.cols && y >=0 && y < this.rows && z >= 0 && z < this.height&& zFeet >= 0 && zFeet < this.height);
		}
		
		/**
		 * Called by the action to *surprise* move the agent
		 * @param state state in which the action is moving the agent
		 */
		private void moveAgent(State state){
			ObjectInstance agent = state.getObjectsOfTrueClass(NameSpace.CLASSAGENT).get(0);
			int oldX = agent.getDiscValForAttribute(NameSpace.ATTX);
			int oldY = agent.getDiscValForAttribute(NameSpace.ATTY);
			int oldZ = agent.getDiscValForAttribute(NameSpace.ATTZ);
			
			int newX = oldX+xChange;
			int newY = oldY+yChange;
			int newZ = oldZ+zChange;
			
			//Update position if nothing in agent's way and new position is within map
			if (agentWithinMapAt(newX, newY, newZ) && emptySpaceForAgentAt(newX, newY, newZ, state)) {
				agent.setValue(NameSpace.ATTX, newX);
				agent.setValue(NameSpace.ATTY, newY);
				agent.setValue(NameSpace.ATTZ, newZ);
			}
		}
		
		/**
		 * 
		 * @param possibleAction an action that might as a result of indeterminism in the space
		 * @param weight the relative weight (representing a likelihood) of that action
		 */
		private void addPossibleResultingAction(MovementAction possibleAction, Double weight) {
			this.actionToProb.put(possibleAction, weight);
		}
		
		/**
		 * turns all the weights in this.actionToProb into a probability distribution
		 */
		private void normalizeWeights () {
			MovementAction[] keys = (MovementAction[]) this.actionToProb.keySet().toArray();
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
		public void addResultingActionsWithWeights(MovementAction [] actions, double [] weights) {
			assert(actions.length == weights.length);
			for (int i = 0; i < actions.length; i++) {
				addPossibleResultingAction(actions[i], weights[i]);
			}
			normalizeWeights();
		}
		
		
		@Override
		protected State performActionHelper(State state, String[] params) {
			ArrayList<MovementAction> keys = new ArrayList<MovementAction>();
			for(MovementAction key: this.actionToProb.keySet()) {
				keys.add(key);
			}
			
			//Sample actions until one is deemed probabilistic enough
			MovementAction currActionCandidate = keys.get(rand.nextInt(keys.toArray().length));
			double randProb = rand.nextDouble() ;
			while (actionToProb.get(currActionCandidate) < randProb) {
				currActionCandidate = keys.get(rand.nextInt(keys.toArray().length));
				randProb = rand.nextDouble();
			}
			
			//currActionCandidate is now the action to perform
			currActionCandidate.moveAgent(state);
			return state;
		}
	}
	
}
