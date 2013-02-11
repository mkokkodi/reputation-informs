package kokkodis.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import kokkodis.holders.PropertiesFactory;

public class EMComputeLambdas {

	/**
	 * @param args
	 */
	/*
	 * A succesful prediction is one with less than 0.01 error.
	 */
	private static HashMap<String, ArrayList<HashMap<String, HashMap<Double, Boolean>>>> data;
	private static HashMap<String, HashMap<String, Double>> lambdas;
	private static double numberOfLevels;
	private static HashMap<String, HashMap<String, Double>> alphas;
	private static HashMap<String, Boolean> convergedModels;
	private static final double convergenceCritirion = 0.002;
	private static boolean allConverged = false;
	private static final int maxIterations = 500;
	private static int categories = -1;
	private static boolean hierarchyFlag;
	private static int folds;
	private static boolean crossValidation = false;
	private static PrintToFile outputFile;
	private static boolean real = false;
	private static HashMap<Double, Double> errorWeights;
	private static HashMap<String, HashMap<String, Double>> keyToProbSuccessGivenM;
	private static boolean evaluateOnTransitions =false;

	public static void main(String[] args) {

		initErrorWeights();
		keyToProbSuccessGivenM = new HashMap<String, HashMap<String, Double>>();
		String[] hier = PropertiesFactory.getInstance().getProps()
				.getProperty("hierarchyStructure").split(",");
		if (hier.length == 3) {
			System.out.println("We are dealing with hierarchies.");
			numberOfLevels = 3;
			hierarchyFlag = true;
		} else {
			numberOfLevels = 2;
			hierarchyFlag = false;
		}

		for (int i = 0; i < args.length; i++) {
			if (args[i].contains("-c"))

				categories = Integer.parseInt(args[i + 1].trim());

			if (args[i].contains("-f")) {
				crossValidation = true;
				folds = Integer.parseInt(args[i + 1].trim());
				System.out.println("Cross validation: folds:" + folds);
			}
			if (args[i].contains("-r")) {
				real = true;
			}if (args[i].contains("-o")) {
				evaluateOnTransitions = true;
			}
		}
		System.out.println("Categories:" + categories);

		String[] scoreThresholds = PropertiesFactory.getInstance().getProps()
				.getProperty("scoreThresholds").split(",");
		if (crossValidation) {
			for (int i = 2; i <= folds; i++) {
				initFile(i);

				// For binomial.
				for (String threshold : scoreThresholds) {
					allConverged = false;
					runEM(i, threshold);
				}
				// for multinomial
				allConverged = false;
				runEM(i, null);
				outputFile.closeFile();
			}
		} else if (real) {
			initFile(null);
			for (String threshold : scoreThresholds) {
				allConverged = false;
				runEM(null, threshold);
			}
			allConverged = false;
			runEM(null, null); // for multinomial.
			outputFile.closeFile();
		} else {
			allConverged = false;
			initFile(null);
			runEM(null, null);
			outputFile.closeFile();
		}

		System.out.println("Completed.");

	}

	private static void initErrorWeights() {
		errorWeights = new HashMap<Double, Double>();
		errorWeights.put(0.01, 0.3);
		errorWeights.put(0.02, 0.25);
		errorWeights.put(0.03, 0.15);
		errorWeights.put(0.04, 0.1);
		errorWeights.put(0.05, 0.1);
		errorWeights.put(0.06, 0.05);
		errorWeights.put(0.07, 0.05);
		errorWeights.put(0.08, 0.05);
		errorWeights.put(0.09, 0.05);

	}

	private static void initFile(Integer currentFold) {
		outputFile = new PrintToFile();
		String outFile = PropertiesFactory.getInstance().getProps()
				.getProperty("results");
		String f = outFile + "lambdas"
				+ ((categories != -1) ? "" + categories : "")
				+ (currentFold != null ? ("_cv" + currentFold) : "") + ".csv";
		System.out.println("Output File:" + f);
		outputFile.openFile(f);

		outputFile
				.writeToFile("model,approach,ScoreThreshold,  cluster, averageLambda, rLambda, clusterLambda");

	}

