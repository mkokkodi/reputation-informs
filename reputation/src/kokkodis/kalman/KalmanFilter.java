package kokkodis.kalman;

import java.util.ArrayList;
import java.util.List;

import kokkodis.holders.KalmanParameterHolder;
import kokkodis.holders.KalmanStateHolder;
import kokkodis.holders.ModelCategory;
import kokkodis.utils.GlobalVariables;
import kokkodis.utils.Utils;

public class KalmanFilter extends ModelCategory {

	/**
	 * q : latent f: observed
	 */
	private int n; // current interation
	private float q_n; // used as q_n+1 in EM
	private float k_n; // Kalman gain
	private float p_n;
	private float mu_n;
	private float v_n; // used as v_n+1 in EM
	private List<Float> f_n;

	private int previousCategory;

	private KalmanParameterHolder theta;

	public KalmanFilter(KalmanParameterHolder theta) {
		f_n = new ArrayList<Float>();
		this.theta = theta;
		this.n = 1;
		q_n = (float) getEstimate(theta.getMu_0(), theta.getP_0());

		k_n = (float) ((theta.getC() * theta.getP_0()) / ((Math.pow(
				theta.getC(), 2) * theta.getP_0()) + theta.getR()));

		v_n = estimate_v_n(theta.getP_0());// p_0
		p_n = (float) (Math.pow(theta.getA(), 2) * v_n + theta.getG());

	}

	/**
	 * 
	 * @param f_n
	 *            - current feedback, not yet seen.
	 * @return
	 */
	public KalmanStateHolder getCurrentState(float f_n) {
		if (n == 1) {
			// System.out.println("mu_n before:"+theta.getMu_0());
			mu_n = (float) (theta.getMu_0() + k_n
					* (f_n - theta.getC() * theta.getMu_0()));
			// System.out.println("F_n:"+f_n+" new mu_n:"+mu_n);
		}

		return new KalmanStateHolder(n, q_n, mu_n, v_n, p_n);

	}

	/**
	 * Does everything
	 * 
	 * @param f_n
	 */
	public void update(double f_n) {

		if (n == 1) {
			mu_n = (float) (theta.getMu_0() + k_n
					* (f_n - theta.getC() * theta.getMu_0()));
		}

		double cSquare = Math.pow(theta.getC(), 2);

		// estimate new p_n - use it in the next update as p_{n-1}

		// System.out.println("n="+n+" Previous q_n:"+q_n);
		// System.out.println("New f_n:"+f_n);
		float p_n_minus_1 = p_n;

		n++;

		k_n = (float) ((theta.getC() * p_n_minus_1) / (cSquare * p_n_minus_1 + theta
				.getR()));

		v_n = estimate_v_n(p_n_minus_1);

		float difference = (float) (f_n - theta.getA() * theta.getC() * mu_n);
		mu_n = (float) (theta.getA() * mu_n + k_n * difference);

		// System.out.println("Difference:"+difference+
		// "(a="+theta.getA()+" c="+theta.getC()+" mu:"+mu_n);
		q_n = (float) getEstimate(mu_n, v_n);

		// System.out.println("New q_n:"+q_n);
		p_n = (float) (Math.pow(theta.getA(), 2) * v_n + theta.getG());

	}

	/**
	 * 
	 * @return estimate for next feedback, f_n.
	 */
	public double predict() {
		if (n == 1)
			return getEstimate((theta.getC() * q_n), theta.getR());

		float q_n_given_q_n_minus_1 = (float) getEstimate(theta.getA() * q_n,
				theta.getG());
		return getEstimate((theta.getC() * q_n_given_q_n_minus_1), theta.getR());

		// double q_n_given_q_n_minus_1 =theta.getA() * q_n;
		// return theta.getC() * q_n_given_q_n_minus_1;

	}

	private double getEstimate(float mean, float var) {
		if(GlobalVariables.curApproach!=null &&
				GlobalVariables.curApproach.equals("PE"))
			return mean;
		
		double sum = 0;
		int total = 10;
		for (int i = 0; i < total; i++) {
			sum += Utils.getNormalEstimate(mean, var);
		}

		return sum / (double) (total);
	}

	private float estimate_v_n(float p_n_minus_1) {

		return (float) ((1 - theta.getC() * k_n) * p_n_minus_1);
	}

	@Override
	public double getN() {
		// TODO Auto-generated method stub
		return n - 1;
	}

	public float getAverage() {

		return Utils.getListAverage(f_n);
	}

	@Override
	public void setPreviousCategory(int cat) {
		previousCategory = cat;
	}

	@Override
	public int getPreviousCategory() {
		// TODO Auto-generated method stub
		return previousCategory;
	}

	@Override
	public void updateFeedbackList(float f_n) {
		this.f_n.add(f_n);
	}
}
