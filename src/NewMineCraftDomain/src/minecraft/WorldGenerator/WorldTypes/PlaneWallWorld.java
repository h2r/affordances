package minecraft.WorldGenerator.WorldTypes;

import minecraft.NameSpace;

public class PlaneWallWorld extends MinecraftWorld{
	private int numWalls;
	
	/**
	 * @param numWalls
	 */
	public PlaneWallWorld(int numWalls) {
		this.numWalls = numWalls;
	}
	
	@Override
	public int getGoal() {
		return NameSpace.INTXYZGOAL;
	}
	
	@Override
	public int getNumWalls(){
		return this.numWalls;
	}

	@Override
	public String getName() {
		return "PlaneWallWorld";
	}

}
