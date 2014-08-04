package minecraft.MinecraftDomain.Options;

import java.util.ArrayList;
import java.util.List;

import minecraft.NameSpace;
import burlap.behavior.singleagent.options.MacroAction;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.GroundedAction;

public class SprintMacroActionWrapper {

	private State state;
	private Domain domain;
	private int numSprints = 2;
	
	public SprintMacroActionWrapper(State state, Domain domain) {
		this.state = state;
		this.domain = domain;
	}
	
	public SprintMacroActionWrapper(State state, Domain domain, int numSprints) {
		this.state = state;
		this.domain = domain;
		this.numSprints = numSprints;
	}
	
	public List<GroundedAction> getGroundedActions() {
		List<GroundedAction> toReturn = new ArrayList<GroundedAction>();
		
		GroundedAction moveGroundedAction = this.domain.getAction(NameSpace.ACTIONMOVE).getAllApplicableGroundedActions(state).get(0);
		
		for (int i = 0; i < this.numSprints; i++) {
			toReturn.add(moveGroundedAction);
		}
		
		return toReturn;
	}
	
	private class SprintMacroAction extends MacroAction {

		public SprintMacroAction() {
			super("Sprint macro action", getGroundedActions());
		}
		
	}
	
	public MacroAction getMacroAction() {
		return new SprintMacroAction();
	}

}
