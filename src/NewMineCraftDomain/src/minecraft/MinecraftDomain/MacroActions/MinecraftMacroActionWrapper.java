package minecraft.MinecraftDomain.MacroActions;

import java.util.List;

import burlap.behavior.singleagent.options.MacroAction;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;


public abstract class MinecraftMacroActionWrapper {

	protected State state;
	protected Domain domain;
	protected StateHashFactory hashingFactory;
	protected RewardFunction rf;
	protected double gamma;
	protected String name;
	
	public MinecraftMacroActionWrapper(String name, State state, Domain domain, StateHashFactory hashingFactory, RewardFunction rf, double gamma) {
		this.rf = rf;
		this.gamma = gamma;
		this.state = state;
		this.domain = domain;
		this.hashingFactory = hashingFactory;
		this.name = name;
	}

	public abstract List<GroundedAction> getGroundedActions();
	
	public MacroAction getMacroAction() {
		MacroAction toReturn = new MacroAction(this.name, this.getGroundedActions());
		toReturn.keepTrackOfRewardWith(this.rf, this.gamma);
		toReturn.setExpectationHashingFactory(this.hashingFactory);
		
		return toReturn;
	}
}