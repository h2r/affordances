package minecraft.MinecraftBehavior.Planners;

import burlap.behavior.singleagent.planning.OOMDPPlanner;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
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
		//OPTIONS
		if (this.addOptions) {
			//Trench build option
			toAddTo.addNonDomainReferencedAction(new TrenchBuildOption(NameSpace.OPTBUILDTRENCH, this.initialState, this.domain,
					this.mcBeh.getRewardFunction(), this.gamma, this.hashingFactory));
			
			//Walk until can't option
			toAddTo.addNonDomainReferencedAction(new WalkUntilCantOption(NameSpace.OPTWALKUNTILCANT, this.initialState, this.domain,
					this.rf, this.gamma, this.hashingFactory));
			
			//Look all the way down option
			toAddTo.addNonDomainReferencedAction(new LookAllTheWayDownOption(NameSpace.OPTLOOKALLTHEWAYDOWN, this.initialState, this.domain,
					this.rf, this.gamma, this.hashingFactory));
			
			//Destroy wall option
			toAddTo.addNonDomainReferencedAction(new DestroyWallOption(NameSpace.OPTDESTROYWALL, this.initialState, this.domain,
					this.rf, this.gamma, this.hashingFactory));
			
//			//Jump block option
			toAddTo.addNonDomainReferencedAction(new JumpBlockOption(NameSpace.OPTJUMPBLOCK, this.initialState, this.domain,
					this.rf, this.gamma, this.hashingFactory, this.mcBeh));
			
//			//Dig down option
			toAddTo.addNonDomainReferencedAction(new DigDownOption(NameSpace.OPTDIGDOWN, this.initialState, this.domain,
					this.rf, this.gamma, this.hashingFactory));
		}

		//MACROACTIONS
		if (this.addMacroActions) {
			//Sprint macro-action(2)
			toAddTo.addNonDomainReferencedAction(new SprintMacroAction(NameSpace.MACROACTIONSPRINT, this.rf, 
					this.gamma, this.hashingFactory, this.domain, this.initialState, 2));	
			//Turn around macro-action
			toAddTo.addNonDomainReferencedAction(new TurnAroundMacroAction(NameSpace.MACROACTIONTURNAROUND, this.rf, 
					this.gamma, this.hashingFactory, this.domain, this.initialState));	
			//Look down a lot macro-action(2)
			toAddTo.addNonDomainReferencedAction(new LookDownAlotMacroAction(NameSpace.MACROACTIONLOOKDOWNALOT, this.rf, 
					this.gamma, this.hashingFactory, this.domain, this.initialState, 2));	
			//Look up a lot macro-action(2)
			toAddTo.addNonDomainReferencedAction(new LookUpAlotMacroAction(NameSpace.MACROACTIONLOOKUPALOT, this.rf, 
					this.gamma, this.hashingFactory, this.domain, this.initialState, 2));
			//Trench build macro-action
			toAddTo.addNonDomainReferencedAction(new BuildTrenchMacroAction(NameSpace.MACROACTIONBUILDTRENCH, this.rf, 
					this.gamma, this.hashingFactory, this.domain, this.initialState));
			//Jump block macro-action
			toAddTo.addNonDomainReferencedAction(new JumpBlockMacroAction(NameSpace.MACROACTIONJUMPBLOCK, this.rf, 
					this.gamma, this.hashingFactory, this.domain, this.initialState));
			//Dig down macro-action(2)
			toAddTo.addNonDomainReferencedAction(new DigDownMacroAction(NameSpace.MACROACTIONDIGDOWN, this.rf, 
					this.gamma, this.hashingFactory, this.domain, this.initialState, 2));	
			//Destroy wall macro-action
			toAddTo.addNonDomainReferencedAction(new DestroyWallMacroAction(NameSpace.MACROACTIONDESTROYWALL, this.rf, 
					this.gamma, this.hashingFactory, this.domain, this.initialState));			
			
		}	
	}
	
	public void updateMap(MapIO map) {
		this.mcBeh.updateMap(map);
	}
	
	
}
