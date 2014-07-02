package minecraft.WorldGenerator;

import java.util.ArrayList;
import java.util.List;

import minecraft.MapIO;
import minecraft.NameSpace;

public class MapFileGenerator {
	int rows;
	int cols;
	int height;
	WorldGenerator worldGenerator;
	String directoryPath;
	
	public MapFileGenerator(int rows, int cols, int height, String directoryPath) {
		this.worldGenerator = new WorldGenerator(rows, cols, height);
		this.directoryPath = directoryPath;
		
	}
	
	public void generateNMaps(int numMaps, int goal, int numTrenches, String baseFileName) {
		List<MapIO> toWriteToFile = new ArrayList<MapIO>();
		
		for (int i = 0; i < numMaps; i++) {
			this.worldGenerator.randomizeMap(goal, numTrenches, 0);
			MapIO currIO = this.worldGenerator.getCurrMapIO();
			toWriteToFile.add(currIO);
		}
		
		int i = 0;
		for (MapIO currIO : toWriteToFile) {
			currIO.printHeaderAndMapToFile(this.directoryPath + baseFileName + i++ + ".map");
		}
			
	}
	
	public static void main(String[] args) {
		String filePath = "src/minecraft/maps/randomMaps/";
		MapFileGenerator test = new MapFileGenerator(4,4,3,filePath);
		test.generateNMaps(20, NameSpace.INTXYZGOAL, 0, "plane");
		test.generateNMaps(20, NameSpace.INTXYZGOAL, 1, "shallowTrench");
		test.generateNMaps(20, NameSpace.INTGOLDOREGOAL, 0, "goldOrePlane");
		test.generateNMaps(20, NameSpace.INTGOLDBARGOAL, 0, "goldBarPlane");
		test.generateNMaps(20, NameSpace.INTGOLDOREGOAL, 1, "goldOreTrench");
		test.generateNMaps(20, NameSpace.INTGOLDBARGOAL, 1, "goldBarTrench");
	}
	

}
