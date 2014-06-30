package affordances.WorldClusterer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import minecraft.MapIO;
import burlap.oomdp.singleagent.GroundedAction;

public class KMeaner {
	
	HashMap<MapIO, double[]> mapPositionInData;
	HashMap<MapIO, Integer> clusterMembership;
	HashMap<Integer, double[]> clusterCentroids;
	List<GroundedAction> allActions;
	
	private Random rand;
		
	private int numClusters;
	
	/**
	 * @param normalizedActionCountsForWorlds hashMap from mapIOs to HM of actions to normalized counts
	 * @param numClusters
	 */
	public KMeaner(HashMap<MapIO, HashMap<GroundedAction, Double>> normalizedActionCountsForWorlds, int numClusters) {
		this.numClusters = numClusters;
		this.clusterMembership = new HashMap<MapIO, Integer>();
		this.clusterCentroids = new HashMap<Integer, double[]>();
		this.rand = new Random();
		this.allActions = allActions(normalizedActionCountsForWorlds);
		this.mapPositionInData = hashMapToArray(normalizedActionCountsForWorlds);
		int numIterations = 0;
		try {
			numIterations = runClustering(0);
		} catch (NotEnoughMapsException e) {
			e.printStackTrace();
		}
		System.out.println("Num iterations: " + numIterations);
			
	}
	
	/**
	 * @return mapping from integer of cluster index to list of clustered mapIOs
	 */
	public HashMap<Integer, List<MapIO>> getClusters() {
		HashMap<Integer, List<MapIO>> toReturn = new HashMap<Integer, List<MapIO>>();
		//Initialize empty lists
		for (int clusterIndex = 0; clusterIndex < this.numClusters; clusterIndex++) {
			toReturn.put(clusterIndex, new ArrayList<MapIO>());
			
		}
		
		
		for (MapIO currIO : this.clusterMembership.keySet()) {
			Integer IOCluster = this.clusterMembership.get(currIO);
			List<MapIO> oldList = toReturn.get(IOCluster);
			
			oldList.add(currIO);
			toReturn.put(IOCluster, oldList);
		}
		
	
		return toReturn;
	}
	
	/**
	 * runs k means
	 * @return the number of iterations required to converge
	 * @throws NotEnoughMapsException
	 */
	private int runClustering(int minIterations) throws NotEnoughMapsException {
		initializeClusters();
		boolean converged = false;
		int numIterations = 0;
		while(!converged) {
			converged = runOneIteration();
			numIterations += 1;
			converged = converged && numIterations > minIterations;
		}
		return numIterations;
	}
	
	/**
	 * Randomly assigns the first k clusters
	 * @throws NotEnoughMapsException
	 */
	private void initializeClusters() throws NotEnoughMapsException {
		HashSet<MapIO> usedMaps = new HashSet<MapIO>();
		if (this.numClusters > this.mapPositionInData.keySet().toArray().length) {
			throw new NotEnoughMapsException();
		}
		
		for(int clusterIndex = 0; clusterIndex < this.numClusters; clusterIndex++) {
			Object[] keys = this.mapPositionInData.keySet().toArray();
			int randomIndex = rand.nextInt(keys.length-1);
			MapIO randomMap = (MapIO) keys[randomIndex];
			while(usedMaps.contains(randomMap)) {
				randomIndex = rand.nextInt(keys.length);
				randomMap = (MapIO) keys[randomIndex];
			}
			
			this.clusterMembership.put(randomMap, clusterIndex);
			this.clusterCentroids.put(clusterIndex, this.mapPositionInData.get(randomMap));
			usedMaps.add(randomMap);
		}
	}
	
