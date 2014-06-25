package minecraft.MinecraftDomain.Actions;

import minecraft.NameSpace;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;

public class MovementAction extends StochasticAgentAction{
	
	/**
	 * 
	 * @param name
	 * @param domain
	 * @param rows
	 * @param cols
	 * @param height
	 */
	public MovementAction(String name, Domain domain, int rows, int cols, int height){
		super(name, domain, rows, cols, height, true);		
	}

	private Boolean emptySpaceForAgentAt(int x, int y, int z, State state) {
		return ActionHelpers.emptySpaceAt(x, y, z, state) && ActionHelpers.emptySpaceAt(x, y, z-1, state);
		
	}
	
	protected void doAction(State state){
		ObjectInstance agent = state.getObjectsOfTrueClass(NameSpace.CLASSAGENT).get(0);
		ObjectInstance agentFeet = state.getObjectsOfTrueClass(NameSpace.CLASSAGENTFEET).get(0);

		int[] inFrontAgent = ActionHelpers.positionInFrontOfAgent(1, state);
		
		int newX = inFrontAgent[0];
		int newY = inFrontAgent[1];
		int newZ = inFrontAgent[2];
		
		//Update position if nothing in agent's way and new position is within map
		if (ActionHelpers.withinMapAt(newX, newY, newZ, cols, rows, height) && ActionHelpers.withinMapAt(newX, newY, newZ-1, cols, rows, height) &&
				emptySpaceForAgentAt(newX, newY, newZ, state)) {
			agent.setValue(NameSpace.ATX, newX);
			agent.setValue(NameSpace.ATY, newY);
			agent.setValue(NameSpace.ATZ, newZ);
			
			agentFeet.setValue(NameSpace.ATX, newX);
			agentFeet.setValue(NameSpace.ATY, newY);
			agentFeet.setValue(NameSpace.ATZ, newZ-1);
		}
	}
	
}