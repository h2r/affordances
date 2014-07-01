package minecraft.WorldGenerator;

import java.util.HashMap;
import java.util.Random;

import minecraft.MapIO;
import minecraft.NameSpace;
import minecraft.MinecraftDomain.Helpers;

public class WorldGenerator {

  //Class variables
  final int rows;
  final int cols;
  final int height;
  
  int depthOfDirtFloor = 1;
  double probTrenchChangeDirection = .2;
  
  char[][][] charArray;
  HashMap<String, Integer> headerInfo;
  
  Random rand;
  
  /**
   * 
   * @param rows
   * @param cols
   * @param height
   */
  public WorldGenerator(int rows, int cols, int height) {
    this.rows = rows;
    this.cols = cols;
    this.height = height;
    this.rand = new Random();
  }
  
  /**
   * 
   * @param rows
   * @param cols
   * @param height
   * @param depthOfDirtFloor
   * @param probTrenchChangeDir
   */
  public WorldGenerator(int rows, int cols, int height, int depthOfDirtFloor, double probTrenchChangeDir) {
    this.rows = rows;
    this.cols = cols;
    this.height = height;
    this.rand = new Random();
    this.depthOfDirtFloor = depthOfDirtFloor;
    this.probTrenchChangeDirection = probTrenchChangeDirection;
  }
  
  protected void emptifyCharArray(char [][][] toChange) {
    for(int row = 0; row < this.rows; row++) {
      for(int col = 0; col < this.cols; col++) {
        for(int currHeight = 0; currHeight < this.height; currHeight++) {
          toChange[row][col][currHeight] = NameSpace.CHAREMPTY;
        }
      }
    }
  }
  
  protected void addIndestFloor(char [][][] toChange) {
    assert(this.height >= depthOfDirtFloor);
    for (int currHeight = 0; currHeight < depthOfDirtFloor; currHeight++) {
      for(int row = 0; row < this.rows; row++) {
        for(int col = 0; col < this.cols; col++) {
          toChange[row][col][currHeight] = NameSpace.CHARINDBLOCK;
        }
      }
    }
  }
  
  private int[] addCharRandomly(char toAdd, Integer x, Integer y, Integer z, char[][][] toChange) {
    // Randomly add the given char toAdd to the map so that it does not conflict with already placed characters
	return addCharRandomlyHelper(toAdd,x,y,z,toChange,0);
  }
  
  private int[] addCharRandomlyHelper(char toAdd, Integer x, Integer y, Integer z, char[][][] toChange, int counter) {
  	// If we tried placing a reasonable number of times and failed, exit.
	try {
		if(counter > this.rows*this.cols*this.height) {
			throw new WorldIsTooSmallException();
	    }
  	}
  	catch (WorldIsTooSmallException e) {
		e.printStackTrace();
		System.exit(0);
	}
  	
	//Randomize any unspecifed coordinate
	Integer nx = x;
	Integer ny = y;
	Integer nz = z;
	  
	if (nx == null) {
	  nx = this.rand.nextInt(this.cols);
	}
	
	if (ny == null) {
	  ny = this.rand.nextInt(this.rows);
	}
	
	if (nz == null) {
	  nz = this.rand.nextInt(this.height);
	}
	
	if(toChange[ny][nx][nz] != NameSpace.CHAREMPTY && toChange[ny][nx][nz] != toAdd) {
		return addCharRandomlyHelper(toAdd, x, y, z, toChange, ++counter);
	}
	else {
		toChange[ny][nx][nz] = toAdd;
		return new int[]{nx,ny,nz};
	}
  }
  
