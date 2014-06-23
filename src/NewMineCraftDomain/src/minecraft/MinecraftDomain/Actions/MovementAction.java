package minecraft.MinecraftDomain.Actions;

import java.util.Random;

import minecraft.NameSpace;
import burlap.debugtools.RandomFactory;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;

/**
 * Action to move the agent in space
 */
public class MovementAction extends StochasticAgentAction{
		
	/**
	 * Random object for sampling distribution
	 */
	protected Random rand;
	
	
	//Amount of position change from moving (probably 1)
	private int amountOfChange;


	public MovementAction(String name, Domain domain, int amountOfChange, int rows, int cols, int height){
		super(name, domain, rows, cols, height, true);
		this.rand = RandomFactory.getMapped(0);
		
		this.amountOfChange = amountOfChange;
		
		this.rows = rows;
		this.cols = cols;
		this.height = height;
	}
	
	/**
	 * Determines if block in agents way at x,y,z locations (including z-1 since agent has feet!)
	 * @param x x location to check
	 * @param y y location to check
	 * @param z z location to check
	 * @param state the burlap state to check for collisions in
	 * @returns Boolean of whether there are blocks in the agents way at input location
	 */
	private Boolean emptySpaceForAgentAt(int x, int y, int z, State state) {
		return ActionHelpers.emptySpaceAt(x, y, z, state) && ActionHelpers.emptySpaceAt(x, y, z-1, state);
		
	}
	

	/**
	 * Called by the action to *surprise* move the agent
	 * @param state state in which the action is moving the agent
	 */
	protected void doAction(State state){
		ObjectInstance agent = state.getObjectsOfTrueClass(NameSpace.CLASSAGENT).get(0);
		ObjectInstance agentFeet = state.getObjectsOfTrueClass(NameSpace.CLASSAGENTFEET).get(0);
		
		int oldX = agent.getDiscValForAttribute(NameSpace.ATX);
		int oldY = agent.getDiscValForAttribute(NameSpace.ATY);
		int oldZ = agent.getDiscValForAttribute(NameSpace.ATZ);
		
		int xChange = 0;
		int yChange = 0;
		int zChange = 0;
		
		//Move based on direction of agent
		int directionInt = agent.getDiscValForAttribute(NameSpace.ATROTDIR);
		NameSpace.RotDirection directionEnum = NameSpace.RotDirection.fromInt(directionInt);
		switch(directionEnum){
		case NORTH:
			yChange = -1;
			break;
		case EAST:
			xChange = 1;
			break;
		case SOUTH:
			yChange = 1;
			break;
		case WEST:
			xChange = -1;
			break;
		default:
			System.out.println("Couldn't find this direction for value: " + directionInt);
			break;
		
		}
		
		int newX = oldX+xChange;
		int newY = oldY+yChange;
		int newZ = oldZ+zChange;
		
		//Update position if nothing in agent's way and new position is within map
		if (ActionHelpers.withinMapAt(newX, newY, newZ, cols, rows, height) && ActionHelpers.withinMapAt(newX, newY, newZ-1, cols, rows, height) &&
				emptySpaceForAgentAt(newX, newY, newZ, state)) {
			agent.setValue(NameSpace.ATX, newX);
			agent.setValue(NameSpace.ATY, newY);
			agent.setValue(NameSpace.ATZ, newZ);
			
			agentFeet.setValue(NameSpace.ATX, newX);
			agentFeet.setValue(NameSpace.ATY, newY);
			agentFeet.setValue(NameSpace.ATZ, newZ-1);
		}
	}
	
}