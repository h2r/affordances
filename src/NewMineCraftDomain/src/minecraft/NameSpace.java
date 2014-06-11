package minecraft;

public class NameSpace {
	//-------------ATTRIBUTE STRINGS-------------
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
	public static final String							ATTCOLLIDESWITHAGENT = "collides";
	
	
	//-------------BURLAP OBJECT STRINGS-------------
	/**
	 * Constant for the name of the agent class
	 */
	public static final String							CLASSAGENT = "agent";
	
	/**
	 * Constant for name of xyz goal
	 */
	public static final String							CLASSGOAL = "goal";
	
	/**
	 * Constant for name of dirt blocks
	 */
	public static final String							CLASSDIRTBLOCK = "dirtblock";

	
	//-------------STRINGS FROM ASCII MAPS-------------
	public static final char GOAL = 'g';
	public static final char bAddSym = '+';
	public static final char AGENT = 'a';
	public static final char bRmSym = '-';
	public static final char bRmAllSym = '/';
	public static final char DIRTBLOCK = '.';
	public static final char wallSym = '=';
	public static final char DOOR = 'd';
	public static final char GOLDORE = '*';
	public static final char FURNACE = 'o';
	public static final char LAVA = 'V';
	public static final char twoBlockSym = '^';
	public static final char							BLOCKEMPTY = 'e';
	
	
	//-------------ACTIONS-------------
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
	
	
	//-------------PROPOSITIONAL FUNCTIONS-------------
	public static final String					PFATGOAL = "AtGoal";
	
	
	
}
