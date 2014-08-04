package minecraft.MinecraftDomain.Options;

import java.util.ArrayList;
import java.util.List;

import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.Policy.ActionProb;
import burlap.behavior.singleagent.options.Option;
import burlap.behavior.singleagent.options.PolicyDefinedSubgoalOption;
import burlap.behavior.singleagent.planning.StateConditionTest;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.GroundedAction;

interface MinecraftOptionInterface {

	public abstract Policy getOptionPolicy();
	public abstract StateConditionTest getStateConditionTest();
	
	
//	@Override
//	public List<ActionProb> getActionDistributionForState(State state, String[] params) {
//		List<ActionProb> actionProbs = new ArrayList<ActionProb>();
//		actionProbs.add(new ActionProb(this.getActionFromState(state), 1.0));
//		return actionProbs;
//	}
//
//	@Override
//	public void initiateInStateHelper(State arg0, String[] arg1) {
//		
//	}
//
//	@Override
//	public boolean isMarkov() {
//		return true;
//	}
//
//	@Override
//	public GroundedAction oneStepActionSelection(State state, String[] arg1) {
//		return this.getActionFromState(state);
//	}
//
//	@Override
//	public double probabilityOfTermination(State state, String[] arg1) {
//		if (this.optionTerminates(state)) return 1;
//		return 0;
//	}
//
//	@Override
//	public boolean usesDeterministicPolicy() {
//		return true;
//	}
//
//	@Override
//	public boolean usesDeterministicTermination() {
//		return true;
//	}

}
