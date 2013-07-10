import java.io.BufferedWriter;

import weka.classifiers.Classifier;


public class ClassificationResults {
	
	public int tp,fp,tn,fn; 
	public double Accuracy,F1,Precision,Recall;
	private Classifier classifier;
	
	public static void writetitle(BufferedWriter outresult) throws Exception{
		outresult.write("maxNonePCDays,minPCDays,lowPercentile,PCPercentile,EPCPercentile,PCLowBound,PCUpBound" +
				",Accuracy,F1,Precision,Recall,PercentileUsed, support_start, support_end" +
				",Confidence_start,Confidence_end,start_month,end_month,classifier");
	}
	
	public String one_record() throws Exception{
		return "\n"+flooding_prediction.maxNonePCDays+","
				+flooding_prediction.minPCDays+","
				+flooding_prediction.lowPercentile+","
				+flooding_prediction.PCPercentile+","
				+flooding_prediction.EPCPercentile+","
				+flooding_prediction.PCLowBound+","
				+flooding_prediction.PCUpBound+","
				+this.toString()+","
				+flooding_prediction.idFile[flooding_prediction.PercentileUsed]+","
				+flooding_prediction.support_start+","
				+flooding_prediction.support_end+","
				+flooding_prediction.confidence_start+","
				+flooding_prediction.confidence_end+","
				+flooding_prediction.start_month+","
				+flooding_prediction.end_month+","
				+this.classifier.getClass().getSimpleName();
	}


	public ClassificationResults(int tp,int fp,int tn,int fn,Classifier classifier){
		this.tp = tp;
		this.fp = fp;
		this.tn = tn;
		this.fn = fn;
		this.Accuracy = (tp+tn)/(tp+fp+tn+fn+0.0);
		this.F1 = (tp+tp)/(tp+fp+tp+fn+0.0);
		this.Precision = (tp)/(tp+fp+0.0);
		this.Recall = (tp)/(tp+fn+0.0);
		this.classifier = classifier;
	}
	
	public void printout(){
		System.out.print("TP:" +tp+" FP:" +fp+" TN:" +tn+" FN:" +fn);
		System.out.print(" -> Accuracy = " + Accuracy);
		System.out.print(" F1 = " +F1);
		System.out.print(" Precision = " +Precision);
		System.out.println(" Recall = " +Recall);
		System.out.println("================================================");
	}
	
	public String toString(){
		String result="";
		result= Accuracy +", "+F1+", "+Precision+", "+Recall;
		return result;
	}
		
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
