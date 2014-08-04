package minecraft.WorldGenerator.WorldTypes;

import minecraft.NameSpace;

public class DeepTrenchWorld extends MinecraftWorld {
	private int numTrenches;
	
	/**
	 * @param numTrenches
	 */
	public DeepTrenchWorld(int numTrenches) {
		this.numTrenches = numTrenches;
	}
	


	@Override
	public int getGoal() {
		return NameSpace.INTXYZGOAL;
	}
	
	@Override
	public int getNumTrenches() {
		return this.numTrenches;
	}
	
	@Override
	public int getNumPlaceBlocks() {
		return this.numTrenches;
	}
	
	@Override
	public int getFloorDepth(){
		return 2;
	}



	@Override
	public String getName() {
		return "DeepTrenchWorld";
	}
}
