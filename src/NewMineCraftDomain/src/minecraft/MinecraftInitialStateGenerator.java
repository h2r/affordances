package minecraft;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.List;

import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.PropositionalFunction;
import burlap.oomdp.core.State;

/**
 * @author dhershko
 *
 */
public class MinecraftInitialStateGenerator {
	
	/**
	 * initial state of the generator stored for later use
	 */
	private State initialState;
	
	/**
	 * Domain associated with this state generator
	 */
	private Domain myDomain;
	

	
	public static State createInitialState(char[][][] mapAsCharArray, HashMap<String, Integer> stateInfo, Domain domain) {
		State toReturn = new State();
		//Process map
		processMapCharArray(mapAsCharArray, domain, toReturn);
		
		//Process stateInfo hashmap
		processStateInfoHM(stateInfo, domain,  toReturn);
		
		return toReturn;
	}
	
	private static void processStateInfoHM(HashMap<String, Integer> stateInfo, Domain domain, State stateToAddTo) {
	}
	
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
	
	private static boolean processChar(char inputChar, int row, int col, int height, Domain domain, State stateToAddto, int objectIndex) {
		ObjectInstance toAdd = null;
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
		default:
			return false;
			
		
		}
		
		//Actually add the object
		stateToAddto.addObject(toAdd);
		return true;
		
	}
	
	
	
	//METHODS FOR ADDING TO STATE
	private static ObjectInstance createGoal(Domain d, int x, int y, int z, int objectIndex) {
		String objectName = NameSpace.CLASSGOAL;
		ObjectInstance goal = new ObjectInstance(d.getObjectClass(objectName), objectName + objectIndex);
		setObjectLocation(goal, x, y, z);
		return goal;
	}
	
	private static ObjectInstance createDirtBlock(Domain d, int x, int y, int z, int objectIndex) {
		String objectName = NameSpace.CLASSDIRTBLOCK;
		ObjectInstance block = new ObjectInstance(d.getObjectClass(objectName), objectName+objectIndex);
		setObjectLocation(block, x, y, z);
		return block;	
	}
	
	private static ObjectInstance createAgent(Domain d, int x, int y, int z, int objectIndex) {
		String objectName = NameSpace.CLASSAGENT;
		ObjectInstance agent = new ObjectInstance(d.getObjectClass(objectName), objectName + objectIndex);
		setObjectLocation(agent, x, y, z);
		return agent;
	}
	
	
	
	
	
	private static void setObjectLocation(ObjectInstance object, int x, int y, int z) {
		object.setValue(NameSpace.ATTX, x);
		object.setValue(NameSpace.ATTY, y);
		object.setValue(NameSpace.ATTZ, z);
	}
	

	

}
