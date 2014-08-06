package tests;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import minecraft.MapIO;
import minecraft.NameSpace;
import minecraft.MinecraftBehavior.MinecraftBehavior;
import minecraft.MinecraftBehavior.Planners.AffordanceRTDPPlanner;
import minecraft.MinecraftBehavior.Planners.AffordanceVIPlanner;
import minecraft.MinecraftBehavior.Planners.RTDPPlanner;
import minecraft.MinecraftBehavior.Planners.VIPlanner;
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
		
		mapMaker.generateNMaps(numMaps, new DeepTrenchWorld(1, numLavaBlocks), 2, 3, 4);
		mapMaker.generateNMaps(numMaps, new PlaneGoldMineWorld(numLavaBlocks), 2, 3, 4);
		mapMaker.generateNMaps(numMaps, new PlaneGoldSmeltWorld(numLavaBlocks), 2, 3, 4);
		mapMaker.generateNMaps(numMaps, new PlaneWallWorld(1, numLavaBlocks), 2, 3, 4);
		mapMaker.generateNMaps(numMaps, new PlaneWorld(numLavaBlocks), 5, 5, 4);
		mapMaker.generateNMaps(numMaps, new PlaneGoalShelfWorld(2,1, numLavaBlocks), 3, 3, 5);
		
	}
	
	/**
	 * Run tests on all world types, for each of the planners and record results
	 * @param numMapsPerGoal
	 * @param nametag: name to indicate which results file corresponds to these tests
	 * @param shouldLearn: flag indicating whether or not to learn a new KB
	 * @param planners: a list of strings indicating which planners to run
	 * @param addOptions: a boolean indicating whether or not to add options to planners
	 * @param addMacroActions: a boolean indicating whether or not to add macroactions to planners
	 */
	public static void runMinecraftTests(int numMapsPerGoal, String nametag, boolean shouldLearn, List<String> planners, boolean addOptions, boolean addMacroActions) {
		
		// Create behavior, planners, result objects, test maps
		MinecraftBehavior mcBeh = new MinecraftBehavior("src/minecraft/maps/template.map");
		String testMapDir = "src/minecraft/maps/test/";
		generateTestMaps(mcBeh, testMapDir, numMapsPerGoal);
		File testDir = new File(testMapDir);
		String[] testMaps = testDir.list();
		
		// Result objects
		Result rtdpResults = new Result(NameSpace.RTDP);
		Result expertRTDPResults = new Result(NameSpace.ExpertRTDP);
		Result learnedHardRTDPResults = new Result(NameSpace.LearnedHardRTDP);
		Result learnedSoftRTDPResults = new Result(NameSpace.LearnedSoftRTDP);
		Result viResults = new Result(NameSpace.VI);
		Result expertVIResults = new Result(NameSpace.ExpertVI);
		Result learnedHardVIResults = new Result(NameSpace.LearnedHardVI);
		
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

			// --- PLANNING ---
			
			// Loop over each map and solve for each planner given
			for(String map : testMaps) {
				System.out.println("Starting new map: " + map);
				statusBW.write("Running on map: " + map + "\n");
				statusBW.flush();
				
				// Update planners with new map
				MapIO mapIO = new MapIO(testMapDir + map);
				mcBeh.updateMap(mapIO);
				
				// --- Plan for each planner given ---
				
				// RTDP
				if(planners.contains(NameSpace.RTDP)) {
					statusBW.write("\t...RTDP");
					statusBW.flush();
					RTDPPlanner rtdp = new RTDPPlanner(mcBeh, addOptions, addMacroActions);
					rtdpResults.addTrial(rtdp.runPlanner());
					statusBW.write(" Finished\n");
					statusBW.flush();
				}
				
				// Expert RTDP
				if(planners.contains(NameSpace.ExpertRTDP)) {
					statusBW.write("\t...Expert RTDP");
					statusBW.flush();
					AffordanceRTDPPlanner affRTDP = new AffordanceRTDPPlanner(mcBeh, addOptions, addMacroActions, expertAffKB);
					affRTDP.updateKB(expertAffKB);
					expertRTDPResults.addTrial(affRTDP.runPlanner());
					statusBW.write(" Finished\n");
					statusBW.flush();
				}
				
				// Learned Hard RTDP
				if(planners.contains(NameSpace.LearnedHardRTDP)) {
					statusBW.write("\t...LearnedHard RTDP");
					statusBW.flush();
					AffordanceRTDPPlanner affRTDP = new AffordanceRTDPPlanner(mcBeh, addOptions, addMacroActions, learnedHardAffKB);
					affRTDP.updateKB(learnedHardAffKB);
					learnedHardRTDPResults.addTrial(affRTDP.runPlanner());
					statusBW.write(" Finished\n");
					statusBW.flush();
				}
				
				// Learned Soft RTDP
				if(planners.contains(NameSpace.LearnedSoftRTDP)) {
					statusBW.write("\t...Learned Soft RTDP");
					statusBW.flush();
					AffordanceRTDPPlanner affRTDP = new AffordanceRTDPPlanner(mcBeh, addOptions, addMacroActions, learnedSoftAffKB);
					affRTDP.updateKB(learnedSoftAffKB);
					learnedSoftRTDPResults.addTrial(affRTDP.runPlanner());
					statusBW.write(" Finished\n");
					statusBW.flush();
				}
				
				// VI
				if(planners.contains(NameSpace.VI)) {
					statusBW.write("\t...VI");
					statusBW.flush();
					VIPlanner vi = new VIPlanner(mcBeh, addOptions, addMacroActions);
					viResults.addTrial(vi.runPlanner());
					statusBW.write(" Finished\n");
					statusBW.flush();
				}
				
				// Expert VI
				if(planners.contains(NameSpace.ExpertVI)) {
					statusBW.write("\t...Expert VI");
					statusBW.flush();
					AffordanceVIPlanner affVI = new AffordanceVIPlanner(mcBeh, addOptions, addMacroActions, expertAffKB);
					affVI.updateKB(expertAffKB);
					expertVIResults.addTrial(affVI.runPlanner());
					statusBW.write(" Finished\n");
					statusBW.flush();
				}
				
				// Learned Hard VI
				if(planners.contains(NameSpace.LearnedHardVI)) {
					statusBW.write("\t...Learned Hard VI");
					statusBW.flush();
					AffordanceVIPlanner affVI = new AffordanceVIPlanner(mcBeh, addOptions, addMacroActions, learnedHardAffKB);
					affVI.updateKB(learnedHardAffKB);
					learnedHardVIResults.addTrial(affVI.runPlanner());
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
		
		// For keeping track of the status (writes to status file)
//		BufferedWriter statusBW = new BufferedWriter(null);
//		try {
//			File statusFile = new File("src/tests/results/learning_rate/status_lr.txt");
//			FileWriter statusFW = new FileWriter(statusFile.getAbsoluteFile());
//			statusBW = new BufferedWriter(statusFW);
//		} catch (IOException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}
		
		// Run Expert and Vanilla planners
		for (String map : testMaps) {
			for(int i = 0; i < numMapsPerGoal; i++) {
				MapIO mapIO = new MapIO(testMapDir + map);
				mcBeh.updateMap(mapIO);
				
				// Vanilla
				RTDPPlanner rtdp = new RTDPPlanner(mcBeh, false, false);
				rtdpResults.addTrial(rtdp.runPlanner());

				// Expert
				affKB.loadHard(mcBeh.getDomain(), "expert.kb");
				AffordanceRTDPPlanner affExpertRTDP = new AffordanceRTDPPlanner(mcBeh, false, false, affKB);
				expertRTDPResults.addTrial(affExpertRTDP.runPlanner());
//				try {
//					statusBW.write("Done with vanilla/expert on map: " + map);
//					statusBW.flush();
//				} catch (IOException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
			}
		}
		
		// Run learning planners with varied size KBs
		for(String kbName : kbNames) {
			for (String map : testMaps) {
				MapIO mapIO = new MapIO(testMapDir + map);
				mcBeh.updateMap(mapIO);
				
				// Hard
				affKB.loadHard(mcBeh.getDomain(), "learning_rate/"  + kbName);
				AffordanceRTDPPlanner affHardRTDP = new AffordanceRTDPPlanner(mcBeh, false, false, affKB);
				learnedHardRTDPResults.addTrialForKB(kbName, affHardRTDP.runPlanner());
				
				// Soft
				AffordanceRTDPPlanner affSoftRTDP = new AffordanceRTDPPlanner(mcBeh, false, false, affKB);
				affKB.loadSoft(mcBeh.getDomain(), "learning_rate/" + kbName);
				learnedSoftRTDPResults.addTrialForKB(kbName, affSoftRTDP.runPlanner());
//				try {
//					statusBW.write("Done with learned on map: " + map + ", with kb: " + kbName);
//					statusBW.flush();
//				} catch (IOException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}

				
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
//		planners.add(NameSpace.RTDP);
//		planners.add(NameSpace.ExpertRTDP);
//		planners.add(NameSpace.LearnedHardRTDP);
//		planners.add(NameSpace.LearnedSoftRTDP);
//		planners.add(NameSpace.VI);
		planners.add(NameSpace.ExpertVI);
//		planners.add(NameSpace.LearnedHardVI);
		
		boolean addOptions = false;
		boolean addMacroActions = false;
//		runMinecraftTests(1, "1", learningFlag, planners, addOptions, addMacroActions);
		
		
		// --- Learning Rate Results ---
		boolean shouldLearn = false;
		int numMapsPerGoalTest = 1;
		int maxKBSize = 20;
		int kbSizeIncrement = 5;
		runLearningRateTests("upto20", numMapsPerGoalTest, maxKBSize, kbSizeIncrement, shouldLearn);
	}

}
