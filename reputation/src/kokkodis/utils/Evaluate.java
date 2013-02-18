package kokkodis.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import kokkodis.holders.BinCategory;
import kokkodis.holders.ErrorHolder;
import kokkodis.holders.EvalWorker;
import kokkodis.holders.KalmanParameterHolder;
import kokkodis.holders.ModelCategory;
import kokkodis.holders.MultCategory;
import kokkodis.holders.PropertiesFactory;
import kokkodis.holders.RawInstance;
import kokkodis.kalman.KalmanFilter;

public class Evaluate {

	private static ErrorHolder errorHolder;
	private static int historyThreshold;
	private static GlobalVariables globalVariables;
	private static ArrayList<Integer> curCatIds;
	private static String basedon;
	private static HashMap<String, HashMap<String, Double>> lambdas;
	private static HashMap<Integer, KalmanParameterHolder> thetaPerCategory;
	private static int hugeErrors = 0;
	private static final double trustError = 0.5;
	private static double largerErrorModel = 0;
	private static PrintToFile bigErrors = new PrintToFile();

	public static void evaluate() {

		globalVariables = GlobalVariables.getInstance();

		if (GlobalVariables.curModel.equals("Kalman"))
			thetaPerCategory = Utils.readThetas();

		/*
		 * if (!GlobalVariables.evaluateOnTrain) readLambdas();
		 */
		if (GlobalVariables.printFiles)
			printCoefficients();

		System.out.println(GlobalVariables.line);

		System.out
				.println("model | approach |  ScoreThreshold | HistoryThreshold | MAE-model"
						+ " | MAE-Baseline"
						// + " | MAE-EM"
						+ " | MSE-model" + " | MSE-Baseline |");
		// Number oF Baseline Prediction");

		int limit = (GlobalVariables.evaluateOnTrain ? 3 : 15);
		int initial = (GlobalVariables.evaluateOnTrain ? 3 : 3);
		// bigErrors.openFile(PropertiesFactory.getInstance().getProps().getProperty("results")+"bigErrors.csv");
		// bigErrors.writeToFile("history,model,error");
		for (historyThreshold = initial; historyThreshold <= limit; historyThreshold += 2) {

			errorHolder = new ErrorHolder();
			hugeErrors = 0;
			largerErrorModel = 0;
			readAndEvaluate();

			// System.out.println("Huge Errors:"+hugeErrors+" Total:"+(errorHolder.getTotalEvaluations()+hugeErrors));
			/*
			 * double maeBaseline = errorHolder.getBaselineMAESum() /
			 * errorHolder.getTotalEvaluations(); double maeBasicModel =
			 * errorHolder.getBasicModelMAESum() /
			 * errorHolder.getTotalEvaluations();
			 * 
			 * double mseBasicModel = errorHolder.getBinomialModelMSESum() /
			 * errorHolder.getTotalEvaluations();
			 * 
			 * double mseBaseline = errorHolder.getBaselineMSESum() /
			 * errorHolder.getTotalEvaluations();
			 */

			DescriptiveStatistics modelMAE = getDescriptiveStatistics(errorHolder
					.getMaeModelList());
			DescriptiveStatistics modelMSE = getDescriptiveStatistics(errorHolder
					.getMseModelList());
			DescriptiveStatistics baselineMAE = getDescriptiveStatistics(errorHolder
					.getMaeBaselineList());
			DescriptiveStatistics baselineMSE = getDescriptiveStatistics(errorHolder
					.getMseBaselineList());

			// modelMAE.ge
			// double maeEMModel = errorHolder.getEMModelMAESum()
			// / errorHolder.getTotalEvaluations();

			// double maClusteredMModel = errorHolder.getClusteredModelMAESum()
			// / errorHolder.getTotalEvaluations();

			String resStr = (GlobalVariables.curModel.equals("Binomial") ? GlobalVariables.currentBinomialThreshold
					: "NA")
					+ " | " + historyThreshold + " | ";

			// mseBinomialModel + " | "
			// + mseBaseline;
			if (GlobalVariables.printFiles) {
				if (!GlobalVariables.baselinePrinted) {
					GlobalVariables.allResultsFile
							.writeToFile(((GlobalVariables.synthetic) ? ("Baseline,")
									: "")
									+ "Baseline"
									+ ","
									+ "Baseline,"
									// +( (GlobalVariables.curModel
									// .equals("Multinomial") &&
									// !GlobalVariables.synthetic) ? "Baseline,"
									// :
									+ (GlobalVariables.curApproach + ",")
									// )
									+ resStr.replaceAll(" \\| ", ",")
									+ baselineMAE.getMean()
									+ ","
									+ baselineMAE.getMean()
									+ ","
									+ baselineMSE.getMean()
									+ ","
									+ baselineMSE.getMean());

				}

				/*
				 * GlobalVariables.allResultsFile
				 * .writeToFile(((GlobalVariables.synthetic) ? (GlobalVariables
				 * .getInstance().getClusterCategories().get("r").length - 1 +
				 * ",") : "") + GlobalVariables.curModel + ",Shrinkage," +
				 * resStr.replaceAll(" \\| ", ",") + maeEMModel + "," +
				 * maeBaseline+","+mseBasicModel+","+mseBaseline);
				 */
				resStr = GlobalVariables.curApproach + " | " + resStr;
				GlobalVariables.allResultsFile
						.writeToFile(((GlobalVariables.synthetic) ? (GlobalVariables
								.getInstance().getClusterCategories().get("r").length - 1 + ",")
								: "")
								+ GlobalVariables.curModel
								+ ",Basic Model,"
								+ resStr.replaceAll(" \\| ", ",")
								+ modelMAE.getMean()
								+ ","
								+ baselineMAE.getMean()
								+ ","
								+ modelMSE.getMean()
								+ ","
								+ baselineMSE.getMean());
				/*
				 * GlobalVariables.allResultsFile
				 * .writeToFile(((GlobalVariables.synthetic) ? (GlobalVariables
				 * .getInstance().getClusterCategories().get("r").length - 1 +
				 * ",") : "") + GlobalVariables.curModel + ",Clusters," +
				 * resStr.replaceAll(" \\| ", ",") + maClusteredMModel + "," +
				 * maeBaseline);
				 */

			}

			resStr += Utils.getStringFromDouble(modelMAE.getMean())
					+ "("
					+ Utils.getStringFromDouble(modelMAE.getStandardDeviation())
					+ ")"
					+ " | "
					+ Utils.getStringFromDouble(baselineMAE.getMean())
					+ "("
					+ Utils.getStringFromDouble(baselineMAE
							.getStandardDeviation()) + ")" + "|"
					+ Utils.getStringFromDouble(modelMSE.getMean()) + " | "
					+ Utils.getStringFromDouble(baselineMSE.getMean()) + "|";
			// +
			// (double)hugeErrors/(double)(errorHolder.getTotalEvaluations()+hugeErrors)+
			// "|"+
			// (double)largerErrorModel/(double)(errorHolder.getTotalEvaluations()+hugeErrors);
			// + maeEMModel + " | " + maClusteredMModel;

			System.out.println(GlobalVariables.curModel + " | " + resStr);

		}
		// bigErrors.closeFile();

	}

