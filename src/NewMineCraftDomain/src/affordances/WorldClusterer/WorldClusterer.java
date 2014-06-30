package affordances.WorldClusterer;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import cc.mallet.types.SparseVector;
import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.planning.OOMDPPlanner;
import burlap.behavior.singleagent.planning.QComputablePlanner;
import burlap.behavior.singleagent.planning.ValueFunctionPlanner;
import burlap.behavior.singleagent.planning.commonpolicies.GreedyDeterministicQPolicy;
import burlap.behavior.singleagent.planning.stochastic.valueiteration.ValueIteration;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.Action;
import burlap.oomdp.singleagent.GroundedAction;
import minecraft.MapIO;
import minecraft.MinecraftBehavior;

public class WorldClusterer {
	HashMap<String, MapIO> mapFileToIO;
	HashMap<MapIO, String> fileIOToMapString;
	
	HashMap<MapIO, HashMap<GroundedAction, Integer>> actionCountsForWorlds = new HashMap<MapIO, HashMap<GroundedAction, Integer>>();
	HashMap<MapIO, HashMap<GroundedAction, Double>> normalizedActionCountsForWorlds = new HashMap<MapIO, HashMap<GroundedAction, Double>>();
	HashMap<Integer, List<MapIO>> clusters;
	String directory;
	private static String mapFileExtension = "map";
	
	public WorldClusterer(String filePath, int numClusters) {
		//Read in IOs
		readInMapIOs(filePath);
		
		//Solve maps and perform action counts
		calculateCountsForAllMaps();
		normalizeActionCounts();
		
		//Perform clustering
		KMeaner kMean = new KMeaner(this.normalizedActionCountsForWorlds, numClusters);
		this.clusters = kMean.getClusters();
		
	}

	
	/**
	 * 
	 * @param filePath path to directory to find the .map files in
	 * @return a mapping from .map filenames to MapIOs retrieved in the input directory
	 */
	private void readInMapIOs(String filePath) {
		File folder = new File(filePath);
		this.directory = filePath;
		
		HashMap<String, MapIO> mapFileToIO = new HashMap<String, MapIO>();
		HashMap<MapIO, String> fileIOToMapString = new HashMap<MapIO, String>();
		
		for(File file : folder.listFiles()) {
			String fileName = file.getName();
			String[] fileNameSplit = fileName.split("\\.");
			if (fileNameSplit.length > 0) {
				String fileExtension = fileNameSplit[fileNameSplit.length-1];
				if (fileExtension.equals(mapFileExtension)){//Check that ends in .map
					String mapFilePath = filePath+file.getName();
					MapIO currIO = new MapIO(mapFilePath);
					mapFileToIO.put(file.getName(), currIO);
					fileIOToMapString.put(currIO, file.getName());
				}
			}
		}
		this.mapFileToIO = mapFileToIO;
		this.fileIOToMapString = fileIOToMapString;
	}
	
	/**
	 * Solves all the MapIOs and then adds up their counts for each action
	 */
	private void calculateCountsForAllMaps() {
		for (String mapFileString: this.mapFileToIO.keySet()) {
			System.out.println("Calculating action counts for " + mapFileString);
			MapIO currIO = this.mapFileToIO.get(mapFileString);
			MinecraftBehavior mcBeh = new MinecraftBehavior(this.directory + mapFileString);
			ValueFunctionPlanner planner = new ValueIteration(mcBeh.getDomain(), mcBeh.getRewardFunction(), mcBeh.getTerminalFunction(), mcBeh.getGamma(), mcBeh.getHashFactory(), mcBeh.getMinDelta(), Integer.MAX_VALUE);
			
			GreedyDeterministicQPolicy p = (GreedyDeterministicQPolicy)mcBeh.solve(planner);
			
			EpisodeAnalysis ea = p.evaluateBehavior(mcBeh.getInitialState(), mcBeh.getRewardFunction(), mcBeh.getTerminalFunction());
			List<State> allStates = ea.stateSequence;//((ValueFunctionPlanner)planner).getAllStates();
			HashMap<GroundedAction, Integer> countsHashMapForMap = new HashMap<GroundedAction, Integer>();

			//Prune the terminal state
			allStates.remove(allStates.size()-1);
			
			for(State currState: allStates) {
				GroundedAction currGroundedAction = (GroundedAction) p.getAction(currState);
				Integer oldCount = countsHashMapForMap.get(currGroundedAction);
				if (oldCount == null) {
					oldCount = 0;
				}
				
				oldCount += 1;
				
				countsHashMapForMap.put(currGroundedAction, oldCount);			
			}
			
			this.actionCountsForWorlds.put(currIO, countsHashMapForMap);
			
		}
	}
	
