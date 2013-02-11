package kokkodis.kalman;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import flanagan.control.Prop;

import kokkodis.holders.KalmanEMInstanceHolder;
import kokkodis.holders.KalmanParameterHolder;
import kokkodis.holders.KalmanStateHolder;
import kokkodis.holders.PropertiesFactory;
import kokkodis.utils.PrintToFile;
import kokkodis.utils.RunRegressions;
import kokkodis.utils.Utils;

public class EstimateParameters {

	/**
	 * @param args
	 */

	private static HashMap<String, KalmanParameterHolder> thetaPerCategory;
	private static  final int N = 3;
	private static final float e = 0.001f;
	private static final int maxInterations = 300;
	private static HashMap<String, ArrayList<Float[]>> dataPerCategory;
	private static KalmanParameterHolder curTheta;
	private static KalmanParameterHolder newTheta;
	private static KalmanStateHolder[] statesHolder;
	private static KalmanFilter curContractorKalmanFilter;

	private static float[] J_n;
	private static float[] hat_v_n;
	private static float[] hat_q_n;
	private static float[] expectation_q_n_q_n_minus_1;
	private static float[] expectation_q_n_square;

	private static float hat_q_1_sum;
	private static float sum_for_new_p0;
	private static float nom_for_new_a;
	private static float denom_for_new_a;
	private static float nom_for_new_c;
	private static float denom_for_new_c;

	private static Float[] curFeedbacks;
	private static int M;
	private static boolean converged;

	private static ArrayList<KalmanEMInstanceHolder> instancesHolder;

	public static void main(String[] args) {

		thetaPerCategory = new HashMap<String, KalmanParameterHolder>();
		dataPerCategory = getSequencialData();
		for (Entry<String, ArrayList<Float[]>> e : dataPerCategory.entrySet()) {

			converged = false;
			int iteration = 0;
			curTheta = thetaPerCategory.get(e.getKey());

			// if (e.getKey().equals("0")) {
			while (!converged && iteration < maxInterations) {
				iteration++;
				// System.out.println("Iteration:" +
				// iteration+" "+curTheta.toString());

				instancesHolder = new ArrayList<KalmanEMInstanceHolder>();

				newTheta = new KalmanParameterHolder();
				M = e.getValue().size();
				if (iteration == 1)
					System.out.println("Evaluating on " + M
							+ " instances with " + N + " observations.");
				hat_q_1_sum = 0;
				sum_for_new_p0 = 0;
				nom_for_new_a = 0;
				denom_for_new_a = 0;
				nom_for_new_c = 0;
				denom_for_new_c = 0;

				for (Float[] contractorFeedbacks : e.getValue()) {
					curFeedbacks = contractorFeedbacks;
					statesHolder = new KalmanStateHolder[N];
					J_n = new float[N];
					hat_v_n = new float[N];
					hat_q_n = new float[N];
					expectation_q_n_q_n_minus_1 = new float[N];
					expectation_q_n_square = new float[N];
					curContractorKalmanFilter = new KalmanFilter(curTheta);

					for (int n = 1; n <= N; n++) {
						runKalmanFilter(contractorFeedbacks[n - 1], n);

					}
					runEStep();

					// break;
				}

				runMStep();
				computeRestExpectations();
				converged = checkConvergence();
				curTheta = newTheta;
				// System.out.println(newTheta.toString());
				// break;
			}
			thetaPerCategory.put(e.getKey(), curTheta);
			if (iteration == maxInterations)
				System.out.println("Max number of iterations reached!");
			System.out.println("Thetas:" + newTheta.toString());
		}
		// }
		printThetas();

	}

	private static void printThetas() {

		PrintToFile pf = new PrintToFile();
		String outPath = PropertiesFactory.getInstance().getProps()
				.getProperty("rawPath");
		pf.openFile(outPath + "real_kalmanPriors.csv");
		pf.writeToFile("category,mu_0,p_0,a,g,c,r");
		for (Entry<String, KalmanParameterHolder> e : thetaPerCategory
				.entrySet())
			pf.writeToFile(e.getKey() + "," + e.getValue().toStringCSV());
		pf.closeFile();
	}

