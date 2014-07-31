package tests;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
		
		MapFileGenerator mapMaker = new MapFileGenerator(1, 3, 4, testMapDir);
		
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
		// Build tower
		mapMaker.generateNMaps(numMaps, 3, 0, floorOf, numTrenches, straightTrench, numWalls, wallOf, straightWall, depthOfGoldOre, baseFileNames[4]);	
	}
	
	/**
	 * Run tests on all world types, for each of the 3 planners and record results
	 * @param numIterations: the number of times to perform testing
	 * @param nametag: a flag for the name of the results file
	 */
	public static void runTests(int numMapsPerGoal, String nametag, boolean shouldLearn, List<String> planners) {
		
		// Generate Behavior and test maps
		MinecraftBehavior mcBeh = new MinecraftBehavior("src/minecraft/maps/template.map");
		String testMapDir = "src/minecraft/maps/test/";
		generateTestMaps(mcBeh, testMapDir, numMapsPerGoal);
		File testDir = new File(testMapDir);
		String[] testMaps = testDir.list();
		
		// --- Create Knowledge Bases ---
		
		// Expert KB
		KnowledgeBase expertAffKB = new KnowledgeBase();
		if(planners.contains(NameSpace.ExpertRTDP) || planners.contains(NameSpace.ExpertVI)) {
			expertAffKB.loadHard(mcBeh.getDomain(), "expert.kb");
		}

		// Learn if we're supposed to learn a new KB
		String learnedKBName;
		if(shouldLearn) {
			learnedKBName = AffordanceLearner.generateMinecraftKB(mcBeh);
		}
		else {
			learnedKBName = "test5.kb";
		}
		
		// Hard Learned KB
		KnowledgeBase learnedHardAffKB = new KnowledgeBase();
		if(planners.contains(NameSpace.LearnedHardRTDP) || planners.contains(NameSpace.LearnedHardVI)) {
			learnedHardAffKB.loadHard(mcBeh.getDomain(), learnedKBName);
		}
		
		// Soft Learned KB
		KnowledgeBase learnedSoftAffKB = new KnowledgeBase();;
		if(planners.contains(NameSpace.LearnedSoftRTDP) || planners.contains(NameSpace.LearnedSoftVI)) {
			learnedSoftAffKB.load(mcBeh.getDomain(), learnedKBName);
		}
		
		// --- FILE WRITER SETUP ---
		File resultsFile = new File("src/tests/results/" + nametag + "_results.txt");
		BufferedWriter resultsBW;
		FileWriter resultsFW;
		
		File statusFile = new File("src/tests/results/status.txt");
		BufferedWriter statusBW;
		FileWriter statusFW;
		try {
			// Initialize Result objects and file writing objects
			resultsFW = new FileWriter(resultsFile.getAbsoluteFile());
			resultsBW = new BufferedWriter(resultsFW);
			statusFW = new FileWriter(statusFile.getAbsoluteFile());
			statusBW = new BufferedWriter(statusFW);
			int mapCounter = 0;
			Result rtdpResults = new Result(NameSpace.RTDP);
			Result expertRTDPResults = new Result(NameSpace.ExpertRTDP);
			Result learnedHardRTDPResults = new Result(NameSpace.LearnedHardRTDP);
			Result learnedSoftRTDPResults = new Result(NameSpace.LearnedSoftRTDP);
			Result viResults = new Result(NameSpace.VI);
			Result expertVIResults = new Result(NameSpace.ExpertVI);
			Result learnedHardVIResults = new Result(NameSpace.LearnedHardVI);
			
			// --- PLANNING ---
			
			// Loop over each map and solve for each planner given
			for(String map : testMaps) {
				System.out.println("Starting new map: " + map);
				statusBW.write("Running on map: " + map + "\n");
				statusBW.flush();
				// Update behavior with new map
				MapIO mapIO = new MapIO(testMapDir + map);
				mcBeh.updateMap(mapIO);
				// --- Plan for each planner given ---
				
				// RTDP
				if(planners.contains(NameSpace.RTDP)) {
					statusBW.write("\t...RTDP");
					statusBW.flush();
					rtdpResults.addTrial(mcBeh.RTDP());
					statusBW.write(" Finished\n");
					statusBW.flush();
				}
				
				// Expert RTDP
				if(planners.contains(NameSpace.ExpertRTDP)) {
					statusBW.write("\t...Expert RTDP");
					statusBW.flush();
					expertRTDPResults.addTrial(mcBeh.AffordanceRTDP(expertAffKB));
					statusBW.write(" Finished\n");
					statusBW.flush();
				}
				
				// Learned Hard RTDP
				if(planners.contains(NameSpace.LearnedHardRTDP)) {
					statusBW.write("\t...LearnedHard RTDP");
					statusBW.flush();
					learnedHardRTDPResults.addTrial(mcBeh.AffordanceRTDP(learnedHardAffKB));
					statusBW.write(" Finished\n");
					statusBW.flush();
				}
				
				// Learned Soft RTDP
				if(planners.contains(NameSpace.LearnedSoftRTDP)) {
					statusBW.write("\t...Learned Soft RTDP");
					statusBW.flush();
					learnedSoftRTDPResults.addTrial(mcBeh.AffordanceRTDP(learnedSoftAffKB));
					statusBW.write(" Finished\n");
					statusBW.flush();
				}
				
				// VI
				if(planners.contains(NameSpace.VI)) {
					statusBW.write("\t...VI");
					statusBW.flush();
					viResults.addTrial(mcBeh.ValueIterationPlanner());
					statusBW.write(" Finished\n");
					statusBW.flush();
				}
				
				// Expert VI
				if(planners.contains(NameSpace.ExpertVI)) {
					statusBW.write("\t...Expert VI");
					statusBW.flush();
					expertVIResults.addTrial(mcBeh.AffordanceVI(expertAffKB));
					statusBW.write(" Finished\n");
					statusBW.flush();
				}
				
				// Learned Hard VI
				if(planners.contains(NameSpace.LearnedHardVI)) {
					statusBW.write("\t...Learned Hard VI");
					statusBW.flush();
					learnedHardVIResults.addTrial(mcBeh.AffordanceVI(learnedHardAffKB));
					statusBW.write(" Finished\n");
					statusBW.flush();
				}
				statusBW.write("\n");
				statusBW.flush();
				// --- RECORD RESULTS TO FILE ---
				if(mapCounter == numMapsPerGoal - 1) {
					resultsBW.write("map: " + map.substring(0, map.length() - 5) + "\n");
					if(planners.contains(NameSpace.RTDP)) {
						resultsBW.write("\t" + rtdpResults.toString() + "\n");
						rtdpResults.clear();
					}
					if(planners.contains(NameSpace.ExpertRTDP)) {
						resultsBW.write("\t" + expertRTDPResults.toString() + "\n");
						expertRTDPResults.clear();
					}
					if(planners.contains(NameSpace.LearnedHardRTDP)) {
						resultsBW.write("\t" + learnedHardRTDPResults.toString() + "\n");
						learnedHardRTDPResults.clear();
					}
					if(planners.contains(NameSpace.LearnedSoftRTDP)) {
						resultsBW.write("\t" + learnedSoftRTDPResults.toString() + "\n");
						learnedSoftRTDPResults.clear();
					}
					if(planners.contains(NameSpace.VI)) {
						resultsBW.write("\t" + viResults.toString() + "\n");
						viResults.clear();
					}
					if(planners.contains(NameSpace.ExpertVI)) {
						resultsBW.write("\t" + expertVIResults.toString() + "\n");
						expertVIResults.clear();
					}
					if(planners.contains(NameSpace.LearnedHardVI)) {
						resultsBW.write("\t" + learnedHardVIResults.toString() + "\n");
						learnedHardVIResults.clear();
					}
					resultsBW.flush();
					
					// Reset
					mapCounter = 0;
					continue;
				}
				
				mapCounter++;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	public static void main(String[] args) {
//		System.out.close();
		boolean learningFlag = false;
		
		// Choose which planners to collect results for
		List<String> planners = new ArrayList<String>();
		planners.add(NameSpace.RTDP);
		planners.add(NameSpace.ExpertRTDP);
		planners.add(NameSpace.LearnedHardRTDP);
		planners.add(NameSpace.LearnedSoftRTDP);
		planners.add(NameSpace.VI);
		planners.add(NameSpace.ExpertVI);
		planners.add(NameSpace.LearnedHardVI);
		
		runTests(10, "10allplanners", learningFlag, planners);
	}

}
