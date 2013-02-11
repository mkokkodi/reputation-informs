package kokkodis.holders;

public abstract class ModelCategory {

	public abstract double getN();
	public abstract int getPreviousCategory();
	public abstract void setPreviousCategory(int cat);
	public abstract float getAverage();
	public abstract void updateFeedbackList(float d);
}


