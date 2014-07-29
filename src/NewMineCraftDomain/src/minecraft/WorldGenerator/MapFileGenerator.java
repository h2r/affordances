package minecraft.WorldGenerator;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import minecraft.MapIO;
import minecraft.NameSpace;
import minecraft.WorldGenerator.Exceptions.RandomMapGenerationException;
import minecraft.WorldGenerator.Exceptions.WorldIsTooSmallException;

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
	public void generateNMaps(int numMaps, int goal, int floorDepth, char floorOf, int numTrenches, boolean trenchesStraightAndBetweenAgentAndGoal, int numWalls, char wallOf, boolean wallsStraightAndBetweenAgentAndGoal, Integer depthOfGoldOre, String baseFileName) {
		System.out.println("Generating " + baseFileName + " maps...");
		List<MapIO> toWriteToFile = new ArrayList<MapIO>();
		
		for (int i = 0; i < numMaps; i++) {
			
			try {
				this.worldGenerator.randomizeMap(goal, floorOf, numTrenches, trenchesStraightAndBetweenAgentAndGoal, numWalls, wallOf, wallsStraightAndBetweenAgentAndGoal, depthOfGoldOre, floorDepth);
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
	 * Deletes all of the map files in the current directoryPath (used in test pipeline)
	 */
	public void clearMapsInDirectory() {
		File mapDir = new File(this.directoryPath);       
		String[] mapsToDelete = mapDir.list();
	    
        for (int i = 0; i < mapsToDelete.length; i++) {  
            File map = new File(mapDir, mapsToDelete[i]);   
            map.delete();  
        }  
	}
	
	public static void main(String[] args) {
		String filePath = "src/minecraft/maps/randomMaps/";
		MapFileGenerator test = new MapFileGenerator(4,4,4,filePath);
		
		//Constant map parameters
		int numMaps = 50;
		boolean trenchStraightAndBetweenAgentAndGoal = true;
		boolean wallsStraightAndBetweenAgentAndGoal = true;
		char wallOf = NameSpace.CHARDIRTBLOCKNOTPICKUPABLE;
		
		test.generateNMaps(numMaps, NameSpace.INTTOWERGOAL, 1, NameSpace.CHARDIRTBLOCKNOTPICKUPABLE, 0, trenchStraightAndBetweenAgentAndGoal, 0, wallOf, wallsStraightAndBetweenAgentAndGoal, null, "TowerPlaneWorld");
		test.generateNMaps(numMaps, NameSpace.INTGOLDOREGOAL, 2, NameSpace.CHARDIRTBLOCKNOTPICKUPABLE, 0, trenchStraightAndBetweenAgentAndGoal, 0, wallOf, wallsStraightAndBetweenAgentAndGoal, -2, "PlaneGoldMining");
		test.generateNMaps(numMaps, NameSpace.INTXYZGOAL, 1, NameSpace.CHARINDBLOCK, 0, trenchStraightAndBetweenAgentAndGoal, 1, NameSpace.CHARDIRTBLOCKPICKUPABLE, wallsStraightAndBetweenAgentAndGoal, null, "WallPlaneWorld");
		test.generateNMaps(numMaps, NameSpace.INTXYZGOAL, 2, NameSpace.CHARINDBLOCK, 1, trenchStraightAndBetweenAgentAndGoal, 0, wallOf, wallsStraightAndBetweenAgentAndGoal, null, "DeepTrenchWorld");
		test.generateNMaps(numMaps, NameSpace.INTGOLDBARGOAL, 1, NameSpace.CHARINDBLOCK, 0, trenchStraightAndBetweenAgentAndGoal, 0, wallOf, wallsStraightAndBetweenAgentAndGoal, 0, "PlaneGoldSmelting");

	}
	

}