	private static void runEM(Integer currentFold, String threshold) {

		loadData(currentFold, threshold);
		initializeLambdas();
		System.out.println("Data loaded. Current Fold:" + currentFold
				+ " currentThreshold:" + threshold);
		int iteration = 0;
		estimateProbsSuccessGivenM();
		while (!allConverged && iteration < maxIterations) {
			allConverged = true;
			iteration++;
			System.out.println("Iteration:" + iteration);
			for (Entry<String, ArrayList<HashMap<String, HashMap<Double, Boolean>>>> e : data
					.entrySet()) {
				String key = e.getKey();

				if (!convergedModels.get(key)) {
					HashMap<String, Double> probSuccessGivenM = keyToProbSuccessGivenM
							.get(key);
					allConverged = false;
					String curCluster = getCurClusterFromKey(key);
					HashMap<String, Double> currentLambdas = lambdas.get(key);
					// System.out.println("Estimating Lambdas for "+key);
					HashMap<String, Double> currentAplhas = alphas.get(key);

					double denom = estimateAlphaDenom(probSuccessGivenM,
							currentLambdas);
					currentAplhas.put(
							"average",
							estimateAlpha(probSuccessGivenM, currentLambdas,
									"average", denom));
					currentAplhas.put(
							"r",
							estimateAlpha(probSuccessGivenM, currentLambdas,
									"r", denom));
					if (hierarchyFlag) {
						if (!curCluster.equals("r"))
							currentAplhas.put(
									curCluster,
									estimateAlpha(probSuccessGivenM,
											currentLambdas, curCluster, denom));
					}
					estimateNewLambdas(key, currentAplhas, currentLambdas);
				}
			}
		}
		for (Entry<String, HashMap<String, Double>> eOut : lambdas.entrySet()) {
			System.out
					.println("-----------------------------------------------");
			String key = eOut.getKey();
			System.out.println(key);
			System.out.println("Converged:" + convergedModels.get(key));
			Double aveLambda = eOut.getValue().get("average");
			Double rLambda = eOut.getValue().get("r");
			if (hierarchyFlag) {
				String curCluster = getCurClusterFromKey(key);
				Double clusterLambda = eOut.getValue().get(curCluster);

				System.out.println(curCluster);

				System.out.println(" avg : " + aveLambda + " rLambda:"
						+ rLambda + " cluster:" + clusterLambda);
				String[] tmpAr = key.split("_");

				outputFile.writeToFile(tmpAr[0] + "," + tmpAr[1] + ","
						+ tmpAr[2] + "," + curCluster + "," + aveLambda + ","
						+ rLambda + "," + clusterLambda);
			} else {

				System.out.println(" avg : " + aveLambda + " rLambda:"
						+ rLambda);
				String[] tmpAr = key.split("_");

				outputFile.writeToFile(tmpAr[0] + "," + tmpAr[1] + ","
						+ tmpAr[2] + "," + "r" + "," + aveLambda + ","
						+ rLambda);
			}
		}

	}

	private static void estimateProbsSuccessGivenM() {

		for (Entry<String, ArrayList<HashMap<String, HashMap<Double, Boolean>>>> e : data
				.entrySet()) {
			HashMap<String, Double> probSuccessGivenM = new HashMap<String, Double>();
			String key = e.getKey();
			keyToProbSuccessGivenM.put(e.getKey(), probSuccessGivenM);
			probSuccessGivenM.put("average",
					estimateProbSuccessgivenM("average", e.getValue()));
			probSuccessGivenM.put("r",
					estimateProbSuccessgivenM("r", e.getValue()));
			if (hierarchyFlag) { // i.e. if we have hierarchical
									// model.
				String curCluster = getCurClusterFromKey(key);
				if (!curCluster.equals("r")) {
					probSuccessGivenM
							.put(curCluster,
									estimateProbSuccessgivenM(curCluster,
											e.getValue()));
				}
			}
		}

	}

	private static Double estimateAlpha(
			HashMap<String, Double> probSuccessGivenM,
			HashMap<String, Double> currentLambdas, String curCluster,
			double denom) {

		double nom = currentLambdas.get(curCluster)
				* probSuccessGivenM.get(curCluster);
		double prob = nom / denom;
		//System.out.println("Alpha:" + curCluster + " :" + prob);
		return prob;
	}

	private static double estimateAlphaDenom(
			HashMap<String, Double> probSuccessGivenM,
			HashMap<String, Double> currentLambdas) {
		double denom = 0;
		for (Entry<String, Double> e : probSuccessGivenM.entrySet()) {
			denom += (currentLambdas.get(e.getKey()) * e.getValue());
		}

		return denom;
	}

