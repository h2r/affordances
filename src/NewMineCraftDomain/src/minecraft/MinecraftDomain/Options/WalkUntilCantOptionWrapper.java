package minecraft.MinecraftDomain.Options;

import burlap.behavior.singleagent.planning.StateConditionTest;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;

public class WalkUntilCantOptionWrapper extends MinecraftOptionWrapper {

	public WalkUntilCantOptionWrapper(String optionName, Domain domain,
			RewardFunction rf, double gamma, StateHashFactory hashingFactory) {
		super(optionName, domain, rf, gamma, hashingFactory);
	}

	@Override
	public boolean getTermTest(State state) {
		return false;
	}

	@Override
	public GroundedAction getPolicyGroundedAction(State state) {
		return null;
	}


}
