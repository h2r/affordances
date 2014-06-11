package minecraft;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import burlap.debugtools.RandomFactory;
import burlap.domain.singleagent.gridworld.GridWorldDomain;
import burlap.domain.singleagent.gridworld.GridWorldDomain.AtLocationPF;
import burlap.domain.singleagent.gridworld.GridWorldDomain.EmptyCellToPF;
import burlap.domain.singleagent.gridworld.GridWorldDomain.MovementAction;
import burlap.domain.singleagent.gridworld.GridWorldDomain.WallToPF;
import burlap.oomdp.auxiliary.DomainGenerator;
import burlap.oomdp.core.Attribute;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectClass;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.PropositionalFunction;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TransitionProbability;
import burlap.oomdp.singleagent.Action;
import burlap.oomdp.singleagent.SADomain;

public class MinecraftDomain implements DomainGenerator{
	//String and char aliases
	

	//------------Generic class variables
	/**
	 * The rows of the minecraft world
	 */
	protected int										rows;
	
	/**
	 * The cols of minecraft world
	 */
	protected int										cols;
	
	/**
	 * The height of the minecraft world
	 */
	protected int										height;
	
	/**
	 * The map of the world in row-col-height major order.
	 */
	private char [][][]									map;
	
	/**
	 * Mapping of string keys to int values in header
	 */
	private HashMap<String, Integer>		headerInfo;

	public MinecraftDomain(int rows, int cols, int height){
		this.rows = rows;
		this.cols = cols;
		this.height = height;
		
		//Set up map since none input
		this.makeEmptyMap();
		this.makeDirtFloor();
	}
	
	/**
	 * Constructs a deterministic world based on the provided map and a string of the state info.
	 * @param map the first index is the x index, the second the y; 1 entries indicate a wall
	 */
	public MinecraftDomain(char [][][] mapAsCharArray, HashMap<String, Integer> headerInfo){
		this.setMap(mapAsCharArray);
		this.setHeaderHashMap(headerInfo);
	}
	
	
	private void setHeaderHashMap(HashMap<String, Integer> headerInfo) {
		this.headerInfo = headerInfo;
	}

	private void setMap(char [][][] map) {
		this.rows = map.length;
		this.cols = map[0].length;
		this.height = map[0][0].length;
		this.map = map.clone();
	}
	
	/**
	 * Makes the map empty
	 */
	public void makeEmptyMap(){
		this.map = new char[this.rows][this.cols][this.height];
		for(int i = 0; i < this.rows; i++){
			for(int j = 0; j < this.cols; j++){
				for(int k = 0; k < this.height; k++) {
					this.map[i][j][k] = NameSpace.BLOCKEMPTY;
				}
			}
		}
	}
	
	/**
	 * Makes a floor of dirt blocks
	 */
	public void makeDirtFloor(){
		for(int i = 0; i < this.rows; i++){
			for(int j = 0; j < this.cols; j++){
					this.map[i][j][0] = '.';
			}
		}
	}
	
	/**
	 * Determines if block in agents way at x,y,z locations
	 * @param x x location to check
	 * @param y y location to check
	 * @param z z location to check
	 * @returns Boolean of whether there are blocks in the agents way at input location
	 * 	 */
	
