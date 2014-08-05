package minecraft.MinecraftDomain.Options;

import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.options.DeterminisitcTerminationOption;
import burlap.behavior.singleagent.options.Option;
import burlap.behavior.singleagent.options.PolicyDefinedSubgoalOption;
import burlap.behavior.singleagent.planning.StateConditionTest;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.oomdp.core.Domain;
import burlap.oomdp.singleagent.RewardFunction;

public abstract class MinecraftOptionWrapper{
	
	protected String name;
	protected Domain domain;
	protected RewardFunction rf;
	protected double gamma;
	protected StateHashFactory hashingFactory;
	
	public abstract MinecraftOptionPolicy getOptionPolicy();
	public abstract StateConditionTest getInitTest();
	public abstract StateConditionTest getTermTest();

	public Option getOption() {
		Policy p = getOptionPolicy();
		StateConditionTest initTest = getInitTest();
		StateConditionTest termTest = getTermTest();
		
		Option toReturn = new DeterminisitcTerminationOption(this.name, p, initTest, termTest);
		toReturn.keepTrackOfRewardWith(rf, gamma);
		toReturn.setExpectationHashingFactory(hashingFactory);
		
		return toReturn;
	}
	
	
	public MinecraftOptionWrapper(String optionName, Domain domain, RewardFunction rf, double gamma, StateHashFactory hashingFactory) {
		this.name = optionName;
		this.domain = domain;
		this.rf = rf;
		this.gamma = gamma;
		this.hashingFactory = hashingFactory;
	}


}
