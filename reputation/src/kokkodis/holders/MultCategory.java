package kokkodis.holders;

import java.util.ArrayList;
import java.util.List;

import kokkodis.utils.GlobalVariables;
import kokkodis.utils.Utils;

public class MultCategory extends ModelCategory {

	private double[] bucketSuccesses;
	private double n; /* total trials */
	private double[] q_ijk; /* probability distro across buckets! */
	private int previousCategory;
	private List<Float> f_n;


	public double[] getQ_ijk() {
		return q_ijk;
	}

	public MultCategory() {
		f_n = new ArrayList<Float>();
		bucketSuccesses = new double[(int) GlobalVariables.K];
		for (int i = 0; i < bucketSuccesses.length; i++)
			bucketSuccesses[i] = 0;
		q_ijk = new double[(int) GlobalVariables.K];
		n = 0;
		calculateNewDistroQijk();
	}

	public double[] getBucketSuccesses() {
		return bucketSuccesses;
	}

	public void increaseTotalTrials() {
		n++;
		calculateNewDistroQijk();
	}

	private void calculateNewDistroQijk() {

		for (int i = 0; i < q_ijk.length; i++)
			q_ijk[i] = (bucketSuccesses[i] + GlobalVariables.multinomialPrior[i])
					/ (n + GlobalVariables.multinomialPriorTotal);
	}

	public double getN() {
		return n;
	}

	@Override
	public int getPreviousCategory() {
		// TODO Auto-generated method stub
		return previousCategory;
	}

	@Override
	public void setPreviousCategory(int cat) {
		previousCategory = cat;
	}
	
	@Override
	public void updateFeedbackList(float f_n) {
		this.f_n.add(f_n);
	}
	
	@Override
	public float getAverage() {
		return Utils.getListAverage(f_n);
	}

}
