package tests;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import minecraft.MapIO;
import minecraft.MinecraftBehavior;
import minecraft.NameSpace;
import minecraft.WorldGenerator.MapFileGenerator;
import minecraft.WorldGenerator.WorldTypes.DeepTrenchWorld;
import minecraft.WorldGenerator.WorldTypes.PlaneGoalShelfWorld;
import minecraft.WorldGenerator.WorldTypes.PlaneGoldMineWorld;
import minecraft.WorldGenerator.WorldTypes.PlaneGoldSmeltWorld;
import minecraft.WorldGenerator.WorldTypes.PlaneWallWorld;
import minecraft.WorldGenerator.WorldTypes.PlaneWorld;
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
		
		MapFileGenerator mapMaker = new MapFileGenerator(1, 3, 5, testMapDir);
		
		// Get rid of old maps
		mapMaker.clearMapsInDirectory();
		
		int numLavaBlocks = 1;
		
		mapMaker.generateNMaps(numMaps, new DeepTrenchWorld(1, numLavaBlocks), 3, 3, 4);
		mapMaker.generateNMaps(numMaps, new PlaneGoldMineWorld(numLavaBlocks), 3, 3, 4);
		mapMaker.generateNMaps(numMaps, new PlaneGoldSmeltWorld(numLavaBlocks), 3, 3, 4);
		mapMaker.generateNMaps(numMaps, new PlaneWallWorld(1, numLavaBlocks), 2, 3, 4);
		mapMaker.generateNMaps(numMaps, new PlaneWorld(numLavaBlocks) , 6, 6, 4);
		mapMaker.generateNMaps(numMaps, new PlaneGoalShelfWorld(2,1, numLavaBlocks), 3, 3, 5);
		
	}
	
	/**
	 * Run tests on all world types, for each of the 3 planners and record results
	 * @param numIterations: the number of times to perform testing
	 * @param nametag: a flag for the name of the results file
	 */
	public static void runMinecraftTests(int numMapsPerGoal, String nametag, boolean shouldLearn, List<String> planners, boolean addOptions, boolean addMacroActions) {
		
		// Create behavior and test maps
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
			// TODO: improve method of selecting number of worlds to learn with (currently set to 5)
			learnedKBName = AffordanceLearner.generateMinecraftKB(mcBeh, 5, false);
		}
		else {
			learnedKBName = "test50.kb";
		}
		
		// Hard Learned KB
		KnowledgeBase learnedHardAffKB = new KnowledgeBase();
		if(planners.contains(NameSpace.LearnedHardRTDP) || planners.contains(NameSpace.LearnedHardVI)) {
			learnedHardAffKB.loadHard(mcBeh.getDomain(), learnedKBName);
		}
		
		// Soft Learned KB
		KnowledgeBase learnedSoftAffKB = new KnowledgeBase();;
		if(planners.contains(NameSpace.LearnedSoftRTDP) || planners.contains(NameSpace.LearnedSoftVI)) {
			learnedSoftAffKB.loadSoft(mcBeh.getDomain(), learnedKBName);
		}
		
		// --- FILE WRITER SETUP ---
		String outputFileName= "src/tests/results/" + nametag;
		if(addOptions){
			outputFileName = outputFileName + "_opt";
		}
		if(addMacroActions){
			outputFileName = outputFileName + "_ma";
		}
		outputFileName = outputFileName + "_results.txt";
		File resultsFile = new File(outputFileName);
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
					rtdpResults.addTrial(mcBeh.RTDP(addOptions, addMacroActions));
					statusBW.write(" Finished\n");
					statusBW.flush();
				}
				
				// Expert RTDP
				if(planners.contains(NameSpace.ExpertRTDP)) {
					statusBW.write("\t...Expert RTDP");
					statusBW.flush();
					expertRTDPResults.addTrial(mcBeh.AffordanceRTDP(expertAffKB, addOptions, addMacroActions));
					statusBW.write(" Finished\n");
					statusBW.flush();
				}
				
				// Learned Hard RTDP
				if(planners.contains(NameSpace.LearnedHardRTDP)) {
					statusBW.write("\t...LearnedHard RTDP");
					statusBW.flush();
					learnedHardRTDPResults.addTrial(mcBeh.AffordanceRTDP(learnedHardAffKB, addOptions, addMacroActions));
					statusBW.write(" Finished\n");
					statusBW.flush();
				}
				
				// Learned Soft RTDP
				if(planners.contains(NameSpace.LearnedSoftRTDP)) {
					statusBW.write("\t...Learned Soft RTDP");
					statusBW.flush();
					learnedSoftRTDPResults.addTrial(mcBeh.AffordanceRTDP(learnedSoftAffKB, addOptions, addMacroActions));
					statusBW.write(" Finished\n");
					statusBW.flush();
				}
				
				// VI
				if(planners.contains(NameSpace.VI)) {
					statusBW.write("\t...VI");
					statusBW.flush();
					viResults.addTrial(mcBeh.ValueIterationPlanner(addOptions, addMacroActions));
					statusBW.write(" Finished\n");
					statusBW.flush();
				}
				
				// Expert VI
				if(planners.contains(NameSpace.ExpertVI)) {
					statusBW.write("\t...Expert VI");
					statusBW.flush();
					expertVIResults.addTrial(mcBeh.AffordanceVI(expertAffKB, addOptions, addMacroActions));
					statusBW.write(" Finished\n");
					statusBW.flush();
				}
				
				// Learned Hard VI
				if(planners.contains(NameSpace.LearnedHardVI)) {
					statusBW.write("\t...Learned Hard VI");
					statusBW.flush();
					learnedHardVIResults.addTrial(mcBeh.AffordanceVI(learnedHardAffKB, addOptions, addMacroActions));
					statusBW.write(" Finished\n");
					statusBW.flush();
				}
				statusBW.write("mapCounter: " + mapCounter + "\n");
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
	
	/**
	 * Run tests on all world types, for each of the 3 planners and record results
	 * @param numIterations: the number of times to perform testing
	 * @param nametag: a flag for the name of the results file
	 */
	public static void runLearningRateTests(String nametag, int numMapsPerGoal, int maxNumLearningWorlds, int learningIncrement, boolean shouldLearn) {
		// Generate Behavior and test maps
		MinecraftBehavior mcBeh = new MinecraftBehavior("src/minecraft/maps/template.map");
		String testMapDir = "src/minecraft/maps/learningRateTest/";
		generateTestMaps(mcBeh, testMapDir, numMapsPerGoal);
		System.out.println("------------after generation");
		
		File testDir = new File(testMapDir);
		String[] testMaps = testDir.list();
		
		List<String> kbNames = new ArrayList<String>();
		
		if(shouldLearn){
			// --- Create Knowledge Bases ---
			for(int numWorldsToLearn = 0; numWorldsToLearn < maxNumLearningWorlds; numWorldsToLearn = numWorldsToLearn + learningIncrement) {
				// Learn if we're supposed to learn a new KB
				String learnedKBName = AffordanceLearner.generateMinecraftKB(mcBeh, numWorldsToLearn, true);
				kbNames.add(learnedKBName);
			}
		}
		else{
			// If we're not learning new knowledge bases, use the existing ones.
			File learningRateKBDir = new File("src/minecraft/kb/learning_rate/");
			String[] kbsToUse = learningRateKBDir.list();
			kbNames = new ArrayList<String>(Arrays.asList(kbsToUse));
		}
		
		// Make knowledge base and result objects
		KnowledgeBase affKB = new KnowledgeBase();
		Result rtdpResults = new Result(NameSpace.RTDP);
		Result expertRTDPResults = new Result(NameSpace.ExpertRTDP);
		LearningRateResult learnedHardRTDPResults = new LearningRateResult(NameSpace.LearnedHardRTDP);
		LearningRateResult learnedSoftRTDPResults = new LearningRateResult(NameSpace.LearnedSoftRTDP);
		
		// Run Expert and Vanilla planners
		for (String map : testMaps) {
			for(int i = 0; i < numMapsPerGoal; i++) {
				MapIO mapIO = new MapIO(testMapDir + map);
				mcBeh.updateMap(mapIO);
				
				// Vanilla
				rtdpResults.addTrial(mcBeh.RTDP(false, false));
				
				// Expert
				affKB.loadHard(mcBeh.getDomain(), "expert.kb");
				expertRTDPResults.addTrial(mcBeh.AffordanceRTDP(affKB, false, false));
			}
		}
		
		// Run learning planners with varied size KBs
		for(String kbName : kbNames) {
			
			for (String map : testMaps) {
				MapIO mapIO = new MapIO(testMapDir + map);
				mcBeh.updateMap(mapIO);
				
				// Hard
				affKB.loadHard(mcBeh.getDomain(), kbName);
				learnedHardRTDPResults.addTrialForKB(kbName, mcBeh.AffordanceRTDP(affKB, false, false));
				
				// Soft
				affKB.loadSoft(mcBeh.getDomain(), kbName);
				learnedSoftRTDPResults.addTrialForKB(kbName, mcBeh.AffordanceRTDP(affKB, false, false));
			}
		}
		
		// Record results to file

		try {
			File resultsFile = new File("src/tests/results/learning_rate/" + nametag + "_lr.txt");
			FileWriter resultsFW = new FileWriter(resultsFile.getAbsoluteFile());
			BufferedWriter resultsBW = new BufferedWriter(resultsFW);
			
			resultsBW.write("Learning Rate Results: 0-" + maxNumLearningWorlds + "\n");
			resultsBW.write(rtdpResults.toString() + "\n");
			resultsBW.write(expertRTDPResults.toString() + "\n");
			
			resultsBW.write("-- hard --\n");
			for(String kbName : learnedHardRTDPResults.getResults().keySet()) {
				resultsBW.write("\t [hard." + kbName + "]\t" + learnedHardRTDPResults.getResults().get(kbName).toString() + "\n");
			}
			resultsBW.write("-- soft --\n");
			for(String kbName : learnedSoftRTDPResults.getResults().keySet()) {
				resultsBW.write("\t [soft." + kbName + "]\t" + learnedSoftRTDPResults.getResults().get(kbName).toString() + "\n");
			}
			resultsBW.flush();
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
//		System.out.close();
		
		// --- Basic Minecraft Results ---
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
		
		boolean addOptions = false;
		boolean addMacroActions = false;
//		runMinecraftTests(3, "3", learningFlag, planners, addOptions, addMacroActions);
		
		
		// --- Learning Rate Results ---
		boolean shouldLearn = false;
		int numMapsPerGoalTest = 1;
		int maxKBSize = 20;
		int kbSizeIncrement = 5;
		runLearningRateTests("upto20", numMapsPerGoalTest, maxKBSize, kbSizeIncrement, shouldLearn);
	}

}
