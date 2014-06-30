package minecraft;

import java.util.HashMap;

import minecraft.MinecraftStateGenerator.MinecraftStateGenerator;
import minecraft.MinecraftStateGenerator.Exceptions.StateCreationException;
import burlap.oomdp.auxiliary.StateParser;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;


public class MinecraftStateParser implements StateParser {

	private Domain				domain;
	
	public MinecraftStateParser(Domain domain){
		this.domain = domain;
	}
	
	@Override
	public String stateToString(State s) {
		
		StringBuffer sbuf = new StringBuffer(256);
		
		ObjectInstance agent = s.getObjectsOfTrueClass(NameSpace.CLASSAGENT).get(0);
//		ObjectInstance goal = s.getObjectsOfTrueClass(NameSpace.CLASSGOAL).get(0);
		
		String xa = NameSpace.ATX;
		String ya = NameSpace.ATY;
		String za = NameSpace.ATZ;
		int numberOfBlocks = agent.getDiscValForAttribute(NameSpace.ATPLACEBLOCKS);
		sbuf.append(agent.getDiscValForAttribute(xa)).append(",").append(agent.getDiscValForAttribute(ya)).append(",").append(agent.getDiscValForAttribute(za)).append(" ");
		sbuf.append(numberOfBlocks);
//		sbuf.append(goal.getDiscValForAttribute(xa)).append(",").append(goal.getDiscValForAttribute(ya)).append(",").append(goal.getDiscValForAttribute(za)).append(" ");
		
		return sbuf.toString();
	}

	@Override
	public State stringToState(String str) {
		String[] splitOnFirstNewLine = str.split("\n",1);
		assert(splitOnFirstNewLine.length == 2);
		String stateInfoString = splitOnFirstNewLine[0];
		String mapString = splitOnFirstNewLine[1];
		
		HashMap<String, Integer> header = MapIO.processHeader(stateInfoString);
		char[][][] mapAsCharArray = MapIO.processMapString(mapString);
		
		try {
			return MinecraftStateGenerator.createInitialState(mapAsCharArray, header, domain);
		} catch (StateCreationException e) {
			e.printStackTrace();
		}
		return null;
				
	}
	
	

}

