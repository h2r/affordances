package tests;

import java.util.ArrayList;
import java.util.List;

import minecraft.NameSpace;

public class Result {

	private double avgBellmanUpdates;
	private double avgAccumulatedReward;
	private double taskCompletedRate;
	private double avgCpuTime;

	private List<Double> bellmanUpdateTrials = new ArrayList<Double>();
	private List<Double> accumulatedRewardTrials = new ArrayList<Double>();
	private List<Double> didFinishTrials = new ArrayList<Double>();
	private List<Integer> cpuTrials = new ArrayList<Integer>();
	private int numTrials;
	private int numCompleted;
	
	private String plannerName;
	private double bellmanDeviation;
	private double rewardDeviation;
	private double completedDeviation;
	private double cpuDeviation;
	
	public Result(String plannerName) {
		this.plannerName = plannerName;
	}
	
	/**
	 * Computes the average values given data across this.numTrials.
	 */
	private void computeAverages() {
		// If we haven't received any trials, return.
		if(numTrials < 1) {
			return;
		}
		
		double totalBellmanUpdates = 0.0;
		double totalReward = 0.0;
		double totalCpuTime = 0.0;
		for(int i = 0; i < this.numTrials; i++) {
			if(this.didFinishTrials.get(i) == 1.0) {
				totalBellmanUpdates += this.bellmanUpdateTrials.get(i);
				totalReward += this.accumulatedRewardTrials.get(i);
				totalCpuTime += this.cpuTrials.get(i);
			}
		}
		
		// Task completion rate
		double totalFinishedTrials = 0.0;
		for (Double df : didFinishTrials) {
			totalFinishedTrials = totalFinishedTrials + df;
		}

		this.avgBellmanUpdates = totalBellmanUpdates / this.numCompleted;
		this.avgAccumulatedReward = totalReward / this.numCompleted; // Only average over completed trials;
		this.taskCompletedRate = totalFinishedTrials / this.numTrials;
		this.avgCpuTime = (totalCpuTime / this.numCompleted) / 1000.0; //convert from milliseconds to seconds
	}
	
	/**
	 * Computes the (sample) standard deviation of each data type
	 */
	private void computeDeviations() {
		// If we haven't received any trials, return.
		if(numTrials < 1) {
			return;
		}
		
		// If we only ran 1 trial, deviation is 0.
		if(this.numTrials == 1) {
			this.bellmanDeviation = 0.0;
			this.rewardDeviation = 0.0;
			this.completedDeviation = 0.0;
			this.cpuDeviation = 0.0;
			return;
		}
		
		// Bellman updates and Reward
		if(this.numCompleted <= 1) {
			// If only 1 or 0 tasks completed, then deviation is 0, otherwise compute as normal.
			this.rewardDeviation = 0.0;
			this.bellmanDeviation = 0.0;
			this.cpuDeviation = 0.0;
		}
		else {
			double sumOfAvgDiffBellmanSqd = 0.0;
			double sumOfAvgDiffRewardSqd = 0.0;
			double sumOfAvgDiffCPUSqd = 0.0;
			for (int i = 0; i < this.numTrials; i++) {
				if(this.didFinishTrials.get(i) == 1.0) {
					sumOfAvgDiffBellmanSqd += Math.pow((this.bellmanUpdateTrials.get(i) - this.avgBellmanUpdates),2);
					sumOfAvgDiffRewardSqd += Math.pow((this.accumulatedRewardTrials.get(i) - this.avgAccumulatedReward),2);
					sumOfAvgDiffCPUSqd += Math.pow(((this.cpuTrials.get(i) / 1000.0) - this.avgCpuTime),2); // Divide by 1000 to convert from ms to seconds
				}
			}
			this.bellmanDeviation = Math.sqrt(((double)sumOfAvgDiffBellmanSqd) / (this.numTrials - 1));
			this.rewardDeviation = Math.sqrt(((double)sumOfAvgDiffRewardSqd) / (this.numCompleted - 1));
			this.cpuDeviation = Math.sqrt(((double)sumOfAvgDiffCPUSqd) / (this.numTrials - 1));
		}
		
		// Task completion rate
		double sumOfAvgDiffCompeletedSqd = 0.0;
		for (Double df : didFinishTrials) {
			sumOfAvgDiffCompeletedSqd = Math.pow((df - this.taskCompletedRate),2);
		}
		this.completedDeviation = Math.sqrt(((double)sumOfAvgDiffCompeletedSqd) / (this.numTrials - 1));
	}
	
