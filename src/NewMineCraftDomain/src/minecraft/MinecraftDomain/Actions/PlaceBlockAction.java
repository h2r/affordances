package minecraft.MinecraftDomain.Actions;

import java.util.List;

import minecraft.MinecraftInitialStateGenerator;
import minecraft.NameSpace;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;

public class PlaceBlockAction extends AgentAction {

	public PlaceBlockAction(String name, Domain domain, int rows, int cols,int height) {
		super(name, domain, rows, cols, height, true);
		
	}

	@Override
	void doAction(State state) {
		int numberOfObjects = state.getAllObjects().toArray().length;
		ObjectInstance agent = state.getObjectsOfTrueClass(NameSpace.CLASSAGENT).get(0);
		List<ObjectInstance> objectsTwoInfrontAgent = ActionHelpers.getBlocksInfrontOfAgent(2, state);
		
		boolean canPlace = false;
		
		//Can place against another block that collides
		for (ObjectInstance object: objectsTwoInfrontAgent) {
			if (object.getObjectClass().hasAttribute(NameSpace.ATCOLLIDES) && object.getDiscValForAttribute(NameSpace.ATCOLLIDES) == 1) {
				canPlace = true;
			}
		}
		
		int[] positionTwoAway = ActionHelpers.positionInFrontOfAgent(2, state);
		//Or can place against edge of map
		if (!ActionHelpers.withinMapAt(positionTwoAway[0], positionTwoAway[1], positionTwoAway[2], cols, rows, height)) {
			canPlace = true;
		}
		
		
		int[] whereWantToPlace = ActionHelpers.positionInFrontOfAgent(1, state);
		int toPlaceX = whereWantToPlace[0];
		int toPlaceY = whereWantToPlace[1];
		int toPlaceZ = whereWantToPlace[2];
		
		//Need empty space to place
		canPlace = canPlace && ActionHelpers.emptySpaceAt(toPlaceX, toPlaceY, toPlaceZ, state);
		
		//Need to place in bounds
		canPlace = canPlace && ActionHelpers.withinMapAt(toPlaceX, toPlaceY, toPlaceZ, cols, rows, height);
		
		
		//Need remaining blocks to place
		int blocksLeft = agent.getDiscValForAttribute(NameSpace.ATPLACEBLOCKS);
		canPlace = canPlace && blocksLeft > 0;
		

		
		if (canPlace) {
			//Update state
			ObjectInstance toAdd = MinecraftInitialStateGenerator.createDirtBlock(this.domain, toPlaceX, toPlaceY, toPlaceZ, numberOfObjects);
			state.addObject(toAdd);
			
			//Update agent's number of blocks
			agent.setValue(NameSpace.ATPLACEBLOCKS, blocksLeft-1);
		}
		
	}

}
