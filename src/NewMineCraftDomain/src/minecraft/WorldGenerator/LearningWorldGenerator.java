package minecraft.WorldGenerator;

import burlap.oomdp.logicalexpressions.LogicalExpression;

import java.util.Random;

import minecraft.NameSpace;

public class LearningWorldGenerator extends WorldGenerator {
	private static Random 	r = new Random();
	private static int 		maxTrenches = 2;
	private static int 		depthOfDirtFloor = 2;
	private static double 	probOfTrenchChangeDir = 0; // Straight trenches for now
	private 	   int	 	numTrenches = 1;
	
	public LearningWorldGenerator(int rows, int cols, int height) {
		super(rows, cols, height, depthOfDirtFloor, probOfTrenchChangeDir);
	}

	public char[][][] generateMap(LogicalExpression goalDescription) {
		char[][][] toReturn = new char[this.rows][this.cols][this.height];
		// Initialize empty
		this.emptifyCharArray(toReturn);
		
		// Add dirt floor
		this.addIndestFloor(toReturn);
		
		
		//Add agent
		this.addAgent(toReturn);
		
		// Add trench
		if(goalDescription.toString().contains("trench")) {
			this.addTrenches(this.numTrenches, toReturn);
		}
		
		// Add trench
		if(goalDescription.toString().contains("Gold")) {
			// Add gold stuff
			this.addGoldOre(toReturn);
			this.addFurnace(toReturn);
		} else {
			//Add goal
			this.addRandomSpatialGoal(toReturn);			
		}
				
		return toReturn;
	}

}
