package minecraft.WorldGenerator.WorldTypes;

import minecraft.NameSpace;

public class PlaneWorld extends MinecraftWorld {

	@Override
	public int getGoal() {
		return NameSpace.INTXYZGOAL;
	}

	@Override
	public String getName() {
		return "PlaneWorld";
	}

}