	private static DescriptiveStatistics getDescriptiveStatistics(
			LinkedList<Double> list) {
		double[] tmp = new double[list.size()];
		int i = 0;
		for (double d : list) {
			tmp[i] = d;
			i++;
		}

		DescriptiveStatistics ds = new DescriptiveStatistics(tmp);
		return ds;

	}

	private static void readLambdas() {
		if (lambdas == null) {
			lambdas = new HashMap<String, HashMap<String, Double>>();

			try {
				String f = PropertiesFactory.getInstance().getProps()
						.getProperty("results");
				int categories = -1;
				if (GlobalVariables.synthetic)
					categories = globalVariables.getClusterCategories()
							.get("r").length - 1;
				f += "lambdas" + ((categories != -1) ? categories : "")
						+ ".csv";

				System.out.println("Loading lambdas from:" + f);
				BufferedReader input = new BufferedReader(new FileReader(f));
				String line;
				line = input.readLine();

				/**
				 * model,approach, ScoreThreshold, cluster, averageLambda,
				 * rLambda, clusterLambda
				 */
				while ((line = input.readLine()) != null) {
					String[] tmpAr = line.split(",");
					String key = "";
					if (tmpAr[0].equals("Binomial"))
						key = createKey(tmpAr[0], tmpAr[1], tmpAr[2], tmpAr[3]);
					else
						key = createKey(tmpAr[0], tmpAr[1], "", tmpAr[3]);
					HashMap<String, Double> curLambdas = new HashMap<String, Double>();
					curLambdas.put("average",
							Double.parseDouble(tmpAr[4].trim()));
					curLambdas.put("r", Double.parseDouble(tmpAr[5].trim()));
					if (GlobalVariables.hierarchicalFlag)
						curLambdas.put(tmpAr[3],
								Double.parseDouble(tmpAr[6].trim()));
					lambdas.put(key, curLambdas);

				}
				input.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	private static String createKey(String model, String approach,
			String scoreThreshold, String cluster) {

		return model + "_" + approach + "_" + scoreThreshold + "_" + cluster;
	}

	private static void printCoefficients() {
		int maxCoeffs = -1;
		int totalCoeffs = 0;
		for (Entry<String, String[]> e : GlobalVariables.getInstance()
				.getClusterCategories().entrySet()) {
			totalCoeffs = e.getValue().length * (e.getValue().length - 1);
			maxCoeffs = Math.max(maxCoeffs, totalCoeffs);
		}

		String header = "model,Approach,cluster,ScoreThreshold";
		for (int i = 0; i < maxCoeffs; i++) {
			header += ",coeff" + i;
		}
		globalVariables.getOutputFile().writeToFile(header);
		maxCoeffs += 4;
		System.out.println("Max Coeffs:" + maxCoeffs);

		for (Entry<String, HashMap<String, HashMap<Integer, Double>>> eout : GlobalVariables
				.getCurCoeffs().entrySet()) {
			String str = "";
			str += GlobalVariables.curModel
					+ ","
					+ GlobalVariables.curApproach
					+ ","
					+ eout.getKey()
					+ ","
					+ (GlobalVariables.curModel.equals("Binomial") ? GlobalVariables.currentBinomialThreshold
							: "");
			ArrayList<Integer> cats = Utils.getCurCatIds(eout.getKey());
			String basedon = globalVariables.getClusterToBasedOn().get(
					eout.getKey());

			for (int cat : cats) {
				if (cat != 0) {
					HashMap<Integer, Double> hm;
					hm = eout.getValue().get(cat + basedon);
					for (int cat1 : cats) {

						str += "," + hm.get(cat1);
					}
				}

			}
			int length = str.split(",").length;
			for (int i = length; i < maxCoeffs; i++) {
				str += ",";
			}
			globalVariables.getOutputFile().writeToFile(str);
		}

	}

	private static void readAndEvaluate() {

		HashMap<Integer, EvalWorker> dataMapHolderEval = new HashMap<Integer, EvalWorker>();
		String inputDirectory = PropertiesFactory.getInstance().getProps()
				.getProperty("rawPath");
		String testFile = "test";
		String trainFile = "train";
		if (GlobalVariables.synthetic) {
			// System.out.println("Evaluating on synthetic data...");
			int categories = globalVariables.getClusterCategories().get("r").length - 1;
			trainFile = "syn_train_cat" + categories;
			testFile = "syn_test_cat" + categories;

		} else if (GlobalVariables.syntheticCluster) {
			// System.out.println("Evaluating on synthetic data...");
			trainFile = "syn_cluster_train";
			testFile = "syn_cluster_test";

		}

		inputDirectory += (GlobalVariables.evaluateOnTrain ? trainFile
				: testFile)
				+ ((GlobalVariables.currentFold != null) ? GlobalVariables.currentFold
						: "") + ".csv";

		try {
			BufferedReader input = new BufferedReader(new FileReader(
					inputDirectory));
			String line;
			// System.out.println("Reading test data from file:"+inputDirectory);
			while ((line = input.readLine()).contains("#")) {
			}
			;

			/**
			 * contractor,category,score "
			 */

			while ((line = input.readLine()) != null) {
				RawInstance ri = Utils.stringToRawInstance(line);
				updateEvalWorker(dataMapHolderEval, ri);
			}
			input.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private static void updateEvalWorker(
			HashMap<Integer, EvalWorker> dataMapHolderEval, RawInstance ri) {

		EvalWorker evalWorker = dataMapHolderEval.get(ri.getContractor());
		String catName = globalVariables.getCatIntToName()
				.get(ri.getCategory());
		String currentTaskCluster = globalVariables.getCategoriesToClusters()
				.get(catName);

		/*
		 * Creates the necessary objects in order to add history in the end of
		 * the procedure The update follows the conditions!!
		 */

		if (evalWorker == null) {
			evalWorker = initEvalWorker(ri, currentTaskCluster);
			dataMapHolderEval.put(ri.getContractor(), evalWorker);
		}

		double numberPastTasks = evalWorker.getGenericHistoryMap().get(0)
				.getN();
		if (numberPastTasks > historyThreshold) {

			if (GlobalVariables.evaluateOnTransitions) {
				if (ri.getCategory() != evalWorker.getPreviousCategory()) {
					runUpdateErrors(evalWorker, ri, currentTaskCluster);
				}
			} else
				runUpdateErrors(evalWorker, ri, currentTaskCluster);

		}

		updateEvalWorker(evalWorker, ri, currentTaskCluster);

	}

	private static void runUpdateErrors(EvalWorker evalWorker, RawInstance ri,
			String currentTaskCluster) {

		if (GlobalVariables.curApproach.equals("PE")) {
			updateErrors(evalWorker, ri, currentTaskCluster);
		} else {
			for (int j = 0; j < GlobalVariables.rsTrials; j++) {
				updateErrors(evalWorker, ri, currentTaskCluster);

			}
		}

	}

	private static void updateEvalWorker(EvalWorker evalWorker, RawInstance ri,
			String currentTaskCluster) {
		/**
		 * Adding the other category holders in case is absent.
		 */

		int genericCategory = ri.getCategory();

		ModelCategory specializedCurTaskCat = null;
		ModelCategory specializedOveralCategory = null;
		ModelCategory genericCurTask;
		ModelCategory genericOveral;
		if (GlobalVariables.hierarchicalFlag) {
			if (currentTaskCluster.equals("rr")) {
				specializedOveralCategory = evalWorker.getTechnicalHistoryMap()
						.get(0);
				if (specializedOveralCategory == null) {
					specializedOveralCategory = initBucketAndAddToMap(
							specializedOveralCategory,
							evalWorker.getTechnicalHistoryMap(), 0);
					specializedCurTaskCat = initBucketAndAddToMap(
							specializedCurTaskCat,
							evalWorker.getTechnicalHistoryMap(),
							ri.getCategory());

				} else {
					specializedCurTaskCat = evalWorker.getTechnicalHistoryMap()
							.get(ri.getCategory());
					if (specializedCurTaskCat == null) {
						specializedCurTaskCat = initBucketAndAddToMap(
								specializedCurTaskCat,
								evalWorker.getTechnicalHistoryMap(),
								ri.getCategory());

					}
				}

			} else {
				/* Non technical, rl */
				specializedOveralCategory = evalWorker.getNonTechHistoryMap()
						.get(0);
				if (specializedOveralCategory == null) {

					specializedCurTaskCat = initBucketAndAddToMap(
							specializedCurTaskCat,
							evalWorker.getNonTechHistoryMap(), ri.getCategory());
					specializedOveralCategory = initBucketAndAddToMap(
							specializedOveralCategory,
							evalWorker.getNonTechHistoryMap(), 0);

				} else {
					specializedCurTaskCat = evalWorker.getNonTechHistoryMap()
							.get(ri.getCategory());
					if (specializedCurTaskCat == null) {
						specializedCurTaskCat = initBucketAndAddToMap(
								specializedCurTaskCat,
								evalWorker.getNonTechHistoryMap(),
								ri.getCategory());
					}
				}

			}
		}
		genericCurTask = evalWorker.getGenericHistoryMap().get(genericCategory);
		genericOveral = evalWorker.getGenericHistoryMap().get(0);
		if (genericOveral == null) {

			genericOveral = initBucketAndAddToMap(genericOveral,
					evalWorker.getGenericHistoryMap(), 0);
			genericCurTask = initBucketAndAddToMap(genericCurTask,
					evalWorker.getGenericHistoryMap(), genericCategory);

		} else {
			if (genericCurTask == null) {
				genericCurTask = initBucketAndAddToMap(genericCurTask,
						evalWorker.getGenericHistoryMap(), genericCategory);
			}

		}

		Utils.addTaskOutcomeToCategory(genericOveral, ri.getScore());
		Utils.addTaskOutcomeToCategory(genericCurTask, ri.getScore());
		if (GlobalVariables.hierarchicalFlag) {
			Utils.addTaskOutcomeToCategory(specializedOveralCategory,
					ri.getScore());
			Utils.addTaskOutcomeToCategory(specializedCurTaskCat, ri.getScore());

		}
		evalWorker.setPreviousCategory(ri.getCategory());
		evalWorker.getGenericHistoryMap().get(0)
				.updateFeedbackList((float) ri.getScore());
	}

	private static ModelCategory initBucketAndAddToMap(ModelCategory mc,
			HashMap<Integer, ModelCategory> hashMap, int category) {
		if (GlobalVariables.curModel.equals("Binomial"))
			mc = new BinCategory();
		else if (GlobalVariables.curModel.equals("Kalman")) {
			mc = new KalmanFilter(thetaPerCategory.get(category));
		} else
			mc = new MultCategory();
		hashMap.put(category, mc);
		return mc;

	}

	private static void updateErrors(EvalWorker evalWorker, RawInstance ri,
			String currentTaskCluster) {

		errorHolder.setTotalEvaluations(errorHolder.getTotalEvaluations() + 1);

		double modelAbsoluteError = 0;
		// double clusterSpecificAbsoluteError = 0;

		double baselineAbsoluteError = 0;
		// double emAbsoluteError = 0;

		double baselineEstimatedQuality = 0;
		double modelQuality = 0;
		double rlQuality = -1;
		double rrQuality = -1;
		double clusterQuality = -1;

		double emquality = 0;

		baselineEstimatedQuality = estimateAvg(evalWorker);

		modelQuality = predictModelQuality(
				evalWorker,
				ri,
				globalVariables.getCategoriesToClusters()
						.get(globalVariables.getCatIntToName().get(
								ri.getCategory())), baselineEstimatedQuality);
		//System.out.println(modelQuality);

		/*
		 * if (GlobalVariables.hierarchicalFlag) { if
		 * (currentTaskCluster.equals("rr")) { rrQuality =
		 * predictModelQuality(evalWorker, ri, currentTaskCluster);
		 * clusterSpecificAbsoluteError = Math.abs(rrQuality - ri.getScore());
		 * clusterQuality = rrQuality; } else { rlQuality =
		 * predictModelQuality(evalWorker, ri, currentTaskCluster);
		 * clusterSpecificAbsoluteError = Math.abs(rlQuality - ri.getScore());
		 * clusterQuality = rlQuality;
		 * 
		 * } errorHolder.setClusteredModelMAESum(errorHolder
		 * .getClusteredModelMAESum() + clusterSpecificAbsoluteError); }
		 */
		modelAbsoluteError = (Math.abs(modelQuality - ri.getScore()));

		/*
		 * if (GlobalVariables.curModel.equals("Binomial")) {
		 * baselineEstimatedQuality =
		 * estimateBinomialBaselineQuality(evalWorker); } else
		 * if(GlobalVariables.curModel.equals("Kalman")){
		 * baselineEstimatedQuality = estimateAvg(evalWorker); }else{
		 * baselineEstimatedQuality =
		 * estimateMultinomialBaselineQuality(evalWorker);
		 * 
		 * }
		 */

		/*
		 * try { emquality = estimateEMQuality(baselineEstimatedQuality,
		 * modelQuality, (rlQuality != -1) ? rlQuality : rrQuality,
		 * currentTaskCluster);
		 * 
		 * emAbsoluteError = Math.abs(emquality - ri.getScore());
		 * errorHolder.setEMModelMAESum(errorHolder.getEMModelMAESum() +
		 * emAbsoluteError); } catch (NullPointerException ne) {
		 * 
		 * }
		 */
		baselineAbsoluteError = (Math.abs(baselineEstimatedQuality
				- ri.getScore()));

		/*
		 * if(modelAbsoluteError - baselineAbsoluteError>0.1){
		 * largerErrorModel++; //System.out.println(modelQuality+
		 * " "+baselineEstimatedQuality+" "+ri.getScore()+" "+ri.getCategory());
		 * // bigErrors.writeToFile(historyThreshold+",model,"+modelQuality);
		 * bigErrors
		 * .writeToFile(historyThreshold+",baseline,"+baselineEstimatedQuality);
		 * bigErrors.writeToFile(historyThreshold+",score,"+ri.getScore()); }
		 */

		errorHolder.getMaeModelList().add(modelAbsoluteError);
		errorHolder.getMseModelList().add(Math.pow(modelAbsoluteError, 2));
		errorHolder.getMaeBaselineList().add(baselineAbsoluteError);
		errorHolder.getMseBaselineList()
				.add(Math.pow(baselineAbsoluteError, 2));

		/*
		 * errorHolder.setBasicModelMAESum(errorHolder.getBasicModelMAESum() +
		 * modelAbsoluteError);
		 * 
		 * errorHolder.setBinomialModelMSESum(errorHolder.getBinomialModelMSESum(
		 * ) + (Math.pow(modelAbsoluteError, 2)));
		 * 
		 * errorHolder.setBaselineMAESum(errorHolder.getBaselineMAESum() +
		 * baselineAbsoluteError);
		 * 
		 * errorHolder.setBaselineMSESum(errorHolder.getBaselineMSESum() +
		 * Math.pow(baselineAbsoluteError, 2));
		 */
		if (GlobalVariables.printFiles && GlobalVariables.outputPredictions) {
			if (GlobalVariables.evaluateOnTrain) {

				GlobalVariables.predictions
						.writeToFile(GlobalVariables.curModel
								+ ","
								+ GlobalVariables.curApproach
								+ ","
								+ historyThreshold
								+ ","
								+ Utils.getStringFromDouble(ri.getScore())
								+ ","
								+ Utils.getStringFromDouble(modelQuality)
								+ ","
								+ Utils.getStringFromDouble(baselineEstimatedQuality)
								+ "," + Utils.getStringFromDouble(rlQuality)
								+ "," + Utils.getStringFromDouble(rrQuality));
			} else {
				GlobalVariables.predictions.writeToFile(evalWorker
						.getPreviousCategory()
						+ "->"
						+ ri.getCategory()
						+ ","
						+ GlobalVariables.curModel
						+ ","
						+ GlobalVariables.curApproach
						+ ","
						+ historyThreshold
						+ ","
						+ Utils.getStringFromDouble(ri.getScore())
						+ ","
						+ Utils.getStringFromDouble(modelQuality)
						+ ","
						+ Utils.getStringFromDouble(baselineEstimatedQuality)
						+ ","
						+ Utils.getStringFromDouble(clusterQuality)
						+ ","
						+ Utils.getStringFromDouble(emquality));
			}
		}

	}

	private static double estimateAvg(EvalWorker evalWorker) {

		return evalWorker.getGenericHistoryMap().get(0).getAverage();
	}

	private static double estimateEMQuality(double average,
			double rIndependent, double clusterPrediction, String cluster) {

		if (clusterPrediction == -1)
			clusterPrediction = 0;
		HashMap<String, Double> hm = lambdas.get(createKey(
				GlobalVariables.curModel, GlobalVariables.curApproach,
				(GlobalVariables.curModel.equals("Binomial") ? ""
						+ GlobalVariables.currentBinomialThreshold : ""),
				cluster));
		if (GlobalVariables.hierarchicalFlag)
			return (hm.get("average") * average) + (hm.get("r") * rIndependent)
					+ (hm.get(cluster) * clusterPrediction);
		else
			return (hm.get("average") * average) + (hm.get("r") * rIndependent);

	}

	private static double predictModelQuality(EvalWorker evalWorker,
			RawInstance ri, String cluster, double baselineEstimatedQuality) {
		HashMap<Integer, ModelCategory> hm;
		HashMap<Integer, Double> coeffs;
		/**
		 * Use of root model.
		 */

		coeffs = getCurrentCoeffsAndSetBasedOn(cluster, ri.getCategory());
		hm = getAppropriateMap(cluster, evalWorker);
		curCatIds = Utils.getCurCatIds(cluster);
		// System.out.println(" curCluster:R");

		return finalModelEstimation(coeffs, hm, baselineEstimatedQuality);
	}

	private static HashMap<Integer, ModelCategory> getAppropriateMap(
			String cluster, EvalWorker evalWorker) {

		if (cluster.equals("r"))
			return evalWorker.getGenericHistoryMap();
		if (cluster.equals("rl")) // hierarchical flag is true to reach this
									// point
			if (evalWorker.getNonTechHistoryMap().size() > GlobalVariables.gamma){
				//System.out.println("Nion technical");
				return evalWorker.getNonTechHistoryMap();
			}
			else{
			//	System.out.println("Non technical generic");
				return evalWorker.getGenericHistoryMap();
			}
		if (evalWorker.getTechnicalHistoryMap().size() > GlobalVariables.gamma){
			//System.out.println("Technical");
			return evalWorker.getTechnicalHistoryMap();
		}
		//System.out.println("Technical Generic");
		return evalWorker.getGenericHistoryMap();
	}

	private static double finalModelEstimation(HashMap<Integer, Double> coeffs,
			HashMap<Integer, ModelCategory> hm, double baselineEstimatedQuality) {
		double modelQuality = 0;

		if (GlobalVariables.curModel.equals("Binomial")) {
			if (GlobalVariables.curApproach.equals("PE"))

				modelQuality = binomialPointEstimate(coeffs, hm);
			else
				modelQuality = binomialDistroEstimate(coeffs, hm);
		} else if (GlobalVariables.curModel.equals("Kalman")) {
			modelQuality = kalmanEstimate(coeffs, hm);
		} else {
			if (GlobalVariables.curApproach.equals("PE"))

				modelQuality = multinomialPointEstimate(coeffs, hm);
			else
				modelQuality = multinomialDistroEstimate(coeffs, hm);

		}

		modelQuality = Utils.inverseLogit(modelQuality);
		/*
		 * if(Math.abs(modelQuality - baselineEstimatedQuality) > trustError){
		 * hugeErrors++; modelQuality =
		 * (baselineEstimatedQuality+modelQuality)/2; }
		 */

		return modelQuality;

	}

	private static double kalmanEstimate(HashMap<Integer, Double> coeffs,
			HashMap<Integer, ModelCategory> hm) {
		double modelQuality = 0;
		if (coeffs == null) {
			System.out.println("Null coefficients!");
		}
		for (int i : curCatIds) {

			KalmanFilter mc = (KalmanFilter) hm.get(i);
			if (mc == null)
				mc = new KalmanFilter(thetaPerCategory.get(i));

			modelQuality += coeffs.get(i)
					* Utils.getLogit(Utils.fix(mc.predict()));
		}
		return modelQuality;
	}

	private static HashMap<Integer, Double> getCurrentCoeffsAndSetBasedOn(
			String cluster, int cat) {
		HashMap<String, HashMap<Integer, Double>> tmpCoeff = GlobalVariables
				.getCurCoeffs().get(cluster);
		basedon = globalVariables.getClusterToBasedOn().get(cluster);
		HashMap<Integer, Double> tmp = tmpCoeff.get(cat+"");
		if (tmp == null) {
			System.err.println("Cluster:" + cluster + " Cat:" + cat
					+ " basedOn:" + basedon + " gives me null coeffs.");
			for (String key : tmpCoeff.keySet()) {
				System.err.println(key);
			}
			System.exit(-1);

		}
		return tmp;

	}

	private static double multinomialDistroEstimate(
			HashMap<Integer, Double> coeffs, HashMap<Integer, ModelCategory> hm) {

		double modelQuality = 0;
		for (int i : curCatIds) {

			MultCategory bc = (MultCategory) hm.get(i);
			if (bc == null) {
				bc = new MultCategory();
				modelQuality += coeffs.get(i)
						* Utils.getLogit(Utils.fix(Utils
								.getDirichletDistroEstimate(bc
										.getBucketSuccesses())));//
				/*
				 * Parameters estimated by fitting beta matlab (betafit)
				 * getLogit(getCatMeans(i));
				 */
			} else
				modelQuality += coeffs.get(i)
						* Utils.getLogit(Utils.fix(Utils
								.getDirichletDistroEstimate(bc
										.getBucketSuccesses())));
		}
		return modelQuality;
	}

	private static double multinomialPointEstimate(
			HashMap<Integer, Double> coeffs, HashMap<Integer, ModelCategory> hm) {
		double modelQuality = 0;
		for (int i : curCatIds) {

			MultCategory mc = (MultCategory) hm.get(i);
			if (mc == null) {
				mc = new MultCategory();
				modelQuality += coeffs.get(i)
						* Utils.getLogit(Utils.fix(Utils
								.getDirichletPointEstimate(mc.getQ_ijk())));
				// System.out.println(Utils
				// .getDirichletPointEstimate(mc.getQ_ijk()));

			}// getLogit(getCatMeans(i));
			else
				modelQuality += coeffs.get(i)
						* Utils.getLogit(Utils.fix(Utils
								.getDirichletPointEstimate(mc.getQ_ijk())));

		}
		return modelQuality;
	}

	private static double binomialDistroEstimate(
			HashMap<Integer, Double> coeffs, HashMap<Integer, ModelCategory> hm) {
		double modelQuality = 0;
		for (int i : curCatIds) {
			BinCategory bc = (BinCategory) hm.get(i);
			if (bc == null)
				modelQuality += coeffs.get(i)
						* Utils.getLogit(Utils.fix(Utils
								.getDistroEstimate(0, 0)));//
			/*
			 * Parameters estimated by fitting beta matlab (betafit)
			 * getLogit(getCatMeans(i));
			 */
			else
				modelQuality += coeffs.get(i)
						* Utils.getLogit(Utils.fix(Utils.getDistroEstimate(
								bc.getX(), bc.getN())));
		}

		return modelQuality;

	}

	private static double binomialPointEstimate(
			HashMap<Integer, Double> coeffs, HashMap<Integer, ModelCategory> hm) {
		double modelQuality = 0;
		if (coeffs == null) {
			System.out.println("Null coefficients!");
		}
		for (int i : curCatIds) {

			BinCategory bc = (BinCategory) hm.get(i);
			if (bc == null)
				modelQuality += coeffs.get(i)
						* Utils.getLogit(Utils.getBinomialPointEstimate(0, 0));// getLogit(getCatMeans(i));
			else
				modelQuality += coeffs.get(i)
						* Utils.getLogit(Utils.fix(Utils
								.getBinomialPointEstimate(bc.getX(), bc.getN())));
		}
		return modelQuality;

	}

	private static EvalWorker initEvalWorker(RawInstance ri,
			String currentTaskCluster) {

		ModelCategory specializedOveralCategory = null;
		ModelCategory specializedCurTaskCat = null;

		ModelCategory genericCurTaskCat = null;
		ModelCategory genericOveralCategory = null;

		EvalWorker evalWorker = new EvalWorker();
		evalWorker.setWorkerId(ri.getContractor());

		if (GlobalVariables.curModel.equals("Binomial")) {
			specializedCurTaskCat = new BinCategory();
			genericCurTaskCat = new BinCategory();
			specializedOveralCategory = new BinCategory();
			genericOveralCategory = new BinCategory();
		} else if (GlobalVariables.curModel.equals("Kalman")) {
			specializedCurTaskCat = new KalmanFilter(thetaPerCategory.get(ri
					.getCategory()));
			genericCurTaskCat = new KalmanFilter(thetaPerCategory.get(ri
					.getCategory()));
			specializedOveralCategory = new KalmanFilter(
					thetaPerCategory.get(0));
			genericOveralCategory = new KalmanFilter(thetaPerCategory.get(0));
		}

		else {
			specializedCurTaskCat = new MultCategory();
			genericCurTaskCat = new MultCategory();
			specializedOveralCategory = new MultCategory();
			genericOveralCategory = new MultCategory();

		}
		evalWorker.getGenericHistoryMap().put(0, genericOveralCategory);
		evalWorker.getGenericHistoryMap().put(ri.getCategory(),
				genericCurTaskCat);
		if (GlobalVariables.hierarchicalFlag) {
			if (currentTaskCluster.equals("rr")) {
				evalWorker.getTechnicalHistoryMap().put(ri.getCategory(),
						specializedCurTaskCat);
				evalWorker.getTechnicalHistoryMap().put(0,
						specializedOveralCategory);
			} else {
				evalWorker.getNonTechHistoryMap().put(0,
						specializedOveralCategory);
				evalWorker.getNonTechHistoryMap().put(ri.getCategory(),
						specializedCurTaskCat);
			}
		}
		return evalWorker;

	}

	private static double estimateBinomialBaselineQuality(EvalWorker evalWorker) {

		return (((BinCategory) evalWorker.getGenericHistoryMap().get(0)).getX() / ((BinCategory) evalWorker
				.getGenericHistoryMap().get(0)).getN());

	}

	private static double estimateMultinomialBaselineQuality(
			EvalWorker evalWorker) {
		return getAverageHistory(((MultCategory) evalWorker
				.getGenericHistoryMap().get(0)).getBucketSuccesses(),
				((MultCategory) evalWorker.getGenericHistoryMap().get(0))
						.getN());
	}

	private static double getAverageHistory(double[] bucketSuccesses, double n) {
		double sum = 0;
		for (int i = 0; i < bucketSuccesses.length; i++) {
			sum += bucketSuccesses[i] * GlobalVariables.qualities[i];
			// System.out.println("Bucket successes:"+bucketSuccesses[i]+" qualities:"+GlobalVariables.qualities[i]);
		}
		// System.out.println("Bucket successes length:"+bucketSuccesses.length+" n:"+n);
		// System.out.println( sum / n);
		return sum / n;
	}

}
