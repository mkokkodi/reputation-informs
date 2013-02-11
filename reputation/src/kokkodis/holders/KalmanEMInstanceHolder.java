package kokkodis.holders;

public class KalmanEMInstanceHolder {
	private  float[] expectation_q_n_square;
	private  Float [] f_n;
	private  float[] expectation_q_n_q_n_minus_1;
	private  float[] hat_q_n;
	
	
	
	public KalmanEMInstanceHolder(float[] expectation_q_n_square, Float [] f_n,
			float[] expectation_q_n_q_n_minus_1,float[] hat_q_n){
		this.expectation_q_n_square = expectation_q_n_square;
		this.f_n = f_n;
		this.expectation_q_n_q_n_minus_1 = expectation_q_n_q_n_minus_1;
		this.hat_q_n = hat_q_n;
	}
	
	public  float[] getExpectation_q_n_square() {
		return expectation_q_n_square;
	}
	public  Float[]  getF_n() {
		return f_n;
	}

	public  float[] getExpectation_q_n_q_n_minus_1() {
		return expectation_q_n_q_n_minus_1;
	}

	public float[] getHat_q_n() {
		return hat_q_n;
	}
	
	
}
