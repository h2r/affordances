package tests;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

import minecraft.MapIO;
import minecraft.MinecraftBehavior;
import minecraft.NameSpace;
import minecraft.WorldGenerator.MapFileGenerator;
import affordances.AffordanceLearner;
import affordances.KnowledgeBase;

public class MinecraftTestPipeline {

	public MinecraftTestPipeline() {
		// TODO Auto-generated constructor stub
	}
	
	/**
	 * Generates learning and testing maps and learns an affordance knowledge base.
	 * @param mb: MinecraftBehavior
	 * @param testMapDir: Directory to put testing maps in
	 * @param numMaps: Number of maps of each type to generate
	 */
	public static void generateTestMaps(MinecraftBehavior mb, String testMapDir, int numMaps) {
		// Generate learning maps and learn a KB (note: mb's map gets updated several times here)
		
		MapFileGenerator mapMaker = new MapFileGenerator(10, 10, 6, testMapDir);
		
		// Get rid of old maps
		mapMaker.clearMapsInDirectory();
		
		// Map parameters
		int floorDepth = 1;
		char floorOf = NameSpace.CHARINDBLOCK;
		int numTrenches = 0;
		boolean straightTrench = true;
		int numWalls = 0;
		char wallOf = NameSpace.CHARDIRTBLOCKNOTPICKUPABLE;
		boolean straightWall = true;
		int depthOfGoldOre = 1;
		String[] baseFileNames = {"DeepTrenchWorld", "WallPlaneWorld", "PlaneGoldMining", "PlaneGoldSmelting", "TowerPlaneWorld",};
		
		// Trench
		mapMaker.generateNMaps(numMaps, 0, 2, floorOf, 1, straightTrench, numWalls, wallOf, straightWall, depthOfGoldOre, baseFileNames[0]);
		// Wall
		mapMaker.generateNMaps(numMaps, 0, floorDepth, floorOf, numTrenches, straightTrench, 1, wallOf, straightWall, depthOfGoldOre, baseFileNames[1]);
		// Find gold ore
		mapMaker.generateNMaps(numMaps, 1, floorDepth, floorOf, numTrenches, straightTrench, numWalls, wallOf, straightWall, depthOfGoldOre, baseFileNames[2]);
		// Smelt gold bar
		mapMaker.generateNMaps(numMaps, 2, floorDepth, floorOf, numTrenches, straightTrench, numWalls, wallOf, straightWall, depthOfGoldOre, baseFileNames[3]);
//		// Build tower
//		mapMaker.generateNMaps(numMaps, 3, floorDepth, floorOf, numTrenches, straightTrench, numWalls, wallOf, straightWall, depthOfGoldOre, baseFileNames[4]);	
	}
	
	/**
	 * Run tests on all 5 world types, for each of the 3 planners and record results
	 * @param numIterations: the number of times to perform testing
	 * @param nametag: a flag for the name of the results file
	 */
	public static void runTests(int numMapsPerGoal, String nametag, boolean shouldLearn) {
		
		MinecraftBehavior mcBeh = new MinecraftBehavior("src/minecraft/maps/template.map");
		String testMapDir = "src/minecraft/maps/test/";
		generateTestMaps(mcBeh, testMapDir, numMapsPerGoal);
		
		// Solve each map with each planner (RTDP, EARTDP, LARTDP)
		File testDir = new File(testMapDir);
		String[] testMaps = testDir.list();
		
		// Expert KB
		KnowledgeBase expertAffKB = new KnowledgeBase();
		expertAffKB.loadHard(mcBeh.getDomain(), "expertTest.kb");

		// Learned KB
		String learnedKBName;
		if(shouldLearn) {
			learnedKBName = AffordanceLearner.generateMinecraftKB(mcBeh);
		}
		else {
			learnedKBName = "learned10.kb";
		}
		KnowledgeBase learnedAffKB = new KnowledgeBase();
		learnedAffKB.loadHard(mcBeh.getDomain(), learnedKBName);
		
		// Collect results and write to file
		File resultsFile = new File("src/tests/results/" + nametag + "_results.txt");
		BufferedWriter bw;
		FileWriter fw;
		try {
			fw = new FileWriter(resultsFile.getAbsoluteFile());
			bw = new BufferedWriter(fw);
			
			int mapCounter = 0;
			Result rtdpResults = new Result("RTDP");
			Result expertResults = new Result("Expert");
			Result learnedResults = new Result("Learned");
			
			for(String map : testMaps) {
				System.out.println("Starting new map: " + map);
				// Update behavior with new map
				MapIO mapIO = new MapIO(testMapDir + map);
				mcBeh.updateMap(mapIO);
				
				// Vanilla
				rtdpResults.addTrial(mcBeh.RTDP());
				
				// Expert
				expertResults.addTrial(mcBeh.AffordanceRTDP(expertAffKB));
				
				// Learned
				learnedResults.addTrial(mcBeh.AffordanceRTDP(learnedAffKB));
				
				// If we're finishing a single map type, average results and flush buffer
				if(mapCounter == numMapsPerGoal - 1) {
					bw.write("map: " + map.substring(0, map.length() - 5) + "\n");
					bw.write("\t" + rtdpResults.toString() + "\n");
					bw.write("\t" + expertResults.toString() + "\n");
					bw.write("\t" + learnedResults.toString() + "\n");
					bw.flush();
					
					// Reset results
					rtdpResults.clear();
					expertResults.clear();
					learnedResults.clear();
					
					// Reset
					mapCounter = 0;
					continue;
				}
				mapCounter++;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public static void main(String[] args) {
		System.out.close();
		boolean learningFlag = false;
		runTests(2, "2", learningFlag);
	}

}
