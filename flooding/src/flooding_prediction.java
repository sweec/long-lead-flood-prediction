import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.concurrent.Callable;

import weka.classifiers.Classifier;

public class flooding_prediction {
	static public Calendar Base_Date = new GregorianCalendar(1980, 0, 1); // first date's ID starts with 0
	static public String IowaPrecipFile = "./data/text/PRECIP2_1980-2010.txt";
	static public String IowaPCFile = "IowaPCs.txt";
	static public String IowaEPCFile = "IowaEPCs.txt";
	static public Calendar Start_Date = new GregorianCalendar(2001, 0, 1);
	static public Calendar End_Date = new GregorianCalendar(2010, 11, 31);
	static public long Data_start_day =(Start_Date.getTimeInMillis()-Base_Date.getTimeInMillis())/(1000 * 60 * 60 * 24);
	static public long Data_end_day = (End_Date.getTimeInMillis()-Base_Date.getTimeInMillis())/(1000 * 60 * 60 * 24);
	//static public long Data_end_day = 23010-11688; //1980-1-1 to 2010-12-31
	//static int UsingData_start_day = 18993, UsingData_end_day = 23010; //2000-1-1 to 2010-12-31
	//static int UsingData_start_day = 0, UsingData_end_day = 23010;
	static public int trainData_start_year = 2001, trainData_end_year = 2010;
	static public int testData_start_year = 2001, testData_end_year = 2010;
	static public int totalDays = (int)(Data_end_day-Data_start_day+1) , totalSampleLocations = 5328;
	static public int maxNonePCDays = 2, minPCDays = 7;
	static public double lowPercentile = 0.2, PCPercentile=0.6, EPCPercentile=0.90;
	static public double PCLowBound = 0.6, PCUpBound = 0.9;
	static public boolean RandomselectPC = false;
	static public String idFile[] = {"loc_Individuals.txt","loc_Iowa.txt","loc_All.txt"};
	static public int PercentileUsed = 0; //0: Individuals  1: Iowa  2: All locations
	static public int start_month = 5, end_month = 9; // summer time = May to September
	static public int support_start = 0, support_end = Integer.MAX_VALUE;
	static int[] supportranges = new int[]{0,100,200,392,9999};
	static int[] supportthresholds = new int[]{0,100,150,200,250};
	static public double confidence_start = -1, confidence_end = Double.POSITIVE_INFINITY;
	static double[] confidencethresholds = new double[]{0,0.1,0.11,0.12,0.15};
	static public int baseclassifier =2; //0:SVM 1:LADTree 2:J48 3:NaiveBayes
	static public int crossValFolds =5;

	private static String[] resultfiles = new String[]{"./results/Individuals.csv","./results/Iowa.csv","./results/All.csv"};
	
	public static Void Run_maxNonePCDays(int start, int end, boolean append, Callable<Void> func) throws Exception {
		BufferedWriter outresult = new BufferedWriter(new FileWriter(resultfiles[PercentileUsed],append));
		if (! append){
			ClassificationResults.writetitle(outresult);
		}
		for (int days=start;days <= end;days++) {
			maxNonePCDays = days;//
			if (func == null) {
				 ClassificationResults result = run();
				 result.printout();
				 outresult.write(result.one_record());
				 outresult.flush();
			} else {
				func.call();
			}			
		}
		outresult.close();
		
		return null;
	}
	
	public static Void Run_supportRange(int start, int end, boolean append, Callable<Void> func) throws Exception {
		BufferedWriter outresult = new BufferedWriter(new FileWriter(resultfiles[PercentileUsed],append));
		if (! append){
			ClassificationResults.writetitle(outresult);
		}
		for (int s=start;s <= end;s++) {
			support_start = supportranges[s];
			support_end = supportranges[s+1];
			if (func == null) {
				 ClassificationResults result = run();
				 result.printout();
				 outresult.write(result.one_record());
				 outresult.flush();
			} else {
				func.call();
			}			
		}
		outresult.close();
		
		return null;
	}
	
	public static Void Run_supportThreshold(int start, int end, boolean append, Callable<Void> func) throws Exception {
		BufferedWriter outresult = new BufferedWriter(new FileWriter(resultfiles[PercentileUsed],append));
		if (! append){
			ClassificationResults.writetitle(outresult);
		}
		for (int s=start;s <= end;s++) {
			support_start = supportthresholds[s];
			support_end = Integer.MAX_VALUE;
			if (func == null) {
				 ClassificationResults result = run();
				 result.printout();
				 outresult.write(result.one_record());
				 outresult.flush();
			} else {
				func.call();
			}			
		}
		outresult.close();
		
		return null;
	}
	
