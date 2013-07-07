package kokkodis.kalman;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import kokkodis.holders.PropertiesFactory;
import kokkodis.utils.PrintToFile;

public class RawDataToSequencial {

	/**
	 * @param args
	 */
	private static String inputDir;
	public static void main(String[] args) {
		 inputDir = PropertiesFactory.getInstance().getProps()
				.getProperty("rawPath")
				;
		
		 String inputFile = "train_special.csv";
		 String outputFile = "trainSequential_special.csv";
		/**
		 * category -> contractor -> feedbacks
		 */
		 System.out.println("Reading");
		HashMap<String, HashMap<String, ArrayList<String>>> dataByCategoryByContractor = readRawFile(inputFile);
		System.out.println("Writing.");
		printSequencialFile(dataByCategoryByContractor,outputFile);
		System.out.println("Done.");
	
	}

	private static void printSequencialFile(
			HashMap<String, HashMap<String, ArrayList<String>>> dataByCategoryByContractor, String outputFile) {
		
		PrintToFile pf = new PrintToFile();
		pf.openFile(inputDir+outputFile);
		pf.writeToFile("category, contractor, list_of_feedbacks");
	for(Entry<String, HashMap<String, ArrayList<String>>> eOut: dataByCategoryByContractor.entrySet()){
		for(Entry<String,ArrayList<String>> eIn: eOut.getValue().entrySet()){
			String str = eOut.getKey()+","+eIn.getKey();
			for(String feedback: eIn.getValue()){
				str+=","+feedback;
			}
			pf.writeToFile(str);
		//	if(eOut.getKey().equals("0"))
			//	System.out.println(str);
			
		}
	}
	pf.closeFile();
	}

	private static HashMap<String, HashMap<String, ArrayList<String>>> readRawFile(String inputFile) {

		HashMap<String, HashMap<String, ArrayList<String>>> dataByCategoryByContractor = new HashMap<String, HashMap<String, ArrayList<String>>>();

		try {
			BufferedReader input = new BufferedReader(new FileReader(inputDir+ inputFile));
			String line;
			line = input.readLine();

			while((line = input.readLine()).contains("#"))
				;
			/**
			 * contractor,category,score
			 */
			while ((line = input.readLine()) != null) {
				String[] tmpAr = line.split(",");
				String category = tmpAr[1];
				String contractor = tmpAr[0];
				String score = tmpAr[2];
				HashMap<String, ArrayList<String>> curCategoryMap = dataByCategoryByContractor
						.get(category);
				if (curCategoryMap == null) {
					curCategoryMap = new HashMap<String, ArrayList<String>>();
					dataByCategoryByContractor.put(category, curCategoryMap);
				}
				ArrayList<String> curFeedBacks = curCategoryMap.get(contractor);
				if (curFeedBacks == null) {
					curFeedBacks = new ArrayList<String>();
					curCategoryMap.put(contractor, curFeedBacks);
				}
				curFeedBacks.add(score);
		/*		 curCategoryMap = dataByCategoryByContractor
						.get(""+0);
				if (curCategoryMap == null) {
					curCategoryMap = new HashMap<String, ArrayList<String>>();
					dataByCategoryByContractor.put(""+0, curCategoryMap);
				}
				
				curFeedBacks = curCategoryMap.get(contractor);
				if (curFeedBacks == null) {
					curFeedBacks = new ArrayList<String>();
					curCategoryMap.put(contractor, curFeedBacks);
				}
				curFeedBacks.add(score);
		*/
			}
			input.close();
			return dataByCategoryByContractor;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;

	}

}
