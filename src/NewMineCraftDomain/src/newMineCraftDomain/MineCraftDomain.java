package newMineCraftDomain;

import java.util.ArrayList;
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
import burlap.oomdp.core.State;
import burlap.oomdp.core.TransitionProbability;
import burlap.oomdp.singleagent.Action;
import burlap.oomdp.singleagent.SADomain;

public class MineCraftDomain implements DomainGenerator{
	//String and char aliases
	
	//Attribute strings
	/**
	 * Constant for the name of the x attribute
	 */
	public static final String							ATTX = "x";
	
	/**
	 * Constant for the name of the y attribute
	 */
	public static final String							ATTY = "y";
	
	/**
	 * Constant for the name of the y attribute
	 */
	public static final String							ATTZ = "z";
	
	
	/**
	 * Constant for the name of attribute for location object type
	 */
	public static final String							ATTLOCTYPE = "locType";
	
	
	//Burlap object strings
	/**
	 * Constant for the name of the agent class
	 */
	public static final String							CLASSAGENT = "agent";
	
	//Block chars
	/**
	 * Stores space not occupied by a block
	 * 
	 */
	public static final char							EMPTYSPACE = ' ';
	
	
	/**
	 * Constant for the name of the north action
	 */
	public static final String							ACTIONNORTH = "north";
	
	/**
	 * Constant for the name of the south action
	 */
	public static final String							ACTIONSOUTH = "south";
	
	/**
	 * Constant for the name of the east action
	 */
	public static final String							ACTIONEAST = "east";
	
	/**
	 * Constant for the name of the west action
	 */
	public static final String							ACTIONWEST = "west";
	
	/**
	 * Constant for the name of the Jump action
	 */
	public static final String							ACTIONJUMP = "jump";
	
	
	//Generic class variables
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
	protected char [][][]									map;
	
	/**
	 * Constructs an empty map
	 * @param rows rows of the map
	 * @param cols cols of the map
	 * @param height height of the map
	 * 
	 */
	
	
	/**
	 * Stores the transition dynamics where the first element is indexed north,south,east,west,jump and the second
	 * is an array that stores the probability of each action from performing the action of the first index (ordered the same way)
	 */
	double [][] transitionDynamics;
	
	public MineCraftDomain(int rows, int cols, int height){
		this.rows = rows;
		this.cols = cols;
		this.height = height;
		
		//Set up map since none input
		this.makeEmptyMap();
		this.makeDirtFloor();
	}
	
