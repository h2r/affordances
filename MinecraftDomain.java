package burlap.domain.singleagent.minecraft;

import burlap.behavior.singleagent.EpisodeSequenceVisualizer;
import burlap.domain.singleagent.gridworld.GridWorldVisualizer;
import burlap.oomdp.singleagent.Action;
import burlap.oomdp.singleagent.SADomain;
import burlap.oomdp.core.Attribute;
import burlap.oomdp.core.Domain;
import burlap.oomdp.auxiliary.DomainGenerator;
import burlap.oomdp.core.ObjectClass;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.PropositionalFunction;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.explorer.TerminalExplorer;
import burlap.oomdp.singleagent.explorer.VisualExplorer;
import burlap.oomdp.visualizer.Visualizer;


public class MinecraftDomain implements DomainGenerator{

	//Constants
	public static final String					ATTX = "x";
	public static final String					ATTY = "y";
	public static final String					ATTZ = "z";
	public static final String					ATTBLKNUM = "bNum";
	
	public static final String					CLASSAGENT = "agent";
	public static final String					CLASSGOAL = "goal";
	public static final String					CLASSBLOCK = "block";
	
	public static final String					ACTIONFORWARD = "forward";
	public static final String					ACTIONBACKWARD = "back";
	public static final String					ACTIONLEFT = "left";
	public static final String					ACTIONRIGHT = "right";
	public static final String					ACTIONPLACEF = "placeForward";
	public static final String					ACTIONPLACEB = "placeBack";
	public static final String					ACTIONPLACER = "placeRight";
	public static final String					ACTIONPLACEL = "placeLeft";

	
	public static final String					PFATGOAL = "atGoal";
	
	public static final int						MAXX = 9; // 0 - 9, gives us a 10x10 surface
	public static final int						MAXY = 9;
	public static final int						MAXZ = 8;
	public static final int						MAXBLKNUM = 4;
	public static AtGoalPF 						AtGoalPF = null;
	
	public static int[][][]						MAP;
	
	private ObjectClass 						agentClass = null;
	private ObjectClass 						goalClass = null;
	
	public static SADomain						DOMAIN = null;	
	
	
	/**
	 * Constructs an empty map with deterministic transitions
	 * @param width width of the map
	 * @param height height of the map
	 */
	public Domain generateDomain() {
		if(DOMAIN != null){
			return DOMAIN;
		}
		
		DOMAIN = new SADomain();
		
		generateMap();
		
		// CREATE ATTRIBUTES
		Attribute xatt = new Attribute(DOMAIN, ATTX, Attribute.AttributeType.DISC);
		xatt.setDiscValuesForRange(0, MAXX, 1);
		
		Attribute yatt = new Attribute(DOMAIN, ATTY, Attribute.AttributeType.DISC);
		yatt.setDiscValuesForRange(0, MAXY, 1);

		Attribute zatt = new Attribute(DOMAIN, ATTZ, Attribute.AttributeType.DISC);
		zatt.setDiscValuesForRange(0, MAXZ, 1);
		
		// Number of blocks the agent is carrying
		Attribute blknumatt = new Attribute(DOMAIN, ATTBLKNUM, Attribute.AttributeType.DISC);
		blknumatt.setDiscValuesForRange(0, MAXBLKNUM, 1);

		// CREATE AGENT
		agentClass = new ObjectClass(DOMAIN, CLASSAGENT);
		agentClass.addAttribute(xatt);
		agentClass.addAttribute(yatt);
		agentClass.addAttribute(zatt);
		agentClass.addAttribute(blknumatt);
		
		// CREATE GOAL
		ObjectClass goalClass = new ObjectClass(DOMAIN, CLASSGOAL);
		goalClass.addAttribute(xatt);
		goalClass.addAttribute(yatt);
		goalClass.addAttribute(zatt);
		
		// ==== CREATE ACTIONS ====
		
		// Movement
		Action forward = new ForwardAction(ACTIONFORWARD, DOMAIN, "");
		Action backward = new BackwardAction(ACTIONBACKWARD, DOMAIN, "");
		Action right = new RightAction(ACTIONRIGHT, DOMAIN, "");
		Action left = new LeftAction(ACTIONLEFT, DOMAIN, "");

		// Placement
		Action placeF = new PlaceActionF(ACTIONPLACEF, DOMAIN, "");
		Action placeB = new PlaceActionB(ACTIONPLACEB, DOMAIN, "");
		Action placeR = new PlaceActionL(ACTIONPLACER, DOMAIN, "");
		Action placeL = new PlaceActionR(ACTIONPLACEL, DOMAIN, "");
		
		// CREATE PROPOSITIONAL FUNCTIONS
		PropositionalFunction atGoal = new AtGoalPF(PFATGOAL, DOMAIN,
				new String[]{CLASSAGENT, CLASSGOAL});
		
		return DOMAIN;
	}
	
