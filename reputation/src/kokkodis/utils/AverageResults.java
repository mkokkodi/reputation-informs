package kokkodis.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.TreeMap;

import kokkodis.holders.PropertiesFactory;

public class AverageResults {

	/**
	 * @param args
	 */
	private static boolean onTransitions = false;

	private static String resultPath;
	private static HashMap<String, TreeMap<Integer, HashMap<String, ArrayList<Double>>>> data;
	private static PrintToFile resultsFile;

	public static void main(String[] args) {
		averageAndPrint();
		
	}

	public static void averageAndPrint() {
		GlobalVariables.getInstance();
		initResultFile();

		/**
		 * data Map: model_approach_scoreThresholds -> History -> Metric
		 */

		data = getResultData();

		printAverages();

		resultsFile.closeFile();
	}

	private static void initResultFile() {
		resultPath = PropertiesFactory.getInstance().getProps()
				.getProperty("results");

		
		resultsFile = new PrintToFile();
		resultsFile.openFile(resultPath + "cvAverageResults"+(GlobalVariables.hierarchicalFlag?"_hier":"")+(onTransitions?"_onTransitions":"")+".csv");
		resultsFile
				.writeToFile("model,approach,ScoreThreshold,HistoryThreshold,MAEModel"
						+ ",MAEBaseline");

		System.out
				.println("---------------------------------------------------------");
		System.out
				.println("| Model  |  Approach | ScoreThresholds | History | MAE-model | MAE - Baseline");

		System.out
				.println("---------------------------------------------------------");

	}

	private static void printAverages() {
		for (Entry<String, TreeMap<Integer, HashMap<String, ArrayList<Double>>>> e1 : data
				.entrySet()) {
			String[] tmpAr = e1.getKey().split("_");
			String model = tmpAr[0];
			String approach = tmpAr[1];
			String score = tmpAr[2];
			for (Entry<Integer, HashMap<String, ArrayList<Double>>> e2 : e1
					.getValue().entrySet()) {
				String line = model + "," + approach + "," + score + ","
						+ e2.getKey() + ",";
				System.out.print(line.replaceAll(",", " | "));
				for (Entry<String, ArrayList<Double>> e3 : e2.getValue()
						.entrySet()) {
					double sum = 0;
					for (double d : e3.getValue())
						sum += d;

					line += sum / e3.getValue().size() + ",";
					System.out.print(sum / e3.getValue().size() + "|");
				}
				resultsFile.writeToFile(line.substring(0, line.length() - 1));
				System.out.println();
			}
		}

	}

	private static HashMap<String, TreeMap<Integer, HashMap<String, ArrayList<Double>>>> getResultData() {

		HashMap<String, TreeMap<Integer, HashMap<String, ArrayList<Double>>>> data = new HashMap<String, TreeMap<Integer, HashMap<String, ArrayList<Double>>>>();
			String inFile = resultPath + "results_cv"+(onTransitions?"_onTransitions":"")+".csv";
			try {
				BufferedReader input = new BufferedReader(
						new FileReader(inFile));
				String line;
				line = input.readLine();

				/*
				 * model,approach,ScoreThreshold,HistoryThreshold,
				 * MAE-model,MAE-Baseline,MSE-model,MSE-Baseline
				 */
				HashMap<String,Integer> header = Utils.getHeader(line);
				int modelInd = header.get("model");
				int approachInd = header.get("approach");
				int thresholdInd = header.get("ScoreThreshold");
				int historyInd = header.get("HistoryThreshold");
				int maeModelInd =  header.get("MAEModel");
				int maeBaselineInd =  header.get("MAEBaseline");
				int mseModelInd =  header.get("MSEModel");
				int mseBaselineInd =  header.get("MSEBaseline");
				
				while ((line = input.readLine()) != null) {

					String[] tmpAr = line.split(",");

					String key = tmpAr[modelInd] + "_" + 
					tmpAr[approachInd] + "_" + 
							tmpAr[thresholdInd];
					int history = Integer.parseInt(tmpAr[historyInd].trim());
					double maeModel = Double.parseDouble(tmpAr[maeModelInd].trim());
					double maeBaseline = Double.parseDouble(tmpAr[maeBaselineInd].trim());

					TreeMap<Integer, HashMap<String, ArrayList<Double>>> curScoreMap = data
							.get(key);
					if (curScoreMap == null) {
						curScoreMap = new TreeMap<Integer, HashMap<String, ArrayList<Double>>>();
						data.put(key, curScoreMap);
					}
					HashMap<String, ArrayList<Double>> curHistoryMap = curScoreMap
							.get(history);

					if (curHistoryMap == null) {
						curHistoryMap = new HashMap<String, ArrayList<Double>>();
						curScoreMap.put(history, curHistoryMap);
					}

					ArrayList<Double> modelMAE = curHistoryMap.get("maeModel");
					if (modelMAE == null) {
						modelMAE = new ArrayList<Double>();
						curHistoryMap.put("maeModel", modelMAE);
					}
					modelMAE.add(maeModel);

					ArrayList<Double> baselineMAE = curHistoryMap
							.get("baselineModel");
					if (baselineMAE == null) {
						baselineMAE = new ArrayList<Double>();
						curHistoryMap.put("baselineModel", baselineMAE);
					}
					baselineMAE.add(maeBaseline);

				}
				input.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		return data;
	}
}
