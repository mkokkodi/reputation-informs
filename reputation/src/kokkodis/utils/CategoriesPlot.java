package kokkodis.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

import kokkodis.holders.PropertiesFactory;
import flanagan.control.Prop;

public class CategoriesPlot {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		HashMap<String, HashSet<String>> hm = new HashMap<String, HashSet<String>>();
		String path = PropertiesFactory.getInstance().getProps()
				.getProperty("rawPath");
		try {
			BufferedReader input = new BufferedReader(new FileReader(path
					+ "categories.csv"));
			String line;
			line = input.readLine();

			// Level1,Level2
			while ((line = input.readLine()) != null) {
				String[] tmpAr = line.split(",");
				HashSet<String> curList = hm.get(tmpAr[0]);
				if (curList == null) {
					curList = new HashSet<String>();
					hm.put(tmpAr[0], curList);
				}
				curList.add(tmpAr[1]);
			}
			input.close();

		} catch (IOException e) {

		}

		PrintToFile pf = new PrintToFile();
		pf.openFile(path + "hier.dot");
		/*
		 * digraph simple {
		 * PAP[label="PAP",style="filled",color="darkolivegreen1"];
		 */
		String[] colors = { "gray95", "gray90",
				"gray86", "gray81", "gray77", "gray72" };

	//	HashMap<String, String> colorMap = new HashMap<String, String>();
		pf.writeToFile("digraph simple {");
		int i = 0;
		for (String cat : hm.keySet()) {
			
			pf.writeToFile("subgraph cluster_"+i +"{");
			pf.writeToFile("style=filled;");
			System.out.println(cat);
				pf.writeToFile("color="+colors[i]+";");
				for(String insideCat:hm.get(cat)){
					if(insideCat.contains("Other"))
						
					pf.writeToFile(insideCat.replaceAll("\\s+", "").replaceAll("\\&", "").replaceAll("-", "")  + "[label=\"Other" 
							+ "\",style=\"filled\",color=white];");
					else{
						pf.writeToFile(insideCat.replaceAll("\\s+", "").replaceAll("\\&", "").replaceAll("-", "") + "[label=\"" + insideCat
								+ "\",style=\"filled\",color=white];");
					}
				}
		//		pf.writeToFile("label =\"\"");
				pf.writeToFile("}");
		
			
			
				pf.writeToFile(cat.replaceAll("\\s+", "").replaceAll("\\&", "").replaceAll("-", "") + "[label=\"" + cat
						+ "\",style=\"filled\",color=\""+colors[i]+"\"];");

			
			
	/*		pf.writeToFile(cat.replaceAll("\\s+", "").replaceAll("\\&", "").replaceAll("-", "") + "[label=\"" + cat
					+ "\",style=\"filled\",color=\"" + colors[i] + "\"];");
			//colorMap.put(cat, colors[i]);
			for(String insideCat:hm.get(cat)){
				if(insideCat.contains("Other"))
					
				pf.writeToFile(insideCat.replaceAll("\\s+", "").replaceAll("\\&", "").replaceAll("-", "")  + "[label=\"Other" 
						+ "\",style=\"filled\",color=\"" + colors[i] + "\"];");
				else{
					pf.writeToFile(insideCat.replaceAll("\\s+", "").replaceAll("\\&", "").replaceAll("-", "") + "[label=\"" + insideCat
							+ "\",style=\"filled\",color=\"" + colors[i] + "\"];");
				}
			}
			*/
			i++;
		}
		//SAO2->CATECHOL[dir=none]
		i=0;
		String lastcategory=null;
		for (String cat : hm.keySet()) {
			String tmp=null;
			int j=0;
			HashSet<String> tmpSet = hm.get(cat);
			Iterator<String> it = tmpSet.iterator();
			String prevCat = it.next();
			pf.writeToFile(cat.replaceAll("\\s+", "").replaceAll("\\&", "").replaceAll("-", "") +"->"
					+prevCat.replaceAll("\\s+", "").replaceAll("\\&", "").replaceAll("-", "") );
			String curCat = null;
			while(it.hasNext()){
				curCat = it.next();
				pf.writeToFile(prevCat.replaceAll("\\s+", "").replaceAll("\\&", "").replaceAll("-", "") +"->"
						+curCat.replaceAll("\\s+", "").replaceAll("\\&", "").replaceAll("-", "") +"[style=invis];" );
				prevCat = curCat;
			}
			
		}
		pf.writeToFile("}");

		pf.closeFile();
	}

}
