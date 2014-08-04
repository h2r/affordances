package minecraft.WorldGenerator.WorldTypes;

import minecraft.NameSpace;

public abstract class MinecraftWorld {
	//Default values
	private final int defaultNumWalls = 0;
	private final int defaultNumTrenches = 0;
	private final char defaultWallOf = NameSpace.CHARDIRTBLOCKNOTPICKUPABLE;
	private final char defaultFloorOf = NameSpace.CHARINDBLOCK;
	private final boolean defaultTrenchStraightAndBetweenAgentAndGoal = true;
	private final boolean defaultWallsStraightAndBetweenAgentAndGoal = true;
	private final int defaultFloorDepth = 1;
	private final Integer defaultGoldOreDepth = 0;//Agent's feet is the origin
	private final int defaultNumPlaceBlocks = 0;//Num blocks agent can place
	private final int defaultGoalShelfHeight = 0;
	
	//Things that definetely need to be overridden
	public abstract int getGoal();
	public abstract String getName();

	
	
	//Defaulted world features getters:
	public int getGoalShelfHeight() {
		return this.defaultGoalShelfHeight;
	}
	
	public Integer getDepthOfGoldOre() {
		return this.defaultGoldOreDepth;
	}
	
	public boolean getTrenchStraightAndBetweenAgentAndGoal() {
		 return this.defaultTrenchStraightAndBetweenAgentAndGoal;
	}
	public int getNumWalls() {
		return this.defaultNumWalls;
	}
	
	public char getWallOf() {
		return this.defaultWallOf;
	}
	
	public char getFloorOf() {
		return this.defaultFloorOf;
	}
	
	public int getNumTrenches() {
		return this.defaultNumTrenches;
	}
	
	public int getFloorDepth() {
		return this.defaultFloorDepth;
	}
	
	public boolean getwallsStraightAndBetweenAgentAndGoal() {
		return defaultWallsStraightAndBetweenAgentAndGoal;
	}
	
	public int getNumPlaceBlocks() {
		return this.defaultNumPlaceBlocks;
	}
	
}
