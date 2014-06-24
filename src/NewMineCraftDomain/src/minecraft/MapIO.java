package minecraft;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;

/**
 * Used to turn .map minecraft ascii files into easily usable data structures
 * @author Dhershkowitz
 *
 */
public class MapIO {
	//-----CLASS VARIABLES-----
	/**
	 * Stores the first line of the map ascii file as a hashing from keys to values
	 */
	private HashMap<String, Integer> headerMap;
	
	/**
	 * Stores the ascii map (header excluded) as a 3D map of chars
	 */
	private char[][][] mapAsCharArray;
	


	
	//-----CLASS METHODS-----
	public MapIO(String filePath) {
		//Open file
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(filePath));
		} catch (FileNotFoundException e1) {
			System.out.println("Couldn't open map file: " + filePath);
		}
		
		StringBuilder sb = new StringBuilder();
		
		//Build header string and map string
		String line = "";
		String stateInfoAsString = "";
		try {
			stateInfoAsString = reader.readLine();//String to store things like number of placeable blocks
			while ((line = reader.readLine()) != null) {
			    sb.append(line + "\n");
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		String mapAsString = sb.toString();
				
		this.headerMap = processHeader(stateInfoAsString);
		this.mapAsCharArray = processMapString(mapAsString);	
	}
	
	public MapIO(HashMap<String, Integer> headerInfo, char[][][] mapAsCharArray) {
		this.headerMap = headerInfo;
		this.mapAsCharArray = mapAsCharArray;
	}
	
	/**
	 * @return a copy of 3D char array of the map of the ascii file
	 */
	public char[][][] getMapAs3DCharArray() {
		return this.mapAsCharArray.clone();
	}
	/**
	 * @return a hashmap mapping string keys to values in the first line of the ascii file
	 */
	@SuppressWarnings("unchecked")
	public HashMap<String, Integer> getHeaderHashMap() {
		return (HashMap<String, Integer>) this.headerMap.clone();
	}
	
	/**
	 * Used to process the first line of a map file formatted "param1=value1,param2=value2..."
	 * @param stateInfo the first line of a map file with CSV values
	 * @returns hashmap mapping string of parameter to its int value
	 */
	public static HashMap<String, Integer> processHeader(String stateInfo) {
		HashMap<String, Integer> toReturn = new HashMap<String, Integer>();
		String [] splitOnCommas = stateInfo.split(",");
		
		for (int i = 0; i < splitOnCommas.length; i++) {
			String[] currKVPairAsStringArray = splitOnCommas[i].split("=");
			assert(currKVPairAsStringArray.length == 2);
			String key = currKVPairAsStringArray[0];
			Integer value = Integer.parseInt(currKVPairAsStringArray[1]);
			toReturn.put(key, value);
		}
		
		return toReturn;
	}
	
	/**
	 * @param mapString the ascii map stored as a string (header excluded)
	 * @return the input map string as a 3D char array
	 */
	public static char[][][] processMapString(String mapString) {
		String[] splitByHorPlanes = mapString.split(NameSpace.planeSeparator);
		int height = splitByHorPlanes.length;
		int rows = splitByHorPlanes[0].split(NameSpace.rowSeparator).length;
		int cols = splitByHorPlanes[0].split(NameSpace.rowSeparator)[0].length();
				
		char[][][] arrayToReturn = new char[rows][cols][height];
		
		for(int currHeight = height-1; currHeight >= 0; currHeight--) {
			String currPlane = splitByHorPlanes[currHeight];
			String[] planeIntoRows = currPlane.split(NameSpace.rowSeparator);
			for(int row = 0; row < rows; row++) {
				String currRow = planeIntoRows[row];
				for(int col = 0; col < cols; col++) {
					char currCharacter = currRow.charAt(col);
					arrayToReturn[row][col][height-currHeight-1] = currCharacter;
				}
			}
		}
		return arrayToReturn;
	}
	
	public String getCharArrayAsString() {
		StringBuilder sb = new StringBuilder();
		int rows = this.mapAsCharArray.length;
		int cols = this.mapAsCharArray[0].length;
		int height = this.mapAsCharArray[0][0].length;
		
		for (int currHeight = height-1; currHeight >= 0; currHeight--) {
			for(int row = 0; row < rows; row++) {
				for(int col = 0; col < cols; col++) {
					char currChar = this.mapAsCharArray[row][col][currHeight];
					sb.append(currChar);
				}
				if (!(row == rows-1)) {
					sb.append(NameSpace.rowSeparator);
				}
				else {
					if (!(currHeight == 0))
						sb.append(NameSpace.planeSeparator);
				}
			}
		}
		return sb.toString();
	}
	
	public String getHeaderAsString() {
		StringBuilder sb = new StringBuilder();
		for (String key : this.headerMap.keySet()) {
			Integer value = this.headerMap.get(key);
			sb.append(key + "=" + value + ",");
		}
		sb.deleteCharAt(sb.length()-1);
		
		sb.append("\n");
		
		return sb.toString();
	}
	
	public void printHeaderAndMapToFile(String filePath) {
		String toPrint = getHeaderAsString() + getCharArrayAsString();
		PrintWriter outPrinter = null;
		try {
			outPrinter = new PrintWriter(filePath);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		outPrinter.print(toPrint);
		outPrinter.close();
	}



	public static void main(String[] args) {
		String filePath = "src/minecraft/maps/";
		MapIO myIO = new MapIO(filePath + "jumpworld.map");
		myIO.printHeaderAndMapToFile(filePath + "TESTING.map");
	}
}