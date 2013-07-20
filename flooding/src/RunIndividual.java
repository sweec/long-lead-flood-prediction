import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
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

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		flooding_prediction.PercentileUsed = 1;
		BufferedWriter outresult = new BufferedWriter(new FileWriter(flooding_prediction.resultfiles[flooding_prediction.PercentileUsed],false));
		ClassificationResults.writetitle(outresult);
		outresult.close();
		flooding_prediction.Run_supportRange(4,4,true,null);
		
		flooding_prediction.Run_maxNonePCDays(1, 2, true,new Callable<Void>() {
			   public Void call() throws Exception {
			       return flooding_prediction.Run_minPCDays(5, 9, true,null); }});
		
	}

}
