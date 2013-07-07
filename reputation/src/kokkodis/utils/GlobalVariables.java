package kokkodis.utils;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Properties;


import kokkodis.holders.PropertiesFactory;

/*
 * Global Variables for Train, Regressions and Test
 */

public class GlobalVariables {

	public static  boolean printRegressionFiles = false;

	private static GlobalVariables globaleVars = null;

	public static String curCluster;
	public static String curApproach;
	public static String curModel;
	public static String line = "-----------------------------------------------------";
	public static double K;
	public static float currentBinomialThreshold;
	public static int rsTrials;
	public static PrintToFile allResultsFile;
	public static PrintToFile predictions;
	public static boolean printFiles = false;
	public static boolean hierarchicalFlag = false;
	public static boolean evaluateOnTrain=false;
	public static boolean outputPredictions = false;
	public static boolean baselinePrinted = false;
	

	/**
	 * Cluster -> cat_id based_x on cats -> (cat in cats) coeffs
	 */
	private static HashMap<String, HashMap<String, HashMap<Integer, Double>>> curCoeffs;

	public static double[] qualities;
	public static double[] binomialPrior;
	public static double[] multinomialPrior;
	public static double multinomialPriorTotal;
	public static int gamma;
	public static boolean evaluateKalman;

	private int historyThr;
	private String[] approaches;
	private String[] hierarchyStructure;
	private String[] models;
	private float[] scoreThresholds;

	private PrintToFile outputFile;
	private static Properties props;
	private HashMap<String, String[]> clusterToCategories;
	private HashMap<String, String> categoriesToClusters;
	//private HashMap<String, Integer> catNameToInt;
	private HashMap<String, String> clusterToBasedOn;
	//private HashMap<Integer,String> categoriesToRoot;
	//private HashMap<String,String> clustersToAbstractCategories;
	//private HashMap<Integer,Integer> categoryToAbstractCategory;
	public static int folds=-1;
	public static Integer currentFold = null;

	public static boolean synthetic = false;

	public static boolean syntheticCluster=false;

	public static boolean evaluateOnTransitions=false;

//	private double outOfScore;
	

//	public double getOutOfScore() {
	//	return outOfScore;
//	}

	/*public HashMap<Integer, String> getCategoriesToRoot() {
		return categoriesToRoot;
	}
*/
	public static HashMap<String, HashMap<String, HashMap<Integer, Double>>> getCurCoeffs() {

		return curCoeffs;
	}

	public HashMap<String, String> getClusterToBasedOn() {
		return clusterToBasedOn;
	}


	public HashMap<String, String> getCategoriesToClusters() {
		return categoriesToClusters;
	}

/*	public HashMap<String, Integer> getCatNameToInt() {
		return catNameToInt;
	}
*/
/*	public HashMap<Integer, String> getCatIntToName() {
		return catIntToName;
	}

	private HashMap<Integer, String> catIntToName;
*/
	public HashMap<String, String[]> getClusterCategories() {
		return clusterToCategories;
	}

	public void setClusterCategories(HashMap<String, String[]> clusterCategories) {
		this.clusterToCategories = clusterCategories;
	}

