package minecraft.WorldGenerator;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Random;

import minecraft.NameSpace;
import minecraft.MinecraftDomain.Actions.ActionHelpers;

public class WorldGenerator {
	//Class variables
	final int rows;
	final int cols;
	final int height;
	
	static int depthOfDirtFloor = 1;
	static double probTrenchChangeDirection = .2;
	
	char[][][] charArray;
	Random rand;
	
	public WorldGenerator(int rows, int cols, int height) {
		this.rows = rows;
		this.cols = cols;
		this.height = height;
		this.rand = new Random();
	}
	
	private void emptifyCharArray(char [][][] toChange) {
		for(int row = 0; row < this.rows; row++) {
			for(int col = 0; col < this.cols; col++) {
				for(int currHeight = 0; currHeight < this.height; currHeight++) {
					toChange[row][col][currHeight] = NameSpace.CHAREMPTY;
				}
			}
		}
	}
	
	private void addDirtFloor(char [][][] toChange) {
		assert(this.height >= WorldGenerator.depthOfDirtFloor);
		for (int currHeight = 0; currHeight < WorldGenerator.depthOfDirtFloor; currHeight++) {
			for(int row = 0; row < this.rows; row++) {
				for(int col = 0; col < this.cols; col++) {
					toChange[row][col][currHeight] = NameSpace.CHARDIRTBLOCK;
				}
			}
		}
	}
	
	private int[] addCharRandomly(char toAdd, Integer x, Integer y, Integer z, char[][][] toChange) {
		//Randomize any unspecifed coordinate
		if (x == null) {
			x = this.rand.nextInt(this.cols);
		}
		
		if (y == null) {
			y = this.rand.nextInt(this.rows);
		}
		
		if (z == null) {
			z = this.rand.nextInt(this.height);
		}
		
		toChange[y][x][z] = toAdd;
		
		return new int[]{x,y,z};
	}
	
	private void addRandomSpatialGoal(char[][][] toChange) {
		assert(WorldGenerator.depthOfDirtFloor+1 < this.height);
		addCharRandomly(NameSpace.CHARGOAL, null, null, WorldGenerator.depthOfDirtFloor+1, toChange);
	}
	
	
	private void addCharColAt(int x, int y, char[][][] toChange, char toAdd) {
		for (int currHeight = 0; currHeight < this.height; currHeight++) {
			toChange[y][x][currHeight] = toAdd;
		}
	}
	
	private boolean fairCoinFlip() {
		return this.rand.nextFloat() < .5;
	}
	
	private boolean allCharactersAtCol(char charToCheck, int x, int y, char[][][] toCheck) {
		boolean toReturn = true;
		for (int currHeight = 0; currHeight < this.height; currHeight++) {
			toReturn = toReturn && (toCheck[y][x][currHeight] == charToCheck);
		}
		return toReturn;
	}
	
	private void runCharColWalk(char[][][] toChange, int startX, int startY, int startXChange, int startYChange, char charToAdd) {
		int currX = startX;
		int currY = startY;
		int xChange = startXChange;
		int yChange = startYChange;
		
		//Random walk until off map or doubled back on self or other walk (a la snake)
				while (ActionHelpers.withinMapAt(currX, currY, 0, this.cols, this.rows, this.height) && !allCharactersAtCol(charToAdd, currX, currY, toChange)) {
					//Add hole
					this.addCharColAt(currX, currY, toChange, charToAdd);
					
					//Change direction with some probability
					if (rand.nextFloat() < WorldGenerator.probTrenchChangeDirection) {
						if (this.fairCoinFlip()) {
							//Horizontal change
							yChange = 0;
							if (this.fairCoinFlip()) {
								xChange = 1;
								
							}
							else {
								xChange = -1;
							}
						}
						else {
							//Vertical change
							xChange = 0;
							if (this.fairCoinFlip()) {
								yChange = 1;
							}
							else {
								yChange = -1;
							}
						}
					}
						currX += xChange;
						
						currY += yChange;
				}
	}
	
	private void randomWalkInsertOfCharacterColumns(char[][][] toChange, char toInsert) {
	
		boolean startingBotOrTop = this.fairCoinFlip();
		
		int startX = 0;
		int startY = 0;
		int startXChange = 0;
		int startYChange = 0;
		
		if (startingBotOrTop) {
			startX = rand.nextInt(this.cols);
			// Starting at top
			if (this.fairCoinFlip()) {
				startYChange = 1;
				startY = 0;
			}
			//Starting at bottom
			else {
				startYChange = -1;
				startY = this.rows-1;
			}
		}
		else {
			startY = rand.nextInt(this.rows);
			// Starting at left
			if (this.fairCoinFlip()) {
				startXChange = 1;
				startX = 0;
			}
			//Starting at right
			else {
				startXChange = -1;
				startX = this.cols-1;
			}
		}
		
		//Do the trench walk
		runCharColWalk(toChange, startX, startY, startXChange, startYChange, toInsert);
	}
	
	private void addTrenches(int numTrenches, char[][][] toChange) {
		for (int trenchIndex = 0; trenchIndex < numTrenches; trenchIndex++) {
			this.randomWalkInsertOfCharacterColumns(toChange, NameSpace.CHAREMPTY);
		}
	}
	
	private void addWalls(int numWalls, char[][][] toChange) {
		for (int trenchIndex = 0; trenchIndex < numWalls; trenchIndex++) {
			this.randomWalkInsertOfCharacterColumns(toChange, NameSpace.CHARDIRTBLOCK);
		}
	}
	
	private void addAgent(char [][][] toChange) {
		assert(WorldGenerator.depthOfDirtFloor+1 < this.height);
		int[] headLocation = addCharRandomly(NameSpace.CHARAGENT, null, null, WorldGenerator.depthOfDirtFloor+1, toChange);
		toChange[headLocation[1]][headLocation[0]][headLocation[2]-1] = NameSpace.CHARAGENTFEET;
	}
	
	private char[][][] generateNewCharArray(int numTrenches, int numWalls) {
		char[][][] toReturn = new char[this.rows][this.cols][this.height];
		//Initialize empty
		this.emptifyCharArray(toReturn);
		
		//Add dirt floor
		this.addDirtFloor(toReturn);
		
		//Add trench
		this.addTrenches(numTrenches, toReturn);
		
		//Add walls
		this.addWalls(numWalls, toReturn);
		
		//Add goal
		this.addRandomSpatialGoal(toReturn);
		
		//Add agent
		this.addAgent(toReturn);
		
		return toReturn;
	}
	
	private void randomizeMap(int numTrenches, int numWalls) {
		this.charArray = generateNewCharArray(numTrenches, numWalls);
	}
	
	public char[][][] getCurrCharArray() {
		return this.charArray;
	}
	
	
	public String getCurrMapAsString() {
		StringBuilder sb = new StringBuilder();
		
		for (int currHeight = this.height-1; currHeight >= 0; currHeight--) {
			for(int row = 0; row < this.rows; row++) {
				for(int col = 0; col < this.cols; col++) {
					char currChar = this.charArray[row][col][currHeight];
					sb.append(currChar);
				}
				if (!(row == this.rows-1)) {
					sb.append(NameSpace.rowSeparator);
				}
				else {
					if (!(currHeight == 0))
						sb.append(NameSpace.planeSeparator);
				}
			}
		}
		return sb.toString();
	}
	
	
	public static void main(String[] args) {
		String fileName = "src/minecraft/maps/testingWorld.map";
		WorldGenerator generator = new WorldGenerator(2, 2, 3);
		generator.randomizeMap(1, 1);
		String map = generator.getCurrMapAsString();
		System.out.println(map);
	}

}
