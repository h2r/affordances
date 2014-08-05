package minecraft.MinecraftDomain.MacroActions;

import java.util.ArrayList;
import java.util.List;

import minecraft.NameSpace;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;

public class SprintMacroActionWrapper extends MinecraftMacroActionWrapper {

	private int numSprints = 2;
	
	/**
	 * 
	 * @param name
	 * @param state
	 * @param domain
	 * @param hashingFactory
	 * @param rf
	 * @param gamma
	 * @param numSprints
	 */
	public SprintMacroActionWrapper(String name, State state, Domain domain, StateHashFactory hashingFactory, RewardFunction rf, double gamma, int numSprints) {
		super(name, state, domain, hashingFactory, rf, gamma);
		this.numSprints = numSprints;
		this.name = this.name + numSprints;
	}
	
	public List<GroundedAction> getGroundedActions() {
		List<GroundedAction> toReturn = new ArrayList<GroundedAction>();
		
		GroundedAction moveGroundedAction = this.domain.getAction(NameSpace.ACTIONMOVE).getAllApplicableGroundedActions(state).get(0);
		
		for (int i = 0; i < this.numSprints; i++) {
			toReturn.add(moveGroundedAction);
		}
		
		return toReturn;
	}

	

}