	/**
	 * Sets the first agent object in s to the specified x,y,z position.
	 * @param s the state with the agent whose position to set
	 * @param x the x position of the agent
	 * @param y the y position of the agent
	 * @param z the z position of the agent
	 */
	public static void setAgent(State s, int x, int y, int z, int numBlocks){
		ObjectInstance o = s.getObjectsOfTrueClass(CLASSAGENT).get(0);
		
		o.setValue(ATTX, x);
		o.setValue(ATTY, y);
		o.setValue(ATTZ, z);
		o.setValue(ATTBLKNUM, numBlocks);
	}
	
	/**
	 * Sets the first goal object in s to the specified x,y,z position.
	 * @param s the state with the goal whose position to set
	 * @param x the x position of the goal
	 * @param y the y position of the goal
	 * @param z the z position of the goal
	 */
	public static void setGoal(State s, int x, int y, int z) {
		ObjectInstance o = s.getObjectsOfTrueClass(CLASSGOAL).get(0);
		
		o.setValue(ATTX, x);
		o.setValue(ATTY, y);
		o.setValue(ATTZ, z);
	}
	
	/**
	 * Will return a state object with a single agent object and a single goal object
	 * @param d the domain object that is used to specify the min/max dimensions
	 * @return a state object with a single agent object and a single goal object
	 */
	public static State getState(Domain d){
		State s = new State();
		
		s.addObject(new ObjectInstance(d.getObjectClass(CLASSAGENT), CLASSAGENT+0));
		s.addObject(new ObjectInstance(d.getObjectClass(CLASSGOAL), CLASSGOAL+0));
		
		return s;
	}
	
	/* ==== MapCreation ==== */

	public static void generateMap(){
		
		//Initializes the map two-dimensional array to be [13][13]
		MAP = new int[MAXX+1][MAXY+1][MAXZ + 1]; //+1 to handle zero base
		initBlocks();
			
	}
				
	public static void initBlocks(){
		// Adds the initial blocks to the world
		createFloor();
		addHorizTrench(0,MAXX,4,2,1);
	}
	
	// Creates floor and initializes all other blocks to be air
	protected static void createFloor() {
		for (int x = 0; x <= MAXX; x++) {
			for (int y = 0; y <= MAXY; y++) {
				MAP[x][y][0] = 1;  // 1 is a non-air block
				MAP[x][y][1] = 1;  // 1 is a non-air block
				for (int z = 2; z <=MAXZ; z++) {
					MAP[x][y][z] = 0;  // Everything else is air upon world creation
				}
			}
		}
	}
	
	// Creates a trench of air blocks in the map starting at height z and down to trenchDepth
	protected static void addHorizTrench(int startX, int endX, int y, int z, int trenchDepth) {
		
		if (trenchDepth < 0) {
			// Trying to make a trench that is deeper than the world, squish it to maxDepth.
			trenchDepth = 0;
		}
		
		for (int x=startX;x <= endX; x++) {
			for(int h = trenchDepth; h <= z; h++) {
				MAP[x][y][h] = 0; // Set to air block
			}
		}
	}
	
	// Creates floor and initializes all other blocks to be air
	protected static void addHorizWall(int startX, int endX, int y, int z, int wallHeight) {
		
		if (wallHeight > MAXZ) {
			// Trying to make a wall that is larger than the world, shrink.
			wallHeight = MAXZ;
		}
		
		for (int x=startX;x <= endX; x++) {
			for(int h = z; h <= wallHeight; h++) {
				MAP[x][y][h] = 1; // Set to non-air block
			}
		}
	}
	
	/* =====ACTIONS===== */
	
