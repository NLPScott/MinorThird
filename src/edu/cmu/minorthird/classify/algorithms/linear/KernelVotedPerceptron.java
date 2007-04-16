package edu.cmu.minorthird.classify.algorithms.linear;

import edu.cmu.minorthird.classify.ClassLabel;
import edu.cmu.minorthird.classify.Classifier;
import edu.cmu.minorthird.classify.Example;
import edu.cmu.minorthird.classify.ExampleSchema;
import edu.cmu.minorthird.classify.Explanation;
import edu.cmu.minorthird.classify.Feature;
import edu.cmu.minorthird.classify.Instance;
import edu.cmu.minorthird.classify.MutableInstance;
import edu.cmu.minorthird.classify.OnlineBinaryClassifierLearner;
import edu.cmu.minorthird.classify.algorithms.linear.Winnow.MyClassifier;
import edu.cmu.minorthird.util.gui.SmartVanillaViewer;
import edu.cmu.minorthird.util.gui.TransformedViewer;
import edu.cmu.minorthird.util.gui.Viewer;
import edu.cmu.minorthird.util.gui.Visible;

import java.io.*;
import java.util.ArrayList;

/**
 * Voted perceptron algorithm.  As described in "Large Margin
 * Classification Using the Perceptron Algorithm", Yoav Freund and
 * Robert E. Schapire, Proceedings of the Eleventh Annual Conference
 * on Computational Learning Theory,
 * 1998. 
 * 
 * Polynomial kernel is implemented: K(x,y) = (coef0+gamma*<x,y>)^d 
 * Both "voted" and "averaged" modes are implemented (unnormalized). Poly degree = 0 
 * means that no kernel is used.
 * 
 * Therefore, mode "averaged" with degree=0 should be equivalent to results in 
 * VotedPerceptron.java (file with a faster implementation of the averaged nonnormalized perceptron)
 *
 * @author Vitor Carvalho
 */

public class KernelVotedPerceptron extends OnlineBinaryClassifierLearner implements Serializable
{
	static private final long serialVersionUID = 1;//serialization stuff
	private final int CURRENT_SERIAL_VERSION = 1;
	
	private Hyperplane v_k; //current hypothesis
	private int c_k;//mistake counter
	private ArrayList listVK,listCK;//list with v_k, and list with c_k
	private String mode = "voted";// "voted"(default) or "averaged"
	
	//poly kernel
	private int degree = 3; //degree of poly kernel; default is 3
	private double gamma = 10,coef0 = 1;//K(x,y) = (coef0+gamma*<x,y>)^d 
	
	//speeds up inference by using only last MAXVEC kernels. Approximate results.
	private boolean speedup = false;//false;//
	private int MAXVEC = 300;//maximum of 1000 support vectors, for speed up
	
	/**
	 * Constructor: specifies degree of poly kernel and mode
	 * Example KernelVotedPerceptron(3,"averaged") or (5,"voted")
	 * @param degree
	 * @param mode
	 */
	public KernelVotedPerceptron(int degree, String mode) { 
		reset(); 
		this.degree = degree;
		this.mode = mode;
	}
	
	/**
	 * Standard Constructor: degree=3 and mode="voted"
	 */
	public KernelVotedPerceptron() { 
		reset(); 
	}
	
	/**
	 * set degree of poly kernel  K(x,y) = (coef0+ gamma*<x,y>)^d
	 * if set to 0, usual <x,v> crossproduct is used.
	 * @param degree
	 */
	public void setKernel(int d){degree = d;}
	/**
	 * set params of poly kernel K(x,y) = (coef0+ gamma*<x,y>)^d
	 * @param coef0
	 * @param gamma
	 */
	public void setPolyKernelParams(double coef0,double gamma){
		this.coef0 = coef0; this.gamma = gamma;
	}

	public void reset() 
	{
		v_k = new Hyperplane();
		listVK = new ArrayList();
		listCK = new ArrayList();
		c_k=0;
	}
	
	//set mode: voted or averaged
	public void setModeVoted(){mode = "voted";}
	public void setModeAveraged(){mode = "averaged";}
	/**
	 * Set speed-up: use only last 300 support vectors in testing
	 */
	public void setSpeedUp(){speedup = true;}
		
