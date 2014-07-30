package minecraft.MinecraftDomain.PropositionalFunctions;

import java.util.List;

import minecraft.NameSpace;
import minecraft.MinecraftDomain.Helpers;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.PropositionalFunction;
import burlap.oomdp.core.State;

public class AgentLookForwardAndWalkablePF extends PropositionalFunction{
	

	
	String objectStringToCheckAgainst; 
	
	/**
	 * @param name
	 * @param domain
	 * @param parameterClasses
	 * @param object
	 */
	public AgentLookForwardAndWalkablePF(String name, Domain domain, String[] parameterClasses) {
		super(name, domain, parameterClasses);
	}

	@Override
	public boolean isTrue(State state, String[] params) {

		ObjectInstance agent = state.getFirstObjectOfClass(NameSpace.CLASSAGENT);
		
		int ax = agent.getDiscValForAttribute(NameSpace.ATX);
		int ay = agent.getDiscValForAttribute(NameSpace.ATY);
		int az = agent.getDiscValForAttribute(NameSpace.ATZ);
		
		int agentDir = agent.getDiscValForAttribute(NameSpace.ATROTDIR);
		
		int dx, dy;
		switch(agentDir) {
			case(0):
				dx = 0;
				dy = -1;
				break;
			case(1):
				dx = 1;
				dy = 0;
				break;
			case(2):
				dx = 0;
				dy = 1;
				break;
			default:
				dx = -1;
				dy = 0;
		}
		
		List<ObjectInstance> objectsInFrontHead = Helpers.objectsAt(ax + dx, ay + dy, az, state);
		List<ObjectInstance> objectsInFrontFeet = Helpers.objectsAt(ax + dx, ay + dy, az - 1, state);
		List<ObjectInstance> objectsInFrontBelow = Helpers.objectsAt(ax + dx, ay + dy, az - 2, state);
		
		// Is a block blocking at the head?
		for(ObjectInstance block: objectsInFrontHead) {
			if (block.getBooleanValue(NameSpace.ATCOLLIDES)) {
				return false;
			}
		}
		// Is a block blocking at the feet?
		for(ObjectInstance block: objectsInFrontFeet) {
			if (block.getBooleanValue(NameSpace.ATCOLLIDES)) {
				return false;
			}
		}
		
		int arot = agent.getDiscValForAttribute(NameSpace.ATROTDIR);
		
		// Is there a block below to walk on?
		for(ObjectInstance block: objectsInFrontBelow) {
			if (block.getBooleanValue(NameSpace.ATCOLLIDES)) {
//				System.out.println("AgentLookForwarDAndWalk) afx,ay,az:arot " + ax + "," + ay + "," + az + ":" + arot);
				return true;
			}
		}
		return false;
	}
}
