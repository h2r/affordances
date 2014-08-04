package minecraft.MinecraftDomain.Options;

import java.util.ArrayList;
import java.util.List;

import burlap.behavior.singleagent.Policy;
import burlap.oomdp.core.AbstractGroundedAction;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.GroundedAction;

public abstract class MinecraftOptionPolicy extends Policy {

	protected abstract GroundedAction getGroundedAction(State state);
	
	@Override
	public AbstractGroundedAction getAction(State state) {
		return getGroundedAction(state);
	}

	@Override
	public List<ActionProb> getActionDistributionForState(State state) {
		List<ActionProb> actionProbs = new ArrayList<ActionProb>();
		actionProbs.add(new ActionProb(this.getGroundedAction(state), 1.0));
		return actionProbs;
	}

	@Override
	public boolean isDefinedFor(State state) {
		return true;
	}

	@Override
	public boolean isStochastic() {
		return false;
	}

}