	private static void estimateNewLambdas(String key,
			HashMap<String, Double> currentAplhas,
			HashMap<String, Double> currentLambdas) {
		double denom = 0;

		HashMap<String, Double> newCurLambdas = new HashMap<String, Double>();

		for (Entry<String, Double> e : currentAplhas.entrySet()) {
			denom += e.getValue();
		}

		for (Entry<String, Double> e : currentAplhas.entrySet()) {
			newCurLambdas.put(e.getKey(), e.getValue() / denom);
		}
		updateConvergence(key, newCurLambdas, currentLambdas);

	}

	private static void updateConvergence(String key,
			HashMap<String, Double> newCurLambdas,
			HashMap<String, Double> currentLambdas) {

		boolean curConverged = true;
		for (Entry<String, Double> e : currentLambdas.entrySet()) {

			double newLambda = newCurLambdas.get(e.getKey());
			if (Math.abs(e.getValue() - newLambda) > convergenceCritirion) {
				curConverged = false;
				break;
			}
		}
		// System.out.println("Key:"+key+" Difference:" + diff);
		if (curConverged)
			convergedModels.put(key, true);
		else {
			lambdas.put(key, newCurLambdas);
		}

	}

	private static void initializeLambdas() {
		lambdas = new HashMap<String, HashMap<String, Double>>();
		alphas = new HashMap<String, HashMap<String, Double>>();
		convergedModels = new HashMap<String, Boolean>();
		double initValue = 1.0 / numberOfLevels;
		for (String key : data.keySet()) {
			HashMap<String, Double> curLambda = new HashMap<String, Double>();
			String curCluster = getCurClusterFromKey(key);
			curLambda.put("average", initValue);
			curLambda.put("r", initValue);
			if (hierarchyFlag) {
				if (curCluster.equals("rr"))
					curLambda.put("rr", initValue);
				else if (curCluster.equals("rl"))
					curLambda.put("rl", initValue);
			}
			lambdas.put(key, curLambda);
			alphas.put(key, new HashMap<String, Double>());
			convergedModels.put(key, false);
		}

	}

	private static String getCurClusterFromKey(String key) {
		String[] tmpAr = key.split("_");
		return tmpAr[tmpAr.length - 1];
	}

	private static double estimateProbSuccessgivenM(String cluster,
			ArrayList<HashMap<String, HashMap<Double, Boolean>>> curData) {
		double successes = 0;
		for (HashMap<String, HashMap<Double, Boolean>> hm : curData) {
			HashMap<Double, Boolean> curSuccessMap = hm.get(cluster);
			for (Entry<Double, Boolean> e : curSuccessMap.entrySet()) {
				if (e.getValue()) {
					successes += errorWeights.get(e.getKey()) * e.getKey();
				}
			}

		}
		double prob = successes / curData.size();
		System.out
				.println("Probability of success of " + cluster + " :" + prob);
		return prob;

	}

