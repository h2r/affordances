package taxi.PropositionalFunctions;

import taxi.TaxiNameSpace;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.PropositionalFunction;
import burlap.oomdp.core.State;

public class AgentLookTowardPassenger extends PropositionalFunction {

	public AgentLookTowardPassenger(String name, Domain domain, String[] parameterClasses) {
	    super(name, domain, parameterClasses);
	}
	
	@Override
	public boolean isTrue(State st, String[] params) {
	    ObjectInstance agent = st.getObject(params[0]);
	    int ax = agent.getDiscValForAttribute(TaxiNameSpace.ATTX);
	    int ay = agent.getDiscValForAttribute(TaxiNameSpace.ATTY);

	    for (int i = 1; i <= TaxiNameSpace.MAXPASS; i++) {
	    	ObjectInstance pass = st.getObjectsOfTrueClass(TaxiNameSpace.CLASSPASS).get(i - 1);
			int px = pass.getDiscValForAttribute(TaxiNameSpace.ATTX);
			int py = pass.getDiscValForAttribute(TaxiNameSpace.ATTY);

			if (agent.getDiscValForAttribute(TaxiNameSpace.ATTCARRY) == 0
				&& pass.getDiscValForAttribute(TaxiNameSpace.ATTDROPPED) == 0
				&& pass.getDiscValForAttribute(TaxiNameSpace.ATTCARRIED) == 0
				&& ax == px && ay == py) {
			    	return true;
				}
		    }

	    return false;
	}
}