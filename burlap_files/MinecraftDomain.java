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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Stack;
import java.util.HashMap;


public class MinecraftDomain implements DomainGenerator{

	//Constants
	public static final String					ATTX = "x";
	public static final String					ATTY = "y";
	public static final String					ATTZ = "z";
	public static final String					BLKTYPE = "blkType";
	public static final String					ATTBLKNUM = "bNum";
	
	public static final String					CLASSAGENT = "agent";
	public static final String					CLASSGOAL = "goal";
	public static final String					CLASSBLOCK = "block";
	
	public static final String					ACTIONFORWARD = "forward";
	public static final String					ACTIONBACKWARD = "back";
	public static final String					ACTIONLEFT = "left";
	public static final String					ACTIONRIGHT = "right";
//	public static final String					ACTIONPLACEF = "placeForward";
//	public static final String					ACTIONPLACEB = "placeBack";
//	public static final String					ACTIONPLACER = "placeRight";
	public static final String					ACTIONPLACEL = "placeLeft";

	
	public static final String					PFATGOAL = "atGoal";
	public static final String					ISWALK = "isWalkable";
	public static final String					ISXLESS = "isAgentXLess";
	public static final String					ISYLESS = "isAgentYLess";
	public static final String					ISXMORE = "isAgentXMore";
	public static final String					ISYMORE = "isAgentYMore";
	
	public static final int						MAXX = 9; // 0 - 9, gives us a 10x10 surface
	public static final int						MAXY = 9;
	public static final int						MAXZ = 8;
	public static final int						MAXBLKNUM = 4;
	public static AtGoalPF 						AtGoalPF = null;
	
	public static int[][][]						MAP;
	public static HashMap<String,Affordance>	affordances;
	public static Stack<Subgoal>				goalStack;
	
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
		
		
		// CREATE ATTRIBUTES
		Attribute xatt = new Attribute(DOMAIN, ATTX, Attribute.AttributeType.DISC);
		xatt.setDiscValuesForRange(0, MAXX, 1);
		
		Attribute yatt = new Attribute(DOMAIN, ATTY, Attribute.AttributeType.DISC);
		yatt.setDiscValuesForRange(0, MAXY, 1);

		Attribute zatt = new Attribute(DOMAIN, ATTZ, Attribute.AttributeType.DISC);
		zatt.setDiscValuesForRange(0, MAXZ, 1);
		
		// Number of blocks the agent may carry
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
		
		// CREATE BLOCKS
		ObjectClass blockClass = new ObjectClass(DOMAIN, CLASSBLOCK);
		blockClass.addAttribute(xatt);
		blockClass.addAttribute(yatt);
		blockClass.addAttribute(zatt);
		
		// ==== CREATE ACTIONS ====
		
		// Movement
		Action forward = new ForwardAction(ACTIONFORWARD, DOMAIN, "");
		Action backward = new BackwardAction(ACTIONBACKWARD, DOMAIN, "");
		Action right = new RightAction(ACTIONRIGHT, DOMAIN, "");
		Action left = new LeftAction(ACTIONLEFT, DOMAIN, "");

		// Placement
//		Action placeF = new PlaceActionF(ACTIONPLACEF, DOMAIN, "");
//		Action placeB = new PlaceActionB(ACTIONPLACEB, DOMAIN, "");
//		Action placeR = new PlaceActionL(ACTIONPLACER, DOMAIN, "");
		Action placeL = new PlaceActionR(ACTIONPLACEL, DOMAIN, "");
		
		// CREATE PROPOSITIONAL FUNCTIONS
		PropositionalFunction atGoal = new AtGoalPF(PFATGOAL, DOMAIN,
				new String[]{CLASSAGENT, CLASSGOAL});
		PropositionalFunction isWalkable = new IsWalkablePF(ISWALK, DOMAIN,
				new String[]{"Integer", "Integer", "Integer"});
		PropositionalFunction isXLess = new IsAgentXLess(ISXLESS, DOMAIN,
				new String[]{"Integer", "Integer"});
		PropositionalFunction isYLess = new IsAgentYLess(ISYLESS, DOMAIN,
				new String[]{"Integer", "Integer"});
		PropositionalFunction isXMore = new IsAgentXMore(ISXMORE, DOMAIN,
				new String[]{"Integer", "Integer"});
		PropositionalFunction isYMore = new IsAgentYMore(ISYMORE, DOMAIN,
				new String[]{"Integer", "Integer"});
		
