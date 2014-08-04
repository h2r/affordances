package minecraft.WorldGenerator.WorldTypes;

import minecraft.NameSpace;

public class PlaneTowerWorld extends MinecraftWorld{

	private int numPlaceBlocks;
	
	/**
	 * @param numPlaceBlocks
	 */
	public PlaneTowerWorld(int numPlaceBlocks) {
		this.numPlaceBlocks = numPlaceBlocks;
	}
	
	@Override
	public int getGoal() {
		return NameSpace.INTTOWERGOAL;
	}
	
	@Override 
	public int getNumPlaceBlocks(){
		return this.numPlaceBlocks;
	}

	@Override
	public String getName() {
		return "PlaneTowerWorld";
	}
	

}