	/**
	 * Normalizes action counts so that the they all sum to 1
	 */
	private void normalizeActionCounts() {
		for (String mapString: this.mapFileToIO.keySet()) {
			HashMap<GroundedAction, Double>thisMapsNormalizedCounts = new HashMap<GroundedAction, Double>();
			
			MapIO currIO = this.mapFileToIO.get(mapString);
			HashMap<GroundedAction, Integer> thisMapsCounts = this.actionCountsForWorlds.get(currIO);
			//Get total count
			int totalCount = 0;
			for (GroundedAction currAction : thisMapsCounts.keySet()) {
				Integer count = thisMapsCounts.get(currAction);
				totalCount += count;
			}	
			//Normalize
			for (GroundedAction currAction : thisMapsCounts.keySet()) {
				Integer count = thisMapsCounts.get(currAction);
				
				thisMapsNormalizedCounts.put(currAction, ((double)count/(double)totalCount));
			}
			this.normalizedActionCountsForWorlds.put(currIO, thisMapsNormalizedCounts);
		}
	}
	
	public void printAllMaps() {
		for (String fileName : this.mapFileToIO.keySet()) {
			System.out.println(fileName);
			
			MapIO currIO = this.mapFileToIO.get(fileName);
			System.out.println(currIO.getHeaderAsString() + currIO.getCharArrayAsString());
		}
	}
	
	public void printActionCounts() {
		System.out.println("ACTION COUNTS:");
		for (String mapString: this.mapFileToIO.keySet()) {
			MapIO currIO = this.mapFileToIO.get(mapString);
			System.out.println(mapString + ":");
			HashMap<GroundedAction, Integer> thisMapsCounts = this.actionCountsForWorlds.get(currIO);
			
			for (GroundedAction currAction : thisMapsCounts.keySet()) {
				Integer count = thisMapsCounts.get(currAction);
				System.out.println("\t" + currAction.actionName() + ": " + count);
			}			
		}
	}
	
	public void printNormActionCounts() {
		System.out.println("ACTION COUNTS:");
		for (String mapString: this.mapFileToIO.keySet()) {
			MapIO currIO = this.mapFileToIO.get(mapString);
			System.out.println(mapString + ":");
			HashMap<GroundedAction, Double> thisMapsCounts = this.normalizedActionCountsForWorlds.get(currIO);
			
			for (GroundedAction currAction : thisMapsCounts.keySet()) {
				Double count = thisMapsCounts.get(currAction);
				System.out.println("\t" + currAction.actionName() + ": " + count);
			}			
		}
	}
	

	
	/**
	 * @param byMapName
	 */
	public void printClusters(boolean byMapName) {
		for(Integer clusterNum: this.clusters.keySet()) {
			System.out.println("Cluster number: " + clusterNum);
			List<MapIO> members = this.clusters.get(clusterNum);
			for (MapIO member : members) {
				String mapString = "";
				if (!byMapName) {
					//Whole map
					mapString = member.getCharArrayAsString() + "\n";
				}
				else {
					
					mapString = this.fileIOToMapString.get(member);
				}
				System.out.println("\t"+mapString);
			}
		}
	}



	public static void main(String [] args) {
		String filePath = "src/minecraft/maps/";
		WorldClusterer test = new WorldClusterer(filePath, 3);
		test.printActionCounts();
		test.printNormActionCounts();
		test.printClusters(true);
	}
}