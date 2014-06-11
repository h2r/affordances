package minecraft;

import java.util.HashMap;
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
		
		ObjectInstance a = s.getObjectsOfTrueClass(NameSpace.CLASSAGENT).get(0);
		ObjectInstance goal = s.getObjectsOfTrueClass(NameSpace.CLASSGOAL).get(0);
		
		String xa = NameSpace.ATTX;
		String ya = NameSpace.ATTY;
		String za = NameSpace.ATTZ;
		
		sbuf.append(a.getDiscValForAttribute(xa)).append(",").append(a.getDiscValForAttribute(ya)).append(",").append(a.getDiscValForAttribute(za)).append(" ");
		sbuf.append(goal.getDiscValForAttribute(xa)).append(",").append(goal.getDiscValForAttribute(ya)).append(",").append(goal.getDiscValForAttribute(za)).append(" ");
		
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
		
		return MinecraftInitialStateGenerator.createInitialState(mapAsCharArray, header, domain);
				
	}
	
	

}

