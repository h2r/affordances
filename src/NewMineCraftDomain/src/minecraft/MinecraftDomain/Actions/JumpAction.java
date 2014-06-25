package minecraft.MinecraftDomain.Actions;

import minecraft.NameSpace;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;

public class JumpAction extends AgentAction {

	private int amountOfJump;
	
	/**
	 * 
	 * @param name
	 * @param domain
	 * @param rows
	 * @param cols
	 * @param height
	 * @param amountOfJump
	 */
	public JumpAction(String name, Domain domain, int rows, int cols,int height, int amountOfJump) {
		super(name, domain, rows, cols, height, false);
		this.amountOfJump = amountOfJump;
	}

	@Override
	void doAction(State state) {
		ObjectInstance agent = state.getObjectsOfTrueClass(NameSpace.CLASSAGENT).get(0);
		ObjectInstance agentFeet = state.getObjectsOfTrueClass(NameSpace.CLASSAGENTFEET).get(0);
		
		int agentX = agent.getDiscValForAttribute(NameSpace.ATX);
		int agentY = agent.getDiscValForAttribute(NameSpace.ATY);
		int agentZ = agent.getDiscValForAttribute(NameSpace.ATZ);
		
		int zBelowAgentFeet = agentZ - 2;
		
		int newAgentZ = agentZ + amountOfJump;
		
		//Need a block below or the bottom of the map to jump
		boolean canJump = !ActionHelpers.withinMapAt(agentX, agentY, zBelowAgentFeet, rows, cols, height) || !ActionHelpers.emptySpaceAt(agentX, agentY, zBelowAgentFeet, state);
		//Also need empty space above the agent
		boolean roomAbove = ActionHelpers.withinMapAt(agentX, agentY, newAgentZ, rows, cols, height) && ActionHelpers.emptySpaceAt(agentX, agentY, newAgentZ, state);
		
		if (canJump && roomAbove) {
			agent.setValue(NameSpace.ATZ, newAgentZ);
			agentFeet.setValue(NameSpace.ATZ, agentZ);
			
		}
		
	}
	


	
}