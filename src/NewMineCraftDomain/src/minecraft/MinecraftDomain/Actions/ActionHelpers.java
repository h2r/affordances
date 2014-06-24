package minecraft.MinecraftDomain.Actions;

import java.util.ArrayList;
import java.util.List;

import minecraft.MinecraftInitialStateGenerator;
import minecraft.NameSpace;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;

public class ActionHelpers {
	public static Boolean emptySpaceAt(int x, int y, int z, State state) {
		List<ObjectInstance> objects = objectsAt(x,y,z, state);
		for(ObjectInstance object : objects) {
			if (object.getObjectClass().hasAttribute(NameSpace.ATCOLLIDES) && object.getDiscValForAttribute(NameSpace.ATCOLLIDES) == 1) {
				return false;
			}
			
		}
		return true;		
	}
	
	public static Boolean withinMapAt(int x, int y, int z, int cols, int rows, int height) {
		boolean toReturn = x >= 0 && x < cols && y >=0 && y < rows && z >= 0 && z < height;
		return toReturn;
	}
	
	public static ArrayList<ObjectInstance> objectsAt(int x, int y, int z, State state) {
		List<ObjectInstance> allObjects = state.getAllObjects();
		ArrayList<ObjectInstance> toReturn = new ArrayList<ObjectInstance>();

		//Loop over all objects that collide with the agent and perform collision detection
		for (ObjectInstance object: allObjects) {
				if (object.getDiscValForAttribute(NameSpace.ATX) == x && 
						object.getDiscValForAttribute(NameSpace.ATY) == y &&
						(object.getDiscValForAttribute(NameSpace.ATZ) == z)) {
					toReturn.add(object);
			}
		}
		return toReturn;
	}
	
	public static int[] positionInFrontOfAgent(int distanceFromAgent, State state) {
		ObjectInstance agent = state.getObjectsOfTrueClass(NameSpace.CLASSAGENT).get(0);
		
		int oldX = agent.getDiscValForAttribute(NameSpace.ATX);
		int oldY = agent.getDiscValForAttribute(NameSpace.ATY);
		int oldZ = agent.getDiscValForAttribute(NameSpace.ATZ);
		
		int xChange = 0;
		int yChange = 0;
		int zChange = 0;
		
		//Account for rotational direction
		int directionInt = agent.getDiscValForAttribute(NameSpace.ATROTDIR);
		NameSpace.RotDirection directionEnum = NameSpace.RotDirection.fromInt(directionInt);
		switch(directionEnum){
		case NORTH:
			yChange = -distanceFromAgent;
			break;
		case EAST:
			xChange = distanceFromAgent;
			break;
		case SOUTH:
			yChange = distanceFromAgent;
			break;
		case WEST:
			xChange = -distanceFromAgent;
			break;
		default:
			System.out.println("Couldn't find this rot direction for value: " + directionInt);
			break;
		}
		
		//Account for vertical direction
		int vertDirectionInt = agent.getDiscValForAttribute(NameSpace.ATVERTDIR);
		NameSpace.VertDirection vertDirectionEnum = NameSpace.VertDirection.fromInt(vertDirectionInt);
		switch(vertDirectionEnum) {
		case AHEAD:
			break;
		case DOWNONE:
			zChange = -1;
			break;
		case DOWNTWO:
			zChange = -2;
			break;
		case DOWNTHREE://All the way down
			xChange = 0;
			yChange = 0;
			zChange = -1 - distanceFromAgent;
			break;
		default:
			System.out.println("Couldn't find this vert direction for value: " + vertDirectionInt);
			break;
		}
		int newX = oldX+xChange;
		int newY = oldY+yChange;
		int newZ = oldZ+zChange;
		
		return new int[]{newX, newY, newZ};

	}
	
	public static List<ObjectInstance> getBlocksInfrontOfAgent(int distanceFromAgent, State state) {

		int[] positionInfrontAgent = positionInFrontOfAgent(distanceFromAgent, state);
		assert(positionInfrontAgent.length == 3);
		int newX = positionInfrontAgent[0];
		int newY = positionInfrontAgent[1];
		int newZ = positionInfrontAgent[2];
		
		
		//Get objects at this position
		List<ObjectInstance> objectsHere = ActionHelpers.objectsAt(newX, newY, newZ, state);
		
		return objectsHere;
	}
	
	public static boolean agentHasVisualAccessTo(ObjectInstance block, int distanceOfBlock, State state) {
		ObjectInstance agent = state.getObjectsOfTrueClass(NameSpace.CLASSAGENT).get(0);
		
		int blockX = block.getDiscValForAttribute(NameSpace.ATX);
		int blockY = block.getDiscValForAttribute(NameSpace.ATY);
		int blockZ = block.getDiscValForAttribute(NameSpace.ATZ);
		
		int agentX = agent.getDiscValForAttribute(NameSpace.ATX);
		int agentY = agent.getDiscValForAttribute(NameSpace.ATY);
		int agentZ = agent.getDiscValForAttribute(NameSpace.ATZ);
		
		//If looking down two, can't see block if there is a block above it
		if (distanceOfBlock == 1 && agentZ-blockZ == 2 && !emptySpaceAt(blockX, blockY, blockZ+1, state) ){
			return false;
		}
		
		//If looking down two, can't see block if there is a block above it and towards the agent
		if (distanceOfBlock == 2 && agentZ-blockZ == 2 && !emptySpaceAt(agentX + (blockX - agentX)/2, blockY, blockZ+1, state)) {
			return false;
		}
		
		
		return true;
	}
	
	public static void removeObjectFromState(ObjectInstance object, State state, Domain domain) {
		
		String objectName = object.getTrueClassName();
		
		//DIRTBLOCKS
		if (objectName.equals(NameSpace.CLASSDIRTBLOCK)) {
			//A block item
			if (object.getDiscValForAttribute(NameSpace.ATDESTWHENWALKED) == 1){
				ObjectInstance agent = state.getObjectsOfTrueClass(NameSpace.CLASSAGENT).get(0);
				int oldBlocksPlaced = agent.getDiscValForAttribute(NameSpace.ATPLACEBLOCKS);
				agent.setValue(NameSpace.ATPLACEBLOCKS, oldBlocksPlaced+1);
			}
			//A block
			else {
				int x = object.getDiscValForAttribute(NameSpace.ATX);
				int y = object.getDiscValForAttribute(NameSpace.ATY);
				int z = object.getDiscValForAttribute(NameSpace.ATZ);
				int numberOfObjects = state.getAllObjects().toArray().length;
				
				ObjectInstance itemToAdd = MinecraftInitialStateGenerator.createDirtBlockItem(domain, x, y, z, numberOfObjects);
				state.addObject(itemToAdd);
				
			}
		}
		
		
		state.removeObject(object);
	}

}
