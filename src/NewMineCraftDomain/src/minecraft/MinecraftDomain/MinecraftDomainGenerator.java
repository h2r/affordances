package minecraft.MinecraftDomain;

import java.util.HashMap;
import minecraft.MapIO;
import minecraft.MinecraftInitialStateGenerator;
import minecraft.NameSpace;
import minecraft.MinecraftDomain.Actions.DestroyBlockAction;
import minecraft.MinecraftDomain.Actions.JumpAction;
import minecraft.MinecraftDomain.Actions.MovementAction;
import minecraft.MinecraftDomain.Actions.PlaceBlockAction;
import minecraft.MinecraftDomain.Actions.RotateAction;
import minecraft.MinecraftDomain.Actions.RotateVertAction;
import minecraft.MinecraftDomain.Actions.UseBlockAction;
import minecraft.MinecraftDomain.PropositionalFunctions.AgentHasAtLeastXGoldBarPF;
import minecraft.MinecraftDomain.PropositionalFunctions.AgentHasAtLeastXGoldOrePF;
import minecraft.MinecraftDomain.PropositionalFunctions.AtGoalPF;
import minecraft.MinecraftDomain.PropositionalFunctions.BlockAtPF;
import minecraft.MinecraftDomain.PropositionalFunctions.EmptySpacePF;
import burlap.oomdp.auxiliary.DomainGenerator;
import burlap.oomdp.core.Attribute;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectClass;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.SADomain;
import burlap.oomdp.singleagent.explorer.TerminalExplorer;

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
	private void addSpatialAttributes(ObjectClass object, Attribute xAt, Attribute yAt, Attribute zAt, Attribute collAt, Attribute floatsAt, Attribute destroyWhenWalkedAt, Attribute destAt) {
		object.addAttribute(xAt);
		object.addAttribute(yAt);
		object.addAttribute(zAt);
		object.addAttribute(collAt);
		object.addAttribute(floatsAt);
		object.addAttribute(destroyWhenWalkedAt);
		object.addAttribute(destAt);
		
	}
	
	

	@Override
	public Domain generateDomain() {
		
		Domain domain = new SADomain();
		
		//ATTRIBUTES
		
		//x Position Attribute
		Attribute xAtt = new Attribute(domain, NameSpace.ATX, Attribute.AttributeType.DISC);
		xAtt.setDiscValuesForRange(0, this.cols-1, 1); //-1 due to inclusivity vs exclusivity
		
		//y Position Attribute
		Attribute yAtt = new Attribute(domain, NameSpace.ATY, Attribute.AttributeType.DISC);
		yAtt.setDiscValuesForRange(0, this.rows-1, 1); //-1 due to inclusivity vs exclusivity
		
		//z Position Attribute
		Attribute zAtt = new Attribute(domain, NameSpace.ATZ, Attribute.AttributeType.DISC);
		zAtt.setDiscValuesForRange(0, this.height-1, 1); //-1 due to inclusivity vs exclusivity

		//Rotational direction for agent
		Attribute rotDirAt = new Attribute(domain, NameSpace.ATROTDIR, Attribute.AttributeType.DISC);
		rotDirAt.setDiscValuesForRange(0,NameSpace.RotDirection.size-1,1);
		
		//Agent's vertical direction attribute
		Attribute vertDirAt = new Attribute(domain, NameSpace.ATVERTDIR, Attribute.AttributeType.DISC);
		vertDirAt.setDiscValuesForRange(0,NameSpace.VertDirection.size-1,1);
		
		//Collidable attribute
		Attribute collAt = new Attribute(domain, NameSpace.ATCOLLIDES, Attribute.AttributeType.DISC);
		collAt.setDiscValuesForRange(0,1, 1); 
		
		//Destroyable attribute
		Attribute destAt = new Attribute(domain, NameSpace.ATDEST, Attribute.AttributeType.DISC);
		destAt.setDiscValuesForRange(0,1, 1);
		
		//Floats attribute
		Attribute floatsAt = new Attribute(domain, NameSpace.ATFLOATS, Attribute.AttributeType.DISC);
		floatsAt.setDiscValuesForRange(0,1,1);
		
		//Placeable blocks attribute
		Attribute blocksToPlaceAt = new Attribute(domain, NameSpace.ATPLACEBLOCKS, Attribute.AttributeType.DISC);
		blocksToPlaceAt.setDiscValuesForRange(0,100,1);
		
		//Destroyed by agent when walked on
		Attribute destroyWhenWalkedAt = new Attribute(domain, NameSpace.ATDESTWHENWALKED, Attribute.AttributeType.DISC);
		destroyWhenWalkedAt.setDiscValuesForRange(0,1,1);
		
		//Amount of gold ore of agent attribute
		Attribute amountOfGoldOre = new Attribute(domain, NameSpace.ATAMTGOLDORE, Attribute.AttributeType.DISC);
		amountOfGoldOre.setDiscValuesForRange(0, 100, 1);
		
		//Amount of gold bars of agent attribute
		Attribute amountOfGoldBar = new Attribute(domain, NameSpace.ATAMTGOLDBAR, Attribute.AttributeType.DISC);
		amountOfGoldBar.setDiscValuesForRange(0, 100, 1);
		
		
		//BURLAP OBJECT CLASSES
		
		//Burlap object for the agent
		ObjectClass agentClass = new ObjectClass(domain, NameSpace.CLASSAGENT);
		addSpatialAttributes(agentClass, xAtt, yAtt, zAtt, collAt, floatsAt, destroyWhenWalkedAt, destAt);
		agentClass.addAttribute(rotDirAt);
		agentClass.addAttribute(vertDirAt);
		agentClass.addAttribute(blocksToPlaceAt);
		agentClass.addAttribute(amountOfGoldOre);
		agentClass.addAttribute(amountOfGoldBar);
		//Burlap object for agent's feet
		ObjectClass agentFeetClass = new ObjectClass(domain, NameSpace.CLASSAGENTFEET);
		addSpatialAttributes(agentFeetClass, xAtt, yAtt, zAtt, collAt, floatsAt, destroyWhenWalkedAt, destAt);
		
		//Burlap object for xyz goal
		ObjectClass goalClass = new ObjectClass(domain, NameSpace.CLASSGOAL);
		addSpatialAttributes(goalClass, xAtt, yAtt, zAtt, collAt, floatsAt, destroyWhenWalkedAt, destAt);

		//Burlap object for dirt blocks
		ObjectClass dirtBlockClass = new ObjectClass(domain, NameSpace.CLASSDIRTBLOCK);
		addSpatialAttributes(dirtBlockClass, xAtt, yAtt, zAtt, collAt, floatsAt, destroyWhenWalkedAt, destAt);
		
		//Burlap object for gold blocks
		ObjectClass goldBlockClass = new ObjectClass(domain, NameSpace.CLASSGOLDBLOCK);
		addSpatialAttributes(goldBlockClass, xAtt, yAtt, zAtt, collAt, floatsAt, destroyWhenWalkedAt, destAt);
		
		//Burlap object for indestructable walls
		ObjectClass indWallClass = new ObjectClass(domain, NameSpace.CLASSINDWALL);
		addSpatialAttributes(indWallClass, xAtt, yAtt, zAtt, collAt, floatsAt, destroyWhenWalkedAt, destAt);
		
		//Burlap object for furnace
		ObjectClass furnaceClass = new ObjectClass(domain, NameSpace.CLASSFURNACE);
		addSpatialAttributes(furnaceClass, xAtt, yAtt, zAtt, collAt, floatsAt, destroyWhenWalkedAt, destAt);
		
		
		//ACTIONS
		
		//Set up actions
		new MovementAction(NameSpace.ACTIONMOVE, domain, rows, cols, height);
		new RotateAction(NameSpace.ACTIONROTATEC, domain, 1, rows, cols, height);
		new RotateAction(NameSpace.ACTIONROTATECC, domain, NameSpace.RotDirection.size-1, rows, cols, height); 
		new DestroyBlockAction(NameSpace.ACTIONDESTBLOCK, domain, rows, cols, height);
		new JumpAction(NameSpace.ACTIONJUMP, domain, rows, cols, height, 1);
		new PlaceBlockAction(NameSpace.ACTIONPLACEBLOCK, domain, rows, cols, height);
		new RotateVertAction(NameSpace.ACTIONLOOKDOWN, domain, rows, cols, height, -1);
		new RotateVertAction(NameSpace.ACTIONLOOKUP, domain, rows, cols, height, 1);
		new UseBlockAction(NameSpace.ACTIONUSEBLOCK, domain, rows, cols, height);
		
		//Set up indeterminism in actions
		
		//PROPOSITIONAL FUNCTIONS
		
		//Set up propositional functions
		new AtGoalPF(NameSpace.PFATGOAL, domain, new String[]{NameSpace.CLASSAGENT, NameSpace.CLASSGOAL});
		new EmptySpacePF(NameSpace.PFEMPSPACE, domain, new String[]{}, 0, 0, 0);
		new BlockAtPF(NameSpace.PFBLOCKAT, domain, new String[]{}, 0, 0, 0);
		new AgentHasAtLeastXGoldOrePF(NameSpace.PFATLEASTXGOLDORE, domain, new String[]{NameSpace.CLASSAGENT}, 2);
		new AgentHasAtLeastXGoldBarPF(NameSpace.PFATLEASTXGOLDBAR, domain, new String[]{NameSpace.CLASSAGENT}, 2);
		
		return domain;
	}
	
	public static void main(String[] args) {
		String filePath = "src/minecraft/maps/jumpworld.map";
		MapIO io = new MapIO(filePath);
		
		char[][][] charMap = io.getMapAs3DCharArray();
		HashMap<String, Integer> headerInfo = io.getHeaderHashMap();
		
		DomainGenerator dg = new MinecraftDomainGenerator(charMap, headerInfo);
		Domain d = dg.generateDomain();
		State state = MinecraftInitialStateGenerator.createInitialState(charMap, headerInfo, d);
		
		TerminalExplorer exp = new TerminalExplorer(d);
		exp.addActionShortHand("j", NameSpace.ACTIONJUMP);
		exp.addActionShortHand("w", NameSpace.ACTIONMOVE);
		exp.addActionShortHand("rc", NameSpace.ACTIONROTATEC);
		exp.addActionShortHand("ld", NameSpace.ACTIONLOOKDOWN);
		exp.addActionShortHand("d", NameSpace.ACTIONDESTBLOCK);
		exp.addActionShortHand("u", NameSpace.ACTIONUSEBLOCK);
		
		exp.exploreFromState(state);
	}
}