	/**
	 * Attempts to move the agent into the given position, taking into account blocks (holes and walls)
	 * @param the current state
	 * @param the attempted new X position of the agent
	 * @param the attempted new Y position of the agent
	 * @param the attempted new Z position of the agent
	 */
	public static void move(State s, int xd, int yd, int zd){
		
		ObjectInstance agent = s.getObjectsOfTrueClass(CLASSAGENT).get(0);
		int ax = agent.getDiscValForAttribute(ATTX);
		int ay = agent.getDiscValForAttribute(ATTY);
		int az = agent.getDiscValForAttribute(ATTZ);
		
		int nx = ax+xd;
		int ny = ay+yd;
		int nz = az+zd;
		
		if (nx < 0 || nx > MAXX || ny < 0 || ny > MAXY || nz < 0 || nz > MAXZ) {
			// Trying to move out of bounds, return.
			return;
		}
		
		if (MAP[nx][ny][nz] == 0 && MAP[nx][ny][nz - 1] == 1) {
			// Place we're moving is unobstructed and there is solid ground below us, move
			agent.setValue(ATTX, nx);
			agent.setValue(ATTY, ny);
			agent.setValue(ATTZ, nz);
		}
		else {
			// Can't move - obstructed or there's a hole
//			System.out.println("Can't move in that direction, obstruction or hole");
			return;
		}

	}
	
	public static void destroy(State s, int dx, int dy, int dz) {
		
		// Agent's global coordinates
		ObjectInstance agent = s.getObjectsOfTrueClass(CLASSAGENT).get(0);
		int ax = agent.getDiscValForAttribute(ATTX);
		int ay = agent.getDiscValForAttribute(ATTY);
		int az = agent.getDiscValForAttribute(ATTZ);
		
		// Get global coordinates of the block to be destroyed
		int bx = ax+dx;
		int by = ay+dy;
		int bz = az+dz;
		
		if (bx < 0 || bx > MAXX || by < 0 || by > MAXY || bz < 0 || bz > MAXZ) {
			// Trying to destroy an out of bounds block.
			return;
		}
		
		// If there is a block in the new coordinates, destroy it. Otherwise, do nothing.
		if (MAP[bx][by][bz] == 1) {
			MAP[bx][by][bz] = 0;
		}
		
		// Also destroy one above in case of height 2 (simplifying assumption, currently).
		if (bz + 1 < MAXZ && MAP[bx][by][bz + 1] == 1) {
			MAP[bx][by][bz + 1] = 0;
		}
		
	}
	
	public static void place(State s, int dx, int dy, int dz) {
		
		ObjectInstance agent = s.getObjectsOfTrueClass(CLASSAGENT).get(0);
		
		// Check to see if the agent is out of blocks
		int numAgentsBlocks = agent.getDiscValForAttribute(ATTBLKNUM);
		if (numAgentsBlocks <= 0) {
			return;
		}
		
		// Agent's global coordinates
		int ax = agent.getDiscValForAttribute(ATTX);
		int ay = agent.getDiscValForAttribute(ATTY);
		int az = agent.getDiscValForAttribute(ATTZ);
		
		// Get global coordinates of the loc to place the block
		int bx = ax+dx;
		int by = ay+dy;
		int bz = az+dz;
		
		
		// Make sure we are placing a block in bounds
		if (bx < 0 || bx > MAXX || by < 0 || by > MAXY || bz < 0 || bz > MAXZ) {
			return;
		}
		
		// Place a block one z beneath the agent, if it can.
		if (MAP[bx][by][bz] == 0 && bz - 1 > 0 && MAP[bx][by][bz - 1] == 0 && numAgentsBlocks > 0){
			
			// Place block
			MAP[bx][by][bz - 1] = 1;
			
			// Remove the block from the agent's inventory
			agent.setValue(ATTBLKNUM, numAgentsBlocks - 1);
			numAgentsBlocks = numAgentsBlocks - 1;
		}
		// Now try placing one on agent's z level if it couldn't place one at z - 1
		else if (MAP[bx][by][bz] == 0 && numAgentsBlocks > 0){
			
			// Place block
			MAP[bx][by][bz] = 1;
			
			// Remove the block from the agent's inventory
			agent.setValue(ATTBLKNUM, numAgentsBlocks - 1);
		}
		
	}


	public static class ForwardAction extends Action{

		public ForwardAction(String name, Domain domain, String parameterClasses){
			super(name, domain, parameterClasses);
		}
		
		protected State performActionHelper(State st, String[] params) {
			move(st, 0, 1, 0);
			System.out.println("Action Performed: " + this.name);
			return st;
		}
	}


	public static class BackwardAction extends Action{

		public BackwardAction(String name, Domain domain, String parameterClasses){
			super(name, domain, parameterClasses);
		}
		
		protected State performActionHelper(State st, String[] params) {
			move(st, 0, -1, 0);
			System.out.println("Action Performed: " + this.name);
			return st;
		}		
	}


	public static class RightAction extends Action{

		public RightAction(String name, Domain domain, String parameterClasses){
			super(name, domain, parameterClasses);
		}
		
