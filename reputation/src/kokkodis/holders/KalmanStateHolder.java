package kokkodis.holders;

public class KalmanStateHolder {

	// for kalman smoother
	private float q_n; // is used as q_n in EM.
	private float mu_n; // used as mu_n in EM.
	private float v_n;// used as v_n in EM.
	private float p_n; // used as p_n_minus_1 in EM
	private int n;

	public KalmanStateHolder(int n, float q_n, float mu_n, float v_n, float p_n) {
		this.q_n = q_n;
		this.mu_n = mu_n;
		this.n = n;
		this.v_n = v_n;
		this.p_n = p_n;
	}

	public float getQ_n() {
		return q_n;
	}

	public float getMu_n() {
		return mu_n;
	}

	public float getV_n() {
		return v_n;
	}

	public float getP_n() {
		return p_n;
	}

	public int getN() {
		return n;
	}

	@Override
	public String toString() {
		return "q_" + n + " " + q_n + " mu_" + n + " " + mu_n + " v_" + n
				+ " " + v_n + " p_" + n + " " + p_n;

	}

}
