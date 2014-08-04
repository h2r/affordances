package minecraft.WorldGenerator.WorldTypes;

import minecraft.NameSpace;

public class PlaneGoldMineWorld extends MinecraftWorld {
	@Override
	public char getFloorOf() {
		return NameSpace.CHARDIRTBLOCKNOTPICKUPABLE;
	}
	
	@Override 
	public Integer getDepthOfGoldOre() {
		return -2;
	}

	@Override
	public int getGoal() {
		return NameSpace.INTGOLDOREGOAL;
	}
	
	@Override
	public int getFloorDepth() {
		return 2;
	}

	@Override
	public String getName() {
		return "PlaneGoldMineWorld";
	}
}
