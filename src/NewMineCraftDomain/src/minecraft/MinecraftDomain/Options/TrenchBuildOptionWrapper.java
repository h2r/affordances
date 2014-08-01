package minecraft.MinecraftDomain.Options;

import minecraft.NameSpace;
import minecraft.MinecraftDomain.PropositionalFunctions.AgentLookForwardAndWalkablePF;
import minecraft.MinecraftDomain.PropositionalFunctions.EmptyCellInFrontOfAgentPF;
import minecraft.MinecraftDomain.PropositionalFunctions.EndOfMapInFrontOfAgentPF;
import burlap.behavior.singleagent.planning.StateConditionTest;
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

	public TrenchBuildOptionWrapper(String name, Domain domain, RewardFunction rf, double gamma) {
		super(name, domain, rf, gamma);


		this.trenchPF = (EmptyCellInFrontOfAgentPF) this.domain.getPropFunction(NameSpace.PFEMPTYCELLINFRONT);
		this.canWalkPF = (AgentLookForwardAndWalkablePF) this.domain.getPropFunction(NameSpace.PFAGENTLOOKFORWARDWALK);
		this.endOfMapPF = (EndOfMapInFrontOfAgentPF) this.domain.getPropFunction(NameSpace.PFENDOFMAPINFRONT);
	}

	private class TrenchInitTest implements StateConditionTest {
		@Override
		public boolean satisfies(State state) {
			return true;
		}
		
	}
	
	private class TrenchTerminationTest implements StateConditionTest {
		@Override
		public boolean satisfies(State state) {
			if (endOfMapPF.isTrue(state, new String[]{NameSpace.CLASSAGENT})) return true;
			else if (counter > terminateAfter) return true;
			else if (justPlacedBlock) return true;

			else return false;
		}
	}

	private class TrenchPolicy extends MinecraftOptionPolicy{
		@Override
		protected GroundedAction getGroundedAction(State state) {
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

	@Override
	public StateConditionTest getInitTest() {
		return new TrenchInitTest();
	}

	@Override
	public MinecraftOptionPolicy getOptionPolicy() {
		return new TrenchPolicy();
	}

	@Override
	public StateConditionTest getTermTest() {
		return new TrenchTerminationTest();
	}


}

//	@Override
//	public GroundedAction getActionFromState(State state) {
//		ObjectInstance agent = state.getObjectsOfTrueClass(NameSpace.CLASSAGENT).get(0);
//		int vertDir = agent.getDiscValForAttribute(NameSpace.ATVERTDIR);
//		this.counter++;
//		//Place Block
//		if (this.trenchPF.isTrue(state, this.parameterClasses) && vertDir == 1) {
//			this.justPlacedBlock = true;
//			return this.domain.getAction(NameSpace.ACTIONPLACEBLOCK).getAllApplicableGroundedActions(state).get(0);
//		}
//		//Look down
//		else if (this.trenchPF.isTrue(state, this.parameterClasses) && vertDir > 1) {
//			return this.domain.getAction(NameSpace.ACTIONLOOKDOWN).getAllApplicableGroundedActions(state).get(0);
//		}
//			
//		//Default is to move (up to  times)
//		return this.domain.getAction(NameSpace.ACTIONMOVE).getAllApplicableGroundedActions(state).get(0);
//	}
//
//	@Override
//	public boolean optionTerminates(State state) {
//		if (this.endOfMapPF.isTrue(state, this.parameterClasses)) return true;
//		else if (counter > this.terminateAfter) return true;
//		else if (this.justPlacedBlock) return true;
//
//		else return false;
//	}

