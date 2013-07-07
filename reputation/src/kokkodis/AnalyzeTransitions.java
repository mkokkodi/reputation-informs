package kokkodis;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import kokkodis.holders.PropertiesFactory;
import kokkodis.holders.RawInstance;
import kokkodis.utils.Counter;
import kokkodis.utils.GlobalVariables;
import kokkodis.utils.PrintToFile;
import kokkodis.utils.Utils;

public class AnalyzeTransitions {

	/**
	 * @param args
	 */
	private static GlobalVariables globalVariables;
	private static String 		inputDirectory;
	private static HashMap<String,List<String>> transitions;
	private static HashMap<Integer,List<Double>> categoriesScores;
	private static PrintToFile pf = new PrintToFile();
	
	public static void main(String[] args) {
	transitions = new HashMap<String, List<String>>();
	categoriesScores = new HashMap<Integer, List<Double>>();
	generateTrainTestSets();
	}

	public static void generateTrainTestSets() {

		globalVariables = GlobalVariables.getInstance();
		
		String trainFile = "train.csv";
		initInput("rawPath", trainFile);
		createSet();
		printFiles();

	}

	/**
	 * 
	 * @param inputVar
	 *            the input path variable of config.properties
	 */
	private static void initAndRun(String outputVar,String catToCat) {

		String fileName = catToCat+".csv";

		System.out.println(GlobalVariables.line);
		globalVariables.openFile(PropertiesFactory.getInstance().getProps()
				.getProperty(outputVar)
				+ "transitions/"+fileName);

		pf.openFile(PropertiesFactory.getInstance().getProps()
				.getProperty(outputVar)
				+ "transitions/ScoresPerCats.csv");
		System.out.println("Current output filename:/" + fileName);
	
	
	}

	private static void printFiles() {
		
		
		for(Entry<String,List<String>> e : transitions.entrySet()){
			initAndRun("results",e.getKey());
			globalVariables.getOutputFile().writeToFile("prevScore,CurScore");
			for(String s:e.getValue()){
				globalVariables.getOutputFile().writeToFile(s);
				
			}
			globalVariables.getOutputFile().closeFile();

		}
		
	
	}

	private static void initInput(String inputVar, String inputFile) {

		inputDirectory = PropertiesFactory.getInstance().getProps()
				.getProperty(inputVar)
				+ inputFile;

	}

	/**
	 */
	private static void createSet() {
		
		HashMap<Integer,String> contractorHolder = new HashMap<Integer, String>();

		try {
			BufferedReader input = new BufferedReader(new FileReader(
					inputDirectory));
			String line;
			System.out.println("Reading from file:" + inputDirectory);
			while ((line = input.readLine()).contains("#")) {
			}

			/**
			 * contractor,category,score "
			 */
			while ((line = input.readLine()) != null) {
				RawInstance ri = Utils.stringToRawInstance(line);
				String prevTaskCatScore = contractorHolder.get(ri.getContractor());
				LinkedList<Double> scores = (LinkedList<Double>) categoriesScores.get(ri.getCategory());
				if(scores == null){
					scores = new LinkedList<Double>();
					categoriesScores.put(ri.getCategory(), scores);
				}
				scores.add(ri.getScore());
				if(prevTaskCatScore!=null){
			
					
					String [] tmpAr = prevTaskCatScore.split("_");
					String prevTaskCat = tmpAr[0];
					String prevScore = tmpAr[1];
				String key = prevTaskCat+"_"+ri.getCategory();
				LinkedList<String> l =  (LinkedList<String>) transitions.get(key);
				if(l==null){
					l = new LinkedList<String>();
					transitions.put(key,l);
				}
				l.add(prevScore+","+ri.getScore());
				}
				contractorHolder.put(ri.getContractor(),ri.getCategory()+"_"+ri.getScore());
				
			}			
		
			input.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
