package minecraft.MinecraftDomain.Options;

import minecraft.NameSpace;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.PropositionalFunction;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;

public class TrenchBuildOption extends MinecraftOption {
	
	private PropositionalFunction endOfMapPF;
	private PropositionalFunction trenchPF;
	public int counter;
	private final int terminateAfter = 2;
	private boolean justPlacedBlock;

	/**
	 * 
	 * @param name
	 * @param state
	 * @param domain
	 * @param rf
	 * @param gamma
	 * @param hashFactory
	 */
	public TrenchBuildOption(String name, State state, Domain domain,
			RewardFunction rf, double gamma, StateHashFactory hashFactory) {
		super(name, state, domain, rf, gamma, hashFactory);
		this.endOfMapPF = domain.getPropFunction(NameSpace.PFENDOFMAPINFRONT);
		this.trenchPF = domain.getPropFunction(NameSpace.PFEMPTYCELLINFRONT);
	}

	@Override
	public GroundedAction getGroundedAction(State state) {
//		System.out.println("Getting action");
		ObjectInstance agent = state.getObjectsOfTrueClass(NameSpace.CLASSAGENT).get(0);
		int vertDir = agent.getDiscValForAttribute(NameSpace.ATVERTDIR);
		//Place Block
		if (trenchPF.isTrue(state, "") && vertDir == 1) {
			justPlacedBlock = true;
			return domain.getAction(NameSpace.ACTIONPLACEBLOCK).getAllApplicableGroundedActions(state).get(0);
		}
		//Look down
		else if (trenchPF.isTrue(state, "") && vertDir > 1) {
			return domain.getAction(NameSpace.ACTIONLOOKDOWN).getAllApplicableGroundedActions(state).get(0);
		}
//		System.out.println(this.counter++);
		this.counter++;
		//Default is to move (up to  times)
		return domain.getAction(NameSpace.ACTIONMOVE).getAllApplicableGroundedActions(state).get(0);
	}

	@Override
	public boolean shouldTerminate(State state) {
//		System.out.println("Checking termination");
		if (this.endOfMapPF.isTrue(state, new String[]{NameSpace.CLASSAGENT})) return true;
		else if (this.counter > this.terminateAfter) {
			return true;
		}
		else if (this.justPlacedBlock) return true;
		else return false;
	}
	
	@Override
	public boolean shouldInitiate(State state) {
//		System.out.println("Checking initiation conditions");
		return true;
	}

	@Override
	public void initiateOptionVariables() {		
//		System.out.println("Initializing option variables");
		this.justPlacedBlock = false;
		this.counter = 0;
	}

	@Override
	public void updateVariablesAfterOneAction() {	
//		System.out.println("Updating option variables");
//		this.counter++;
	}



}
