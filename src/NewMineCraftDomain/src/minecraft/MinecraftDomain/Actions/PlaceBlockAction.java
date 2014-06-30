package minecraft.MinecraftDomain.Actions;

import java.util.List;

import minecraft.NameSpace;
import minecraft.MinecraftDomain.Helpers;
import minecraft.MinecraftStateGenerator.MinecraftStateGenerator;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;

public class PlaceBlockAction extends AgentAction {

	
	/**
	 * 
	 * @param name
	 * @param domain
	 * @param rows
	 * @param cols
	 * @param height
	 */
	public PlaceBlockAction(String name, Domain domain, int rows, int cols,int height) {
		super(name, domain, rows, cols, height, true);
		
	}

	@Override
	void doAction(State state) {
		
		ObjectInstance agent = state.getObjectsOfTrueClass(NameSpace.CLASSAGENT).get(0);
		List<ObjectInstance> objectsTwoInfrontAgent = Helpers.getBlocksInfrontOfAgent(2, state);
		
		boolean canPlace = false;
		
		//Can place against another block that collides
		for (ObjectInstance object: objectsTwoInfrontAgent) {
			if (object.getObjectClass().hasAttribute(NameSpace.ATCOLLIDES) && object.getDiscValForAttribute(NameSpace.ATCOLLIDES) == 1) {
				canPlace = true;
			}
		}
		
		int[] positionTwoAway = Helpers.positionInFrontOfAgent(2, state);
		//Or can place against edge of map
		if (!Helpers.withinMapAt(positionTwoAway[0], positionTwoAway[1], positionTwoAway[2], cols, rows, height)) {
			canPlace = true;
		}
		
		
		int[] whereWantToPlace = Helpers.positionInFrontOfAgent(1, state);
		int toPlaceX = whereWantToPlace[0];
		int toPlaceY = whereWantToPlace[1];
		int toPlaceZ = whereWantToPlace[2];
		
		//Need empty space to place
		canPlace = canPlace && Helpers.emptySpaceAt(toPlaceX, toPlaceY, toPlaceZ, state);
		
		//Need to place in bounds
		canPlace = canPlace && Helpers.withinMapAt(toPlaceX, toPlaceY, toPlaceZ, cols, rows, height);
		
		
		//Need remaining blocks to place
		int blocksLeft = agent.getDiscValForAttribute(NameSpace.ATPLACEBLOCKS);
		canPlace = canPlace && blocksLeft > 0;
		
		if (canPlace) {
			int numberOfObjects = state.getAllObjects().toArray().length;
			//Update state
			ObjectInstance toAdd = MinecraftStateGenerator.createIndWall(this.domain, toPlaceX, toPlaceY, toPlaceZ, numberOfObjects);
			state.addObject(toAdd);
			
			//Update agent's number of blocks
			agent.setValue(NameSpace.ATPLACEBLOCKS, blocksLeft-1);
		}
		
	}

}