	/**
	 * Constructs a deterministic world based on the provided map.
	 * @param map the first index is the x index, the second the y; 1 entries indicate a wall
	 */
	public MineCraftDomain(char [][][] map){
		this.setMap(map);
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
					this.map[i][j][k] = EMPTYSPACE;
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
	
	private Boolean emptySpaceForAgentAt(int x, int y, int z) {
		char headBlock = this.map[y][x][z];
		char feetBlock = this.map[y][x][z-1];
		return headBlock == EMPTYSPACE && feetBlock == EMPTYSPACE;
		
		
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
	 * Attempts to move the agent into the given position, taking into account walls and blocks
	 * @param the current state
	 * @param the delta of X position of the agent
	 * @param the delta of Y position of the agent
	 * @param the delta of Z position of the agent
	 */
	protected void move(State s, int xChange, int yChange, int zChange){
		
		ObjectInstance agent = s.getObjectsOfTrueClass(CLASSAGENT).get(0);
		int oldX = agent.getDiscValForAttribute(ATTX);
		int oldY = agent.getDiscValForAttribute(ATTY);
		int oldZ = agent.getDiscValForAttribute(ATTZ);
		
		int newX = oldX+xChange;
		int newY = oldY+yChange;
		int newZ = oldZ+zChange;
		
		if (agentWithinMapAt(newX, newY, newZ) && emptySpaceForAgentAt(newX, newY, newZ)) {
			agent.setValue(ATTX, newX);
			agent.setValue(ATTY, newY);
			agent.setValue(ATTZ, newZ);
		}
		
	}
	
	
	
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
		 * Initializes for the given name, domain and actually direction probabilities the agent will go
		 * @param name name of the action
		 * @param domain the domain of the action
		 * @param directions the probability for each direction (index 0,1,2,3,4 corresponds to north,east,south,west,jump respectively).
		 */
		public MovementAction(String name, Domain domain, double [] directions){
			super(name, domain, "");
			assert(directions.length == 5);
			this.directionProbs = directions;
			this.rand = RandomFactory.getMapped(0);
		}
		
		@Override
		protected State performActionHelper(State st, String[] params) {
			
			double roll = rand.nextDouble();
			double curSum = 0.;
			int dir = 0;
			for(int i = 0; i < directionProbs.length; i++){
				curSum += directionProbs[i];
				if(roll < curSum){
					dir = i;
					break;
				}
			}
			
			int [] dcomps = MineCraftDomain.this.movementDirectionFromIndex(dir);
			MineCraftDomain.this.move(st, dcomps[0], dcomps[1], dcomps[2]);
	
			return st;
		}
		
		
	}
	
	
	/**Gets the transition dyanmics for an action
	 * @returns an array that adheres to the transitionDynamics specification
	 */
	private double[][] setTransitionDynamics() {
		double probSuccess = .9;
		double probFailureOverTwo = .05;
		double[][] toReturn = new double[5][5];
		//North
		toReturn[0] = new double[]{probSuccess,probFailureOverTwo,probFailureOverTwo,0,0};
		//East
		toReturn[1] = new double[]{probFailureOverTwo,probSuccess,probFailureOverTwo,0,0};
		//South
		toReturn[2] = new double[]{0,probFailureOverTwo,probSuccess,probFailureOverTwo,0,0};
		//West
		toReturn[3] = new double[]{probFailureOverTwo,0,probFailureOverTwo,probSuccess,0};
		//Jump
		toReturn[4] = new double[]{0,0,0,0,probSuccess};
		
		return toReturn;
	}
	
	
	/**
	 * Returns the change in x and y position for a given direction number.
	 * @param i the direction number (0,1,2,3,4 indicates north,south,east,west,jump respectively)
	 * @return the change in direction for x, y and z; the first index of the returned double is change in x, the second index is change in y and the third is change in z.
	 */
	protected int [] movementDirectionFromIndex(int i){
		
		int [] result = null;
		
		switch (i) {
		case 0:
			result = new int[]{0,1,0};
			break;
			
		case 1:
			result = new int[]{0,-1,0};
			break;
			
		case 2:
			result = new int[]{1,0,0};
			break;
			
		case 3:
			result = new int[]{-1,0,0};

		case 4:
			result = new int[]{0,0,1};
		default:
			result = new int[]{0,0,0};
		}
		
		return result;
	}
	
	
	@Override
	public Domain generateDomain() {
		
		Domain domain = new SADomain();
		
		//x Position Attribute
		Attribute xatt = new Attribute(domain, ATTX, Attribute.AttributeType.DISC);
		xatt.setDiscValuesForRange(0, this.cols-1, 1); //-1 due to inclusivity vs exclusivity
		
		//y Position Attribute
		Attribute yatt = new Attribute(domain, ATTY, Attribute.AttributeType.DISC);
		yatt.setDiscValuesForRange(0, this.rows-1, 1); //-1 due to inclusivity vs exclusivity
		
		//z Position Attribute
		Attribute zatt = new Attribute(domain, ATTZ, Attribute.AttributeType.DISC);
		yatt.setDiscValuesForRange(0, this.height-1, 1); //-1 due to inclusivity vs exclusivity
		
		//Attribute to store if something has a position in space
		Attribute ltatt = new Attribute(domain, ATTLOCTYPE, Attribute.AttributeType.DISC);
		ltatt.setDiscValuesForRange(0, 0, 1);
		
		//Burlap object for the agent
		ObjectClass agentClass = new ObjectClass(domain, CLASSAGENT);
		agentClass.addAttribute(xatt);
		agentClass.addAttribute(yatt);
		agentClass.addAttribute(zatt);
		agentClass.addAttribute(ltatt);

		
		this.transitionDynamics = this.setTransitionDynamics();
		//Set up agent actions
		new MovementAction(ACTIONNORTH, domain, transitionDynamics[0]);
		new MovementAction(ACTIONSOUTH, domain, this.transitionDynamics[1]);
		new MovementAction(ACTIONEAST, domain, this.transitionDynamics[2]);
		new MovementAction(ACTIONWEST, domain, this.transitionDynamics[3]);
		new MovementAction(ACTIONJUMP, domain, this.transitionDynamics[4]);
		
		return domain;
	}
	

}
