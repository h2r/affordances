package affordances;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import burlap.behavior.affordances.AffordanceDelegate;
import burlap.behavior.affordances.AffordancesController;
import burlap.oomdp.core.Domain;

/**
 * The knowledge-base is a wrapper around a collection of objects of type T that are used
 * to encoode knowledge in the minecraft world (ie. affordances, subgoals, etc...)
 * @author dabel
 *
 * @param <T> The type of knowledge (affordance, subgoal, etc...)
 */
public class KnowledgeBase {
	private List<AffordanceDelegate>	kb;
	private AffordancesController		affController;
	private String						kbName;
	private String						basePath = System.getProperty("user.dir") + "minecraft.kb";
	
	
	
	public KnowledgeBase() {
		this.kb = new ArrayList<AffordanceDelegate>();
	}

	public KnowledgeBase(List<AffordanceDelegate> kb) {
		this.kb = new ArrayList<AffordanceDelegate>(kb);
	}
	
	public void add(AffordanceDelegate aff) {
		this.kb.add(aff);
	}
	
	public List<AffordanceDelegate> getAll() {
		return this.kb;
	}
	
	public void save(String filename) {
		String fpath = basePath + "/" + kbName + "/" + filename;
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(fpath)));
			
			for (AffordanceDelegate aff: this.kb) {
				bw.write(aff.toString());
			}
			
			bw.close();
		} catch (IOException e) {
			System.out.println("ERROR");
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void load(Domain d, String filename) {

		AffordanceDelegate aff = null;
		try {
			Scanner scnr = new Scanner(new File(basePath + "/" + kbName + "/" + filename));
			while (scnr.hasNextLine()) {
				aff = AffordanceDelegate.loadSoft(d, scnr);
				
				this.kb.add(aff);
			}

			scnr.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
//		this.affController = new AffordancesController();
		
	}
	
	public void process() {
		for(AffordanceDelegate aff : kb) {
			aff.postProcess();
		}
	}

}