	/**
	 * Adds a single trial to the result data and averages the totals.
	 * @param trial: a double list containing {bellmanUpdates, reward, completed, cpuTime}.
	 */
	public void addTrial(double[] trial) {
		
		// Only count bellmanUpdates, cpu, and reward if we succeeded on the map.
		if(trial[2] == 1.0) {
			this.bellmanUpdateTrials.add(trial[0]);
			this.accumulatedRewardTrials.add(trial[1]);
			this.cpuTrials.add((int) trial[3]);
			++this.numCompleted;
		}
		else{
			this.accumulatedRewardTrials.add(null);
			this.cpuTrials.add(null);
			this.bellmanUpdateTrials.add(null);
		}
		
		this.didFinishTrials.add(trial[2]);
		
		++this.numTrials;
		computeAverages();
		computeDeviations();
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
		this.numCompleted = 0;
		this.avgBellmanUpdates = 0;
		this.avgAccumulatedReward = 0;
		this.taskCompletedRate = 0;
		this.avgCpuTime = 0;
	}
	
	// --- ACCESSORS ---
	
	public double getAvgBellmanUpdates() {
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
	
	public double getBellmanDeviation() {
		return this.bellmanDeviation;
	}
	
	public double getRewardDeviation() {
		return this.rewardDeviation;
	}
	
	public double getCompletedDeviation() {
		return this.completedDeviation;
	}
	
	public double getCPUDeviation() {
		return this.cpuDeviation;
	}
	
	/**
	 * Returns a string containing the results from every trial.
	 * @return
	 */
	public String getAllResults() {
		computeAverages();
		
		String bellman = "Bellman: ";
		String reward = "Reward: ";
		String completed = "Completed: ";
		String cpu = "CPU: ";
		for(int i = 0; i < this.numTrials; i++) {

			
			completed += String.format("%.2f", this.didFinishTrials.get(i)) + ",";
			if(this.didFinishTrials.get(i) == 1.0) {
				reward += String.format("%.2f", this.accumulatedRewardTrials.get(i)) + ",";
				bellman += String.format("%.2f", this.bellmanUpdateTrials.get(i)) + ",";
				cpu += (this.cpuTrials.get(i) / 1000.0) + "s,";
			}
			else {
				reward += "null,";
				bellman += "null,";
				cpu += "null,";
			}
		}
		
		String result = plannerName + "\n" + bellman + "\n" + reward + "\n" + completed + "\n" + cpu + "\n";
		
		return result;
	}
	
	/**
	 * Returns a string containing the averages (with deviations) of the trials.
	 * @return
	 */
	public String getAverages() {
		return this.toString();
	}
	
	public String toString() {
		computeAverages();
		computeDeviations();
		String result = plannerName + " " + this.avgBellmanUpdates + ".(" 
		+ String.format(NameSpace.DOUBLEFORMAT, this.bellmanDeviation) + ") , " 
		+ String.format(NameSpace.DOUBLEFORMAT, this.avgAccumulatedReward) + ".(" 
		+ String.format(NameSpace.DOUBLEFORMAT, this.rewardDeviation) + ") , " 
		+ String.format(NameSpace.DOUBLEFORMAT, this.taskCompletedRate) + ".(" 
		+ String.format(NameSpace.DOUBLEFORMAT, this.completedDeviation) + ") , " 
		+ String.format(NameSpace.DOUBLEFORMAT, this.avgCpuTime) + "s.(" 
		+ String.format(NameSpace.DOUBLEFORMAT, this.cpuDeviation) + ")"; 
		
		return result;
	}

}
