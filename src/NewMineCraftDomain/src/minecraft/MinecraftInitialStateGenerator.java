package minecraft;

import java.util.HashMap;
import java.util.List;

import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;

/**
 *Used to generate a minecraft state from a 3D char map and hashmap of stateInfo
 * @author dhershko
 *
 */
public class MinecraftInitialStateGenerator {
	
	public static State createInitialState(char[][][] mapAsCharArray, HashMap<String, Integer> stateInfo, Domain domain) {
		State toReturn = new State();
		//Process map
		processMapCharArray(mapAsCharArray, domain, toReturn);
		
		//Process stateInfo hashmap
		processStateInfoHM(stateInfo, domain,  toReturn);
		
		//Assert relevant info
		assertInfo(toReturn);
		
		return toReturn;
	}
	
	private static void assertInfo (State state) {
		//Assert 1 agent
		List<ObjectInstance> agents = state.getObjectsOfTrueClass(NameSpace.CLASSAGENTFEET);
		assert(agents.size() == 1);
		//Assert 1 agent feet
		List<ObjectInstance> feets =state.getObjectsOfTrueClass(NameSpace.CLASSAGENTFEET);
		assert(feets.size() == 1);
	}
	
	
	/**
	 * Parses the header as a hashmap and adjusts the state as necessary
	 * @param stateInfo the header as a hashmap from string keys to int values.
	 *  Used for things like the number of blocks that the agent can carry.
	 * @param domain the relevant domain
	 * @param stateToAddTo
	 */
	private static void processStateInfoHM(HashMap<String, Integer> stateInfo, Domain domain, State stateToAddTo) {
		ObjectInstance agent = stateToAddTo.getObjectsOfTrueClass(NameSpace.CLASSAGENT).get(0);
		
		for (String key : stateInfo.keySet()) {
			Integer value = stateInfo.get(key);
			char keyAsChar = key.charAt(0);
			switch(keyAsChar) {
			case NameSpace.PLACEABLEBLOCKS:
				agent.setValue(NameSpace.ATPLACEBLOCKS, value);
				break;
			default:
				break;
			}

			
			
		}
		
	}
	
	/**
	 * 
	 * @param mapAsCharArray the minecraft ascii map (header excluded) as a 3D char array
	 * @param domain the domain that the state belonds to
	 * @param stateToAddTo the state to add to
	 */
	private static void processMapCharArray(char[][][] mapAsCharArray, Domain domain, State stateToAddTo) {
		int objectIndex = 0;//Stores index of object added -- for naming purposes
		int rows = mapAsCharArray.length;
		int cols = mapAsCharArray[0].length;
		int height = mapAsCharArray[0][0].length;
		for (int row = 0; row < rows; row++) {
			for (int col = 0; col < cols; col++) {
				for (int currHeight = 0; currHeight < height; currHeight++) {
					char currChar = mapAsCharArray[row][col][currHeight];
					if (processChar(currChar, row, col, currHeight, domain, stateToAddTo, objectIndex)) {
						objectIndex += 1;
					}
				}
			}		
		}
	}
	
	/**
	 * Adds necessary object to the state for a given character.
	 * @returns if the object was added to the input state
	 */
	private static boolean processChar(char inputChar, int row, int col, int height, Domain domain, State stateToAddto, int objectIndex) {
		ObjectInstance toAdd = null;
		
		//Determine what ObjectInstance needs to be added
		switch (inputChar) {
		case NameSpace.DIRTBLOCK:
			toAdd = createDirtBlock(domain, col, row, height, objectIndex);
			break;
		case NameSpace.GOAL:
			toAdd = createGoal(domain, col, row, height, objectIndex);
			break;
		case NameSpace.AGENT:
			toAdd = createAgent(domain, col, row, height, objectIndex);
			break;
		case NameSpace.AGENTFEET:
			toAdd = createAgentFeet(domain, col, row, height, objectIndex);
			break;
		default:
			return false;
		}
		
		//Actually add the object
		stateToAddto.addObject(toAdd);
		return true;
	}
	
	
	//METHODS FOR CREATING OBJECTS TO ADD TO THE STATE
	private static ObjectInstance createGoal(Domain d, int x, int y, int z, int objectIndex) {
		String objectName = NameSpace.CLASSGOAL;
		ObjectInstance goal = new ObjectInstance(d.getObjectClass(objectName), objectName + objectIndex);
		setObjectLocation(goal, x, y, z, false, true, false);
		return goal;
	}
	
	public static ObjectInstance createDirtBlock(Domain d, int x, int y, int z, int objectIndex) {
		String objectName = NameSpace.CLASSDIRTBLOCK;
		ObjectInstance block = new ObjectInstance(d.getObjectClass(objectName), objectName+objectIndex);
		setObjectLocation(block, x, y, z, true, true, false);
		block.setValue(NameSpace.ATDEST, 1);
		return block;	
	}
	
	public static ObjectInstance createDirtBlockItem(Domain d, int x, int y, int z, int objectIndex) {
		String objectName = NameSpace.CLASSDIRTBLOCK;
		ObjectInstance block = new ObjectInstance(d.getObjectClass(objectName), objectName+objectIndex);
		setObjectLocation(block, x, y, z, false, false, true);
		block.setValue(NameSpace.ATDEST, 0);
		return block;
	}
	
	private static ObjectInstance createAgent(Domain d, int x, int y, int z, int objectIndex) {
		String objectName = NameSpace.CLASSAGENT;
		ObjectInstance agent = new ObjectInstance(d.getObjectClass(objectName), objectName + objectIndex);
		agent.setValue(NameSpace.ATROTDIR, NameSpace.RotDirection.NORTH.toInt());//Facing north by default
		agent.setValue(NameSpace.ATVERTDIR, NameSpace.VertDirection.AHEAD.toInt());//Facing ahead by default
		setObjectLocation(agent, x, y, z, false, false, false);
		return agent;
	}
	
	private static ObjectInstance createAgentFeet(Domain d, int x, int y, int z, int objectIndex) {
		String objectName = NameSpace.CLASSAGENTFEET;
		ObjectInstance agentFeet = new ObjectInstance(d.getObjectClass(objectName), objectName + objectIndex);
		setObjectLocation(agentFeet, x, y, z, false, false, false);
		return agentFeet;
	}
	
	
	/**
	 * Sets the necessary attributes for an object with a spatial component
	 * @param object object to add to
	 * @param x x of object
	 * @param y y of object
	 * @param z z of object
	 * @param collidesWithAgent a boolean of if the object collides with the agent
	 * 
	 */
	private static void setObjectLocation(ObjectInstance object, int x, int y, int z, boolean collides, boolean objectFloats, boolean destroyedWhenWalked) {
		object.setValue(NameSpace.ATX, x);
		object.setValue(NameSpace.ATY, y);
		object.setValue(NameSpace.ATZ, z);
		if (collides) {
			object.setValue(NameSpace.ATCOLLIDES, 1);
		}
		else {
			object.setValue(NameSpace.ATCOLLIDES, 0);
		}
		
		if (objectFloats) {
			object.setValue(NameSpace.ATFLOATS, 1);
		}
		
		else {
			object.setValue(NameSpace.ATFLOATS, 0);
		}
		
		if (destroyedWhenWalked) {
			object.setValue(NameSpace.ATDESTWHENWALKED, 1);
		}
		
		else {
			object.setValue(NameSpace.ATDESTWHENWALKED, 0);
		}
		
		
	}
	

	

}
