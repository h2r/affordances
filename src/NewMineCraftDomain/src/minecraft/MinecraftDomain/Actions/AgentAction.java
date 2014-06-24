package minecraft.MinecraftDomain.Actions;

import java.util.List;

import minecraft.NameSpace;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.Action;

public abstract class AgentAction extends Action {
	
	protected int rows;
	protected int cols;
	protected int height;
	protected boolean causesAgentToFall;
	
	
	abstract void doAction(State state);
	
	public AgentAction(String name, Domain domain, int rows, int cols, int height, boolean causesAgentToFall){
		super(name, domain, "");
		this.rows = rows;
		this.cols = cols;
		this.height = height;
		this.causesAgentToFall = causesAgentToFall;
	}
	
	protected AgentAction getAction() {
		return this;
	}
	
	private void performPostActionUpdates(State state) {
		fallAllObjects(state, rows, cols, height);
		pickUpItems(state);
	}
	
	@Override
	protected State performActionHelper(State state, String[] params) {
		AgentAction toPerform = getAction();
		toPerform.doAction(state);
		performPostActionUpdates(state);
		return state;
	}
	
	private void pickUpItems(State state) {
		ObjectInstance agent = state.getObjectsOfTrueClass(NameSpace.CLASSAGENT).get(0);
		
		for (ObjectInstance object: state.getAllObjects()) {
			if (object.getObjectClass().hasAttribute(NameSpace.ATDESTWHENWALKED) && object.getDiscValForAttribute(NameSpace.ATDESTWHENWALKED) == 1 &&
					objectAtAgentLocation(object, agent)) {
				
				ActionHelpers.removeObjectFromState(object, state, this.domain);
			}
		}
	}
	
	private boolean objectAtAgentLocation(ObjectInstance object, ObjectInstance agent) {
		if (!object.getObjectClass().hasAttribute(NameSpace.ATX)) {
			return false;
		}
		int objX = object.getDiscValForAttribute(NameSpace.ATX);
		int objY = object.getDiscValForAttribute(NameSpace.ATY);
		int objZ = object.getDiscValForAttribute(NameSpace.ATZ);
		
		int agentX = agent.getDiscValForAttribute(NameSpace.ATX);
		int agentY = agent.getDiscValForAttribute(NameSpace.ATY);
		int agentZ = agent.getDiscValForAttribute(NameSpace.ATZ);
		
		return objX == agentX && objY == agentY && (objZ == agentZ || objZ == agentZ-1);
	}
	
	
	
	private boolean fall(ObjectInstance object, State state, int rows,  int cols, int height) {
		int x = object.getDiscValForAttribute(NameSpace.ATX);
		int y = object.getDiscValForAttribute(NameSpace.ATY);
		int z = object.getDiscValForAttribute(NameSpace.ATZ);
		int newZ = z-1;
		String objectName = object.getObjectClass().name;
		
		//Agent feet falling
		if (objectName.equals(NameSpace.CLASSAGENTFEET)) {
			
			
			//Break if action doesn't cause falling
			if (!this.causesAgentToFall) {
				return false;
			}
			
			if (ActionHelpers.withinMapAt(x, y, newZ, cols, rows, height) && ActionHelpers.emptySpaceAt(x, y, newZ, state)) {
				object.setValue(NameSpace.ATZ, newZ);
			}
			
		}
		//Other falling
		if (ActionHelpers.withinMapAt(x, y, newZ, cols, rows, height) && ActionHelpers.emptySpaceAt(x, y, newZ, state)) {
			object.setValue(NameSpace.ATZ, newZ);
			return true;
		}
		return false;
		
		
	}
	
	private void fallAllObjects(State state, int rows, int cols, int height) {
		//Make agents feet then agent fall
		ObjectInstance agent = state.getObjectsOfTrueClass(NameSpace.CLASSAGENT).get(0);
		ObjectInstance agentFeet = state.getObjectsOfTrueClass(NameSpace.CLASSAGENTFEET).get(0);
		String agentName = agent.getName();
		String agentFeetName = agentFeet.getName();
		
		
		if (fall(agentFeet, state, rows, cols, height)){
			fall(agent, state, rows, cols, height);
		}
		
		//Make rest fall
		List<ObjectInstance> allObjects = state.getAllObjects();
		for (ObjectInstance object: allObjects) {
			if (!object.getName().equals(agentName)&&!object.getName().equals(agentFeetName)&&
					object.getObjectClass().hasAttribute(NameSpace.ATFLOATS) && object.getDiscValForAttribute(NameSpace.ATFLOATS) == 0) {
				fall(object, state, rows, cols, height);
			}
			
		}
	}
	
}