	//store support vectors and their counts (number of mistakes)
	private void store(Hyperplane h, int count){
		Hyperplane hh = new Hyperplane();
		hh.increment(h);
		listVK.add(hh);
		listCK.add(new Integer(count));	
	}

	//update rule for training: Figure 1 in Freund & Schapire paper
	public void addExample(Example example)
	{
		double y_t = example.getLabel().numericLabel();		
		if (Kernel(v_k,example.asInstance()) * y_t <= 0) {//prediction error occurred
			store(v_k,c_k);
			v_k.increment(example, y_t);
			c_k =1;						
		}
		else{
			c_k++;
		}
	}
	
//	poly kernel function
	double Kernel(Hyperplane h, Instance ins){
		double score = h.score(ins);
		if(degree==0) return score;  //no kernels
		else return Math.pow(coef0+(score*gamma),degree);		
		}
	
	//TESTING ------------------------------------------------------------
	
	public Classifier getClassifier() {		
		return new MyClassifier(listVK,listCK);	
	}
	
	 public class MyClassifier implements Classifier, Serializable,Visible
	 {
		static private final long serialVersionUID = 1;
		private final int CURRENT_SERIAL_VERSION = 1;
		
		ArrayList listVK, counts;
		
		public MyClassifier(ArrayList li, ArrayList cc) 
		{
			this.listVK = li;this.counts=cc; 
			System.out.println("KernelVotedPerceptron: number sup vectors = "+listVK.size()+" mode="+mode+" kernel="+degree);
		}
		
			//implements decision rule
			public ClassLabel classification(Instance ins) 
			{		
				double dec = 0;
				if(mode.equalsIgnoreCase("voted")){
					dec = calculateVoted(ins);
				}
				else if(mode.equalsIgnoreCase("averaged")){
					dec = calculateAveraged(ins);
				}
				else{
					System.out.println("Mode("+mode+") is not allowed\n Please use either \"voted\" or \"averaged\"");
					System.exit(0);
				}
				return dec>=0 ? ClassLabel.positiveLabel(dec) : ClassLabel.negativeLabel(dec);
			}
						
			//voted mode
			private double calculateVoted(Instance ins){
				double score = 0;
				int FIRSTVEC = 0;
				if(speedup){
					int MAX = Math.min(MAXVEC, listVK.size());
					FIRSTVEC = listVK.size() - MAX;
				}
				for(int i=FIRSTVEC; i<listVK.size(); i++){
					Hyperplane v_k = (Hyperplane)listVK.get(i);//v_k
					int countt = ((Integer)counts.get(i)).intValue();//c_k counts	
					double kernelScore = Kernel(v_k,(ins));
					double sign = (kernelScore>0)?+1:-1;//voting
					score += countt*(sign);
				}
				return score;
			}
			
			//average unnormalized mode
			private double calculateAveraged(Instance ins){
				double score = 0;	
				int FIRSTVEC = 0;
				if(speedup){
					int MAX = Math.min(MAXVEC, listVK.size());
					FIRSTVEC = listVK.size() - MAX;
				}
				for(int i=FIRSTVEC;i<listVK.size();i++){
					Hyperplane hp = (Hyperplane)listVK.get(i);
					int countt = ((Integer)counts.get(i)).intValue();
					score += countt*Kernel(hp,ins);
//					System.out.println(score);
				}			
//				System.out.println("----------------------------");
				return score;
			}	
			
			public String explain(Instance instance) 
			{
				return "KernelVotedPerceptron: Not implemented yet";
			}

			public Explanation getExplanation(Instance instance) {
			    Explanation.Node top = new Explanation.Node("Kernel Perceptron Explanation (not valid!)");
			    Explanation ex = new Explanation(top);		    
			    return ex;
			}
			public Viewer toGUI()
			{
			    Viewer v = new TransformedViewer(new SmartVanillaViewer()) {
				    public Object transform(Object o) {
					MyClassifier mycl = (MyClassifier)o;
					
					//dummy hyperplane: return last perceptron
					Hyperplane hh = (Hyperplane)mycl.listVK.get(listVK.size()-1);
					return (Classifier)hh;
				    }
				};
			    v.setContent(this);
			    return v;
			}
	 }

	public String toString() { return "Kernel Voted Perceptron"; }
}