	private static boolean checkConvergence() {

		if (Math.abs(curTheta.getA() - newTheta.getA()) > e)
			return false;
		if (Math.abs(curTheta.getC() - newTheta.getC()) > e)
			return false;
		if (Math.abs(curTheta.getR() - newTheta.getR()) > e)
			return false;
		if (Math.abs(curTheta.getG() - newTheta.getG()) > e)
			return false;
		if (Math.abs(curTheta.getMu_0() - newTheta.getMu_0()) > e)
			return false;
		if (Math.abs(curTheta.getP_0() - newTheta.getP_0()) > e)
			return false;

		return true;

	}

	private static double calculateLikelihood() {

		/*
		 * if (curLikelihood == -1) { curLikelihood = calculate(curTheta);
		 * 
		 * }
		 */
		return calculate(newTheta);
	}

	private static double calculate(KalmanParameterHolder theta) {

		double Q = 0;
		for (KalmanEMInstanceHolder instance : instancesHolder) {
			float[] q = new float[N];
			q[0] = (float) Utils.getNormalEstimate(theta.getMu_0(),
					theta.getP_0());
			while (q[0] <= 0)
				q[0] = (float) Utils.getNormalEstimate(theta.getMu_0(),
						theta.getP_0());
			Double instanceSum = Math.log(q[0]);
			instanceSum += Math.log(Utils.getNormalEstimate(
					theta.getC() * q[0], theta.getR()));

			for (int i = 1; i < N; i++) {
				q[i] = (float) Utils.getNormalEstimate(theta.getA() * q[i - 1],
						theta.getG());
				while (q[i] <= 0)
					q[i] = (float) Utils.getNormalEstimate(theta.getA()
							* q[i - 1], theta.getG());
				instanceSum += Math.log(q[i]);
				double temp = Utils.getNormalEstimate(theta.getC() * q[i],
						theta.getR());
				while (temp <= 0)
					temp = Utils.getNormalEstimate(theta.getC() * q[i],
							theta.getR());
				instanceSum += Math.log(temp);
				if (instanceSum.isNaN()) {
					System.out.println("i:" + i + " q[i]:" + q[i] + " temp:"
							+ temp + " Log temp:" + Math.log(temp) + " q[i-1]:"
							+ q[i - 1] + "Log q[i]:" + Math.log(q[i]));
					return Double.NaN;
				}

			}

			Q += instanceSum;
		}
		return Q;
	}

	private static void runMStep() {

		newTheta.setMu_0(hat_q_1_sum / (float) M);
		newTheta.setP_0(sum_for_new_p0 / (float) M);
		newTheta.setA(nom_for_new_a / denom_for_new_a);
		newTheta.setC(nom_for_new_c / denom_for_new_c);

		// System.out.println("Nom for C:"+nom_for_new_c);
		// System.out.println("DeNom for C:"+denom_for_new_c);

	}

	private static void runKalmanFilter(float f_n, int n) {
		statesHolder[n - 1] = curContractorKalmanFilter.getCurrentState(f_n);
		// System.out.println(statesHolder[n-1].toString());
		// System.out.println("Feedback:"+f_n);
		curContractorKalmanFilter.update(f_n);

	}

