package kokkodis.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Map.Entry;

import kokkodis.holders.PropertiesFactory;
import kokkodis.utils.Counter;
import kokkodis.utils.PrintToFile;

public class Transitions {

	/**
	 * @param args
	 */
	public static String[] colors = { "azure2", "azure2", "lightblue",
			"lightblue", "azure2", "lightblue", "grey83", "orange",
			"aquamarine" };

	public static HashMap<String, String> l1colors;

	public static void main(String[] args) {
		System.out.println("Reading...");
		l1colors = new HashMap<String, String>();
		HashSet<String> categories = new HashSet<String>();
		HashMap<String, String> contractorLastCategory = new HashMap<String, String>();
		HashMap<String, Counter<String>> transitions = new HashMap<String, Counter<String>>();
		HashMap<String,String> catIdToName =initMap();

		try {

			/**
			 * contractor,category,score
			 */
			String inputDirectory = PropertiesFactory.getInstance().getProps()
					.getProperty("rawPath")
					+ "train.csv";
			BufferedReader input = new BufferedReader(new FileReader(
					inputDirectory));
			String line;
			line = input.readLine();
			while ((line = input.readLine()) != null) {
				String[] tmpAr = line.split(",");
				
				String prevCat = contractorLastCategory.get(tmpAr[0]);
				if (prevCat != null) {
					Counter<String> curCounter = transitions.get(prevCat);
					if (curCounter == null) {
						curCounter = new Counter<String>();
						transitions.put(prevCat, curCounter);
					}
					curCounter.incrementCount(catIdToName.get(tmpAr[1]), 1);
				} else
					contractorLastCategory.put(tmpAr[0], catIdToName.get(tmpAr[1]));
				categories.add(catIdToName.get(tmpAr[1]));

			}
			input.close();
			System.out.println("size:" + transitions.size());
			for (Counter<String> c : transitions.values())
				c.normalize();
		} catch (IOException e) {
			e.printStackTrace();

		}

		HashMap<String, String> nodes = new HashMap<String, String>();
		crateL1ColorMap(categories);
		PrintToFile pf = new PrintToFile();
		pf.openFile("/Users/mkokkodi/Dropbox/workspace/latex/reputation_informs/figures/transitions.dot");
		pf.writeToFile("digraph simple { \n ");
		int i = 0;
		for (String key : categories) {
			nodes.put(key, "n" + i);
			pf.writeToFile("n" + i + "[label=\"" + key
					+ "\",style=\"filled\",color=\"" + l1colors.get(key)
					+ "\"];");
			i++;
		}

		for (Entry<String, Counter<String>> e : transitions.entrySet()) {
			{
				for(Entry<String,Double> eIn:e.getValue().getEntrySet()){

			double weight = eIn.getValue();
			if (weight >= 0.05) {
				String[] curNodes = {e.getKey(),eIn.getKey()};
				pf.writeToFile(nodes.get(curNodes[0]) + "->"
						+ nodes.get(curNodes[1]) + "[weight="
						+ Math.round(50 * weight) + ",label=\""
						+ Utils.getStringFromDouble(weight) + "\"]");

				System.out.println(e.getKey() + "->" + e.getValue());
			}
			}
		}
		}
		pf.writeToFile("}");

	}

	private static HashMap<String, String> initMap() {
		HashMap<String,String> hm = new HashMap<String,String>();
		hm.put("10", "Web Dev.");
		hm.put("20", "Software Dev.");
		hm.put("40", "Writing");
		hm.put("50", "Administrative");
		hm.put("60", "Design & Mult.");
		hm.put("80", "Sales & Marketing");
		return hm;
	}
	
	private static void crateL1ColorMap(Set<String> set) {


		l1colors.put("Web Dev.", "azure2");
		l1colors.put("Software Dev.", "azure2");
		l1colors.put("Design & Mult.", "azure2");
		l1colors.put("Writing", "lightblue");
		l1colors.put("Administrative", "lightblue");
		l1colors.put("Sales & Marketing", "lightblue");
		

		}




}