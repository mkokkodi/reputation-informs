package kokkodis.utils;

import kokkodis.holders.PropertiesFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

public class CoefficientsAnalysis {

	/**
	 * @param args
	 */
	private static HashMap<String, HashMap<String, TreeMap<String, Double[]>>> data;

	private static final int folds = 10;
	private static boolean evaluateOnTransitions = true;

	public static void main(String[] args) {

		String[] models = GlobalVariables.getInstance().getModels();
		String[] approaches = GlobalVariables.getInstance().getApproaches();

		/**
		 * Model_Approach ->basedOn
		 */
		data = new HashMap<String, HashMap<String, TreeMap<String, Double[]>>>();

		for (String model : models)
			for (String approach : approaches) {
				if (model.equals("Binomial")) {
					for (float currentBinomialThreshold : GlobalVariables
							.getInstance().getScoreThresholds()) {
						GlobalVariables.currentBinomialThreshold = currentBinomialThreshold;

						for (int i = 1; i <= 10; i++) {
							updateMap(model, approach, i);
						}
					}
				} else {
					for (int i = 1; i <= 10; i++) {
						updateMap(model, approach, i);
					}
				}
			}
		printResults();
	}

	private static void printResults() {
		for (Entry<String, HashMap<String, TreeMap<String, Double[]>>> e : data
				.entrySet()) {

			String model_approach = e.getKey();
			String outStr = model_approach.split("_")[0];
			//+ " | "
				//	+ model_approach.split("_")[1];
			for (Entry<String, TreeMap<String, Double[]>> eIn : e.getValue()
					.entrySet()) {

				String mainCat = GlobalVariables.getInstance().getCatIntToName().get(Integer.parseInt(eIn.getKey()));
				String out2Str = outStr + " | " + mainCat;
				
				for (Entry<String, Double[]> e2In : eIn.getValue().entrySet()) {
					double[] tmp = new double[folds];
					for (int i = 0; i < e2In.getValue().length; i++)
						tmp[i] = e2In.getValue()[i].doubleValue();

					DescriptiveStatistics ds = new DescriptiveStatistics(tmp);
					double mean = ds.getMean();
					double stdev = ds.getStandardDeviation();
					out2Str += " | " + Utils.getStringFromDouble(mean) + " ("
							+ Utils.getStringFromDouble(stdev) + ")";

				}
				System.out.println(out2Str);

			}

		}

	}

	private static void updateMap(String model, String approach, int i) {
		try {
			String key =  model;
					//model;
			//+ "_" + approach;

			String inputPath = PropertiesFactory.getInstance().getProps()
					.getProperty("regressionOutPath");

			String f = inputPath
					+ "coeffs_"
					+ model
					+ "_"
					+ approach
					+ "_r_"
					+ (model.equals("Binomial") ? GlobalVariables.currentBinomialThreshold
							+ ""
							: "") + i + (evaluateOnTransitions?"_onTransitions":"")+".csv";
			BufferedReader input = new BufferedReader(new FileReader(f));
			String line;

			/**
			 * mainCategory-> category ->coeffs.
			 */
			HashMap<String, TreeMap<String, Double[]>> hm = data.get(key);
			if (hm == null) {
				hm = new HashMap<String, TreeMap<String, Double[]>>();
				data.put(key, hm);
			}
			TreeSet<String> uniqueCats = null;
			while ((line = input.readLine()) != null) {
				String[] tmpAr = line.split(",");
				String basedOn = tmpAr[0];
				String mainCategory = basedOn.split("_")[0];
				TreeMap<String, Double[]> coeffs = hm.get(mainCategory);
				if (coeffs == null) {
					coeffs = new TreeMap<String, Double[]>();
					hm.put(mainCategory, coeffs);
				}
				if (uniqueCats == null) {

					uniqueCats = new TreeSet<String>();
					basedOn = basedOn.replace("BasedOn", "");
					for (String cat : basedOn.split("_")) {
						if (!cat.equals(""))
							uniqueCats.add(cat);
					}
				}
				int j = 1;
				for (String category : uniqueCats) {
					Double[] catCoeffs = coeffs.get(category);
					if (catCoeffs == null) {
						catCoeffs = new Double[folds];
						coeffs.put(category, catCoeffs);
					}
					catCoeffs[i - 1] = Double.parseDouble(tmpAr[j].trim());

					j++;
				}
			}

			input.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
