package kokkodis.factory;

import java.util.HashMap;

public class EvalWorker {

	private int workerId;
	public int getWorkerId() {
		return workerId;
	}

	public void setWorkerId(int workerId) {
		this.workerId = workerId;
	}

	private HashMap<Integer, ModelCategory> genericHistoryMap;
	private HashMap<Integer, ModelCategory> technicalHistoryMap;
	private HashMap<Integer, ModelCategory> nonTechHistoryMap;

	public HashMap<Integer, ModelCategory> getTechnicalHistoryMap() {
		return technicalHistoryMap;
	}

	public HashMap<Integer, ModelCategory> getNonTechHistoryMap() {
		return nonTechHistoryMap;
	}

	public HashMap<Integer, ModelCategory> getGenericHistoryMap() {
		return genericHistoryMap;
	}

	public EvalWorker() {
		genericHistoryMap = new HashMap<Integer, ModelCategory>();
		technicalHistoryMap = new HashMap<Integer, ModelCategory>();
		nonTechHistoryMap = new HashMap<Integer, ModelCategory>();
	}

}
