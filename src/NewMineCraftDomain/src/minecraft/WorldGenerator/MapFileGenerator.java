package minecraft.WorldGenerator;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import burlap.oomdp.logicalexpressions.LogicalExpression;
import minecraft.MapIO;
import minecraft.WorldGenerator.Exceptions.RandomMapGenerationException;
import minecraft.WorldGenerator.WorldTypes.DeepTrenchWorld;
import minecraft.WorldGenerator.WorldTypes.PlaneGoalShelfWorld;
import minecraft.WorldGenerator.WorldTypes.PlaneGoldMineWorld;
import minecraft.WorldGenerator.WorldTypes.MinecraftWorld;
import minecraft.WorldGenerator.WorldTypes.PlaneGoldSmeltWorld;
import minecraft.WorldGenerator.WorldTypes.PlaneTowerWorld;
import minecraft.WorldGenerator.WorldTypes.PlaneWallWorld;
import minecraft.WorldGenerator.WorldTypes.PlaneWorld;

public class MapFileGenerator {
	int rows;
	int cols;
	int height;
	WorldGenerator worldGenerator;
	String directoryPath;
	
	public MapFileGenerator(int rows, int cols, int height, String directoryPath) {
		this.worldGenerator = new WorldGenerator(rows, cols, height);
		this.directoryPath = directoryPath;
		
	}
	
	
	/**
	 * 
	 * @param numMaps
	 * @param goal
	 * @param floorDepth
	 * @param floorOf
	 * @param numTrenches
	 * @param trenchesStraightAndBetweenAgentAndGoal
	 * @param numWalls
	 * @param wallOf
	 * @param wallsStraightAndBetweenAgentAndGoal
	 * @param depthOfGoldOre
	 * @param baseFileName
	 */
	public void generateNMaps(MinecraftWorld minecraftWorld, String baseFileName, int numMaps) {
		System.out.println("Generating " + baseFileName + " maps...");
		List<MapIO> toWriteToFile = new ArrayList<MapIO>();
		
		for (int i = 0; i < numMaps; i++) {
			
			try {
				this.worldGenerator.randomizeMap(minecraftWorld.getGoal(), minecraftWorld.getFloorOf(), 
						minecraftWorld.getNumTrenches(), minecraftWorld.getTrenchStraightAndBetweenAgentAndGoal(),
						minecraftWorld.getNumWalls(), minecraftWorld.getWallOf(), minecraftWorld.getwallsStraightAndBetweenAgentAndGoal(),
						minecraftWorld.getDepthOfGoldOre(), minecraftWorld.getFloorDepth(), minecraftWorld.getNumPlaceBlocks(),
						minecraftWorld.getGoalShelfHeight(), minecraftWorld.getNumLava());
			} catch (RandomMapGenerationException e) {
				System.out.println("\tCouldn't make one of the maps: " + e.toString());
			}
			MapIO currIO = this.worldGenerator.getCurrMapIO();
			toWriteToFile.add(currIO);
		}
		
		int i = 0;
		for (MapIO currIO : toWriteToFile) {
			currIO.printHeaderAndMapToFile(this.directoryPath + baseFileName + i++ + ".map");
		}
			
	}
	
	/**
	 * 
	 * @param numMaps
	 * @param minecraftWorld
	 */
	public void generateNMaps(int numMaps, MinecraftWorld minecraftWorld) {
		String baseFileName = minecraftWorld.getName();
		
		System.out.println("Generating " + baseFileName + " maps...");
		List<MapIO> toWriteToFile = new ArrayList<MapIO>();
		
		for (int i = 0; i < numMaps; i++) {
						
			try {
				this.worldGenerator.randomizeMap(minecraftWorld.getGoal(), minecraftWorld.getFloorOf(), minecraftWorld.getNumTrenches(),
						minecraftWorld.getTrenchStraightAndBetweenAgentAndGoal(), minecraftWorld.getNumWalls(),
						minecraftWorld.getWallOf(), minecraftWorld.getwallsStraightAndBetweenAgentAndGoal(), minecraftWorld.getDepthOfGoldOre(),
						minecraftWorld.getFloorDepth(), minecraftWorld.getNumPlaceBlocks(), minecraftWorld.getGoalShelfHeight(), minecraftWorld.getNumLava());
			} catch (RandomMapGenerationException e) {
				System.out.println("\tCouldn't make one of the maps: " + e.toString());
			}
			MapIO currIO = this.worldGenerator.getCurrMapIO();
			toWriteToFile.add(currIO);
		}
		
		int i = 0;
		for (MapIO currIO : toWriteToFile) {
			currIO.printHeaderAndMapToFile(this.directoryPath + baseFileName + i++ + ".map");
		}
			
	}
	
