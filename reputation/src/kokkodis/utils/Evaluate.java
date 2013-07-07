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
	private static HashMap<String, KalmanParameterHolder> thetaPerCategory;

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
		int initial = (GlobalVariables.evaluateOnTrain ? 3 : 0);
		for (historyThreshold = initial; historyThreshold <= limit; historyThreshold += 2) {

			errorHolder = new ErrorHolder();
			readAndEvaluate();

			DescriptiveStatistics modelMAE = getDescriptiveStatistics(errorHolder
					.getMaeModelList());
			DescriptiveStatistics modelMSE = getDescriptiveStatistics(errorHolder
					.getMseModelList());
			DescriptiveStatistics baselineMAE = getDescriptiveStatistics(errorHolder
					.getMaeBaselineList());
			DescriptiveStatistics baselineMSE = getDescriptiveStatistics(errorHolder
					.getMseBaselineList());

			String resStr = (GlobalVariables.curModel.equals("Binomial") ? GlobalVariables.currentBinomialThreshold
					: "NA")
					+ " | " + historyThreshold + " | ";

			// mseBinomialModel + " | "
			// + mseBaseline;
			if (GlobalVariables.printFiles) {
				if (!GlobalVariables.baselinePrinted) {
					GlobalVariables.allResultsFile
							.writeToFile((GlobalVariables.hierarchicalFlag ? GlobalVariables.gamma
									+ ","
									: "")
									+ ((GlobalVariables.synthetic) ? ("Baseline,")
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

				resStr = GlobalVariables.curApproach + " | " + resStr;
				GlobalVariables.allResultsFile
						.writeToFile((GlobalVariables.hierarchicalFlag ? GlobalVariables.gamma
								+ ","
								: "")
								+ ((GlobalVariables.synthetic) ? (GlobalVariables
										.getInstance().getClusterCategories()
										.get("r").length - 1 + ",")
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

			System.out.println(GlobalVariables.curModel + " | " + resStr);

		}

	}

	public static DescriptiveStatistics getDescriptiveStatistics(
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

			for (int cat : cats) {
				if (cat != 0) {
					HashMap<Integer, Double> hm;

					hm = eout.getValue().get(cat + "");

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
						: "")
				+ (GlobalVariables.hierarchicalFlag ? "_special" : "") + ".csv";

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
		String catName = "" + ri.getCategory();
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

		
		
		double numberPastTasks = evalWorker.getClusterHistoryMap().get("r")
				.get(0).getN();
		if (numberPastTasks >= historyThreshold) {

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

		int curCategory = ri.getCategory();

		ModelCategory specializedCurTask;
		ModelCategory specializedOveral;

		/*
		 * Deal with root.
		 */
		if (GlobalVariables.hierarchicalFlag) {

			ModelCategory genericCurTask = null;
			ModelCategory genericOveral = null;

			int rootCategory = Integer.parseInt(globalVariables
					.getCategoriesToClusters().get("" + ri.getCategory()));

			genericOveral = evalWorker.getClusterHistoryMap().get("r").get(0);
			if (genericOveral == null) {
				genericOveral = initBucketAndAddToMap(genericOveral, evalWorker
						.getClusterHistoryMap().get("r"), 0);
				genericCurTask = initBucketAndAddToMap(genericCurTask,
						evalWorker.getClusterHistoryMap().get("r"),
						rootCategory);

			} else {
				genericCurTask = evalWorker.getClusterHistoryMap().get("r")
						.get(rootCategory);
				if (genericCurTask == null) {
					genericCurTask = initBucketAndAddToMap(genericCurTask,
							evalWorker.getClusterHistoryMap().get("r"),
							rootCategory);

				}
			}

			Utils.addTaskOutcomeToCategory(genericCurTask, ri.getScore());
			Utils.addTaskOutcomeToCategory(genericOveral, ri.getScore());

		}

		HashMap<Integer, ModelCategory> specializedMcMap = evalWorker
				.getClusterHistoryMap().get(currentTaskCluster);
		if (specializedMcMap == null) {
			specializedMcMap = new HashMap<Integer, ModelCategory>();
			evalWorker.getClusterHistoryMap().put(currentTaskCluster,
					specializedMcMap);
		}
		specializedCurTask = specializedMcMap.get(curCategory);
		specializedOveral = evalWorker.getClusterHistoryMap()
				.get(currentTaskCluster).get(0);
		if (specializedOveral == null) {

			specializedOveral = initBucketAndAddToMap(specializedOveral,
					evalWorker.getClusterHistoryMap().get(currentTaskCluster),
					0);
			specializedCurTask = initBucketAndAddToMap(specializedCurTask,
					evalWorker.getClusterHistoryMap().get(currentTaskCluster),
					curCategory);

		} else {
			if (specializedCurTask == null) {
				specializedCurTask = initBucketAndAddToMap(
						specializedCurTask,
						evalWorker.getClusterHistoryMap().get(
								currentTaskCluster), curCategory);
			}

		}

		Utils.addTaskOutcomeToCategory(specializedOveral, ri.getScore());
		Utils.addTaskOutcomeToCategory(specializedCurTask, ri.getScore());

		evalWorker.setPreviousCategory(ri.getCategory());
		evalWorker.getClusterHistoryMap().get("r").get(0)
				.updateFeedbackList((float) ri.getScore());
	}

	private static ModelCategory initBucketAndAddToMap(ModelCategory mc,
			HashMap<Integer, ModelCategory> hashMap, int category) {
		if (GlobalVariables.curModel.equals("Binomial"))
			mc = new BinCategory();
		else if (GlobalVariables.curModel.equals("Kalman")) {
			mc = new KalmanFilter(thetaPerCategory.get(category + ""));
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

		baselineEstimatedQuality = estimateAvg(evalWorker, currentTaskCluster);

		modelQuality = predictModelQuality(evalWorker, ri, globalVariables
				.getCategoriesToClusters().get("" + ri.getCategory()),
				baselineEstimatedQuality);

		modelAbsoluteError = (Math.abs(modelQuality - ri.getScore()));

		baselineAbsoluteError = (Math.abs(baselineEstimatedQuality
				- ri.getScore()));

		errorHolder.getMaeModelList().add(modelAbsoluteError);
		errorHolder.getMseModelList().add(Math.pow(modelAbsoluteError, 2));
		errorHolder.getMaeBaselineList().add(baselineAbsoluteError);
		errorHolder.getMseBaselineList()
				.add(Math.pow(baselineAbsoluteError, 2));

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

	private static double estimateAvg(EvalWorker evalWorker,
			String currentTaskCluster) {

		double avg  = evalWorker.getClusterHistoryMap().get("r").get(0).getAverage();
		return ((evalWorker.getClusterHistoryMap().get("r").get(0).getN()>0)?avg:1);
	}

	private static double predictModelQuality(EvalWorker evalWorker,
			RawInstance ri, String cluster, double baselineEstimatedQuality) {
		HashMap<Integer, ModelCategory> hm;
		
		if (GlobalVariables.evaluateKalman) {
			hm = getAppropriateMap("r", evalWorker);
			double modelQuality = kalmanEstimate(null, hm);
			//System.out.println("evaluating kalman, prediction:"+modelQuality);
			return modelQuality;
		} else {

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
	}

	private static HashMap<Integer, ModelCategory> getAppropriateMap(
			String cluster, EvalWorker evalWorker) {

		if (!cluster.equals("r")) {
			HashMap<Integer, ModelCategory> mcMap = evalWorker
					.getClusterHistoryMap().get(cluster);
			try {

				if (mcMap.get(0).getN() > GlobalVariables.gamma) {
					// System.out.println("Nion technical");
					return mcMap;

				}
			} catch (NullPointerException ne) {
			}
		}

		return evalWorker.getClusterHistoryMap().get("r");
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
			KalmanFilter mc = (KalmanFilter) hm.get(0);
			
			modelQuality = Utils.fix(mc.predict());
		} else {
			for (int i : curCatIds) {

				KalmanFilter mc = (KalmanFilter) hm.get(i);
				if (mc == null) {

					mc = new KalmanFilter(thetaPerCategory.get(i + ""));
				}

				modelQuality += coeffs.get(i)
						* Utils.getLogit(Utils.fix(mc.predict()));
			}
		}
		return modelQuality;
	}

	private static HashMap<Integer, Double> getCurrentCoeffsAndSetBasedOn(
			String cluster, int cat) {
		HashMap<String, HashMap<Integer, Double>> tmpCoeff = GlobalVariables
				.getCurCoeffs().get(cluster);
		basedon = globalVariables.getClusterToBasedOn().get(cluster);
		HashMap<Integer, Double> tmp = tmpCoeff.get(cat + "");
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

		ModelCategory genericCurTaskCat = null;
		ModelCategory genericOveralCategory = null;

		EvalWorker evalWorker = new EvalWorker();
		evalWorker.setWorkerId(ri.getContractor());

		if (GlobalVariables.curModel.equals("Binomial")) {
			genericCurTaskCat = new BinCategory();
			genericOveralCategory = new BinCategory();
		} else if (GlobalVariables.curModel.equals("Kalman")) {
			genericCurTaskCat = new KalmanFilter(thetaPerCategory.get(ri
					.getCategory() + ""));
			genericOveralCategory = new KalmanFilter(
					thetaPerCategory.get(0 + ""));
		}

		else {
			genericCurTaskCat = new MultCategory();
			genericOveralCategory = new MultCategory();

		}

		HashMap<Integer, ModelCategory> specializedCatMap = new HashMap<Integer, ModelCategory>();
		specializedCatMap.put(0, genericOveralCategory);
		specializedCatMap.put(ri.getCategory(), genericCurTaskCat);
		if (GlobalVariables.hierarchicalFlag) {
			HashMap<Integer, ModelCategory> genericCatMap = new HashMap<Integer, ModelCategory>();
			genericCatMap.put(0, genericOveralCategory);
			genericCatMap.put(Integer.parseInt(globalVariables
					.getCategoriesToClusters().get("" + ri.getCategory())),
					genericCurTaskCat);
			evalWorker.getClusterHistoryMap().put("r", genericCatMap);
		}
		evalWorker.getClusterHistoryMap().put(currentTaskCluster,
				specializedCatMap);

		
		return evalWorker;

	}

	public static void evaluateKalman() {

		globalVariables = GlobalVariables.getInstance();

		thetaPerCategory = Utils.readThetas();

		System.out.println(GlobalVariables.line);

		System.out
				.println("model | approach |  ScoreThreshold | HistoryThreshold | MAE-model"
						+ " | MAE-Baseline"
						// + " | MAE-EM"
						+ " | MSE-model" + " | MSE-Baseline |");
		// Number oF Baseline Prediction");

		int limit = (GlobalVariables.evaluateOnTrain ? 3 : 25);
		int initial = (GlobalVariables.evaluateOnTrain ? 3 : 1);
		for (historyThreshold = initial; historyThreshold <= limit; historyThreshold += 2) {

			errorHolder = new ErrorHolder();
			readAndEvaluate();

			DescriptiveStatistics modelMAE = getDescriptiveStatistics(errorHolder
					.getMaeModelList());
			DescriptiveStatistics modelMSE = getDescriptiveStatistics(errorHolder
					.getMseModelList());
			DescriptiveStatistics baselineMAE = getDescriptiveStatistics(errorHolder
					.getMaeBaselineList());
			DescriptiveStatistics baselineMSE = getDescriptiveStatistics(errorHolder
					.getMseBaselineList());

			String resStr = (GlobalVariables.curModel.equals("Binomial") ? GlobalVariables.currentBinomialThreshold
					: "NA")
					+ " | " + historyThreshold + " | ";

			// mseBinomialModel + " | "
			// + mseBaseline;
			if (GlobalVariables.printFiles) {
				if (!GlobalVariables.baselinePrinted) {
					GlobalVariables.allResultsFile
							.writeToFile((GlobalVariables.hierarchicalFlag ? GlobalVariables.gamma
									+ ","
									: "")
									+ ((GlobalVariables.synthetic) ? ("Baseline,")
											: "")
									+ "Baseline"+GlobalVariables.curApproach
									+ ","
									+ "Baseline,"
									
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

				resStr = GlobalVariables.curApproach + " | " + resStr;
				GlobalVariables.allResultsFile
						.writeToFile((GlobalVariables.hierarchicalFlag ? GlobalVariables.gamma
								+ ","
								: "")
								+ ((GlobalVariables.synthetic) ? (GlobalVariables
										.getInstance().getClusterCategories()
										.get("r").length - 1 + ",")
										: "")
								+ GlobalVariables.curModel+(GlobalVariables.curApproach)
								+ ",Basic Model,"
								+ resStr.replaceAll(" \\| ", ",")
								+ modelMAE.getMean()
								+ ","
								+ baselineMAE.getMean()
								+ ","
								+ modelMSE.getMean()
								+ ","
								+ baselineMSE.getMean());

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

			System.out.println(GlobalVariables.curModel + " | " + resStr);

		}
	}

}
