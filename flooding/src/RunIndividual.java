import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.io.IOException;

/**
 * 
 */

/**
 * @author Jacky
 *
 */
public class RunIndividual {

	public static void testPercentileUsed(int usePercentile) throws Exception {
		flooding_prediction.PercentileUsed = usePercentile;
		BufferedWriter outresult = new BufferedWriter(new FileWriter(flooding_prediction.resultfiles[flooding_prediction.PercentileUsed],false));
		ClassificationResults.writetitle(outresult);
		outresult.close();
		
		flooding_prediction.Run_minPCDays(10, 15, true, new Callable<Void>() {
			   public Void call() throws Exception {
				   return flooding_prediction.Run_maxNonePCDays(2, 3, true, new Callable<ArrayList<ClassificationResults>>() {
					   public ArrayList<ClassificationResults> call() throws Exception {
					       return flooding_prediction.runInMemory(); }}); }});
		
	}
	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		flooding_prediction.EPCPercentile=0.85;
		flooding_prediction.PCUpBound = 0.85;
		testPercentileUsed(0);		
		/*
		flooding_prediction.trainData_start_year = 2001; flooding_prediction.trainData_end_year = 2009;
		flooding_prediction.testData_start_year = 2001; flooding_prediction.testData_end_year = 2009;
		flooding_prediction.minPCDays =14;
		flooding_prediction.maxNonePCDays=1;
		flooding_prediction.support_start = 0;
		flooding_prediction.confidence_start = 0.15;
		flooding_prediction.PercentileUsed =0;
		flooding_prediction.crossValFolds =5;
		
		flooding_prediction.run();
		*/
	}

}