		/* TODO:
		 * Affordance list
		 * Add goal stack (initialized with making goalProposition true (delta thing)
		 * Affordance Class
		 * Subgoal Class
		 */
		
		// === Set up affordance list ===
		HashMap<String,Affordance> affordances = new HashMap<String,Affordance>();
		
//		// Create subgoals
//		Subgoal isWalkPX = new Subgoal("isWalkPX", isWalkable);
//		Subgoal isWalkNX = new Subgoal("isWalkNX", isWalkable);
		Subgoal isWalkPY = new Subgoal("isWalkPY", isWalkable);
//		Subgoal isWalkNY = new Subgoal("isWalkNY", isWalkable);
		
		// Subgoals that we should not try to satisfy (basically just preconditions, if they're true, then proceed)
		Subgoal isXLessSG = new Subgoal(ISXLESS, isXLess, false);
		Subgoal isYLessSG = new Subgoal(ISYLESS, isYLess, false);
		Subgoal isXMoreSG = new Subgoal(ISXMORE, isXMore, false);
		Subgoal isYMoreSG = new Subgoal(ISYMORE, isYMore, false);
		
		Affordance dPosY = new Affordance("dPosY");
		dPosY.addChild(isWalkPY);
		
		// Add actions to subgoals
		isXLessSG.setAction(right);
		isXMoreSG.setAction(left);
//		isYLessSG.setAction(forward);
		isWalkPY.setAction(forward);
		isYMoreSG.setAction(backward);
		
		// Add affordances to subgoals
		isYLessSG.setAffordance(dPosY);
		
		// Add subgoals to affordances
//		Affordance dPosX = new Affordance("dPosX");
//		dPosX.addChild(isWalkNX);
//		isXLessSG.addAffordance(dPosX);
//		
//		Affordance dNegX = new Affordance("dNegX");
//		dNegX.addChild(isWalkPX);
//		isXMoreSG.addAffordance(dNegX);
//		
//		isYLessSG.addAffordance(dPosY);
//		
//		Affordance dNegY = new Affordance("dNegY");
//		dNegY.addChild(isWalkPY);
//		isYMoreSG.addAffordance(dNegY);
		
		Affordance dIsAtLocation = new Affordance("dIsAtLocation");
		dIsAtLocation.addChild(isXLessSG);
		dIsAtLocation.addChild(isXMoreSG);
		dIsAtLocation.addChild(isYLessSG);
		dIsAtLocation.addChild(isYMoreSG);
		
		// Add affordances to list
		affordances.put("dIsAtLocation", dIsAtLocation);

		// === Set up subogal stack ===

		Stack<Subgoal> goalStack = new Stack<Subgoal>();
		Subgoal goal = new Subgoal("IsAtLocation", atGoal);
		goalStack.push(goal);
		
		// Add to domain
		DOMAIN.setAffordances(affordances);
		DOMAIN.setGoalStack(goalStack);
		
