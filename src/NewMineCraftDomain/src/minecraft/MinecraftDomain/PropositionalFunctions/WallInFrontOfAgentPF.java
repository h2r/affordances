package minecraft.MinecraftDomain.PropositionalFunctions;

import java.util.ArrayList;
import java.util.List;

import minecraft.NameSpace;
import minecraft.MinecraftDomain.Helpers;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.PropositionalFunction;
import burlap.oomdp.core.State;

public class WallInFrontOfAgentPF extends PropositionalFunction{
	
	private int distance;

	/**
	 * @param name
	 * @param domain
	 * @param parameterClasses
	 * @param object
	 */
	public WallInFrontOfAgentPF(String name, Domain domain, String[] parameterClasses, int distance) {
		super(name, domain, parameterClasses);
		this.distance = distance;
	}

	@Override
	public boolean isTrue(State state, String[] params) {
		ObjectInstance agent = state.getFirstObjectOfClass(NameSpace.CLASSAGENT);
		int avert = agent.getDiscValForAttribute(NameSpace.ATVERTDIR);

		int ax = agent.getDiscValForAttribute(NameSpace.ATX);
		int ay = agent.getDiscValForAttribute(NameSpace.ATY);
		int az = agent.getDiscValForAttribute(NameSpace.ATZ);
		
		int arot = agent.getDiscValForAttribute(NameSpace.ATROTDIR);
		
		
		// If agent looking down, then it's not looking at a wall.
		if(avert < 2) {
			return false;
		}
		
		int[]positionInFront = Helpers.positionInFrontOfAgent(1, state);
		List<ObjectInstance> objectsInFront = Helpers.objectsAt(positionInFront[0], positionInFront[1], positionInFront[2], state);
		
		for(ObjectInstance block: objectsInFront) {
			if (block.getTrueClassName().equals(NameSpace.CLASSDIRTBLOCKPICKUPABLE) || block.getTrueClassName().equals(NameSpace.CLASSDIRTBLOCKNOTPICKUPABLE)) {
//				System.out.println("AgentLookForwarDAndWalk) afx,ay,az:arot,avert " + ax + "," + ay + "," + az + ":" + arot + "," + avert);
				
				return true;
			}
		}
		return false;
	}
}