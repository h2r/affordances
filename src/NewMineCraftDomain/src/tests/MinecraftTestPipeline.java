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
		
		// Small
		mapMaker.generateNMaps(numMaps, new DeepTrenchWorld(1, numLavaBlocks), 1, 3, 5);
//		mapMaker.generateNMaps(numMaps, new PlaneGoldMineWorld(numLavaBlocks), 2, 2, 4);
//		mapMaker.generateNMaps(numMaps, new PlaneGoldSmeltWorld(numLavaBlocks), 2, 2, 4);
//		mapMaker.generateNMaps(numMaps, new PlaneWallWorld(1, numLavaBlocks), 1, 3, 4);
//		mapMaker.generateNMaps(numMaps, new PlaneWorld(numLavaBlocks), 2, 2, 4);
//		mapMaker.generateNMaps(numMaps, new PlaneGoalShelfWorld(2,1, numLavaBlocks), 2, 2, 5);
		
		// Big
//		mapMaker.generateNMaps(numMaps, new DeepTrenchWorld(1, numLavaBlocks), 5, 5, 6);
//		mapMaker.generateNMaps(numMaps, new PlaneGoldMineWorld(numLavaBlocks), 5, 5, 4);
//		mapMaker.generateNMaps(numMaps, new PlaneGoldSmeltWorld(numLavaBlocks), 5, 5, 4);
//		mapMaker.generateNMaps(numMaps, new PlaneWallWorld(1, numLavaBlocks), 5, 5, 4);
//		mapMaker.generateNMaps(numMaps, new PlaneWorld(numLavaBlocks), 5, 5, 4);
//		mapMaker.generateNMaps(numMaps, new PlaneGoalShelfWorld(2,1, numLavaBlocks), 5, 5, 5);
		
	}
	
	/**
	 * Run tests on all world types, for each of the planners and record results
	 * @param numMapsPerGoal
	 * @param nametag: name to indicate which results file corresponds to these tests
	 * @param shouldLearn: flag indicating whether or not to learn a new KB
	 * @param planners: a list of strings indicating which planners to run
	 * @param addOptions: a boolean indicating whether or not to add options to planners
	 * @param addMacroActions: a boolean indicating whether or not to add macroactions to planners
	 * @param countStateSpaceSize: a boolean indicating whether or not to count the reachable state space size of each map
	 * @throws IOException 
	 */
	public static void runMinecraftTests(int numMapsPerGoal, String nametag, boolean shouldLearn, List<String> planners, boolean addOptions, boolean addMAs, boolean countStateSpaceSize) throws IOException {
		
		// Create behavior, planners, result objects, test maps
		MinecraftBehavior mcBeh = new MinecraftBehavior();
		String testMapDir = NameSpace.PATHMAPS + "test/";
		generateTestMaps(mcBeh, testMapDir, numMapsPerGoal);
		File testDir = new File(testMapDir);
		String[] testMaps = testDir.list();
		
		// Result objects
		Result rtdpResults = new Result(NameSpace.RTDP);
		Result optionResults = new Result("options" + NameSpace.RTDP);
		Result macroActionResults = new Result("options" + NameSpace.RTDP);
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
			expertAffKB.load(mcBeh.getDomain(), "expert.kb", false);
		}

		// Learn if we're supposed to learn a new KB
		String learnedKBName;
		if(shouldLearn) {
			// TODO: improve method of selecting number of worlds to learn with (currently set to 5)
			learnedKBName = AffordanceLearner.generateMinecraftKB(mcBeh, 5, false, addOptions, addMAs);
		}
		else {
			learnedKBName = "test50.kb";
		}
		
		// Hard Learned KB
		KnowledgeBase learnedHardAffKB = new KnowledgeBase();
		if(planners.contains(NameSpace.LearnedHardRTDP) || planners.contains(NameSpace.LearnedHardVI)) {
			learnedHardAffKB.load(mcBeh.getDomain(), learnedKBName, false);
		}
		
		// Soft Learned KB
		KnowledgeBase learnedSoftAffKB = new KnowledgeBase();;
		if(planners.contains(NameSpace.LearnedSoftRTDP) || planners.contains(NameSpace.LearnedSoftVI)) {
			learnedSoftAffKB.load(mcBeh.getDomain(), learnedKBName, true);
		}
		
		// --- FILE WRITER SETUP ---
		String outputFileName= NameSpace.PATHRESULTS + nametag;
		if(addOptions){
			outputFileName = outputFileName + "_opt";
		}
		if(addMAs){
			outputFileName = outputFileName + "_ma";
		}
		outputFileName = outputFileName + "_results.txt";
		File resultsFile = new File(outputFileName);
		BufferedWriter resultsBW;
		FileWriter resultsFW;
		
		File statusFile = new File(NameSpace.PATHRESULTS + "status.txt");
		BufferedWriter statusBW;
		FileWriter statusFW;
		List<Integer> stateSpaceSizes = new ArrayList<Integer>();
		// Initialize Result objects and file writing objects
			resultsFW = new FileWriter(resultsFile.getAbsoluteFile());
			resultsBW = new BufferedWriter(resultsFW);
			statusFW = new FileWriter(statusFile.getAbsoluteFile());
			statusBW = new BufferedWriter(statusFW);
		int mapCounter = 1;

		// --- PLANNING ---
		
		// Loop over each map and solve for each planner given
		for(String map : testMaps) {
			
			System.out.println("Starting new map: " + map);
				statusBW.write("Running on map: " + map + "\n");
				statusBW.flush();
			
			// Update planners with new map
			MapIO mapIO = new MapIO(NameSpace.PATHMAPS + "test/" + map);
			mcBeh.updateMap(mapIO);
			
			if(countStateSpaceSize && mapCounter == 1) {
				// Count Reachable State Size
				stateSpaceSizes.add(mcBeh.countReachableStates());
			}
			
			// --- Plan for each planner given ---
			
			if(addOptions) {
				statusBW.write("\t...options RTDP");
				statusBW.flush();
			RTDPPlanner rtdp = new RTDPPlanner(mcBeh, true, false);
			optionResults.addTrial(rtdp.runPlanner());
				statusBW.write(" Finished\n");
				statusBW.flush();
			}
			
			if(addMAs) {
				statusBW.write("\t...macro actions RTDP");
				statusBW.flush();
			RTDPPlanner rtdp = new RTDPPlanner(mcBeh, false, true);
			macroActionResults.addTrial(rtdp.runPlanner());
				statusBW.write(" Finished\n");
				statusBW.flush();
			}
			
			// RTDP
			if(planners.contains(NameSpace.RTDP)) {
					statusBW.write("\t...RTDP");
					statusBW.flush();
				RTDPPlanner rtdp = new RTDPPlanner(mcBeh, false, false);
				rtdpResults.addTrial(rtdp.runPlanner());
					statusBW.write(" Finished\n");
					statusBW.flush();
			}
			
			// Expert RTDP
			if(planners.contains(NameSpace.ExpertRTDP)) {
					statusBW.write("\t...Expert RTDP");
					statusBW.flush();
				AffordanceRTDPPlanner affRTDP = new AffordanceRTDPPlanner(mcBeh, addOptions, addMAs, expertAffKB);
				affRTDP.updateKB(expertAffKB);
				expertRTDPResults.addTrial(affRTDP.runPlanner());
					statusBW.write(" Finished\n");
					statusBW.flush();
			}
			
			// Learned Hard RTDP
			if(planners.contains(NameSpace.LearnedHardRTDP)) {
					statusBW.write("\t...LearnedHard RTDP");
					statusBW.flush();
				AffordanceRTDPPlanner affRTDP = new AffordanceRTDPPlanner(mcBeh, addOptions, addMAs, learnedHardAffKB);
				affRTDP.updateKB(learnedHardAffKB);
				learnedHardRTDPResults.addTrial(affRTDP.runPlanner());
					statusBW.write(" Finished\n");
					statusBW.flush();
			}
			
			// Learned Soft RTDP
			if(planners.contains(NameSpace.LearnedSoftRTDP)) {
					statusBW.write("\t...Learned Soft RTDP");
					statusBW.flush();
				AffordanceRTDPPlanner affRTDP = new AffordanceRTDPPlanner(mcBeh, addOptions, addMAs, learnedSoftAffKB);
				affRTDP.updateKB(learnedSoftAffKB);
				learnedSoftRTDPResults.addTrial(affRTDP.runPlanner());
					statusBW.write(" Finished\n");
					statusBW.flush();
			}
			
			// VI
			if(planners.contains(NameSpace.VI)) {
					statusBW.write("\t...VI");
					statusBW.flush();
				VIPlanner vi = new VIPlanner(mcBeh, addOptions, addMAs);
				viResults.addTrial(vi.runPlanner());
					statusBW.write(" Finished\n");
					statusBW.flush();
			}
			
			// Expert VI
			if(planners.contains(NameSpace.ExpertVI)) {
					statusBW.write("\t...Expert VI");
					statusBW.flush();
				AffordanceVIPlanner affVI = new AffordanceVIPlanner(mcBeh, addOptions, addMAs, expertAffKB);
				affVI.updateKB(expertAffKB);
				expertVIResults.addTrial(affVI.runPlanner());
					statusBW.write(" Finished\n");
					statusBW.flush();
			}
			
			// Learned Hard VI
			if(planners.contains(NameSpace.LearnedHardVI)) {
					statusBW.write("\t...Learned Hard VI");
					statusBW.flush();
				AffordanceVIPlanner affVI = new AffordanceVIPlanner(mcBeh, addOptions, addMAs, learnedHardAffKB);
				affVI.updateKB(learnedHardAffKB);
				learnedHardVIResults.addTrial(affVI.runPlanner());
					statusBW.write(" Finished\n");
					statusBW.flush();
			}
				statusBW.write("mapCounter: " + mapCounter + "\n");
				statusBW.flush();
				
			// --- RECORD RESULTS TO FILE ---
			if(mapCounter == numMapsPerGoal) {
				double avgStateSpaceSize = 0;
				if(countStateSpaceSize) {
					int total = 0;
					for(Integer size : stateSpaceSizes) {
						total += size;
					}
					avgStateSpaceSize = ((double)total) / mapCounter;
				}
				
					resultsBW.write("map: " + map.substring(0, map.length() - 5) + "stateSpace=" + avgStateSpaceSize + "\n");
				if(planners.contains(NameSpace.RTDP)) {
						resultsBW.write("\t" + rtdpResults.getAllResults() + "Avgs: " + rtdpResults + "\n");
					System.out.println(rtdpResults.toString());
					rtdpResults.clear();
				}
				if(planners.contains(NameSpace.ExpertRTDP)) {
						resultsBW.write("\t" + expertRTDPResults.getAllResults() + "Avgs: " + expertRTDPResults + "\n");
					System.out.println(expertRTDPResults.toString());
					expertRTDPResults.clear();
				}
				if(planners.contains(NameSpace.LearnedHardRTDP)) {
						resultsBW.write("\t" + learnedHardRTDPResults.getAllResults() + "Avgs: " + learnedHardRTDPResults + "\n");
					System.out.println(learnedHardRTDPResults.toString());
					learnedHardRTDPResults.clear();
				}
				if(planners.contains(NameSpace.LearnedSoftRTDP)) {
						resultsBW.write("\t" + learnedSoftRTDPResults.getAllResults() + "Avgs: " + learnedSoftRTDPResults + "\n");
					System.out.println(learnedSoftRTDPResults.toString());
					learnedSoftRTDPResults.clear();
				}
				if(planners.contains(NameSpace.VI)) {
						resultsBW.write("\t" + viResults.getAllResults() + "Avgs: " + viResults + "\n");
					System.out.println(viResults.toString());
					viResults.clear();
				}
				if(planners.contains(NameSpace.ExpertVI)) {
						resultsBW.write("\t" + expertVIResults.getAllResults() + "Avgs: " + expertVIResults + "\n");
					System.out.println(expertVIResults.toString());
					expertVIResults.clear();
				}
				if(planners.contains(NameSpace.LearnedHardVI)) {
						resultsBW.write("\t" + learnedHardVIResults.toString() + "Avgs: " + learnedHardVIResults + "\n");
//					System.out.println(learnedHardVIResults.toString());
					learnedHardVIResults.clear();
				}
					resultsBW.flush();
				
				// Reset
				mapCounter = 1;
				continue;
			}
			
			mapCounter++;
		}
		
	}
	
	/**
	 * Run tests on all world types, for each of the 3 planners and record results
	 * @param numIterations: the number of times to perform testing
	 * @param nametag: a flag for the name of the results file
	 * @throws IOException 
	 */
	public static void runLearningRateTests(String nametag, int numMapsPerGoal, int maxNumLearningWorlds, int learningIncrement, boolean shouldLearn, boolean countStateSpaceSize) throws IOException {
		// Generate Behavior and test maps
		MinecraftBehavior mcBeh = new MinecraftBehavior();
		String testMapDir = NameSpace.PATHMAPS;
		generateTestMaps(mcBeh, testMapDir, numMapsPerGoal);
		System.out.println("------------after generation");
		
		File testDir = new File(testMapDir);
		String[] testMaps = testDir.list();
		
		List<String> kbNames = new ArrayList<String>();
		
		if(shouldLearn){
			// --- Create Knowledge Bases ---
			for(int numWorldsToLearn = 0; numWorldsToLearn <= maxNumLearningWorlds; numWorldsToLearn = numWorldsToLearn + learningIncrement) {
				// Learn if we're supposed to learn a new KB
				String learnedKBName = AffordanceLearner.generateMinecraftKB(mcBeh, numWorldsToLearn, true, false, false);
				kbNames.add(learnedKBName);
			}
		}
		else{
			// If we're not learning new knowledge bases, use the existing ones.
			kbNames = new ArrayList<String>();
			for(int i = 0; i <= maxNumLearningWorlds; i += learningIncrement) {
				kbNames.add("lr_" + i + ".kb");
			}
//			File learningRateKBDir = new File("src/minecraft/kb/learning_rate/");
//			String[] kbsToUse = learningRateKBDir.list();
//			kbNames = new ArrayList<String>(Arrays.asList(kbsToUse));
		}
		
		// Make knowledge base and result objects
		KnowledgeBase affKB = new KnowledgeBase();
		Result rtdpResults = new Result(NameSpace.RTDP);
		Result expertRTDPResults = new Result(NameSpace.ExpertRTDP);
		LearningRateResult learnedHardRTDPResults = new LearningRateResult(NameSpace.LearnedHardRTDP);
		LearningRateResult learnedSoftRTDPResults = new LearningRateResult(NameSpace.LearnedSoftRTDP);
		Result viResults = new Result(NameSpace.VI);
		
		// For keeping track of the status (writes to status file)
		File statusFile = new File("src/tests/results/learning_rate/status_lr.txt");
		FileWriter statusFW = new FileWriter(statusFile.getAbsoluteFile());
		BufferedWriter statusBW = new BufferedWriter(statusFW);;
		
		// Run Expert and Vanilla planners
		for (String map : testMaps) {
				MapIO mapIO = new MapIO(testMapDir + map);
				mcBeh.updateMap(mapIO);
				
				rtdpResults.clear();
				expertRTDPResults.clear();
				
				// Vanilla
				RTDPPlanner rtdp = new RTDPPlanner(mcBeh, false, false);
				rtdpResults.addTrial(rtdp.runPlanner());

				// Expert
				affKB.load(mcBeh.getDomain(), "expert.kb", false);
				AffordanceRTDPPlanner affExpertRTDP = new AffordanceRTDPPlanner(mcBeh, false, false, affKB);
				expertRTDPResults.addTrial(affExpertRTDP.runPlanner());
				
				int numStates = -1;
				if(countStateSpaceSize) {
					// Count Reachable State Size
					numStates = mcBeh.countReachableStates();
				}

				try {
					statusBW.write("Done with vanilla/expert on map: [van,exp, numStates] " + map + " [" + rtdpResults.getAvgBellmanUpdates() + "," + expertRTDPResults.getAvgBellmanUpdates() + ", numStates: " + numStates + "]\n");
					statusBW.flush();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}
		
		// Run learning planners with varied size KBs
		for(String kbName : kbNames) {
			statusBW.write("(MinecraftTestPipeline) USING KB: " + kbName);
			for (String map : testMaps) {
				MapIO mapIO = new MapIO(testMapDir + map);
				mcBeh.updateMap(mapIO);
				
				// Hard
				affKB.load(mcBeh.getDomain(), "learning_rate/"  + kbName, false);
				AffordanceRTDPPlanner affHardRTDP = new AffordanceRTDPPlanner(mcBeh, false, false, affKB);
				learnedHardRTDPResults.addTrialForKB(kbName, affHardRTDP.runPlanner());
				
				// Soft
				AffordanceRTDPPlanner affSoftRTDP = new AffordanceRTDPPlanner(mcBeh, false, false, affKB);
				affKB.load(mcBeh.getDomain(), "learning_rate/" + kbName, true);
				learnedSoftRTDPResults.addTrialForKB(kbName, affSoftRTDP.runPlanner());
				try {
					statusBW.write("Done with learned on map: " + map + ", with kb: " + kbName + "\n");
					statusBW.flush();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
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
	
	private void writeResultsToFile(List<Result> resultObjects) {
		
	}
	
	public static void main(String[] args) {
//		System.out.close();
		
		// --- Basic Minecraft Results ---
		boolean learningFlag = false;
		// Choose which planners to collect results for
		List<String> planners = new ArrayList<String>();
		planners.add(NameSpace.RTDP);
		planners.add(NameSpace.ExpertRTDP);
//		planners.add(NameSpace.LearnedHardRTDP);
//		planners.add(NameSpace.LearnedSoftRTDP);
//		planners.add(NameSpace.VI);
//		planners.add(NameSpace.ExpertVI);
//		planners.add(NameSpace.LearnedHardVI);
		
		boolean addOptions = false;
		boolean addMacroActions = false;
		boolean countStateSpaceSize = false;
		
		try {
			runMinecraftTests(3, "3", learningFlag, planners, addOptions, addMacroActions, countStateSpaceSize);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// --- Learning Rate Results ---
//		boolean shouldLearn = false;
//		boolean countStateSpaceSize = true;
//		int numMapsPerGoalTest = 1;
//		int maxKBSize = 100;
//		int kbSizeIncrement = 100;
//		try {
//			runLearningRateTests("0v100", numMapsPerGoalTest, maxKBSize, kbSizeIncrement, shouldLearn, countStateSpaceSize);
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
	}

}
//