import java.io.BufferedWriter;

import weka.classifiers.Classifier;


public class ClassificationResults {
	
	public int tp,fp,tn,fn; 
	public double Accuracy,F1,Precision,Recall;
	private Classifier classifier;
	// keep an copy of support, confidence threshold values
	// these values are changed in runInMemory()
	// one_record will return wrong values if not use the copy
	public int support_start, support_end;
	public double confidence_start, confidence_end;
	public int instancenumber, locnumber;
	
	public static void writetitle(BufferedWriter outresult) throws Exception{
		outresult.write("maxNonePCDays,minPCDays,lowPercentile,PCPercentile,EPCPercentile,PCLowBound,PCUpBound" +
				",Accuracy,F1,Precision,Recall,PercentileUsed, support_start, support_end" +
				",Confidence_start,Confidence_end,start_month,end_month,instance_number,location_number,classifier");
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
				+support_start+","
				+support_end+","
				+confidence_start+","
				+confidence_end+","
				+flooding_prediction.start_month+","
				+flooding_prediction.end_month+","
				+instancenumber+","
				+locnumber+","
				+this.classifier.getClass().getSimpleName();
	}


	public ClassificationResults(int tp,int fp,int tn,int fn,Classifier classifier, int instancenumber, int locnumber){
		this.tp = tp;
		this.fp = fp;
		this.tn = tn;
		this.fn = fn;
		this.Accuracy = (tp+tn)/(tp+fp+tn+fn+0.0);
		this.F1 = (tp+tp)/(tp+fp+tp+fn+0.0);
		this.Precision = (tp)/(tp+fp+0.0);
		this.Recall = (tp)/(tp+fn+0.0);
		this.classifier = classifier;
		this.support_start = flooding_prediction.support_start;
		this.support_end = flooding_prediction.support_end;
		this.confidence_start = flooding_prediction.confidence_start;
		this.confidence_end = flooding_prediction.confidence_end;
		this.instancenumber = instancenumber;
		this.locnumber = locnumber;
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
