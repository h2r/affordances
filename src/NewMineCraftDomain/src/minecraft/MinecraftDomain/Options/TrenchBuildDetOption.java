package minecraft.MinecraftDomain.Options;

import java.util.ArrayList;
import java.util.List;

import minecraft.MinecraftStateParser;
import minecraft.NameSpace;
import minecraft.MinecraftDomain.PropositionalFunctions.AgentAdjacentToTrenchPF;
import minecraft.MinecraftDomain.PropositionalFunctions.AgentLookForwardAndWalkablePF;
import minecraft.MinecraftDomain.PropositionalFunctions.EmptyCellInFrontOfAgentPF;
import minecraft.MinecraftDomain.PropositionalFunctions.EndOfMapInFrontOfAgentPF;
import burlap.behavior.singleagent.Policy.ActionProb;
import burlap.behavior.singleagent.options.Option;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.oomdp.core.AbstractGroundedAction;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.Action;
import burlap.oomdp.singleagent.GroundedAction;

public class TrenchBuildDetOption extends Option {
	EmptyCellInFrontOfAgentPF trenchPF;
	AgentLookForwardAndWalkablePF canWalkPF;
	EndOfMapInFrontOfAgentPF endOfMapPF;
	boolean justPlacedBlock;
	int counter;
	int terminateAfter = 10;
	
	public TrenchBuildDetOption(String name, Domain domain, StateHashFactory shf) {
		super(name, domain, new String[]{});
		this.trenchPF = (EmptyCellInFrontOfAgentPF) this.domain.getPropFunction(NameSpace.PFEMPTYCELLINFRONT);
		this.canWalkPF = (AgentLookForwardAndWalkablePF) this.domain.getPropFunction(NameSpace.PFAGENTLOOKFORWARDWALK);
		this.endOfMapPF = (EndOfMapInFrontOfAgentPF) this.domain.getPropFunction(NameSpace.PFENDOFMAPINFRONT);
		this.setExpectationHashingFactory(shf);
		
	}
	
	@Override
	public List<ActionProb> getActionDistributionForState(State state, String[] params) {
		List<ActionProb> toReturn = new ArrayList<ActionProb>();
		GroundedAction move = this.domain.getAction(NameSpace.ACTIONMOVE).getAllApplicableGroundedActions(state).get(0);
		
		toReturn.add(new ActionProb(move, 1.));
		return toReturn;
	}

	//Initializes local vars when option called
	@Override
	public void initiateInStateHelper(State arg0, String[] arg1) {
		this.justPlacedBlock = false;
		this.counter = 0;
	}

	@Override
	public boolean isMarkov() {
		return true;
	}
	
	//Gets the next action in the option
	@Override
	public GroundedAction oneStepActionSelection(State state, String[] params) {
		counter++;
		
		ObjectInstance agent = state.getObjectsOfTrueClass(NameSpace.CLASSAGENT).get(0);
		int vertDir = agent.getDiscValForAttribute(NameSpace.ATVERTDIR);
		this.counter++;
		//Place Block
		if (this.trenchPF.isTrue(state, params) && vertDir == 1) {
			this.justPlacedBlock = true;
			return this.domain.getAction(NameSpace.ACTIONPLACEBLOCK).getAllApplicableGroundedActions(state).get(0);
		}
		//Look down
		else if (this.trenchPF.isTrue(state, params) && vertDir > 1) {
			return this.domain.getAction(NameSpace.ACTIONLOOKDOWN).getAllApplicableGroundedActions(state).get(0);
		}
			
		
		//Default is to move (up to  times)
		
		return this.domain.getAction(NameSpace.ACTIONMOVE).getAllApplicableGroundedActions(state).get(0);
	}
	
	
	@Override
	public double probabilityOfTermination(State state, String[] params) {
		if (this.endOfMapPF.isTrue(state, params)) return 1;
		if (counter > this.terminateAfter) return 1;
		if (this.justPlacedBlock) return 1;

		else return 0;
//		if (!this.justPlacedBlock && this.canWalkPF.isTrue(state, params) || this.endOfMapPF.isTrue(state, params)) {
//			return 1;
//		}
//		return 0;
	}

	@Override
	public boolean usesDeterministicPolicy() {
		return true;
	}

	@Override
	public boolean usesDeterministicTermination() {
		return true;
	}

}
