package burlap.domain.singleagent.minecraft;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;
import java.nio.charset.Charset;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectInstance;


import burlap.oomdp.core.State;

/**
 * @author dabel, gabrielbm
 * Reads in an ascii map of a 10x10 minecraft map and initializes a minecraft state.
 * Here is an example map file:
 * g
 * +++++++++ 
 * 
 * a
 * This will create a goal at (0,0,1), a wall from (0,1,2) to (8,1,2), and an agent
 * at (0,3,2).
 */
public class MCStateGenerator {
	
	private Path fpath;
	private static Charset ENCODING = StandardCharsets.US_ASCII;
	
//	Symbols for parsing file
	private static final char gSym = 'g';
	private static final char bAddSym = '+';
	private static final char aSym = 'a';
	private static final char bRmSym = '-';
	

	/**
	 * @param path the file path for the map file.
	 */
	public MCStateGenerator(String path) {
		// TODO Auto-generated constructor stub
		this.fpath = Paths.get(path);
	}
	
	/**
	 * This is the main method for the MCStateGenerator class.
	 * A new state is created, and we create an empty 10x10 floor.
	 * Next, the map file is read and adjustments are made to the empty 10x10 floor
	 * as necessary.
	 * @param d the uninitialized domain.
	 * @return the initialized State object.
	 */
	public State getCleanState(Domain d) {

		State s = new State();
		int nrow = 0;
		
		buildEmptyFloor(s, d);
		
		try {
			Scanner scnr = new Scanner(this.fpath, ENCODING.name());
			while (scnr.hasNextLine()) {
				processRow(s, d, scnr.nextLine(), nrow);
				nrow++;
			}
			scnr.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return s;
				
	}
	
	/**
	 * Here we read each ascii character in the row and make adjustments to the
	 * 10x10 empty state.
	 * @param s the state we are building
	 * @param d the uninitialized domain
	 * @param row the current map row in ascii format
	 * @param nrow the row number
	 */
	public static void processRow(State s, Domain d, String row, int nrow) {
		char ch;

		for (int ncol = 0; ncol < row.length(); ncol++) {
			ch = row.charAt(ncol);
			
			switch (ch) {
			case bAddSym:
				addBlock(s, d, nrow, ncol, 2);
			case aSym:
				addAgent(s, d, nrow, ncol, 1);
			case gSym:
				addGoal(s, d, nrow, ncol, 1);
			case bRmSym:
				removeBlock(s, d, nrow, ncol, 1);
			default:
				continue;
			}
		}
	}
	
	/**
	 * Creates a base floor for the State
	 */	
	private static void buildEmptyFloor(State s, Domain d) {

		for (int i = 0; i < 10; i++) {
			for (int j = 0; j < 10; j++) {
				addBlock(s, d, i, j, 0);				
			}
		}
	}
	
	private static void addBlock(State s, Domain d, int x, int y, int z) {
		ObjectInstance block = new ObjectInstance(d.getObjectClass("block"), "block"+x+y+z);
		addObject(block, s, d, x, y, z);
	}
	
	private static void removeBlock(State s, Domain d, int x, int y, int z) {
		ObjectInstance block = s.getObject("block" + Integer.toString(x) + Integer.toString(y) + Integer.toString(z));
		s.removeObject(block);
	}
	
	private static void addAgent(State s, Domain d, int x, int y, int z) {
		ObjectInstance agent = new ObjectInstance(d.getObjectClass("agent"), "agent0");
		addObject(agent, s, d, x, y, z);
	}

	private static void addGoal(State s, Domain d, int x, int y, int z) {
		ObjectInstance goal = new ObjectInstance(d.getObjectClass("goal"), "goal0");
		addObject(goal, s, d, x, y, z);
	}
	
	private static void addObject(ObjectInstance obj, State s, Domain d, int x, int y, int z) {
		obj.setValue("x", x);
		obj.setValue("y", y);
		obj.setValue("z", z);
		s.addObject(obj);
	}
	

	public static void main(String[] args) {
		MinecraftDomain mcdg = new MinecraftDomain();
		Domain domain = mcdg.generateDomain();
		
		MCStateGenerator mcsg = new MCStateGenerator("/home/gbarthm/research/test.txt");
		mcsg.getCleanState(domain);
		System.out.println(domain.toString());

	}

}