		protected State performActionHelper(State st, String[] params) {
			move(st, 1, 0, 0);
			System.out.println("Action Performed: " + this.name);
			return st;
		}
	}


	public static class LeftAction extends Action{

		public LeftAction(String name, Domain domain, String parameterClasses){
			super(name, domain, parameterClasses);
		}
		
		protected State performActionHelper(State st, String[] params) {
			move(st, -1, 0, 0);
			System.out.println("Action Performed: " + this.name);
			return st;
		}	
	}
	
	public static class PlaceActionF extends Action{

		public PlaceActionF(String name, Domain domain, String parameterClasses){
			super(name, domain, parameterClasses);
		}
		
		protected State performActionHelper(State st, String[] params) {
			place(st, 0, 1, 0);
			System.out.println("Action Performed: " + this.name);
			return st;
		}	
	}
	
	public static class PlaceActionB extends Action{

		public PlaceActionB(String name, Domain domain, String parameterClasses){
			super(name, domain, parameterClasses);
		}
		
		protected State performActionHelper(State st, String[] params) {
			place(st, 0, -1, 0);
			System.out.println("Action Performed: " + this.name);
			return st;
		}	
	}
	
	public static class PlaceActionR extends Action{

		public PlaceActionR(String name, Domain domain, String parameterClasses){
			super(name, domain, parameterClasses);
		}
		
		protected State performActionHelper(State st, String[] params) {
			place(st, -1, 0, 0);
			System.out.println("Action Performed: " + this.name);
			return st;
		}	
	}
	
	public static class PlaceActionL extends Action{

		public PlaceActionL(String name, Domain domain, String parameterClasses){
			super(name, domain, parameterClasses);
		}
		
		protected State performActionHelper(State st, String[] params) {
			place(st, 1, 0, 0);
			System.out.println("Action Performed: " + this.name);
			return st;
		}	
	}
	
	/* ==== Propositional Functions ==== */
	
	public static class AtGoalPF extends PropositionalFunction{

		public AtGoalPF(String name, Domain domain, String[] parameterClasses) {
			super(name, domain, parameterClasses);
		}

		@Override
		public boolean isTrue(State st, String[] params) {
			
			ObjectInstance agent = st.getObject(params[0]);
			
			//get the agent coordinates
			int ax = agent.getDiscValForAttribute(ATTX);
			int ay = agent.getDiscValForAttribute(ATTY);
			int az = agent.getDiscValForAttribute(ATTZ);
			
			ObjectInstance goal = st.getObject(params[1]);
			
			//get the goal coordinates
			int gx = goal.getDiscValForAttribute(ATTX);
			int gy = goal.getDiscValForAttribute(ATTY);
			int gz = goal.getDiscValForAttribute(ATTZ);
			
			if(ax == gx && ay == gy && az == gz){
				return true;
			}
			
			return false;
		}
	}
	
	public static void main(String[] args) {
		
		MinecraftDomain mcd = new MinecraftDomain();
		
		Domain d = mcd.generateDomain();
		
		State s = new State();
		
		s.addObject(new ObjectInstance(DOMAIN.getObjectClass(CLASSAGENT), CLASSAGENT+0));
		s.addObject(new ObjectInstance(DOMAIN.getObjectClass(CLASSGOAL), CLASSGOAL+0));

		ObjectInstance agent = s.getObjectsOfTrueClass(CLASSAGENT).get(0);
		agent.setValue(ATTX, 1);
		agent.setValue(ATTY, 1);
		agent.setValue(ATTZ, 2);
		agent.setValue(ATTBLKNUM, 2);

		ObjectInstance goal = s.getObjectsOfTrueClass(CLASSGOAL).get(0);
		goal.setValue(ATTX, 5);
		goal.setValue(ATTY, 8);
		goal.setValue(ATTZ, 2);

			
		TerminalExplorer exp = new TerminalExplorer(d);
		exp.addActionShortHand("f", ACTIONFORWARD);
		exp.addActionShortHand("b", ACTIONBACKWARD);
		exp.addActionShortHand("r", ACTIONRIGHT);
		exp.addActionShortHand("l", ACTIONLEFT);
		exp.addActionShortHand("pf", ACTIONPLACEF);
		exp.addActionShortHand("pb", ACTIONPLACEB);
		exp.addActionShortHand("pr", ACTIONPLACER);
		exp.addActionShortHand("pl", ACTIONPLACEL);
				
		exp.exploreFromState(s);
	}

	
}