	private void randomizeCentroidForCluster(int clusterIndex) {
		Object[] keys = this.mapPositionInData.keySet().toArray();
		int randomIndex = rand.nextInt(keys.length-1);
		MapIO randomMap = (MapIO) keys[randomIndex];
		this.clusterMembership.put(randomMap, clusterIndex);
		this.clusterCentroids.put(clusterIndex, this.mapPositionInData.get(randomMap));
	}
	

	
	/**
	 * runs one iteration of k means
	 * @return whether or not k means has converged
	 */
	private boolean runOneIteration() {
		boolean converged = true;

		
		//Update data points based on centroids
		HashMap<MapIO, Integer> newClusterMembership = new HashMap<MapIO, Integer>();
		for(MapIO currMap : this.mapPositionInData.keySet()) {
			double[] mapData = this.mapPositionInData.get(currMap);
			Integer bestCluster = getBestFittingCentroidFromData(mapData);
			newClusterMembership.put(currMap, bestCluster);
			Integer oldClusterMembership = this.clusterMembership.get(currMap);
			if (oldClusterMembership == null) {
				oldClusterMembership = -1;
			}
			//Check to see if switched and update converged
			if(oldClusterMembership != bestCluster) {
				converged = false;
			}
			
		}
		
		//Update centroid
		HashMap<Integer, double[]> newClusterCentroids = new HashMap<Integer, double[]> ();
		HashMap<Integer, Integer> membersOfCluster = new HashMap<Integer, Integer>();//From cluster index to number of members of that cluster
		//by first sum up centroids member's data
		for(MapIO currMap : this.clusterMembership.keySet()) {
			int clusterMembership = this.clusterMembership.get(currMap);
			double[] dataForMap = this.mapPositionInData.get(currMap);
			double [] totalDataForCluster = newClusterCentroids.get(clusterMembership);
			//Add
			if (totalDataForCluster != null) {
				dataForMap = addDoubleVectors(dataForMap, totalDataForCluster);
			}
			newClusterCentroids.put(clusterMembership, dataForMap);
			//Update number of  members of cluster
			Integer oldMembers = membersOfCluster.get(clusterMembership);
			
			if (oldMembers == null) {
				oldMembers = 0;
			}
			membersOfCluster.put(clusterMembership, oldMembers+1);
		}
		
		//Reinitializeempty clusters
		if(areEmptyClusters(membersOfCluster)) {
			System.out.println("Empty clusters");
			reinitalizeEmptyClusters(membersOfCluster);
			return false;
		}
		
		//then dividing by number of members
		for(int clusterIndex = 0; clusterIndex < this.numClusters; clusterIndex++) {
			
			double[] totalDataForCluster = newClusterCentroids.get(clusterIndex);
			int totalMembers = membersOfCluster.get(clusterIndex);
			totalDataForCluster = divideDoubleVectorBy(totalDataForCluster, totalMembers);
			newClusterCentroids.put(clusterIndex, totalDataForCluster);
		}
		
		this.clusterCentroids = newClusterCentroids;
		

		this.clusterMembership = newClusterMembership;
		
		return converged;
	}
	
	private void reinitalizeEmptyClusters(HashMap<Integer, Integer> membersOfCluster) {
		//Get empty clusters
		List<Integer> emptyClusters = new ArrayList<Integer>();
		for(int clusterIndex = 0; clusterIndex < this.numClusters; clusterIndex++) {
			Integer totalMembers = membersOfCluster.get(clusterIndex);
			if (totalMembers == null) {
				emptyClusters.add(clusterIndex);
			}
		} 
		//Reinitalize those
		for (Integer emptyClusterIndex : emptyClusters) {
			randomizeCentroidForCluster(emptyClusterIndex);
		}
	}
	
	private boolean areEmptyClusters(HashMap<Integer, Integer> membersOfCluster) {
		for (int clusterIndex = 0; clusterIndex < this.numClusters; clusterIndex++) {
			if (membersOfCluster.get(clusterIndex) == null) {
				return true;
			}
		}
		return false;
	}
	
	private double[] addDoubleVectors(double [] vec1, double[] vec2) {
		assert(vec1.length == vec2.length);
		double[] toReturn = new double[vec1.length];
		
		for(int i = 0; i < vec1.length; i++) {
			toReturn[i] = vec1[i] + vec2[i];
		}
		
		return toReturn;
	}
	