	private Boolean emptySpaceForAgentAt(int x, int y, int z, State s) {
		List<ObjectInstance> allObjects = s.getAllObjects();
		for (ObjectInstance object: allObjects) {
			if (object.getObjectClass().hasAttribute(NameSpace.ATTCOLLIDESWITHAGENT)) {
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
	

	
	// ---- PROPOSITIONAL FUNCTIONS ----
	public static class AtGoalPF extends PropositionalFunction{

		public AtGoalPF(String name, Domain domain, String[] parameterClasses) {
			super(name, domain, parameterClasses);
		}

		@Override
		public boolean isTrue(State state, String[] params) {
			ObjectInstance agent = state.getObjectsOfTrueClass(NameSpace.CLASSAGENT).get(0);
			//get the agent coordinates
			int agentX = agent.getDiscValForAttribute(NameSpace.ATTX);
			int agentY = agent.getDiscValForAttribute(NameSpace.ATTY);
			int agentZ = agent.getDiscValForAttribute(NameSpace.ATTZ);
			
			ObjectInstance goal = state.getObjectsOfTrueClass(NameSpace.CLASSGOAL).get(0);
			
			//get the goal coordinates
			int goalX = goal.getDiscValForAttribute(NameSpace.ATTX);
			int goalY = goal.getDiscValForAttribute(NameSpace.ATTY);
			int goalZ = goal.getDiscValForAttribute(NameSpace.ATTZ);
			
			if(agentX == goalX && agentY == goalY && agentZ == goalZ){
				return true;
			}
			
			return false;
		}
		
	}
	
	
	// ---- ACTIONS ----
	
	/**
	 * Action to move the agent
	 *
	 */
	public class MovementAction extends Action{
	
		/**
		 * Probabilities of the actual direction the agent will go
		 */
		protected double [] directionProbs;
		
		/**
		 * Random object for sampling distribution
		 */
		protected Random rand;
		
		/**
		 * hashmap from actions to probability of that action
		 */
		HashMap<MovementAction, Double> actionToProb;
		
		private int xChange;
		private int yChange;
		private int zChange;
		
		/**
		 * Initializes for the given name, domain and actually direction probabilities the agent will go
		 * @param name name of the action
		 * @param domain the domain of the action
		 */
		public MovementAction(String name, Domain domain, int xChange, int yChange, int zChange){
			super(name, domain, "");
			this.rand = RandomFactory.getMapped(0);
			
			this.xChange = xChange;
			this.yChange = yChange;
			this.zChange = zChange;
			
			this.actionToProb = new HashMap<MovementAction, Double>();
			this.actionToProb.put(this, 1.0);	
		}
		
		/**
		 * Attempts to move the agent into the given position, taking into account walls and blocks
		 * @param the current state
		 * @param the delta of X position of the agent
		 * @param the delta of Y position of the agent
		 * @param the delta of Z position of the agent
		 */
		protected void moveAgent(State s){
			
			ObjectInstance agent = s.getObjectsOfTrueClass(NameSpace.CLASSAGENT).get(0);
			int oldX = agent.getDiscValForAttribute(NameSpace.ATTX);
			int oldY = agent.getDiscValForAttribute(NameSpace.ATTY);
			int oldZ = agent.getDiscValForAttribute(NameSpace.ATTZ);
			
			int newX = oldX+xChange;
			int newY = oldY+yChange;
			int newZ = oldZ+zChange;
			
				
			if (agentWithinMapAt(newX, newY, newZ) && emptySpaceForAgentAt(newX, newY, newZ, s)) {
				agent.setValue(NameSpace.ATTX, newX);
				agent.setValue(NameSpace.ATTY, newY);
				agent.setValue(NameSpace.ATTZ, newZ);
			}
		}
		
		private void addPossibleResultingAction(MovementAction possibleAction, Double weight) {
			this.actionToProb.put(possibleAction, weight);
		}
		
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
	
	
	

	@Override
	public Domain generateDomain() {
		
		Domain domain = new SADomain();
		
		//x Position Attribute
		Attribute xatt = new Attribute(domain, NameSpace.ATTX, Attribute.AttributeType.DISC);
		xatt.setDiscValuesForRange(0, this.cols-1, 1); //-1 due to inclusivity vs exclusivity
		
		//y Position Attribute
		Attribute yatt = new Attribute(domain, NameSpace.ATTY, Attribute.AttributeType.DISC);
		yatt.setDiscValuesForRange(0, this.rows-1, 1); //-1 due to inclusivity vs exclusivity
		
		//z Position Attribute
		Attribute zatt = new Attribute(domain, NameSpace.ATTZ, Attribute.AttributeType.DISC);
		zatt.setDiscValuesForRange(0, this.height-1, 1); //-1 due to inclusivity vs exclusivity
		
		//collidable attribute
		Attribute collAt = new Attribute(domain, NameSpace.ATTCOLLIDESWITHAGENT, Attribute.AttributeType.DISC);
		collAt.setDiscValuesForRange(0,1, 1); //-1 due to inclusivity vs exclusivity
		
		//Burlap object for the agent
		ObjectClass agentClass = new ObjectClass(domain, NameSpace.CLASSAGENT);
		agentClass.addAttribute(xatt);
		agentClass.addAttribute(yatt);
		agentClass.addAttribute(zatt);
		
		//Burlap object for xyz goal
		ObjectClass goalClass = new ObjectClass(domain, NameSpace.CLASSGOAL);
		goalClass.addAttribute(xatt);
		goalClass.addAttribute(yatt);
		goalClass.addAttribute(zatt);
		
		//Burlap object for dirt blocks
		ObjectClass blockClass = new ObjectClass(domain, NameSpace.CLASSDIRTBLOCK);
		blockClass.addAttribute(xatt);
		blockClass.addAttribute(yatt);
		blockClass.addAttribute(zatt);
		blockClass.addAttribute(collAt);
		
		
		
		//Set up actions
		MovementAction northAction = new MovementAction(NameSpace.ACTIONNORTH, domain, 0,-1,0);
		MovementAction southAction = new MovementAction(NameSpace.ACTIONSOUTH, domain, 0,1,0);
		MovementAction eastAction = new MovementAction(NameSpace.ACTIONEAST, domain, 1,0,0);
		MovementAction westAction = new MovementAction(NameSpace.ACTIONWEST, domain, -1,0,0);
		
		//Set up indeterminism in actions
		
		//Set up propositional functions
		new AtGoalPF(NameSpace.PFATGOAL, domain, new String[]{NameSpace.CLASSAGENT, NameSpace.CLASSGOAL});
		
		return domain;
	}
	

}
