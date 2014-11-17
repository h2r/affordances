package tests;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import burlap.oomdp.singleagent.Action;
import minecraft.MapIO;
import minecraft.NameSpace;
import minecraft.MinecraftBehavior.MinecraftBehavior;
import minecraft.MinecraftBehavior.Planners.AffordanceRTDPPlanner;
import minecraft.MinecraftBehavior.Planners.AffordanceVIPlanner;
import minecraft.MinecraftBehavior.Planners.MinecraftPlanner;
import minecraft.MinecraftBehavior.Planners.RTDPPlanner;
import minecraft.MinecraftBehavior.Planners.VIPlanner;
import minecraft.WorldGenerator.MapFileGenerator;
import minecraft.WorldGenerator.WorldTypes.DeepTrenchWorld;
import minecraft.WorldGenerator.WorldTypes.PlaneGoalShelfWorld;
import minecraft.WorldGenerator.WorldTypes.PlaneGoldMineWorld;
import minecraft.WorldGenerator.WorldTypes.PlaneGoldSmeltWorld;
import minecraft.WorldGenerator.WorldTypes.PlaneWallWorld;
import minecraft.WorldGenerator.WorldTypes.PlaneWorld;
import affordances.AffordanceLearnerSokoban;
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
//		mapMaker.generateNMaps(numMaps, new DeepTrenchWorld(1, numLavaBlocks), 1, 3, 5);
//		mapMaker.generateNMaps(numMaps, new PlaneGoldMineWorld(numLavaBlocks), 2, 2, 4);
//		mapMaker.generateNMaps(numMaps, new PlaneGoldSmeltWorld(numLavaBlocks), 2, 2, 4);
//		mapMaker.generateNMaps(numMaps, new PlaneWallWorld(1, numLavaBlocks), 1, 3, 4);
//		mapMaker.generateNMaps(numMaps, new PlaneWorld(numLavaBlocks), 4, 4, 4);

		// Big-RTDP (SIZES LOCKED)
		mapMaker.generateNMaps(numMaps, new DeepTrenchWorld(1, numLavaBlocks), 3, 3, 5);
		mapMaker.generateNMaps(numMaps, new PlaneGoldMineWorld(numLavaBlocks), 3, 3, 4);
		mapMaker.generateNMaps(numMaps, new PlaneGoldSmeltWorld(numLavaBlocks), 4, 4, 5);
		mapMaker.generateNMaps(numMaps, new PlaneWallWorld(1, numLavaBlocks + 1), 2, 3, 5);
		mapMaker.generateNMaps(numMaps, new PlaneWorld(numLavaBlocks), 10, 10, 4);
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
	public static void runMinecraftTests(int numMapsPerGoal, String nametag, boolean shouldLearn, List<String> planners, boolean useOptions, boolean useMAs, boolean countStateSpaceSize, String jobID) throws IOException {
		
		// Create behavior, planners, result objects, test maps
		MinecraftBehavior mcBeh = new MinecraftBehavior();
		String testMapDir = NameSpace.PATHMAPS + "/" + jobID + "/test/";
		generateTestMaps(mcBeh, testMapDir, numMapsPerGoal);
		File testDir = new File(testMapDir);
		String[] testMaps = testDir.list();
		
		// Result objects
		Result rtdpResults = new Result(NameSpace.RTDP);
		Result macroActionOptionsResults = new Result(NameSpace.RTDP + "_MAs(" + useMAs + ")_options(" + useOptions + ")");
		Result expertRTDPResults = new Result(NameSpace.ExpertRTDP);
		Result learnedHardVanillaResults = new Result(NameSpace.LearnedHardRTDP);
		Result learnedHardOptMAResults = new Result(NameSpace.LearnedHardRTDP + "_MAs(" + useMAs + ")_options(" + useOptions + ")");
		Result learnedSoftRTDPResults = new Result(NameSpace.LearnedSoftRTDP);
		Result viResults = new Result(NameSpace.VI);
		Result expertVIResults = new Result(NameSpace.ExpertVI);
		Result learnedHardVIResults = new Result(NameSpace.LearnedHardVI);
		
		// --- Create Knowledge Bases ---
		String learnedKBName = "learned/grid_prim_acts.kb";
		String learnedTempExtKBName = "learned/grid";
		String expertKBName = "expert/expert";
		if(useMAs) { 
			learnedTempExtKBName += "_ma";
			expertKBName += "_ma";
		}
		if(useOptions) {
			learnedTempExtKBName += "_o";
			expertKBName += "_o";
		}
		if(!useMAs && !useOptions) {
			learnedTempExtKBName += "_prim_acts";
			expertKBName += "_prim_acts";
		}
		learnedTempExtKBName += ".kb";
		expertKBName += ".kb";

		// Expert KB
		KnowledgeBase expertAffKB = new KnowledgeBase();
		if(planners.contains(NameSpace.ExpertRTDP)) {
			boolean threshold = false;
			boolean expertFlag = true;
			expertAffKB.load(mcBeh.getDomain(), new HashMap<String,Action>(), expertKBName, threshold, expertFlag);
		}
		
		// Hard Learned KB
		KnowledgeBase learnedHardAffKB = new KnowledgeBase();
		KnowledgeBase learnedHardTempExtAffKB = new KnowledgeBase();
		if(planners.contains(NameSpace.LearnedHardRTDP) || planners.contains(NameSpace.LearnedHardVI)) {
			// Load the temp ext KB
			if(useOptions || useMAs) {
				learnedHardTempExtAffKB.load(mcBeh.getDomain(), MinecraftPlanner.getMapOfMAsAndOptions(mcBeh, useOptions, useMAs), learnedTempExtKBName, true, false);
			}
			// Load the regular KB
			learnedHardAffKB.load(mcBeh.getDomain(), new HashMap<String,Action>(), learnedKBName, true, false);
		}
		
		// --- FILE WRITER SETUP ---
		String outputFileName= NameSpace.PATHRESULTS + nametag;
		if(useOptions){
			outputFileName = outputFileName + "_opt";
		}
		if(useMAs){
			outputFileName = outputFileName + "_ma";
		}
		outputFileName = outputFileName + ".result";
//		File resultsFile = new File(outputFileName);
//		BufferedWriter resultsBW;
//		FileWriter resultsFW;
		
//		File statusFile = new File(NameSpace.PATHRESULTS + "status.txt");
//		BufferedWriter statusBW;
//		FileWriter statusFW;
		List<Integer> stateSpaceSizes = new ArrayList<Integer>();
		// Initialize Result objects and file writing objects
//			resultsFW = new FileWriter(resultsFile.getAbsoluteFile());
//			resultsBW = new BufferedWriter(resultsFW);
//			statusFW = new FileWriter(statusFile.getAbsoluteFile());
//			statusBW = new BufferedWriter(statusFW);
		int mapCounter = 1;

		// --- PLANNING ---
		
		// Loop over each map and solve for each planner given
		for(String map : testMaps) {
			
//			System.out.println("Starting new map: " + map);
//				statusBW.write("Running on map: " + map + "\n");
//				statusBW.flush();
			
			// Update planners with new map
			MapIO mapIO = new MapIO(testMapDir + map);
			mcBeh.updateMap(mapIO);
			
			if(countStateSpaceSize && mapCounter == 1) {
				// Count Reachable State Size
				stateSpaceSizes.add(mcBeh.countReachableStates());
			}
			
			// --- Plan for each planner given ---
			
			// Options and Macroactions
			if(useOptions || useMAs) {
//				statusBW.write("\t...RTDP + MAs(" + useMAs + ")_options(" + useOptions + ")");
//				statusBW.flush();
				RTDPPlanner rtdp = new RTDPPlanner(mcBeh, useOptions, useMAs);
				macroActionOptionsResults.addTrial(rtdp.runPlanner());
//				statusBW.write(" Finished\n");
//				statusBW.flush();
			}
			
			// RTDP (no options or macroactions)
			if(planners.contains(NameSpace.RTDP)) {
//					statusBW.write("\t...RTDP");
//					statusBW.flush();
				RTDPPlanner rtdp = new RTDPPlanner(mcBeh, false, false);
				rtdpResults.addTrial(rtdp.runPlanner());
//					statusBW.write(" Finished\n");
//					statusBW.flush();
			}
			
			if(planners.contains(NameSpace.ExpertRTDP)) {
				AffordanceRTDPPlanner expertRTDP = new AffordanceRTDPPlanner(mcBeh, useOptions, useMAs, expertAffKB);
				expertRTDP.updateKB(expertAffKB);
				expertRTDPResults.addTrial(expertRTDP.runPlanner());
			}
			
			// Learned Hard RTDP w/ Options and Macroactions
			if(planners.contains(NameSpace.LearnedHardRTDP) && (useOptions || useMAs)) {
//								statusBW.write("\t...LearnedHard RTDP_ma(" + useMAs + ")_opt(" + useOptions + ")");
//								statusBW.flush();
				AffordanceRTDPPlanner affRTDP = new AffordanceRTDPPlanner(mcBeh, useOptions, useMAs, learnedHardAffKB);
				affRTDP.updateKB(learnedHardTempExtAffKB);
				learnedHardOptMAResults.addTrial(affRTDP.runPlanner());
//								statusBW.write(" Finished\n");
//								statusBW.flush();
			}
			
			// Learned Hard RTDP
			if(planners.contains(NameSpace.LearnedHardRTDP)) {
//					statusBW.write("\t...LearnedHard RTDP");
//					statusBW.flush();
				AffordanceRTDPPlanner affRTDP = new AffordanceRTDPPlanner(mcBeh, false, false, learnedHardAffKB);
				affRTDP.updateKB(learnedHardAffKB);
				learnedHardVanillaResults.addTrial(affRTDP.runPlanner());
//					statusBW.write(" Finished\n");
//					statusBW.flush();
			}
				
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

				System.out.println("map: " + map.substring(0, map.length() - 5) + "stateSpace=" + avgStateSpaceSize + "\n");
//					resultsBW.write("map: " + map.substring(0, map.length() - 5) + "stateSpace=" + avgStateSpaceSize + "\n");
				if(planners.contains(NameSpace.RTDP)) {
					if(useOptions || useMAs) {
						System.out.println(macroActionOptionsResults);
//						resultsBW.write("\t" + macroActionOptionsResults.getAllResults() + "AVERAGES: " + macroActionOptionsResults + "\n");
						macroActionOptionsResults.clear();
					}
					System.out.println(rtdpResults);
//						resultsBW.write("\t" + rtdpResults.getAllResults() + "AVERAGES: " + rtdpResults + "\n");
					rtdpResults.clear();
				}
				if(planners.contains(NameSpace.ExpertRTDP)) {
						System.out.println(expertRTDPResults);
//						resultsBW.write("\t" + expertRTDPResults.getAllResults() + "AVERAGES: " + expertRTDPResults + "\n");
					expertRTDPResults.clear();
				}
				if(planners.contains(NameSpace.LearnedHardRTDP)) {
					if(useOptions || useMAs) {
						System.out.println(learnedHardOptMAResults);
//						resultsBW.write("\t" + learnedHardOptMAResults.getAllResults() + "AVERAGES: " + learnedHardOptMAResults + "\n");
						learnedHardOptMAResults.clear();
					}
					System.out.println(learnedHardVanillaResults);
//						resultsBW.write("\t" + learnedHardVanillaResults.getAllResults() + "AVERAGES: " + learnedHardVanillaResults + "\n");
					learnedHardVanillaResults.clear();
				}
				System.out.println("\n");
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
	public static void runLearningRateTests(String nametag, int numLearningMapsPerLGD, int numTestingMaps, double minFractOfStateSpace, double maxFractOfStateSpace, double increment, boolean shouldLearn, boolean countStateSpaceSize, boolean useOptions, boolean useMAs, String jobID) throws IOException {
		// Generate Behavior and test maps
		MinecraftBehavior mcBeh = new MinecraftBehavior();
		String testMapDir = NameSpace.PATHMAPS + "/" + jobID + "/learningRateTest/";
		generateTestMaps(mcBeh, testMapDir, numTestingMaps);
		
		File testDir = new File(testMapDir);
		String[] testMaps = testDir.list();
		
		List<String> kbNames = new ArrayList<String>();
		
		if(shouldLearn){
			// --- Create Knowledge Bases ---
			for(double fractOfStateSpace = minFractOfStateSpace; fractOfStateSpace <= maxFractOfStateSpace; fractOfStateSpace = fractOfStateSpace + increment) {
				// Learn if we're supposed to learn a new KB
				String learnedKBName = AffordanceLearnerSokoban.generateSokobanKB(mcBeh, numLearningMapsPerLGD, true, false, false, fractOfStateSpace, jobID);
				kbNames.add(learnedKBName);
			}
		}
		else{
			// If we're not learning new knowledge bases, use the existing ones.
			kbNames = new ArrayList<String>();
			for(double fractOfStateSpace = minFractOfStateSpace; fractOfStateSpace <= maxFractOfStateSpace; fractOfStateSpace = fractOfStateSpace + increment) {
				String newKB = "lr_" + String.format("%.2f", fractOfStateSpace) + ".kb";
				kbNames.add(newKB);
			}
//			File learningRateKBDir = new File("src/minecraft/kb/learning_rate/");
//			String[] kbsToUse = learningRateKBDir.list();
//			kbNames = new ArrayList<String>(Arrays.asList(kbsToUse));
		}
		
		// Make knowledge base and result objects
		KnowledgeBase affKB = new KnowledgeBase();
		LearningRateResult learnedHardRTDPResults = new LearningRateResult(NameSpace.LearnedHardRTDP);
		
		// Run learning planners with varied size KBs
		for(String kbName : kbNames) {
			System.out.println("(MCTP) starting on kb: " + kbName);
			for (String map : testMaps) {
				System.out.println("(MCTP) starting on map: " + map);
				MapIO mapIO = new MapIO(testMapDir + map);
				mcBeh.updateMap(mapIO);
				
				// Hard
				affKB.load(mcBeh.getDomain(), MinecraftPlanner.getMapOfMAsAndOptions(mcBeh, useOptions, useMAs),  "learning_rate/" + kbName, false, true);
				AffordanceRTDPPlanner affHardRTDP = new AffordanceRTDPPlanner(mcBeh, false, false, affKB);
				learnedHardRTDPResults.addTrialForKB(kbName, affHardRTDP.runPlanner());
			}
		}
		
//		String outputFileName= NameSpace.PATHRESULTS + "learning_rate/lr.results";
//		File resultsFile = new File(outputFileName);
//		FileWriter resultsFW = new FileWriter(resultsFile.getAbsoluteFile());
//		BufferedWriter resultsBW = new BufferedWriter(resultsFW);
		
//		resultsBW.write("Learning Rate Results: " + minFractOfStateSpace + "-" + maxFractOfStateSpace + "\n");
		System.out.println("Learning Rate Results: " + minFractOfStateSpace + "-" + maxFractOfStateSpace + "\n");
		for(String kbName : learnedHardRTDPResults.getResults().keySet()) {
//			resultsBW.write("\t [" + kbName + "]\t" + learnedHardRTDPResults.getResults().get(kbName).toString() + "\n");
			System.out.println("\t [" + kbName + "]\t" + learnedHardRTDPResults.getResults().get(kbName).toString() + "\n");
		}
//		resultsBW.flush();
	}
	
	private void writeResultsToFile(List<Result> resultObjects) {
		
	}
	
	private static void runGridMCTests(String[] args) {
		// --- Basic Minecraft Results ---
		boolean learningFlag = false;
//				 Choose which planners to collect results for
		List<String> planners = new ArrayList<String>();
		planners.add(NameSpace.RTDP);
		planners.add(NameSpace.ExpertRTDP);
		planners.add(NameSpace.LearnedHardRTDP);
//				planners.add(NameSpace.LearnedSoftRTDP);
//				planners.add(NameSpace.VI);
//				planners.add(NameSpace.ExpertVI);
//				planners.add(NameSpace.LearnedHardVI);
		
		boolean addOptions = false;
		boolean addMAs = false;
		if(args.length > 1) {
			addOptions = args[2].equals("true") ? true : false;
			addMAs = args[1].equals("true") ? true : false;
		}

		boolean countStateSpaceSize = false;
		int numTestWorldsPerGoal = 1;
		String jobID = args[0];
//				String jobID = "local";
		if(addMAs) jobID += "_ma";
		if(addOptions) jobID += "_o";
		try {
			runMinecraftTests(numTestWorldsPerGoal, "grid", learningFlag, planners, addOptions, addMAs, countStateSpaceSize, jobID);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private static void runGridLRTests(String[] args) {
		// --- Learning Rate Results ---
		boolean shouldLearn = false;
		int numTestingMaps = 5;
		int numLearningMapsPerLGD = 5;
		double minFractStateSpace = (Double.parseDouble(args[0]) / 10.0) - 0.1; 
		double maxFractStateSpace = 1.01;
		double increment = 1000;
		boolean useOptions = false;
		boolean useMAs = false;
		String jobID = "lr_" + args[0];
		if(useMAs) jobID += "_ma";
		if(useOptions) jobID += "_o";
		boolean countStateSpaceSize = false;
		try {
			runLearningRateTests("0-1_.1_legit_", numLearningMapsPerLGD, numTestingMaps, minFractStateSpace, maxFractStateSpace, increment, shouldLearn, countStateSpaceSize, useOptions, useMAs, jobID);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
//		System.out.close();
		
		runGridMCTests(args);
//		runGridLRTests(args);	
	}

}
//