	private static HashMap<String, ArrayList<HashMap<String, HashMap<Double, Boolean>>>> loadData(
			Integer currentFold, String threshold) {
		System.out.println("Loading data...");

		/**
		 * key = model + approach + ScoreThreshold + cluster (rl or rr)
		 */

		data = new HashMap<String, ArrayList<HashMap<String, HashMap<Double, Boolean>>>>();

		try {
			String inFile = PropertiesFactory.getInstance().getProps()
					.getProperty("results");
			String inputFile = inFile + "predictions"
					+ (currentFold != null ? ("_cv" + currentFold) : "")
					+ (threshold != null ? ("_" + threshold) : "_mult") + (real?"_real":"")+
					(evaluateOnTransitions ? "_onTransitions"
							: "")+".csv";
			BufferedReader input = new BufferedReader(new FileReader(inputFile));

			System.out.println("Reading from input File:" + inputFile);
			String line;
			line = input.readLine();
			HashMap<String, Integer> header = Utils.getHeader(line);
			/**
			 * Predictions file:model,approach,HistoryThreshold,
			 * actual,Nohierarchies,baseline,rl,rr
			 */
			int modelInd = header.get("model");
			int approachInd = header.get("approach");
			int actualInd = header.get("actual");
			int baselineInd = header.get("baseline");
			int rInd = header.get("Nohierarchies");
			int rlInd = header.get("rl");
			int rrInd = header.get("rr");

			while ((line = input.readLine()) != null) {
				String[] tmpAr = line.split(",");

				/*
				 * Key = model + approach + cluster
				 */
				String key = tmpAr[modelInd] + "_" + tmpAr[approachInd] + "_"
						+ threshold;

				double actualQuality = Double.parseDouble(tmpAr[actualInd]
						.trim());
				HashMap<String, HashMap<Double, Boolean>> curInstance = new HashMap<String, HashMap<Double, Boolean>>();
				curInstance.put(
						"average",
						getSuccess(
								Double.parseDouble(tmpAr[baselineInd].trim()),
								actualQuality));

				curInstance.put(
						"r",
						getSuccess(Double.parseDouble(tmpAr[rInd].trim()),
								actualQuality));

				double rl = Double.parseDouble(tmpAr[rlInd].trim());
				double rr = Double.parseDouble(tmpAr[rrInd].trim());

				String curCluster = "r";
				if (rl != -1) {
					curInstance.put("rl", getSuccess(rl, actualQuality));
					curCluster = "rl";
				} else if (rr != -1) {
					curInstance.put("rr", getSuccess(rr, actualQuality));
					curCluster = "rr";
				}

				key += "_" + curCluster;

				ArrayList<HashMap<String, HashMap<Double, Boolean>>> list = data
						.get(key);
				if (list == null) {
					list = new ArrayList<HashMap<String, HashMap<Double, Boolean>>>();
					data.put(key, list);
				}

				list.add(curInstance);

			}
			input.close();
			return data;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	private static HashMap<Double, Boolean> getSuccess(double prediction,
			double actualQuality) {
		double diff = Math.abs(prediction - actualQuality);
		HashMap<Double, Boolean> curSuccessMap = new HashMap<Double, Boolean>();
		if(diff>0.09)
			return curSuccessMap;
		if (diff <= 0.01){
			
			curSuccessMap.put(0.01, true);
			curSuccessMap.put(0.02, true);
			curSuccessMap.put(0.03, true);
			curSuccessMap.put(0.04, true);
			curSuccessMap.put(0.05, true);
			curSuccessMap.put(0.06, true);
			curSuccessMap.put(0.07, true);
			curSuccessMap.put(0.08, true);
		/*	curSuccessMap.put(0.09, true);
			*/
		}
		else if (diff <= 0.02){
			curSuccessMap.put(0.02, true);
			curSuccessMap.put(0.03, true);
			curSuccessMap.put(0.04, true);
			curSuccessMap.put(0.05, true);
			curSuccessMap.put(0.06, true);
			curSuccessMap.put(0.07, true);
			curSuccessMap.put(0.08, true);
		/*	curSuccessMap.put(0.09, true);
			*/
		}
		else if (diff <= 0.03){
			curSuccessMap.put(0.03, true);
		curSuccessMap.put(0.04, true);
						curSuccessMap.put(0.05, true);
			curSuccessMap.put(0.06, true);
			curSuccessMap.put(0.07, true);
			curSuccessMap.put(0.08, true);
		/*	curSuccessMap.put(0.09, true);
			*/
		
		}
		else if (diff <= 0.04){
			curSuccessMap.put(0.04, true);
			curSuccessMap.put(0.05, true);
			curSuccessMap.put(0.06, true);
			curSuccessMap.put(0.07, true);
			curSuccessMap.put(0.08, true);
/*			curSuccessMap.put(0.09, true);
			*/
		}
		else if (diff <= 0.05){
			curSuccessMap.put(0.05, true);
			curSuccessMap.put(0.06, true);
			curSuccessMap.put(0.07, true);
			curSuccessMap.put(0.08, true);
/*			curSuccessMap.put(0.09, true);
	*/	}
		else if (diff <= 0.06){
			curSuccessMap.put(0.06, true);
			curSuccessMap.put(0.07, true);
			curSuccessMap.put(0.08, true);
/*			curSuccessMap.put(0.09, true);
	*/	}
		else if (diff <= 0.07){
			curSuccessMap.put(0.07, true);
			curSuccessMap.put(0.08, true);
	/*		curSuccessMap.put(0.09, true);
	*/	}
		else if (diff <= 0.08){
			curSuccessMap.put(0.08, true);
	//		curSuccessMap.put(0.09, true);
		}
	/*	else if (diff <= 0.09)
			curSuccessMap.put(0.09, true);
*/
		return curSuccessMap;
	}

}
