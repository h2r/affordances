package minecraft.MinecraftDomain;

import minecraft.NameSpace;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.PropositionalFunction;
import burlap.oomdp.core.State;

public class PropositionalFunctions {
	/**
	 * Propositional function to determine if the agent is at the goal
	 */
	public static class AtGoalPF extends PropositionalFunction{

		public AtGoalPF(String name, Domain domain, String[] parameterClasses) {
			super(name, domain, parameterClasses);
		}

		@Override
		public boolean isTrue(State state, String[] params) {
			//get the agent coordinates
			ObjectInstance agent = state.getObjectsOfTrueClass(NameSpace.CLASSAGENT).get(0);
			int agentX = agent.getDiscValForAttribute(NameSpace.ATTX);
			int agentY = agent.getDiscValForAttribute(NameSpace.ATTY);
			int agentZ = agent.getDiscValForAttribute(NameSpace.ATTZ);
			
			//get the goal coordinates
			ObjectInstance goal = state.getObjectsOfTrueClass(NameSpace.CLASSGOAL).get(0);
			int goalX = goal.getDiscValForAttribute(NameSpace.ATTX);
			int goalY = goal.getDiscValForAttribute(NameSpace.ATTY);
			int goalZ = goal.getDiscValForAttribute(NameSpace.ATTZ);
			
			//Check if equal
			if(agentX == goalX && agentY == goalY && agentZ == goalZ){
				return true;
			}
			return false;
		}
	}
}
