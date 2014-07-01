package minecraft;

public class NameSpace {
	//-------------ATTRIBUTE STRINGS-------------
	public static final String							ATX = "x";
	public static final String							ATY = "y";
	public static final String							ATZ = "z";
	public static final String							ATCOLLIDES = "collides";
	public static final String 							ATROTDIR = "rotationalDirection";
	public static final String							ATDEST = "destroyable";
	public static final String							ATFLOATS = "floats";
	public static final String							ATPLACEBLOCKS = "placeableBlocks";
	public static final String							ATVERTDIR = "verticalDirection";
	public static final String							ATDESTWHENWALKED = "destroyedByAgentWhenWalkedOn";
	public static final String							ATAMTGOLDORE = "amountOfGoldOre";
	public static final String							ATAMTGOLDBAR = "amountOfGoldBar";
	public static final String							ATTRENCHVECTOR = "trenchVector";
	
	//-------------BURLAP OBJECTCLASS STRINGS-------------
	public static final String							CLASSAGENT = "agent";
	public static final String							CLASSAGENTFEET = "agentsFeet";
	public static final String							CLASSGOAL = "goal";
	public static final String							CLASSDIRTBLOCK = "dirtBlock";
	public static final String							CLASSGOLDBLOCK = "goldBlock";
	public static final String							CLASSINDWALL = "indestrucibleWall";
	public static final String							CLASSFURNACE = "furnace";
	public static final String							CLASSTRENCH = "trench";

	
	//-------------CHARS FOR ASCII MAPS-------------
	//Map chars
	public static final char CHARGOAL = 'G';
	public static final char CHARAGENT = 'A';
	public static final char CHARDIRTBLOCK = '.';
	public static final char CHAREMPTY = 'e';
	public static final char CHARAGENTFEET = 'F';
	public static final char CHARINDBLOCK = 'w';
	public static final char CHARGOLDBLOCK = 'g';
	public static final char CHARFURNACE = 'f';
	
	//Header chars
	public static final char CHARPLACEABLEBLOCKS = 'B';
	public static final char CHARSTARTINGGOLDORE = 'g';
	public static final char CHARSTARTINGGOLDBAR = 'b';
	public static final char CHARGOALDESCRIPTOR = 'G';
	
	//Ints for goals
	public static final int INTXYZGOAL = 0;
	public static final int INTGOLDOREGOAL = 1;
	public static final int INTGOLDBARGOAL = 2;
	
	
	//-------------MAP SEPARATORS-------------
	public static String 							planeSeparator = "\n~\n";
	public static String 							rowSeparator = "\n";	
	
	//-------------ACTIONS STRINGS-------------
	public static final String						ACTIONMOVE = "moveAction";
	public static final String						ACTIONROTATEC = "rotateClockwise";
	public static final String 						ACTIONROTATECC = "rotateCounterClockwise";
	public static final String 						ACTIONDESTBLOCK = "destroyBlock";
	public static final String						ACTIONJUMP = "jump";
	public static final String						ACTIONPLACEBLOCK = "placeBlock";
	public static final String						ACTIONLOOKUP = "lookup";
	public static final String						ACTIONLOOKDOWN = "lookdown";
	public static final String						ACTIONUSEBLOCK = "useBlock";
	
	//-------------PROPOSITIONAL FUNCTION STRINGS-------------
	public static final String				PFATGOAL = "AtGoal";
	public static final String				PFEMPSPACE = "EmptySpace";
	public static final String				PFBLOCKAT = "BlockAt";
	public static final String				PFATLEASTXGOLDORE = "AgentHasXGoldOre";
	public static final String				PFATLEASTXGOLDBAR = "AgentHasXGoldBlock";
	public static final String				PFBLOCKINFRONT = "BlockFrontOfAgent";
	public static final String				PFENDOFMAPINFRONT = "EndMapFrontOfAgent";
	public static final String				PFEMPTYCELLINFRONT = "TrenchFrontOfAgent";
	public static final String				PFAGENTINMIDAIR = "AgentInAir";
	public static final String				PFAGENTADJTRENCH = "AgentAdjacentToTrench";
	public static final String				PFAGENTLOOKFORWARDWALK = "AgentLookForwardWalk";
	public static final String				PFEMPTYCELLINWALK = "EmptyCellInWalk";
	
	//-------------ENUMS-------------
	public enum RotDirection {
		NORTH(0), EAST(1), SOUTH(2), WEST(3);
		public static final int size = RotDirection.values().length;
		private final int intVal;
		
		RotDirection(int i) {
			this.intVal = i;
		}
		
		public int toInt() {
			return this.intVal;
		}
		
		public static RotDirection fromInt(int i) {
			switch (i) {
			case 0:
				return NORTH;
			case 1:
				return EAST;
			case 2:
				return SOUTH;
			case 3:
				return WEST;
			
			}
			return null;
		}
		
	}
	
	public enum VertDirection {
		AHEAD(3), DOWNONE(2), DOWNTWO(1), DOWNTHREE(0);
		public static final int size = VertDirection.values().length;
		private final int intVal;
		
		VertDirection(int i) {
			this.intVal = i;
		}
		
		public int toInt() {
			return this.intVal;
		}
		
		public static VertDirection fromInt(int i) {
			switch (i) {
			case 3:
				return AHEAD;
			case 2:
				return DOWNONE;
			case 1:
				return DOWNTWO;
			case 0:
				return DOWNTHREE;
			
			}
			return null;
		}
	}
}
