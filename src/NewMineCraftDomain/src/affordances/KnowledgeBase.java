package affordances;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import tests.ResourceLoader;
import burlap.behavior.affordances.AffordanceDelegate;
import burlap.behavior.affordances.AffordancesController;
import burlap.behavior.affordances.SoftAffordance;
import burlap.oomdp.core.Domain;

/**
 * The knowledge-base is a wrapper around a collection of objects of type T that are used
 * to encode knowledge in the minecraft world (ie. affordances, subgoals, etc...)
 * @author dabel
 */
public class KnowledgeBase {
	private List<AffordanceDelegate>	affDelegateList;
	private AffordancesController		affController;
//	private String						basePath = System.getProperty("user.dir") + "/minecraft/kb/";
	private String						basePath = "minecraft/kb/";
	private final static ResourceLoader	resLoader = new ResourceLoader();
	
	public KnowledgeBase() {
		this.affDelegateList = new ArrayList<AffordanceDelegate>();
		this.affController = new AffordancesController(this.affDelegateList);
	}
	
	public KnowledgeBase(List<AffordanceDelegate> kb) {
		this.affDelegateList = new ArrayList<AffordanceDelegate>(kb);
		this.affController = new AffordancesController(this.affDelegateList);
	}
	
	public void add(AffordanceDelegate aff) {
		// Only add if it's not already in the KB
		if(!this.affDelegateList.contains(aff)) {
			this.affDelegateList.add(aff);
			this.affController.addAffordanceDelegate(aff);
		}
	}
	
	public void remove(AffordanceDelegate aff) {
		// Only remove if it's in the KB
		if(this.affDelegateList.contains(aff)) {
			this.affDelegateList.remove(aff);
			this.affController.removeAffordance(aff);
		}
	}
	
	public void save(String filename) {
		String fpath = basePath + "/" + filename;
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(fpath)));
			
			// For grid
//			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(System.out));
			
			for (AffordanceDelegate aff: this.affDelegateList) {
				bw.write(((SoftAffordance)aff.getAffordance()).toFile());
			}
			bw.close();
		} catch (IOException e) {
			System.out.println("ERROR");
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void load(Domain d, String filename, boolean softFlag) {
		AffordanceDelegate aff = null;
		try {
			BufferedReader reader = resLoader.getBufferedReader(basePath + filename);
			
			StringBuilder sBuilder = new StringBuilder();
			String nextLine = reader.readLine();
			
			while(nextLine != null) {
				sBuilder.append(nextLine + "\n");
				nextLine = reader.readLine();
			}
			
			String[] kbStrings = sBuilder.toString().split("===");
			
			String[] processedStrings = new String[kbStrings.length - 1];
			
			// Remove the last element from KBString (closing equals signs)
			for(int i = 0; i < processedStrings.length; i++) {
				processedStrings[i] = kbStrings[i];
			}
			
			for(String affString : processedStrings) {
				// Remove the final newline character from the affordance
				String slicedString = affString.substring(0, affString.length() - 1);
				if(softFlag) {
					aff = AffordanceDelegate.loadSoft(d, slicedString);
				}
				else {
					aff = AffordanceDelegate.loadHard(d, slicedString);
				}
				this.affDelegateList.add(aff);
				reader.close();
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		this.affController = new AffordancesController(this.affDelegateList);
	}
	
	public void processSoft() {
		for(AffordanceDelegate affDelegate : affDelegateList) {
			((SoftAffordance)affDelegate.getAffordance()).postProcess();
		}
	}
	
	// --- ACCESSORS ---
	
	public AffordancesController getAffordancesController() {
		return this.affController;
	}
	
	public List<AffordanceDelegate> getAffordances() {
		return this.affDelegateList;
	}

}