	private GlobalVariables() {

		curCoeffs = new HashMap<String, HashMap<String, HashMap<Integer, Double>>>();
		outputFile = new PrintToFile();
		props = PropertiesFactory.getInstance().getProps();
		gamma = Integer.parseInt(props.getProperty("gamma"));
		rsTrials = Integer.parseInt(props.getProperty("RS-trials"));
		models = props.getProperty("models").split(",");
		approaches = props.getProperty("approaches").split(",");
		hierarchyStructure = props.getProperty("hierarchyStructure").split(",");
		if (hierarchyStructure.length > 1)
			hierarchicalFlag = true;

		evaluateKalman = props.getProperty("evaluateKalman").equals("true")?true:false;
		historyThr = Integer.parseInt(props.getProperty("historyThr").trim());
		K = Integer.parseInt(props.getProperty("K").trim());
		String[] tmpAr = props.getProperty("scoreThresholds").split(",");
		scoreThresholds = new float[tmpAr.length];
		for (int i = 0; i < tmpAr.length; i++)
			scoreThresholds[i] = Float.parseFloat(tmpAr[i]);

		clusterToCategories = new HashMap<String, String[]>();
		categoriesToClusters = new HashMap<String, String>();
		for (String cluster : hierarchyStructure) {
			String[] t = props.getProperty(cluster.trim()).split(",");
			clusterToCategories.put(cluster, t);
			for (String catName : t){
				System.out.println("Category "+catName+" to cluster:"+cluster);
				categoriesToClusters.put(catName, cluster);
			}

		}
	/*	catNameToInt = new HashMap<String, Integer>();
		catIntToName = new HashMap<Integer, String>();
		String[] catMapping = props.getProperty("category-mapping").split(",");
		for (String s : catMapping) {
			String[] t = s.split(":");
			catNameToInt.put(t[1], Integer.parseInt(t[0]));
			catIntToName.put(Integer.parseInt(t[0]), t[1]);
		}
		
		categoriesToRoot = new HashMap<Integer, String>();
		for (String s : props.getProperty("category-to-root").split(",")) {
			String[] t = s.split(":");
			categoriesToRoot.put(Integer.parseInt(t[0].trim()),t[1]);
		}
	
		clusterToBasedOn = new HashMap<String, String>();
		for (String s : props.getProperty("basedon").split(",")) {
			String[] t = s.split(":");
			clusterToBasedOn.put(t[0], t[1]);
		}
*/
		
		clusterToBasedOn = new HashMap<String, String>();
		
		for(Entry<String, String[]> clusterToCategory: clusterToCategories.entrySet()){
			String basedOn="_BasedOn";
			for(String cat:clusterToCategory.getValue())
				basedOn+= "_"+cat;
			clusterToBasedOn.put(clusterToCategory.getKey(), basedOn);
		}
		String[] bptmp = props.getProperty("binomialPrior").split(",");
		binomialPrior = new double[bptmp.length];
		for (int i = 0; i < bptmp.length; i++)
			binomialPrior[i] = Double.parseDouble(bptmp[i]);

		String[] mptmp = props.getProperty("multinomialPrior").split(",");
		multinomialPrior = new double[(int) K];
		for (int i = 0; i < (int) K; i++)
			multinomialPrior[i] = 0;
		multinomialPriorTotal = 0;
		for (int i = 0; i < mptmp.length; i++) {
			String[] tmpAr3 = mptmp[i].split(":");
			int priorInd = Integer.parseInt(tmpAr3[0]);
			multinomialPrior[priorInd] = Double.parseDouble(tmpAr3[1]);
			multinomialPriorTotal += multinomialPrior[priorInd];
		}
		qualities = new double[(int) K];
		double q = 0;
		for (int i = 0; i < K; i++) {
			q += 1.0 / K;
			// System.out.println(i+" - "+q);
			qualities[i] = q;
		}
	//	outOfScore = Double.parseDouble(props.getProperty("outOfScore"));
	/*	clustersToAbstractCategories = new HashMap<String, String>();
		for (String s : props.getProperty("clusters-to-abstract").split(",")) {
			String[] t = s.split(":");
			clustersToAbstractCategories.put(t[0], t[1]);
		}

		categoryToAbstractCategory = new HashMap<Integer, Integer>();
		for(java.util.Map.Entry<String, Integer> e: catNameToInt.entrySet()){
			if(categoriesToClusters.get(e.getKey()).equals("rl")){
				categoryToAbstractCategory.put(e.getValue(),1);
			}else{
				categoryToAbstractCategory.put(e.getValue(),2);
			}
		}
		*/

	}

	public static GlobalVariables getInstance() {
		if (globaleVars == null)
			globaleVars = new GlobalVariables();

		return globaleVars;
	}

	/**
	 * Maps rr to technical and rl to nontechnical. 
	 * Input is "rr" or "rl".
	 * @return
	 */
	/*public HashMap<String, String> getClustersToAbstractCategories() {
		return clustersToAbstractCategories;
	}
/**
 * 
 * @return the mapping of category to "1" (if rl) or 2 (if rr);
 */
/*	public HashMap<Integer, Integer> getCategoryToAbstractCategory() {
		return categoryToAbstractCategory;
	}
*/
	public static GlobalVariables getGlobaleVars() {
		return globaleVars;
	}

	public int getHistoryThr() {
		return historyThr;
	}

	public String[] getApproaches() {
		return approaches;
	}

	public String[] getHierarchyStracture() {
		return hierarchyStructure;
	}

	public String[] getModels() {
		return models;
	}

	public float[] getScoreThresholds() {
		return scoreThresholds;
	}

	public PrintToFile getOutputFile() {
		return outputFile;
	}

	public void openFile(String str) {
		this.outputFile.openFile(str);
	}

}