	public static Void Run_confThreshold(int start, int end, boolean append, Callable<Void> func) throws Exception {
		BufferedWriter outresult = new BufferedWriter(new FileWriter(resultfiles[PercentileUsed],append));
		if (! append){
			ClassificationResults.writetitle(outresult);
		}
		for (int s=start;s <= end;s++) {
			confidence_start = confidencethresholds[s];
			confidence_end = Double.POSITIVE_INFINITY;
			if (func == null) {
				 ClassificationResults result = run();
				 result.printout();
				 outresult.write(result.one_record());
				 outresult.flush();
			} else {
				func.call();
			}			
		}
		outresult.close();
		
		return null;
	}
	
	public static Void Run_minPCDays(int start, int end, boolean append, Callable<Void> func) throws Exception {
		BufferedWriter outresult = new BufferedWriter(new FileWriter(resultfiles[PercentileUsed],append));
		if (! append){
			ClassificationResults.writetitle(outresult);
		}
		for (int days=start;days <= end;days++) {
			minPCDays = days;//
			if (func == null) {
				 ClassificationResults result = run();
				 result.printout();
				 outresult.write(result.one_record());
				 outresult.flush();
			} else {
				func.call();
			}			
		}
		outresult.close();
		
		return null;
	}
	  
	public static ClassificationResults run() throws Exception {
		String delimit = "\\s+";
    	double[] IowaPWs = new double[totalDays];
    	
    	// read Iowa precipitation water data into an array
    	BufferedReader reader = new BufferedReader(new FileReader(IowaPrecipFile));
		String line="";
		int days = 0, i=0;
		while ((line = reader.readLine()) != null) {
			if ( (days >= Data_start_day) && (days <= Data_end_day)) {
				String[] values = line.split(delimit);
				IowaPWs[i++] = Double.parseDouble(values[1]);	//skip values[0] which is empty
			}
			days++;
		}
		reader.close();
		/* print out Iowa precipitation water data
		System.out.println(IowaPWs.length);
		for (double d:IowaPWs){
			System.out.println(String.format("%.4f", d));
		}
		*/
		// calculate the thresholds from Iowa's precipitation water data 
		double low = StdStats.percentile(IowaPWs,lowPercentile);
		double PCThreshold = StdStats.percentile(IowaPWs,PCPercentile);
		double EPCThreshold = StdStats.percentile(IowaPWs,EPCPercentile);
		double PCUPBOUND = StdStats.percentile(IowaPWs,PCUpBound);
		double PCLOWBOUND = StdStats.percentile(IowaPWs,PCLowBound);
		System.out.println("low:"+low);
		System.out.println("PCThreshold:"+PCThreshold);
		System.out.println("EPCThreshold:"+EPCThreshold);
		ArrayList<PWC> pwclist = PWC.FindPWCs(Start_Date,IowaPWs,low,PCThreshold,maxNonePCDays,minPCDays);
		
		System.out.println("pwclist.size:"+pwclist.size());
		//PWC.SortPWCbyLength(pwclist);
		//print out all the PCs
		/*
		for (PWC p:pwclist){
			if (p.average>=PCThreshold) {
				if (p.average>=EPCThreshold) {
					System.out.println(p+"=EPC");
				} else {
					if (p.average<=PCUPBOUND) {
						System.out.println(p+"=PC");
					}
				}
			}
		}
		*/
		// looking for the EPCs of Iowa
		ArrayList<PWC> AllEPCs = PWC.PWCRangeByAverage(pwclist, EPCThreshold,Double.MAX_VALUE,"EPC");
		ArrayList<PWC> testEPCs = PWC.PWCRangeByYear(AllEPCs, testData_start_year, testData_end_year);
		testEPCs = PWC.PWCRangeByMonth(testEPCs, start_month, end_month);
		ArrayList<PWC> trainEPCs = PWC.PWCRangeByYear(AllEPCs, trainData_start_year, trainData_end_year);
		trainEPCs = PWC.PWCRangeByMonth(trainEPCs, start_month, end_month);
		PWC.StorePCData(trainEPCs,IowaEPCFile);

		
		ArrayList<PWC> AllPCs = null;
		// looking for the PCs of Iowa
		if (RandomselectPC) {
			AllPCs = PWC.PWCRangeByAverage(pwclist, PCThreshold,EPCThreshold,"PC");
			AllPCs = PWC.RandomSelection(AllPCs, trainEPCs.size(), 1);
		}
		else {
			AllPCs = PWC.PWCRangeByAverage(pwclist, PCLOWBOUND,PCUPBOUND,"PC");
		}
		ArrayList<PWC> TestPCs = PWC.PWCRangeByYear(AllPCs, testData_start_year, testData_end_year);
		TestPCs = PWC.PWCRangeByMonth(TestPCs, start_month, end_month);
		ArrayList<PWC> TrainPCs = PWC.PWCRangeByYear(AllPCs, trainData_start_year, trainData_end_year);
		TrainPCs = PWC.PWCRangeByMonth(TrainPCs, start_month, end_month);
		PWC.StorePCData(TrainPCs,IowaPCFile);
		System.out.println("# of train PC:"+TrainPCs.size()+"   # of test PC:"+TestPCs.size());
		System.out.println("# of train EPC:"+trainEPCs.size()+"   # of test EPC:"+testEPCs.size());
		// combine PC and EPC into one list
		for (PWC epc:trainEPCs) {
			TrainPCs.add(epc);
		}
		for (PWC epc:testEPCs) {
			TestPCs.add(epc);
		}
		
		
		String features[] = DataLoader.features;
    	String featureFiles[] = DataLoader.featureFiles;
		String delimit2 = "\\s+";
		String trainFile = "./EPC_arff/train"+trainData_start_year+"_"+trainData_end_year+".arff";
		String testFile = "./EPC_arff/test"+testData_start_year+"_"+testData_end_year+".arff";
		String idUsed2 = "locUsed2_"+support_start+".txt";
		
		int back =5, backdays=6;
		double PW20p = 4.85, PW60p=19.87, PW90p=43.27; // from all locations 1980~2010
		double PW20pIA = 7.53, PW60pIA=18.63, PW90pIA=34.08; // from 2 locations of Iowa (3941, 3978) 1980~2010

		PWC.SortPWCbyStartDate(TrainPCs);
		PWC.SortPWCbyStartDate(TestPCs);
		PWC.SortPWCbyStartDate(AllEPCs);
		
		// remove the PCs' which fall out of the range
		while (TrainPCs.get(0).start_date <=(backdays+back)) {
			TrainPCs.remove(0);
		}
		while (AllEPCs.get(0).start_date <=(backdays+back)) {
			AllEPCs.remove(0);
		}

		// find location support and confidence based on the selection of PercentileUsed and then save to a file
		if (PercentileUsed==0) { //0: Individuals  1: Iowa  2: All locations
			PWLocation.CreateLocFile(AllEPCs, featureFiles[2], delimit2, idFile[PercentileUsed], maxNonePCDays,minPCDays,back,backdays);
		} else {
			// Calculate the percentile values
			double[][] PWdata = DataLoader.loadingData(featureFiles[2], delimit2, 
					flooding_prediction.totalSampleLocations,
					(int)flooding_prediction.Data_start_day,
					(int)flooding_prediction.Data_end_day);
			if (PercentileUsed==1) {
				double PWdataIA[][] = new double[][]{PWdata[3941],PWdata[3978]};
				// from Iowa's 2 locations
				PW20pIA = StdStats.percentile(PWdataIA,flooding_prediction.lowPercentile); 
				PW60pIA = StdStats.percentile(PWdataIA,flooding_prediction.PCPercentile);
				PW90pIA = StdStats.percentile(PWdataIA,flooding_prediction.EPCPercentile);
				PWLocation.CreateLocFile(AllEPCs, featureFiles[2], delimit2, idFile[PercentileUsed], maxNonePCDays,minPCDays,back,backdays,PW20pIA,PW60pIA,PW90pIA);
			} else { //(PercentileUsed==2)
				// from all locations
				PW20p = StdStats.percentile(PWdata,flooding_prediction.lowPercentile); 
				PW60p = StdStats.percentile(PWdata,flooding_prediction.PCPercentile);
				PW90p = StdStats.percentile(PWdata,flooding_prediction.EPCPercentile);
				PWLocation.CreateLocFile(AllEPCs, featureFiles[2], delimit2, idFile[PercentileUsed], maxNonePCDays,minPCDays,back,backdays,PW20p,PW60p,PW90p);
			}
		}
		
		
		// load the location support and confidence data from selected file
		ArrayList<PWLocation> loclist = PWLocation.LoadLocData(idFile[PercentileUsed], delimit2);
		// filter out the locations by support range
		ArrayList<PWLocation> locs= PWLocation.LOCRangeBySupport(loclist, support_start, support_end );
		// filter out the locations by confidence range
		locs=PWLocation.LOCRangeByConfidence(locs,confidence_start,confidence_end);
		// store the filtered location result into file 
		PWLocation.StoreLocData(locs, idUsed2);
		
		/* creat weka arff file using location file 
		PWC.createWekaFile(features, featureFiles, delimit2, TrainPCs, backdays, back, idFile[PercentileUsed], delimit2,trainFile);
		PWC.createWekaFile(features, featureFiles, delimit2, TestPCs, backdays, back, idFile[PercentileUsed],delimit2, testFile);
		*/
		
		
		// creat weka file using loc ArrayList
		System.out.println("Creating training set~");
		PWC.createWekaFile(features, featureFiles, delimit2, TrainPCs, backdays, back, locs,trainFile);
		System.out.println("Creating test set~");
		PWC.createWekaFile(features, featureFiles, delimit2, TestPCs, backdays, back, locs, testFile);
		
		if (crossValFolds >1) {
			return RunWeka.runFolds(RunWeka.getBaseClassifier(baseclassifier),null,crossValFolds,trainFile);
		} else {
			return RunWeka.run(RunWeka.getBaseClassifier(baseclassifier),null,trainFile, testFile);
		}
	}
	