	private static void runEStep() {

		float sum_q_n_q_n_minus_1 = 0;
		float sum_q_n_minus_1_square = 0;
		hat_v_n[N - 1] = statesHolder[N - 1].getV_n(); // initialization
		hat_q_n[N - 1] = statesHolder[N - 1].getQ_n();
		J_n[N - 1] = (float) (curTheta.getA() * statesHolder[N - 1].getV_n() / statesHolder[N - 1]
				.getP_n());

		float sum_f_n_times_q_n = hat_q_n[N - 1] * curFeedbacks[N - 1];

		for (int i = N - 2; i >= 0; i--) {

			J_n[i] = (float) (curTheta.getA() * statesHolder[i].getV_n() / statesHolder[i]
					.getP_n());
			hat_v_n[i] = (float) (statesHolder[i].getV_n() + Math
					.pow(J_n[i], 2)
					* (statesHolder[i + 1].getV_n() - statesHolder[i].getP_n()));
			hat_q_n[i] = statesHolder[i].getMu_n()
					+ J_n[i]
					* (hat_q_n[i + 1] - curTheta.getA()
							* statesHolder[i].getMu_n());

			expectation_q_n_q_n_minus_1[i + 1] = hat_v_n[i + 1] * J_n[i]
					+ hat_q_n[i + 1] * hat_q_n[i];

			expectation_q_n_square[i + 1] = (float) (hat_v_n[i + 1] + Math.pow(
					hat_q_n[i + 1], 2));

			/* for new a */
			sum_q_n_q_n_minus_1 += expectation_q_n_q_n_minus_1[i + 1];
			if (i < N - 2)
				sum_q_n_minus_1_square += expectation_q_n_square[i + 1];
			/* */

			sum_f_n_times_q_n += hat_q_n[i] * curFeedbacks[i];

		}

		// System.out.println("new m0 instance:"+hat_q_n[0]
		// +" mu_n:"+statesHolder[0].getMu_n() +" J_n:"+J_n[0]+
		// " a:"+curTheta.getA()+ " q_2:"+hat_q_n[1]);

		expectation_q_n_square[0] = (float) (hat_v_n[0] + Math.pow(hat_q_n[0],
				2));
		sum_q_n_minus_1_square += expectation_q_n_square[0];
		hat_q_1_sum += hat_q_n[0];
		sum_for_new_p0 += (expectation_q_n_square[0] - Math.pow(hat_q_n[0], 2));
		nom_for_new_a += sum_q_n_q_n_minus_1;
		denom_for_new_a += sum_q_n_minus_1_square;
		nom_for_new_c += sum_f_n_times_q_n;
		denom_for_new_c += (sum_q_n_minus_1_square + expectation_q_n_square[N - 1]);
		// System.out.println(denom_for_new_a);
		KalmanEMInstanceHolder instance = new KalmanEMInstanceHolder(
				expectation_q_n_square, curFeedbacks,
				expectation_q_n_q_n_minus_1, hat_q_n);
		instancesHolder.add(instance);

	}

	private static void computeRestExpectations() {

		float sum_for_new_g = 0;
		float sum_for_new_r = 0;
		float alphaSquare = (float) Math.pow(newTheta.getA(), 2);
		float cSquare = (float) Math.pow(newTheta.getC(), 2);

		for (KalmanEMInstanceHolder instance : instancesHolder) {
			for (int i = 1; i < N; i++) {
				sum_for_new_g += instance.getExpectation_q_n_square()[i]
						+ alphaSquare
						* instance.getExpectation_q_n_square()[i - 1] - 2
						* newTheta.getA()
						* instance.getExpectation_q_n_q_n_minus_1()[i];
				sum_for_new_r += Math.pow(instance.getF_n()[i], 2) - 2
						* newTheta.getC() * instance.getF_n()[i]
						* instance.getHat_q_n()[i] + cSquare
						* instance.getExpectation_q_n_square()[i];
			}
			int i = 0;
			sum_for_new_r += Math.pow(instance.getF_n()[i], 2) - 2
					* newTheta.getC() * instance.getF_n()[i]
					* instance.getHat_q_n()[i] + cSquare
					* instance.getExpectation_q_n_square()[i];

		}
		float g = sum_for_new_g / (float) (M * (N - 1));
		if (g < 0) {
			System.out.println("Negative variance for g.");
			g = Math.abs(g);
		}
		newTheta.setG(g);
		newTheta.setR(sum_for_new_r / (float) (M * (N)));
	}

	private static HashMap<String, ArrayList<Float[]>> getSequencialData() {

		HashMap<String, ArrayList<Float[]>> dataPerCategory = new HashMap<String, ArrayList<Float[]>>();
		String inputDir = PropertiesFactory.getInstance().getProps()
				.getProperty("rawPath");
		try {
			BufferedReader input = new BufferedReader(new FileReader(inputDir
					+ "trainSequential.csv")); //
			String line;
			line = input.readLine();
			/**
			 * category, contractor, list_of_feedbacks
			 */
			float denom = Float.parseFloat(PropertiesFactory.getInstance()
					.getProps().getProperty("outOfScore"));
			while ((line = input.readLine()) != null) {
				String[] tmpAr = line.split(",");
				if (tmpAr.length >= (N + 2)) {
					ArrayList<Float[]> curList = dataPerCategory.get(tmpAr[0]);
					if (curList == null) {
						curList = new ArrayList<Float[]>();
						dataPerCategory.put(tmpAr[0], curList);
						thetaPerCategory.put(tmpAr[0],
								new KalmanParameterHolder());
					}
					Float[] tmp = new Float[N];
					for (int i = 2; i < N + 2; i++) {
						tmp[i - 2] = Float.parseFloat(tmpAr[i].trim()) / denom;
					}
					curList.add(tmp);
					// break;
				}
			}
			input.close();

			return dataPerCategory;
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

}
