package kokkodis;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;

import kokkodis.holders.ModelCategory;
import kokkodis.holders.PropertiesFactory;
import kokkodis.holders.RawInstance;
import kokkodis.utils.Counter;
import kokkodis.utils.GlobalVariables;
import kokkodis.utils.PrintToFile;
import kokkodis.utils.Utils;

public class AvgPerTask {

	/**
	 * @param args
	 */
	private static GlobalVariables globalVariables;
	private static String 		inputDirectory;
	private static Counter<Integer> scoresCounter;
	private static Counter<Integer> instanceCounter;
	public static void main(String[] args) {
		generateTrainTestSets();

	}

	public static void generateTrainTestSets() {

		globalVariables = GlobalVariables.getInstance();
		
		scoresCounter = new Counter<Integer>();
		instanceCounter = new Counter<Integer>();
		String trainFile = "train.csv";
		initInput("rawPath", trainFile);
		initAndRun("results");

	}

	/**
	 * 
	 * @param inputVar
	 *            the input path variable of config.properties
	 */
	private static void initAndRun(String outputVar) {

		String fileName = "avgPerTask.csv";

		System.out.println(GlobalVariables.line);
		globalVariables.openFile(PropertiesFactory.getInstance().getProps()
				.getProperty(outputVar)
				+ fileName);

		System.out.println("Current output filename:/" + fileName);
	
		createSet();
		globalVariables.getOutputFile().writeToFile("task,score");
		printFile();
		globalVariables.getOutputFile().closeFile();

	}

	private static void printFile() {
		for(Entry<Integer,Double> e : instanceCounter.getEntrySet()){
		globalVariables.getOutputFile().writeToFile(e.getKey()+","+scoresCounter.getCount(e.getKey())/e.getValue());
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
		
		HashMap<Integer,Integer> contractorHolder = new HashMap<Integer, Integer>();

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
				Integer curTask = contractorHolder.get(ri.getContractor());
				if(curTask==null){
					curTask = 0;
					
				}
				curTask++;
				contractorHolder.put(ri.getContractor(),curTask);
				scoresCounter.incrementCount(curTask, ri.getScore());
				instanceCounter.incrementCount(curTask, 1.0);
				

			}			
		
			input.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
