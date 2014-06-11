package minecraft.MinecraftDomain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import minecraft.NameSpace;
import burlap.debugtools.RandomFactory;
import burlap.oomdp.auxiliary.DomainGenerator;
import burlap.oomdp.core.Attribute;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectClass;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.PropositionalFunction;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.Action;
import burlap.oomdp.singleagent.SADomain;

/**
 * Class to implement minecraft in the burlap domain
 * @author Dhershkowitz
 *
 */
public class MinecraftDomainGenerator implements DomainGenerator{
	//------------CLASS VARIABLES------------
	/**
	 * The rows of the minecraft world
	 */
	private int rows;
	
	/**
	 * The cols of minecraft world
	 */
	protected int cols;
	
	/**
	 * The height of the minecraft world
	 */
	protected int height;
	
	/**
	 * Mapping of string keys to int values for things like the number of blocks that the agent 
	 * has (ultimately derived from the first line of the ascii file).
	 */
	private HashMap<String, Integer> headerInfo;
	
	//------------CONSTRUCTOR------------
	/**
	 * Constructs a burlap domain for minecraft given a map and a hashmap of header information
	 * @param mapAsCharArray the map of the world where each block is a character in row-col-height major order
	 * @param headerInfo a hashmap of other relevant information (from string to int values). E.g. number of blocks agent has.
	 */
	public MinecraftDomainGenerator(char [][][] mapAsCharArray, HashMap<String, Integer> headerInfo){
		this.rows = mapAsCharArray.length;
		this.cols = mapAsCharArray[0].length;
		this.height = mapAsCharArray[0][0].length;
		this.headerInfo = headerInfo;
	}

	//------------DOMAIN GENERATION------------
	/** 
	 * @param object burlap object to add attributes to
	 * @param xAtt the x position attribute
	 * @param yAtt the y position attribute
	 * @param zAtt the z position attribute
	 * @param collAtt the x position attribute
	 * @param shouldCollide a boolean of if the spatial object should collide with the agent
	 */
	private void addSpatialAttributes(ObjectClass object, Attribute xAtt, Attribute yAtt, Attribute zAtt, Attribute collAtt, boolean shouldCollide) {
		object.addAttribute(xAtt);
		object.addAttribute(yAtt);
		object.addAttribute(zAtt);
		object.addAttribute(collAtt);

	}
	

	@Override
	public Domain generateDomain() {
		
		Domain domain = new SADomain();
		
		//x Position Attribute
		Attribute xAtt = new Attribute(domain, NameSpace.ATTX, Attribute.AttributeType.DISC);
		xAtt.setDiscValuesForRange(0, this.cols-1, 1); //-1 due to inclusivity vs exclusivity
		
		//y Position Attribute
		Attribute yAtt = new Attribute(domain, NameSpace.ATTY, Attribute.AttributeType.DISC);
		yAtt.setDiscValuesForRange(0, this.rows-1, 1); //-1 due to inclusivity vs exclusivity
		
		//z Position Attribute
		Attribute zAtt = new Attribute(domain, NameSpace.ATTZ, Attribute.AttributeType.DISC);
		zAtt.setDiscValuesForRange(0, this.height-1, 1); //-1 due to inclusivity vs exclusivity
		
		//collidable attribute
		Attribute collAt = new Attribute(domain, NameSpace.ATTCOLLIDESWITHAGENT, Attribute.AttributeType.DISC);
		collAt.setDiscValuesForRange(0,1, 1); //-1 due to inclusivity vs exclusivity
		
		//Burlap object for the agent
		ObjectClass agentClass = new ObjectClass(domain, NameSpace.CLASSAGENT);
		addSpatialAttributes(agentClass, xAtt, yAtt, zAtt, collAt, false);

		//Burlap object for xyz goal
		ObjectClass goalClass = new ObjectClass(domain, NameSpace.CLASSGOAL);
		addSpatialAttributes(goalClass, xAtt, yAtt, zAtt, collAt, false);

		//Burlap object for dirt blocks
		ObjectClass blockClass = new ObjectClass(domain, NameSpace.CLASSDIRTBLOCK);
		addSpatialAttributes(blockClass, xAtt, yAtt, zAtt, collAt, true);
		
		//Set up actions
		new MinecraftActions.MovementAction(NameSpace.ACTIONNORTH, domain, 0,-1,0, rows, cols, height);
		new MinecraftActions.MovementAction(NameSpace.ACTIONSOUTH, domain, 0,1,0, rows, cols, height);
		new MinecraftActions.MovementAction(NameSpace.ACTIONEAST, domain, 1,0,0, rows, cols, height);
		new MinecraftActions.MovementAction(NameSpace.ACTIONWEST, domain, -1,0,0, rows, cols, height);
		
		//Set up indeterminism in actions
		
		//Set up propositional functions
		new PropositionalFunctions.AtGoalPF(NameSpace.PFATGOAL, domain, new String[]{NameSpace.CLASSAGENT, NameSpace.CLASSGOAL});
		
		return domain;
	}
	

}
