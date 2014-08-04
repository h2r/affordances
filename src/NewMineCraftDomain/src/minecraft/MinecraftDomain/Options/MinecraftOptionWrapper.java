package minecraft.MinecraftDomain.Options;

import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.options.DeterminisitcTerminationOption;
import burlap.behavior.singleagent.options.Option;
import burlap.behavior.singleagent.options.PolicyDefinedSubgoalOption;
import burlap.behavior.singleagent.planning.StateConditionTest;
import burlap.oomdp.core.Domain;
import burlap.oomdp.singleagent.RewardFunction;

public abstract class MinecraftOptionWrapper{
	
	protected String name;
	protected Domain domain;
	protected RewardFunction rf;
	protected double gamma;
	
	public abstract MinecraftOptionPolicy getOptionPolicy();
	public abstract StateConditionTest getInitTest();
	public abstract StateConditionTest getTermTest();

	public Option getOption() {
		Policy p = getOptionPolicy();
		StateConditionTest termTest = getInitTest();
		StateConditionTest initTest = getTermTest();
		
		Option toReturn = new DeterminisitcTerminationOption(this.name, p, initTest, termTest);
		toReturn.keepTrackOfRewardWith(rf, gamma);
		
		return toReturn;
	}
	
	
	public MinecraftOptionWrapper(String optionName, Domain domain, RewardFunction rf, double gamma) {
		this.name = name;
		this.domain = domain;
		this.rf = rf;
		this.gamma = gamma;
	}


}
