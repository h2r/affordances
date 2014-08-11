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
		// Bellman Updates
		int totalBellmanUpdates = 0;
		for (Double bu : bellmanUpdateTrials) {
			totalBellmanUpdates = (int) (totalBellmanUpdates + bu);
		}
		
		// Reward
		double totalReward = 0.0;
		for (Double r : accumulatedRewardTrials) {
			totalReward = totalReward + r;
		}
		
		// Task completion rate
		double totalFinishedTrials = 0.0;
		for (Double df : didFinishTrials) {
			totalFinishedTrials = totalFinishedTrials + df;
		}
		
		// CPU time
		double totalCpuTime = 0.0;
		for (Integer cput : cpuTrials) {
			totalCpuTime = totalCpuTime + cput;
		}
		
		this.avgBellmanUpdates = totalBellmanUpdates / this.numTrials;
		this.avgAccumulatedReward = totalReward / this.numTrials;
		this.taskCompletedRate = totalFinishedTrials / this.numTrials;
		this.avgCpuTime = (totalCpuTime / this.numTrials) / 1000.0; //convert from milliseconds to seconds
	}
	
	/**
	 * Computes the standard deviation of each data type
	 */
	private void computeDeviations() {
		// Bellman updates
		int sumOfAvgDiffBellmanSqd = 0;
		for (Double bu : bellmanUpdateTrials) {
			sumOfAvgDiffBellmanSqd += Math.pow((bu - this.avgBellmanUpdates),2);
		}
		this.bellmanDeviation = Math.sqrt(((double)sumOfAvgDiffBellmanSqd) / this.numTrials);
		
		// Reward
		double sumOfAvgDiffRewardSqd = 0.0;
		for (Double r : accumulatedRewardTrials) {
			sumOfAvgDiffRewardSqd = Math.pow((r - this.avgAccumulatedReward),2);
		}
		this.rewardDeviation = Math.sqrt(((double)sumOfAvgDiffRewardSqd) / this.numTrials);
		
		// Task completion rate
		double sumOfAvgDiffCompeletedSqd = 0.0;
		for (Double df : didFinishTrials) {
			sumOfAvgDiffCompeletedSqd = Math.pow((df - this.taskCompletedRate),2);
		}
		this.completedDeviation = Math.sqrt(((double)sumOfAvgDiffCompeletedSqd) / this.numTrials);
		
		// CPU time
		double sumOfAvgDiffCPUSqd = 0.0;
		for (Integer cput : cpuTrials) {
			sumOfAvgDiffCPUSqd = Math.pow((cput - this.avgCpuTime),2);
		}
		this.cpuDeviation = Math.sqrt(((double)sumOfAvgDiffCPUSqd) / this.numTrials) / 1000.0; // Divide by 1000 to convert from ms to seconds
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
		this.avgBellmanUpdates = 0;
		this.avgAccumulatedReward = 0;
		this.taskCompletedRate = 0;
		this.avgCpuTime = 0;
	}
	
	// --- ACCESSORS ---
	
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
	
	public String toString() {
		computeAverages();
		String result = plannerName + " " 
		+ this.avgBellmanUpdates + ".(" + String.format("%.2f", this.bellmanDeviation) + ") , " 
		+ String.format("%.2f", this.avgAccumulatedReward) + ".(" + String.format("%.2f", this.rewardDeviation) + ") , " 
		+ String.format("%.2f", this.taskCompletedRate) + ".(" + String.format("%.2f", this.completedDeviation) + ") , " 
		+ String.format("%.2f", this.avgCpuTime) + "s.(" + String.format("%.2f", this.cpuDeviation) + ")"; 
		
		return result;
	}

}
