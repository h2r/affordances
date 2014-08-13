package minecraft.MinecraftBehavior.Planners;

import java.util.ArrayList;
import java.util.List;

import burlap.behavior.singleagent.planning.OOMDPPlanner;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.Action;
import burlap.oomdp.singleagent.RewardFunction;
import minecraft.MapIO;
import minecraft.NameSpace;
import minecraft.MinecraftBehavior.MinecraftBehavior;
import minecraft.MinecraftDomain.MacroActions.BuildTrenchMacroAction;
import minecraft.MinecraftDomain.MacroActions.DestroyWallMacroAction;
import minecraft.MinecraftDomain.MacroActions.DigDownMacroAction;
import minecraft.MinecraftDomain.MacroActions.JumpBlockMacroAction;
import minecraft.MinecraftDomain.MacroActions.LookDownAlotMacroAction;
import minecraft.MinecraftDomain.MacroActions.LookUpAlotMacroAction;
import minecraft.MinecraftDomain.MacroActions.SprintMacroAction;
import minecraft.MinecraftDomain.MacroActions.TurnAroundMacroAction;
import minecraft.MinecraftDomain.Options.DestroyWallOption;
import minecraft.MinecraftDomain.Options.DigDownOption;
import minecraft.MinecraftDomain.Options.JumpBlockOption;
import minecraft.MinecraftDomain.Options.LookAllTheWayDownOption;
import minecraft.MinecraftDomain.Options.TrenchBuildOption;
import minecraft.MinecraftDomain.Options.WalkUntilCantOption;

public abstract class MinecraftPlanner {
	MinecraftBehavior mcBeh;
	boolean addOptions;
	boolean addMacroActions;
	double gamma;
	StateHashFactory hashingFactory;
	State initialState;
	RewardFunction rf;
	TerminalFunction tf;
	Domain domain;

	
	protected abstract OOMDPPlanner getPlanner();
	protected abstract double[] runPlannerHelper(OOMDPPlanner planner);
	
	public MinecraftPlanner(MinecraftBehavior mcBeh, boolean addOptions, boolean addMacroActions) {
		this.mcBeh = mcBeh;
		this.addOptions = addOptions;
		this.addMacroActions = addMacroActions;
		this.gamma = mcBeh.getGamma();
		this.hashingFactory = mcBeh.getHashFactory();
		this.initialState = mcBeh.getInitialState();
		this.rf = mcBeh.getRewardFunction();
		this.tf = mcBeh.getTerminalFunction();
		this.domain = mcBeh.getDomain();

	}

	public double[] runPlanner() {
		return runPlannerHelper(retrievePlanner());
	}
	
	public OOMDPPlanner retrievePlanner() {
		OOMDPPlanner toReturn = getPlanner();
		addOptionsToOOMDPPlanner(toReturn);
		return toReturn;
	}
	
	private void addOptionsToOOMDPPlanner(OOMDPPlanner toAddTo) {
		List<Action> toAdd = getListOfMAsAndOptions(this.mcBeh, this.addOptions, this.addMacroActions);
		
		for(Action action : toAdd) {
			toAddTo.addNonDomainReferencedAction(action);
		}
			
	}
	
	public static List<Action> getListOfMAsAndOptions(MinecraftBehavior mcBeh, boolean useOptions, boolean useMAs) {

		double gamma = mcBeh.getGamma();
		StateHashFactory hashingFactory = mcBeh.getHashFactory();
		State initialState = mcBeh.getInitialState();
		RewardFunction rf = mcBeh.getRewardFunction();
		Domain domain = mcBeh.getDomain();
		
		
		List<Action> toReturn = new ArrayList<Action>();
		if (useOptions) {
			//Trench build option
			toReturn.add(new TrenchBuildOption(NameSpace.OPTBUILDTRENCH, initialState, domain,
					mcBeh.getRewardFunction(), gamma, hashingFactory));

			//Walk until can't option
			toReturn.add(new WalkUntilCantOption(NameSpace.OPTWALKUNTILCANT, initialState, domain,
					rf, gamma, hashingFactory));
			
			//Look all the way down option
			toReturn.add(new LookAllTheWayDownOption(NameSpace.OPTLOOKALLTHEWAYDOWN, initialState, domain,
					rf, gamma, hashingFactory));
			
			//Destroy wall option
			toReturn.add(new DestroyWallOption(NameSpace.OPTDESTROYWALL, initialState, domain,
					rf, gamma, hashingFactory));
			
			//Jump block option
			toReturn.add(new JumpBlockOption(NameSpace.OPTJUMPBLOCK, initialState, domain,
					rf, gamma, hashingFactory, mcBeh));
			
			//Dig down option
			toReturn.add(new DigDownOption(NameSpace.OPTDIGDOWN, initialState, domain,
					rf, gamma, hashingFactory));
			
		}
		
		//MACROACTIONS
		if (useMAs) {
			//Sprint macro-action(2)
			toReturn.add(new SprintMacroAction(NameSpace.MACROACTIONSPRINT, rf, 
					gamma, hashingFactory, domain, initialState, 2));	
			//Turn around macro-action
			toReturn.add(new TurnAroundMacroAction(NameSpace.MACROACTIONTURNAROUND, rf, 
					gamma, hashingFactory, domain, initialState));	
			//Look down a lot macro-action(2)
			toReturn.add(new LookDownAlotMacroAction(NameSpace.MACROACTIONLOOKDOWNALOT, rf, 
					gamma, hashingFactory, domain, initialState, 2));	
			//Look up a lot macro-action(2)
			toReturn.add(new LookUpAlotMacroAction(NameSpace.MACROACTIONLOOKUPALOT, rf, 
					gamma, hashingFactory, domain, initialState, 2));
			//Trench build macro-action
			toReturn.add(new BuildTrenchMacroAction(NameSpace.MACROACTIONBUILDTRENCH, rf, 
					gamma, hashingFactory, domain, initialState));
			//Jump block macro-action
			toReturn.add(new JumpBlockMacroAction(NameSpace.MACROACTIONJUMPBLOCK, rf, 
					gamma, hashingFactory, domain, initialState));
			//Dig down macro-action(2)
			toReturn.add(new DigDownMacroAction(NameSpace.MACROACTIONDIGDOWN, rf, 
					gamma, hashingFactory, domain, initialState, 2));	
			//Destroy wall macro-action
			toReturn.add(new DestroyWallMacroAction(NameSpace.MACROACTIONDESTROYWALL, rf, 
					gamma, hashingFactory, domain, initialState));			
			
		}	
		
		
		return toReturn;
	}
	
	public void updateMap(MapIO map) {
		this.mcBeh.updateMap(map);
	}
	
	
}