		return DOMAIN;
	}
	
	/**
	 * Will return a state object with a single agent object and a single goal object, and the blocks placed in the world.
	 * @param d the domain object that is used to specify the min/max dimensions
	 * @return a state object with a single agent object and a single goal object
	 */
	public static State getCleanState(Domain domain, List <Integer> blockX, List <Integer> blockY){
		
		State s = new State();
		
		//start by creating the block objects
		for(int i = 0; i < blockX.size(); i++){

			for(int j = 0;j < blockY.get(i); j++) {
				int x = blockX.get(i);
				int y = j;
				
				ObjectInstance block = new ObjectInstance(domain.getObjectClass(CLASSBLOCK), CLASSBLOCK+x+y+1);
				block.setValue(ATTX, x);
				block.setValue(ATTY, y);
				block.setValue(ATTZ, 1); // NOTE: ASSUMING BLOCKS CAN ONLY BE PLACED @ z=1 CURRENTLY
				s.addObject(block);
			}
		}
		
		//create exit
		s.addObject(new ObjectInstance(domain.getObjectClass(CLASSGOAL), CLASSGOAL+0));
		
		//create agent
		s.addObject(new ObjectInstance(domain.getObjectClass(CLASSAGENT), CLASSAGENT+0));
		
		return s;
		
	}
	
	public HashMap<String,Affordance> getAffordances() {
		return affordances;
	}
	
	public Stack<Subgoal> getGoalStack() {
		return goalStack;
	}
	
	/* === Mutators === */
	
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
	
	public static void setBlock(State s, int i, int x, int y, int z){
		ObjectInstance block = s.getObjectsOfTrueClass(CLASSBLOCK).get(i);
		block.setValue(ATTX, x);
		block.setValue(ATTY, y);
		block.setValue(ATTZ, z);
	}
	
	public static void addBlock(State s, int x, int y, int z){
		ObjectInstance block = new ObjectInstance(DOMAIN.getObjectClass(CLASSBLOCK), CLASSBLOCK+x+y+z);
		block.setValue(ATTX, x);
		block.setValue(ATTY, y);
		block.setValue(ATTZ, z);
		s.addObject(block);
	}
	
	/* === Class Accessors === */
	
	private static ObjectInstance getBlockAt(State s, int x, int y, int z){
		
		List<ObjectInstance> blocks = s.getObjectsOfTrueClass(CLASSBLOCK);
		for(ObjectInstance block : blocks){
			int bx = block.getDiscValForAttribute(ATTX);
			int by = block.getDiscValForAttribute(ATTY);
			int bz = block.getDiscValForAttribute(ATTZ);
			if(bx == x && by == y && bz == z){
				return block;
			}
		}
		
		return null;
	}
	
	
	/* =====ACTIONS===== */
	
	/**
	 * Attempts to move the agent into the given position, taking into account blocks in the world (holes and walls)
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
		
		if (nz - 1 > 0 && getBlockAt(s, nx, ny, nz - 1) == null) {
			// There is no block under us, return.
			return;
		}
		else if (getBlockAt(s, nx, ny, nz) != null) {
			// There is a block where we are trying to move, return.
			return;
		}
		else {
			// Place we're moving is unobstructed and there is solid ground below us, move
			agent.setValue(ATTX, nx);
			agent.setValue(ATTY, ny);
			agent.setValue(ATTZ, nz);
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
		int bz = az+dz; // Try one below, first
		
		
		// Make sure we are placing a block in bounds
		if (bx < 0 || bx > MAXX || by < 0 || by > MAXY || bz < 0 || bz > MAXZ) {
			return;
		}
		
		// If block loc is empty (z-1 from bot), and the loc above is empty (i.e. we can "see" the bottom loc), place it.
		if (bz - 1 > 0 && getBlockAt(s,bx,by,bz - 1) == null && getBlockAt(s,bx,by,bz) == null){
			
			addBlock(s, bx, by, bz - 1);
			
			// Remove the block from the agent's inventory
			agent.setValue(ATTBLKNUM, numAgentsBlocks - 1);
			numAgentsBlocks = numAgentsBlocks - 1;
		}
		// Now try placing one on agent's z level if it couldn't place one at z - 1
		else if (getBlockAt(s, bz, by, bz) == null && numAgentsBlocks > 0){
			
			// Place block
			addBlock(s, bx, by, bz);
			
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
		
		@Override
		public boolean applicableInState(State st, String [] params){
			PropositionalFunction pf = domain.getPropFunction(ISWALK);
			pf.isTrue(st, params);
			return true; 
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
	public static class IsAgentXLess extends PropositionalFunction{
		
		public IsAgentXLess(String name, Domain domain, String[] parameterClasses) {
			super(name, domain, parameterClasses);
		}
		
		public boolean isTrue(State st, String[] params) {
			ObjectInstance agent = st.getObject(CLASSAGENT + "0");
			
			//get the agent coordinates
			int ax = agent.getDiscValForAttribute(ATTX);
			
			ObjectInstance goal = st.getObject(CLASSGOAL + "0");
			
			//get the goal coordinates
			int gx = goal.getDiscValForAttribute(ATTX);
			
			return (ax < gx);
		}
	}
	
	public static class IsAgentXMore extends PropositionalFunction{
		
		public IsAgentXMore(String name, Domain domain, String[] parameterClasses) {
			super(name, domain, parameterClasses);
		}
		
		public boolean isTrue(State st, String[] params) {
			ObjectInstance agent = st.getObject(CLASSAGENT + "0");
			
			//get the agent coordinates
			int ax = agent.getDiscValForAttribute(ATTX);
			
			ObjectInstance goal = st.getObject(CLASSGOAL + "0");
			
			//get the goal coordinates
			int gx = goal.getDiscValForAttribute(ATTX);
			
			return (ax > gx);
		}
	}
	
	public static class IsAgentYLess extends PropositionalFunction{
		
		public IsAgentYLess(String name, Domain domain, String[] parameterClasses) {
			super(name, domain, parameterClasses);
		}
		
		public boolean isTrue(State st, String[] params) {
			ObjectInstance agent = st.getObject(CLASSAGENT + "0");
			
			//get the agent coordinates
			int ay = agent.getDiscValForAttribute(ATTY);
			
			ObjectInstance goal = st.getObject(CLASSGOAL + "0");
			
			//get the goal coordinates
			int gy = goal.getDiscValForAttribute(ATTY);
			
			return (ay < gy);
		}
	}
	
	public static class IsAgentYMore extends PropositionalFunction{
		
		public IsAgentYMore(String name, Domain domain, String[] parameterClasses) {
			super(name, domain, parameterClasses);
		}
		
		public boolean isTrue(State st, String[] params) {
			ObjectInstance agent = st.getObject(CLASSAGENT + "0");
			
			//get the agent coordinates
			int ay = agent.getDiscValForAttribute(ATTY);
			
			ObjectInstance goal = st.getObject(CLASSGOAL + "0");
			
			//get the goal coordinates
			int gy = goal.getDiscValForAttribute(ATTY);
			
			return (ay > gy);
		}
	}
	
	public static class AtGoalPF extends PropositionalFunction{

		public AtGoalPF(String name, Domain domain, String[] parameterClasses) {
			super(name, domain, parameterClasses);
		}

		@Override
		public boolean isTrue(State st, String[] params) {
			ObjectInstance agent = st.getObject(CLASSAGENT + "0");
			
			//get the agent coordinates
			int ax = agent.getDiscValForAttribute(ATTX);
			int ay = agent.getDiscValForAttribute(ATTY);
			int az = agent.getDiscValForAttribute(ATTZ);
			
			ObjectInstance goal = st.getObject(CLASSGOAL + "0");
			
			//get the goal coordinates
			int gx = goal.getDiscValForAttribute(ATTX);
			int gy = goal.getDiscValForAttribute(ATTY);
			int gz = goal.getDiscValForAttribute(ATTZ);
			
			if(ax == gx && ay == gy && az == gz){
				return true;
			}
			
			return false;
		}
		
		// Returns the x,y,z delta(s) needed to satisfy the atGoalPF
		public int[] delta(State st, String[] params) {
			
			ObjectInstance agent = st.getObject(CLASSAGENT + "0");
			
			//get the agent coordinates
			int ax = agent.getDiscValForAttribute(ATTX);
			int ay = agent.getDiscValForAttribute(ATTY);
			int az = agent.getDiscValForAttribute(ATTZ);
			
			ObjectInstance goal = st.getObject(CLASSGOAL + "0");
			
			//get the goal coordinates
			int gx = goal.getDiscValForAttribute(ATTX);
			int gy = goal.getDiscValForAttribute(ATTY);
			int gz = goal.getDiscValForAttribute(ATTZ);
			
			int[] dist = new int[3];
			dist[0] = gx - ax;
			dist[1] = gy - ay;
			dist[2] = gz - az;
			
			return dist;
		}
	}
	
	public static class IsWalkablePF extends PropositionalFunction {

		public IsWalkablePF(String name, Domain domain, String[] parameterClasses) {
			super(name, domain, parameterClasses);
		}
		
		@Override
		public boolean isTrue(State st, String[] params) {
			// Assume everything is walkable for now.
			return true;
//			// The first three elements of params are the amount of change
//			// in the x, y, and z directions
//			ObjectInstance agent = st.getObjectsOfTrueClass(CLASSAGENT).get(0);
//			int ax = agent.getDiscValForAttribute(ATTX);
//			int ay = agent.getDiscValForAttribute(ATTY);
//			int az = agent.getDiscValForAttribute(ATTZ);
//
//			int nx = ax + Integer.parseInt(params[0]);
//			int ny = ay + Integer.parseInt(params[1]);
//			int nz = az + Integer.parseInt(params[2]);
//			
//			if (nx < 0 || nx > MAXX || ny < 0 || ny > MAXY || nz < 0 || nz > MAXZ) {
//				// Trying to move out of bounds, return.
//				return false;
//			}
//			
//			if (nz - 1 > 0 && MinecraftDomain.getBlockAt(st, nx, ny, nz - 1) == null) {
//				// There is no block under us, return.
//				return false;
//			}
//			else if (getBlockAt(st, nx, ny, nz) != null) {
//				// There is a block where we are trying to move, return.
//				return false;
//			}
//			return true;
		}
		
	}
	
	public static void main(String[] args) {
		
		MinecraftDomain mcd = new MinecraftDomain();
		
		Domain d = mcd.generateDomain();
		
		// === Build Map === //
		List <Integer> blockX = new ArrayList<Integer>();
		List <Integer> blockY = new ArrayList<Integer>();
		
		// Row i will have blocks in all 10 locations
		for (int i = 0; i < MAXX; i++){
			// Place a trench @ x = 4
//			if (i == 4)
//			{
//				continue;
//			}
			blockX.add(i);
			blockY.add(MAXY);
		}
		
		State s = getCleanState(d, blockX, blockY);
		
		// === Add agent and goal === //
		ObjectInstance agent = s.getObjectsOfTrueClass(CLASSAGENT).get(0);
		agent.setValue(ATTX, 1);
		agent.setValue(ATTY, 1);
		agent.setValue(ATTZ, 2);
		agent.setValue(ATTBLKNUM, 2);

		ObjectInstance goal = s.getObjectsOfTrueClass(CLASSGOAL).get(0);
		goal.setValue(ATTX, 2);
		goal.setValue(ATTY, 2);
		goal.setValue(ATTZ, 2);
		
		s.addObject(new ObjectInstance(DOMAIN.getObjectClass(CLASSAGENT), CLASSAGENT+0));
		s.addObject(new ObjectInstance(DOMAIN.getObjectClass(CLASSGOAL), CLASSGOAL+0));
			
		
		// Explorer for testing
		TerminalExplorer exp = new TerminalExplorer(d);
		exp.addActionShortHand("f", ACTIONFORWARD);
		exp.addActionShortHand("b", ACTIONBACKWARD);
		exp.addActionShortHand("r", ACTIONRIGHT);
		exp.addActionShortHand("l", ACTIONLEFT);
//		exp.addActionShortHand("pf", ACTIONPLACEF);
//		exp.addActionShortHand("pb", ACTIONPLACEB);
//		exp.addActionShortHand("pr", ACTIONPLACER);
		exp.addActionShortHand("pl", ACTIONPLACEL);
				
		exp.exploreFromState(s);
	}

	
}