  protected void addRandomSpatialGoal(char[][][] toChange) {
    assert(this.depthOfDirtFloor+1 < this.height);
    addCharRandomly(NameSpace.CHARGOAL, null, null, this.depthOfDirtFloor+1, toChange);
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
        while (Helpers.withinMapAt(currX, currY, 0, this.cols, this.rows, this.height) && !allCharactersAtCol(charToAdd, currX, currY, toChange)) {
          //Add hole
          this.addCharColAt(currX, currY, toChange, charToAdd);
          
          //Change direction with some probability
          if (rand.nextFloat() < this.probTrenchChangeDirection) {
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
  
  protected void addTrenches(int numTrenches, char[][][] toChange) {
    for (int trenchIndex = 0; trenchIndex < numTrenches; trenchIndex++) {
      this.randomWalkInsertOfCharacterColumns(toChange, NameSpace.CHAREMPTY);
    }
  }
  
  protected void addGoldOre(char[][][] toChange) {
	  this.addCharRandomly(NameSpace.CHARGOLDBLOCK, null, null, this.depthOfDirtFloor, toChange);
  }
  
  protected void addFurnace(char[][][] toChange) {
	  this.addCharRandomly(NameSpace.CHARFURNACE, null, null, this.depthOfDirtFloor, toChange);
  }
  
  protected void addWalls(int numWalls, char[][][] toChange) {
    for (int trenchIndex = 0; trenchIndex < numWalls; trenchIndex++) {
      this.randomWalkInsertOfCharacterColumns(toChange, NameSpace.CHARDIRTBLOCK);
    }
  }
  
  protected void addAgent(char [][][] toChange) {
    assert(this.depthOfDirtFloor+1 < this.height);
    // Add agent's head
    int[] headLocation = addCharRandomly(NameSpace.CHARAGENT, null, null, this.depthOfDirtFloor+1, toChange);
    
    // Add agent's feet
    	toChange[headLocation[1]][headLocation[0]][headLocation[2]-1] = NameSpace.CHARAGENTFEET;
    	
  }
  
  private char[][][] generateNewCharArray(int goal, int numTrenches, int numWalls) {
    char[][][] toReturn = new char[this.rows][this.cols][this.height];
    //Initialize empty
    this.emptifyCharArray(toReturn);
    
    //Add dirt floor
    this.addIndestFloor(toReturn);
    
    //Add trench
    this.addTrenches(numTrenches, toReturn);
    
    //Add agent
    this.addAgent(toReturn);
    
    //Add walls
//    this.addWalls(numWalls, toReturn);
    
    //Add goal
    if (goal == NameSpace.INTXYZGOAL) {
    	this.addRandomSpatialGoal(toReturn);
    }
    
    //Add gold blocks
    if (goal == NameSpace.INTGOLDBARGOAL || goal == NameSpace.INTGOLDOREGOAL) {
    	addGoldOre(toReturn);
    }
    
    //Add furnace
    if (goal == NameSpace.INTGOLDBARGOAL) {
    	addFurnace(toReturn);
    }
    
    

    
    return toReturn;
  }
  
  private HashMap<String, Integer> generateHeaderInfo(int goal, int numTrenches) {
	  HashMap<String, Integer> toReturn = new HashMap<String, Integer>();
	  
	  //Goal
	  toReturn.put(Character.toString(NameSpace.CHARGOALDESCRIPTOR), goal);
	  //Starting ore
	  toReturn.put(Character.toString(NameSpace.CHARSTARTINGGOLDORE), 0);
	  //Starting gold bars
	  toReturn.put(Character.toString(NameSpace.CHARSTARTINGGOLDBAR), 0);
	  //Placeable blocks
	  if (this.depthOfDirtFloor > 1) {
		  toReturn.put(Character.toString(NameSpace.CHARPLACEABLEBLOCKS), numTrenches);
	  }
	  else {
		  toReturn.put(Character.toString(NameSpace.CHARPLACEABLEBLOCKS), 0);
	  }
	  
	  
	  return toReturn;
  }
  
  /**
   * 
   * @param goal
   * @param numTrenches
   * @param numWalls
   */
  public void randomizeMap(int goal, int numTrenches, int numWalls) {
    this.charArray = generateNewCharArray(goal, numTrenches, numWalls);
    this.headerInfo = generateHeaderInfo(goal, numTrenches);
  }
  
  public char[][][] getCurrCharArray() {
    return this.charArray;
  }
  
  public void setCharArray(char[][][] newCharArray) {
	  this.charArray = newCharArray;
  }
  
  
  public String getCurrMapIOAsString() {
	  return getCurrMapIO().toString();
  }
  
  public MapIO getCurrMapIO() {
	  return new MapIO(this.headerInfo, this.charArray);
	  
  }
  
  public static void main(String[] args) {
    String fileName = "src/minecraft/maps/testingWorld.map";
    WorldGenerator generator = new WorldGenerator(1, 1, 4);
    generator.randomizeMap(NameSpace.INTGOLDOREGOAL,1, 1);
    String map = generator.getCurrMapIOAsString();
    System.out.println(map);
  }
}
