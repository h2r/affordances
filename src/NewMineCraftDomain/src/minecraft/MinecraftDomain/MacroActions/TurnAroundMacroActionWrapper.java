package minecraft.MinecraftDomain.MacroActions;

import java.util.ArrayList;
import java.util.List;

import minecraft.NameSpace;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;

public class TurnAroundMacroActionWrapper extends MinecraftMacroActionWrapper{

	public TurnAroundMacroActionWrapper(String name, State state,
			Domain domain, StateHashFactory hashingFactory, RewardFunction rf,
			double gamma) {
		super(name, state, domain, hashingFactory, rf, gamma);
	}

	@Override
	public List<GroundedAction> getGroundedActions() {
		GroundedAction rotateCGroundedAction = this.domain.getAction(NameSpace.ACTIONROTATEC).getAllApplicableGroundedActions(state).get(0);

		
		List<GroundedAction> toReturn = new ArrayList<GroundedAction>();
		toReturn.add(rotateCGroundedAction);
		toReturn.add(rotateCGroundedAction);
		
		return toReturn;
	}

}
