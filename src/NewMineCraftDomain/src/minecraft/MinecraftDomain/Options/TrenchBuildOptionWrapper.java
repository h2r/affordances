package minecraft.MinecraftDomain.Options;

import minecraft.NameSpace;
import minecraft.MinecraftDomain.PropositionalFunctions.AgentLookForwardAndWalkablePF;
import minecraft.MinecraftDomain.PropositionalFunctions.EmptyCellInFrontOfAgentPF;
import minecraft.MinecraftDomain.PropositionalFunctions.EndOfMapInFrontOfAgentPF;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;

public class TrenchBuildOptionWrapper extends MinecraftOptionWrapper {
	protected EmptyCellInFrontOfAgentPF trenchPF;
	protected AgentLookForwardAndWalkablePF canWalkPF;
	protected EndOfMapInFrontOfAgentPF endOfMapPF;
	boolean justPlacedBlock;
	int counter;
	int terminateAfter = 100;

	/**
	 * 
	 * @param name
	 * @param domain
	 * @param rf
	 * @param gamma
	 * @param hashingFactory
	 */
	public TrenchBuildOptionWrapper(String name, Domain domain, RewardFunction rf, double gamma, StateHashFactory hashingFactory) {
		super(name, domain, rf, gamma, hashingFactory);

		this.trenchPF = (EmptyCellInFrontOfAgentPF) this.domain.getPropFunction(NameSpace.PFEMPTYCELLINFRONT);
		this.canWalkPF = (AgentLookForwardAndWalkablePF) this.domain.getPropFunction(NameSpace.PFAGENTLOOKFORWARDWALK);
		this.endOfMapPF = (EndOfMapInFrontOfAgentPF) this.domain.getPropFunction(NameSpace.PFENDOFMAPINFRONT);
	}
	@Override
	public boolean getTermTest(State state) {
		if (endOfMapPF.isTrue(state, new String[]{NameSpace.CLASSAGENT})) return true;
		else if (counter > terminateAfter) return true;
		else if (justPlacedBlock) return true;
		else return false;
	}

	@Override
	public GroundedAction getPolicyGroundedAction(State state) {
		ObjectInstance agent = state.getObjectsOfTrueClass(NameSpace.CLASSAGENT).get(0);
		int vertDir = agent.getDiscValForAttribute(NameSpace.ATVERTDIR);
		counter++;
		//Place Block
		if (trenchPF.isTrue(state, "") && vertDir == 1) {
			justPlacedBlock = true;
			return domain.getAction(NameSpace.ACTIONPLACEBLOCK).getAllApplicableGroundedActions(state).get(0);
		}
		//Look down
		else if (trenchPF.isTrue(state, "") && vertDir > 1) {
			return domain.getAction(NameSpace.ACTIONLOOKDOWN).getAllApplicableGroundedActions(state).get(0);
		}

		//Default is to move (up to  times)
		return domain.getAction(NameSpace.ACTIONMOVE).getAllApplicableGroundedActions(state).get(0);
	}
		


}


