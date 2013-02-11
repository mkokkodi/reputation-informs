package kokkodis.holders;

public class ErrorHolder {

	private double baselineMAESum;
	private double basicModelMAESum;
	private double EMModelMAESum;
	private double ClusteredModelMAESum;
	private double KalmanMAESum;

	private double baselineMSESum;
	private double basicModelMSESum;
	public double totalEvaluations;

	public ErrorHolder() {
		super();

		baselineMAESum = 0;
		totalEvaluations = 0;
		basicModelMAESum = 0;
		
		EMModelMAESum = 0;
		ClusteredModelMAESum = 0;
		KalmanMAESum = 0;
	}

	
	
	public double getKalmanMAESum() {
		return KalmanMAESum;
	}



	public void setKalmanMAESum(double kalmanMAESum) {
		KalmanMAESum = kalmanMAESum;
	}



	public double getClusteredModelMAESum() {
		return ClusteredModelMAESum;
	}



	public void setClusteredModelMAESum(double clusteredModelMAESum) {
		ClusteredModelMAESum = clusteredModelMAESum;
	}



	public double getEMModelMAESum() {
		return EMModelMAESum;
	}



	public void setEMModelMAESum(double eMModelMAESum) {
		EMModelMAESum = eMModelMAESum;
	}



	public double getBaselineMSESum() {
		return baselineMSESum;
	}



	public void setBaselineMSESum(double baselineMSESum) {
		this.baselineMSESum = baselineMSESum;
	}



	public double getBinomialModelMSESum() {
		return basicModelMSESum;
	}



	public void setBinomialModelMSESum(double binomialModelMSESum) {
		this.basicModelMSESum = binomialModelMSESum;
	}



	public double getBasicModelMAESum() {
		return basicModelMAESum;
	}

	public void setBasicModelMAESum(double abstractModelErrorSum) {
		this.basicModelMAESum = abstractModelErrorSum;
	}

	public double getBaselineMAESum() {
		return baselineMAESum;
	}

	public void setBaselineMAESum(double noModelErrorSum) {
		this.baselineMAESum = noModelErrorSum;
	}

	public double getTotalEvaluations() {
		return totalEvaluations;
	}

	public void setTotalEvaluations(double totalEvaluations) {
		this.totalEvaluations = totalEvaluations;
	}

}
