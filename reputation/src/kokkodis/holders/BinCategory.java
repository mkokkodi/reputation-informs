package kokkodis.holders;

import java.util.ArrayList;
import java.util.List;

import kokkodis.utils.Utils;

/**
 * 
 * @author mkokkodi
 * 
 */
public class BinCategory extends ModelCategory {

	private double x; // # successes ( helpful reviews)
	private double n; // total reviews
	private int previousCategory;
	private List<Float> f_n;

	public BinCategory() {
		x = n = 0;
		f_n = new ArrayList<Float>();
	}

	/**
	 * 
	 * @return succesful number of tasks in this cat
	 */
	public double getX() {
		return x;
	}

	public void setX(double x) {
		this.x = x;
	}

	/**
	 * 
	 * @return total number of tasks in this category
	 */
	public double getN() {
		return n;
	}

	public void setN(double n) {
		this.n = n;
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
	public float getAverage() {
		return Utils.getListAverage(f_n);
	}

	@Override
	public void updateFeedbackList(float f_n) {
		this.f_n.add(f_n);
	}
}
