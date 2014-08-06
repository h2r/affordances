package minecraft.MinecraftDomain.Options;


import minecraft.NameSpace;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.PropositionalFunction;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;

public class WalkUntilCantOption extends MinecraftOption{
	PropositionalFunction canWalkPF;
	PropositionalFunction endOfMapPF;
	private final int giveUpAfter = 5;
	private int tries;
	
	public WalkUntilCantOption(String name, State state, Domain domain,
			RewardFunction rf, double gamma, StateHashFactory hashFactory) {
		super(name, state, domain, rf, gamma, hashFactory);
		this.canWalkPF = domain.getPropFunction(NameSpace.PFAGENTLOOKFORWARDWALK);
		this.endOfMapPF = domain.getPropFunction(NameSpace.PFENDOFMAPINFRONT);
	}

	@Override
	public GroundedAction getGroundedAction(State state) {
		ObjectInstance agent = state.getFirstObjectOfClass(NameSpace.CLASSAGENT);
		
		System.out.println("(walkuntilcant) (" +  agent.getDiscValForAttribute(NameSpace.ATX) + ", " + agent.getDiscValForAttribute(NameSpace.ATY) + ")" + " rot: " + agent.getDiscValForAttribute(NameSpace.ATROTDIR));
		
		System.out.println("Agent can walk: " + this.canWalkPF.isTrue(state, ""));
		
		return domain.getAction(NameSpace.ACTIONMOVE).getAllApplicableGroundedActions(state).get(0);
	}

	@Override
	public boolean shouldInitiate(State state) {
		return this.canWalkPF.isTrue(state, "");
	}

	@Override
	public boolean shouldTerminate(State state) {
		return !this.canWalkPF.isTrue(state, "") || this.endOfMapPF.isTrue(state, "");
	}

	@Override
	public void initiateOptionVariables() {		
		System.out.println("RESTARTING OPTION");
		this.tries = 0;
	}

	@Override
	public void updateVariablesAfterOneAction() {
		this.tries++;
	}

}
