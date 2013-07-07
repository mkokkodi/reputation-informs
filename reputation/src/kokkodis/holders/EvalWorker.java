package kokkodis.holders;

import java.util.HashMap;

public class EvalWorker {

	private int workerId;
	public int getWorkerId() {
		return workerId;
	}

	public void setWorkerId(int workerId) {
		this.workerId = workerId;
	}

	/*
	 * cluster -> historyMap.
	 */
	private HashMap<String,HashMap<Integer, ModelCategory>> clusterHistoryMap;

	private int previousCategory;

	public int getPreviousCategory() {
		return previousCategory;
	}

	public void setPreviousCategory(int previousCategory) {
		this.previousCategory = previousCategory;
	}

	

	public HashMap<String,HashMap<Integer, ModelCategory>> getClusterHistoryMap() {
		return clusterHistoryMap;
	}

	public EvalWorker() {
		clusterHistoryMap = new HashMap<String,HashMap<Integer, ModelCategory>>();
		
	}

}