	public static void main(String[] args) throws Exception {
		
		run();
		/*
		Run_maxNonePCDays(1, 2, true,new Callable<Void>() {
			   public Void call() throws Exception {
			       return Run_minPCDays(5, 9, true,null); }});
		*/
		
		// Generating the one-day feature data sets
		/*
		for (int x=5; x<=10;x++){			
			PWC.createWekaFile(features, featureFiles, delimit2, TrainPCs, 1, x, locs, "./EPC_arff/Single/Train_"+x+".arff");
			PWC.createWekaFile(features, featureFiles, delimit2, TestPCs, 1, x, locs, "./EPC_arff/Single/Test_"+x+".arff");
		}
		*/		
		/*
		System.out.println("=========5~7");
		RunWeka.runSingleDay(5, 7, "./EPC_arff/Single/Train_","./EPC_arff/Single/Test_");
		System.out.println("=========6~8");
		RunWeka.runSingleDay(6, 8, "./EPC_arff/Single/Train_","./EPC_arff/Single/Test_");
		System.out.println("=========7~9");
		RunWeka.runSingleDay(7, 9, "./EPC_arff/Single/Train_","./EPC_arff/Single/Test_");
		System.out.println("=========8~10");
		RunWeka.runSingleDay(8, 10, "./EPC_arff/Single/Train_","./EPC_arff/Single/Test_");
		*/
		/*
		
		for (i=0;i<supportranges.length-1;i++) {
			locs=PWLocation.LOCRangeBySupport(loclist, supportranges[i], supportranges[i+1]);
			//locs=PWLocation.LOCRangeByConfidence(locs,confThreshold,Double.POSITIVE_INFINITY);
			System.out.println( supportranges[i]+"~"+supportranges[i+1]+":"+locs.size());
			locs=PWLocation.RandomSelection(locs, 780, 1);
			PWLocation.StoreLocData(locs, "loc2range_"+supportranges[i]+"_"+supportranges[i+1]+".txt");
			
			System.out.println("Creating training set~"+(i+1));
			trainFile = "./EPC_arff/train2_"+supportranges[i]+"_"+supportranges[i+1]+".arff";
			//PWC.createWekaFile(features, featureFiles, delimit2, TrainPCs, backdays, back, locs,trainFile);
			System.out.println("Creating test set~"+(i+1));
			testFile = "./EPC_arff/test2_"+supportranges[i]+"_"+supportranges[i+1]+".arff";
			//PWC.createWekaFile(features, featureFiles, delimit2, TestPCs, backdays, back, locs,testFile);
			RunWeka.run(RunWeka.getBaseClassifier(baseclassifier),null,trainFile, testFile);
		}
		*/		
	}	
}
