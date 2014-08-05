package minecraft.MinecraftDomain.Options;

import minecraft.NameSpace;
import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.options.DeterminisitcTerminationOption;
import burlap.behavior.singleagent.options.Option;
import burlap.behavior.singleagent.planning.StateConditionTest;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;

public abstract class MinecraftOptionWrapper{
	
	protected String name;
	protected Domain domain;
	protected RewardFunction rf;
	protected double gamma;
	protected StateHashFactory hashingFactory;
	
	public abstract boolean getTermTest(State state);
	public abstract GroundedAction getPolicyGroundedAction(State state);

	public Option getOption() {
		Policy p = new OptionPolicy();
		StateConditionTest initTest = new InitTest();
		StateConditionTest termTest = new TerminationTest();
		
		Option toReturn = new DeterminisitcTerminationOption(this.name, p, initTest, termTest);
		toReturn.keepTrackOfRewardWith(rf, gamma);
		toReturn.setExpectationHashingFactory(hashingFactory);
		
		return toReturn;
	}
	
	
	
	private class TerminationTest implements StateConditionTest {
		@Override
		public boolean satisfies(State state) {
			return getTermTest(state);
		}
	}
	
	private class InitTest implements StateConditionTest {
		@Override
		public boolean satisfies(State state) {
			return true;
		}
		
	}
	
	private class OptionPolicy extends MinecraftOptionPolicy {
		@Override
		protected GroundedAction getGroundedAction(State state) {
			return getPolicyGroundedAction(state);
		}
		
	}
	
	
	public MinecraftOptionWrapper(String optionName, Domain domain, RewardFunction rf, double gamma, StateHashFactory hashingFactory) {
		this.name = optionName;
		this.domain = domain;
		this.rf = rf;
		this.gamma = gamma;
		this.hashingFactory = hashingFactory;
	}


}
