package kokkodis.holders;

public class KalmanParameterHolder {

	private float a;
	private float g;
	private float c;
	private float r;
	private float mu_0;
	private float p_0;

	public KalmanParameterHolder() {
		this.a = 1f;
		this.g = 0.02f;
		this.c = 1f;
		this.r = 0.02f;
		this.mu_0 = 0.87f; 
				//0.64f;
		// 0.912f; // mean of training data
		this.p_0 = 0.005f;
				//0.02f;
		 //0.037f; // variance of training data

	}

	public float getA() {
		return a;
	}

	public void setA(float a) {
		this.a = a;
	}

	public float getG() {
		return g;
	}

	public void setG(float g) {
		this.g = g;
	}

	public float getC() {
		return c;
	}

	public void setC(float c) {
		this.c = c;
	}

	public float getR() {
		return r;
	}

	public void setR(float r) {
		this.r = r;
	}

	public float getMu_0() {
		return mu_0;
	}

	public void setMu_0(float mu_0) {
		this.mu_0 = mu_0;
	}

	public float getP_0() {
		return p_0;
	}

	public void setP_0(float p_0) {
		this.p_0 = p_0;
	}

	@Override
	public String toString() {
		return "mu_0:" + mu_0 + " p_0:" + p_0 + " a:" + a + " g:" + g + " c:"
				+ c + " r:" + r;

	}

	public String toStringCSV() {
		return mu_0 + "," + p_0 + "," + a + "," + g + "," + c + "," + r;

	}


}
