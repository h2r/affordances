package minecraft.WorldGenerator.WorldTypes;

import minecraft.NameSpace;

public class PlaneGoalShelfWorld extends MinecraftWorld {
	private int numPlaceBlocks;
	private int shelfHeight;
	
	/**
	 * @param numPlaceBlocks
	 * @param shelfHeight
	 */
	
	public PlaneGoalShelfWorld(int numPlaceBlocks, int shelfHeight) {
		this.numPlaceBlocks = numPlaceBlocks;
		this.shelfHeight = shelfHeight;
	}
	
	@Override
	public int getGoal() {
		return NameSpace.INTXYZGOAL;
	}

	@Override
	public int getGoalShelfHeight() {
		return this.shelfHeight;
	}
	
	@Override
	public int getNumPlaceBlocks(){
		return this.numPlaceBlocks;
	}
	
	@Override
	public String getName() {
		return "PlaneGoalShelfWorld";
	}

}
