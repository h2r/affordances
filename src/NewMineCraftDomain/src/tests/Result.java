package tests;

import java.util.ArrayList;
import java.util.List;

public class Result {

	private int avgBellmanUpdates;
	private double avgAccumulatedReward;
	private double taskCompletedRate;
	private double avgCpuTime;

	private List<Double> bellmanUpdateTrials = new ArrayList<Double>();
	private List<Double> accumulatedRewardTrials = new ArrayList<Double>();
	private List<Double> didFinishTrials = new ArrayList<Double>();
	private List<Integer> cpuTrials = new ArrayList<Integer>();
	private int numTrials;
	
	private String plannerName;
	
	public Result(String plannerName) {
		this.plannerName = plannerName;
	}
	
	/**
	 * Computes the average values given data across this.numTrials.
	 */
	private void computeAverages() {
		int totalBellmanUpdates = 0;
		for (Double bu : bellmanUpdateTrials) {
			totalBellmanUpdates = (int) (totalBellmanUpdates + bu);
		}
		
		double totalReward = 0.0;
		for (Double r : accumulatedRewardTrials) {
			totalReward = totalReward + r;
		}
		
		double totalFinishedTrials = 0.0;
		for (Double df : didFinishTrials) {
			totalFinishedTrials = totalFinishedTrials + df;
		}
		
		double totalCpuTime = 0.0;
		for (Integer cput : cpuTrials) {
			totalCpuTime = totalCpuTime + cput;
		}
		
		this.avgBellmanUpdates = totalBellmanUpdates / this.numTrials;
		this.avgAccumulatedReward = totalReward / this.numTrials;
		this.taskCompletedRate = totalFinishedTrials / this.numTrials;
		this.avgCpuTime = (totalCpuTime / this.numTrials) / 1000.0; //convert from milliseconds to seconds
	}

	public int getAvgBellmanUpdates() {
		return this.avgBellmanUpdates;
	}
	
	public double getAccumulatedReward() {
		return this.avgAccumulatedReward;
	}
	
	public double getCompletionRate() {
		return this.taskCompletedRate;
	}
	
	public double getAvgCpuTime() {
		return this.avgCpuTime;
	}
	
	/**
	 * Adds a single trial to the result data and averages the totals.
	 * @param trial: a double list containing {bellmanUpdates, reward, completed}.
	 */
	public void addTrial(double[] trial) {
		this.bellmanUpdateTrials.add(trial[0]);
		this.accumulatedRewardTrials.add(trial[1]);
		this.didFinishTrials.add(trial[2]);
		this.cpuTrials.add((int) trial[3]);
		++this.numTrials;
		computeAverages();
	}
	
	/**
	 * Clears all instance data.
	 */
	public void clear() {
		this.bellmanUpdateTrials = new ArrayList<Double>();
		this.accumulatedRewardTrials = new ArrayList<Double>();
		this.didFinishTrials = new ArrayList<Double>();
		this.cpuTrials = new ArrayList<Integer>();
		
		this.numTrials = 0;
		this.avgBellmanUpdates = 0;
		this.avgAccumulatedReward = 0;
		this.taskCompletedRate = 0;
		this.avgCpuTime = 0;
	}
	
	/**
	 * Converts to string for use in writing results to file.
	 */
	public String toString() {
		computeAverages();
		String result = plannerName + " " + this.avgBellmanUpdates + ", " + String.format("%.2f", this.avgAccumulatedReward) + ", " + String.format("%.2f", this.taskCompletedRate) + ", " + String.format("%.2f", this.avgCpuTime) + "s"; 
		return result;
	}

}