	/**
	 * Randomly generates N worlds of the specified world type of the given dimensions
	 * @param numMaps: number of maps to generate
	 * @param minecraftWorld: the world type to create
	 * @param rows: dimensions
	 * @param cols: dimensions
	 * @param height: dimensions
	 */
	public void generateNMaps(int numMaps, MinecraftWorld minecraftWorld, int rows, int cols, int height) {
		String baseFileName = minecraftWorld.getName();
		
		System.out.println("Generating " + baseFileName + " maps...");
		List<MapIO> toWriteToFile = new ArrayList<MapIO>();
		WorldGenerator worldGen = new WorldGenerator(rows, cols, height);
		for (int i = 0; i < numMaps; i++) {		
			try {
				
				worldGen.randomizeMap(minecraftWorld.getGoal(), minecraftWorld.getFloorOf(), minecraftWorld.getNumTrenches(),
						minecraftWorld.getTrenchStraightAndBetweenAgentAndGoal(), minecraftWorld.getNumWalls(),
						minecraftWorld.getWallOf(), minecraftWorld.getwallsStraightAndBetweenAgentAndGoal(), minecraftWorld.getDepthOfGoldOre(),
						minecraftWorld.getFloorDepth(), minecraftWorld.getNumPlaceBlocks(), minecraftWorld.getGoalShelfHeight(), 
						minecraftWorld.getNumLava());
			} catch (RandomMapGenerationException e) {
				System.out.println("\tCouldn't make one of the maps: " + e.toString());
			}
			MapIO currIO = worldGen.getCurrMapIO();
			toWriteToFile.add(currIO);
		}
		
		int i = 0;
		for (MapIO currIO : toWriteToFile) {
			currIO.printHeaderAndMapToFile(this.directoryPath + baseFileName + i++ + ".map");
		}
			
	}
	
	/**
	 * Maps a logical expression representing this map's goal to the int that represents that goal type.
	 * @param lgd
	 * @return
	 */
	private int goalToGoalNum(LogicalExpression lgd) {
		int result;

		if(lgd.toString().equals("AtGoal")) {
			result = 0;
		}
		else if(lgd.toString().equals("AgentHasXGoldOre")) {
			result = 1;
		}
		else if(lgd.toString().equals("AgentHasXGoldBlock")) {
			result = 2;
		}
		else {
			result = 3;
		}
		
		return result;
	}
	
	/**
	 * Deletes all of the map files in the current directoryPath (used in test pipeline)
	 */
	public void clearMapsInDirectory() {
		File mapDir = new File(this.directoryPath);
		String[] mapsToDelete = mapDir.list();
	    if(mapsToDelete != null){
	    	for (int i = 0; i < mapsToDelete.length; i++) {  
	    		File map = new File(mapDir, mapsToDelete[i]);
	    		map.delete();  
	    	}
	    }
	}
	
	public static void main(String[] args) {
		String filePath = "src/minecraft/maps/randomMaps/";
		MapFileGenerator test = new MapFileGenerator(3,3,5,filePath);
		
		// Constant map parameters
		int numMaps = 1;
		int numLava = 0;
		
		test.generateNMaps(numMaps, new DeepTrenchWorld(1, numLava));
		test.generateNMaps(numMaps, new PlaneGoldMineWorld(numLava));
		test.generateNMaps(numMaps, new PlaneGoldSmeltWorld(numLava));
		test.generateNMaps(numMaps, new PlaneTowerWorld(2, numLava));
		test.generateNMaps(numMaps, new PlaneWallWorld(1, numLava));
		test.generateNMaps(numMaps, new PlaneWorld(numLava));
		test.generateNMaps(numMaps, new PlaneGoalShelfWorld(2,1, numLava));
	}

}
