package minecraft.WorldGenerator.WorldTypes;

import minecraft.NameSpace;

public class PlaneGoldSmeltWorld extends MinecraftWorld {

	@Override
	public int getGoal() {
		return NameSpace.INTGOLDBARGOAL;
	}

	@Override
	public String getName() {
		return "PlaneGoldSmeltWorld";
	}

}
