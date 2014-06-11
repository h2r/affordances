package minecraft;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
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
	
	/**
	 * Stores what separates horizontal planes in the ascii files
	 */
	private static String 							planeSeparator = "\n~~~\n";
	
	/**
	 * Stores what separates rows within a plane in the ascii files
	 */
	private static String 							rowSeparator = "\n";	

	
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
		String[] splitByHorPlanes = mapString.split(planeSeparator);
		int height = splitByHorPlanes.length;
		int rows = splitByHorPlanes[0].split(rowSeparator).length;
		int cols = splitByHorPlanes[0].split(rowSeparator)[0].length();
				
		char[][][] arrayToReturn = new char[rows][cols][height];
		
		for(int currHeight = 0; currHeight < height; currHeight++) {
			String currPlane = splitByHorPlanes[currHeight];
			String[] planeIntoRows = currPlane.split(rowSeparator);
			for(int row = 0; row < rows; row++) {
				String currRow = planeIntoRows[row];
				for(int col = 0; col < cols; col++) {
					char currCharacter = currRow.charAt(col);
					arrayToReturn[row][col][currHeight] = currCharacter;
				}
			}
		}
		return arrayToReturn;
	}

}