	private double[] divideDoubleVectorBy(double[] vector, int divideBy) {
		double[] toReturn = new double[vector.length];
		
		for(int i = 0; i < vector.length; i++) {
			toReturn[i] = vector[i] /(double) divideBy;
		}
		return toReturn;
	}
	
	private int getBestFittingCentroidFromData(double[] mapData) {
		double minLoss = Double.POSITIVE_INFINITY;
		List<Integer> bestCentroids = new ArrayList<Integer>();
		
		for(int centroidIndex = 0; centroidIndex < this.numClusters; centroidIndex++) {
			double[] centroidData = this.clusterCentroids.get(centroidIndex);
			double currLoss = getLoss(mapData, centroidData);
			if (currLoss == minLoss) {
				bestCentroids.add(centroidIndex);
			}
			else if (currLoss < minLoss) {
				minLoss = currLoss;
				bestCentroids = new ArrayList<Integer>();
				bestCentroids.add(centroidIndex);
			}
			
		}
		int randIndex = rand.nextInt(bestCentroids.size());
		
		return bestCentroids.get(randIndex);
		
	}
	
	private double getLoss(double[] i1, double[] i2) {
		assert(i1.length == i2.length);
		double totalLoss = 0;
		for (int i = 0; i < i1.length; i++) {
			double loss = Math.pow(i1[i] - i2[i], 2);
			totalLoss += loss;
		}
		return totalLoss;
		
	}
	

	/**
	 * 
	 * @param normalizedActionCountsForWorlds
	 * @return the input HM now mapping from a mapIO to an array of doubles
	 */
	private HashMap<MapIO, double[]> hashMapToArray(HashMap<MapIO, HashMap<GroundedAction, Double>> normalizedActionCountsForWorlds) {
		HashMap<MapIO, double[]> toReturn = new HashMap<MapIO, double[]>();
		
		for(MapIO currMapIO: normalizedActionCountsForWorlds.keySet()) {
			HashMap<GroundedAction, Double> actionCountsForMap = normalizedActionCountsForWorlds.get(currMapIO);
			double[] actionData = new double[allActions.size()];
			for(GroundedAction currAction : actionCountsForMap.keySet()) {
				int index = allActions.indexOf(currAction);
				double normCount = actionCountsForMap.get(currAction);
				actionData[index] = normCount;
			}
			
			toReturn.put(currMapIO, actionData);
			
		}
		
		return toReturn;
	}
	
	/**
	 * 
	 * @param normalizedCountsHashMap
	 * @return an ordered list of all the input actions in the input hm
	 */
	private List<GroundedAction> allActions(HashMap<MapIO, HashMap<GroundedAction, Double>> normalizedCountsHashMap) {
		//Get hash set of all action
		HashSet<GroundedAction> allActionsHS = new HashSet<GroundedAction>();
		for (MapIO currIO : normalizedCountsHashMap.keySet()) {
			HashMap<GroundedAction, Double> actionCountHM = normalizedCountsHashMap.get(currIO);
			for (GroundedAction currAction : actionCountHM.keySet()) {
				allActionsHS.add(currAction);
			}
			
		}
		//Turn into array list
		ArrayList<GroundedAction> toReturn = new ArrayList<GroundedAction>();
		
		for (GroundedAction currAction : allActionsHS) {
			toReturn.add(currAction);
		}
		
		return toReturn;
	}
	
	private void printClusters() {
		System.out.println("CENTROIDS:");
		for(int clusterIndex = 0; clusterIndex < this.numClusters; clusterIndex++) {
			System.out.println("\tIndex: " + clusterIndex);
			System.out.println("\t"+Arrays.toString(this.clusterCentroids.get(clusterIndex)));
		}
		
		System.out.println("MAPS");
		for(MapIO currMap: this.mapPositionInData.keySet()) {
			System.out.println("\tBelongs to cluster "+ this.clusterMembership.get(currMap) + ":");
			double[] data = this.mapPositionInData.get(currMap);
			System.out.println("\t" + Arrays.toString(data));
		}
		
	}
	
	

	
	
}
