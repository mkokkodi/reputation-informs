package kokkodis;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import kokkodis.holders.PropertiesFactory;
import kokkodis.utils.CategoriesPlot;
import kokkodis.utils.Evaluate;
import kokkodis.utils.GlobalVariables;
import kokkodis.utils.RunRegressions;
import kokkodis.utils.Utils;

public class ComputeME {

	/**
	 * @param args
	 */
	
	private static HashMap<Integer, LinkedList<Double>> data;
	private static GlobalVariables globalVariables;
	private static HashMap<Integer,DescriptiveStatistics> categoriesMeans;
	private static HashMap<Integer, Double> completeProductsPerCategory;
	private static HashMap<Integer, Double> incompleteProductsPerCategory;
	public static void main(String[] args) {
		globalVariables = GlobalVariables.getInstance();
		Reputation.crossValidation =false;
		GlobalVariables.curModel = "Kalman";
		GlobalVariables.curApproach = "RS";
		GlobalVariables.currentBinomialThreshold = 0.9f;
		
		GlobalVariables.evaluateOnTransitions = false;
	//GlobalVariables.hierarchicalFlag
		loadData();
		
		String [] models = {"Kalman","Multinomial","Binomial"};
		for(String model : models){
			GlobalVariables.curModel =model;
			compute();
		}
		
	
	}
	private static void compute() {
		for (String cluster : globalVariables.getHierarchyStracture()) {
			GlobalVariables.curCluster = cluster;
			GlobalVariables.getCurCoeffs().put(cluster,
					RunRegressions.readCoeffs());
			
		}
		computeMeans();
		computeCompleteProduct();
		computeIncompleteProduct();
		computeEffects();
	}
	private static void computeEffects() {
		HashMap<String, HashMap<Integer, Double>>  curCoeffs = GlobalVariables.getCurCoeffs().get("r");
		ArrayList<Integer> cats = Utils.getCurCatIds("r");


		for(int cat : cats){
			
			StringBuffer sb = new StringBuffer(); 
		    for(int catIn : cats){
		    	double q_l = categoriesMeans.get(catIn).getMean();
		    	double fraction = q_l/(1-q_l);
		    	double a_jl = curCoeffs.get(cat+"").get(catIn);
		       
		    	double effectTerm1 = (a_jl * Math.pow(fraction,a_jl))/(q_l - Math.pow(q_l,2));
		    	double effectTerm2 = incompleteProductsPerCategory.get(catIn)/Math.pow(1+completeProductsPerCategory.get(cat),2);
		    	double totalEffect = effectTerm1 * effectTerm2;
		    	
		    	sb.append("& "+Utils.getStringFromDouble(totalEffect));
		    }
		 	System.out.println(cat+" "+sb.toString());
			   
		    
		}
		
	}
	private static void computeIncompleteProduct() {
		incompleteProductsPerCategory = new HashMap<Integer, Double>();
		//cat_j -> cat_k -> coeff 
		HashMap<String, HashMap<Integer, Double>>  curCoeffs = GlobalVariables.getCurCoeffs().get("r");
		ArrayList<Integer> cats = Utils.getCurCatIds("r");


		for(int cat : cats){
			double prod = 1;
			
		    for(int catIn : cats){
		    	if(catIn != cat){
		    	double q_k = categoriesMeans.get(catIn).getMean();
		    	double fraction = q_k/(1-q_k);
		    	double a_jk = curCoeffs.get(cat+"").get(catIn);
		       
		    	prod *= Math.pow(fraction, a_jk);
		    	}
		    }
		    incompleteProductsPerCategory.put(cat,prod);
		}
		 
	}
	private static void computeMeans() {
		categoriesMeans = new HashMap<Integer, DescriptiveStatistics>();
		for(Entry<Integer,LinkedList<Double>> e: data.entrySet()){
			categoriesMeans.put(e.getKey(),Evaluate.getDescriptiveStatistics(e.getValue()));
		}
	}
	private static void computeCompleteProduct() {
		completeProductsPerCategory = new HashMap<Integer, Double>();
		//cat_j -> cat_k -> coeff 
		HashMap<String, HashMap<Integer, Double>>  curCoeffs = GlobalVariables.getCurCoeffs().get("r");
		ArrayList<Integer> cats = Utils.getCurCatIds("r");


		for(int cat : cats){
			double prod = 1;
			
		    for(int catIn : cats){
		    	double q_k = categoriesMeans.get(catIn).getMean();
		    	double fraction = q_k/(1-q_k);
		    	double a_jk = curCoeffs.get(cat+"").get(catIn);
		       
		    	prod *= Math.pow(fraction, a_jk);
		    }
		    completeProductsPerCategory.put(cat,prod);
		}
		 
		
	}
	private static void loadData() {
		System.out.println("Loading data...");

		data = new HashMap<Integer, LinkedList<Double>>();
		String f = PropertiesFactory.getInstance().getProps()
				.getProperty("rawPath")+"train.csv";
		
		try{
				BufferedReader input = new BufferedReader(new FileReader(f));
				String line;
				line = input.readLine();
				
				//contractor,category,score
				while ((line = input.readLine()) != null) {
					String [] tmpAr = line.split(",");
					int cat = Integer.parseInt(tmpAr[1]);
					LinkedList<Double> l = data.get(cat);
					if(l==null){
						l = new LinkedList<Double>();
						data.put(cat,l);
					}
					l.add(Double.parseDouble(tmpAr[2]));
					
				}
				input.close();
				System.out.println("Data loaded!");
			} catch (IOException e) {
				e.printStackTrace();
			}
		
	}

}
