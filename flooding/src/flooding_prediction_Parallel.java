import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Random;
import java.util.concurrent.Callable;

import weka.classifiers.Classifier;
import weka.classifiers.trees.J48;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

public class flooding_prediction_Parallel {
	static public Calendar Base_Date = new GregorianCalendar(2001, 0, 1); // first date's ID starts with 0
	static public String IowaPrecipFile = "./data/text/PRECIP2_2001-2010.txt";
	static public String IowaPCFile = "IowaPCs.txt";
	static public String IowaEPCFile = "IowaEPCs.txt";
	static public Calendar Start_Date = new GregorianCalendar(2001, 0, 1);
	static public Calendar End_Date = new GregorianCalendar(2010, 11, 31);
	static public long Data_start_day =(Start_Date.getTimeInMillis()-Base_Date.getTimeInMillis())/(1000 * 60 * 60 * 24);
	static public long Data_end_day = (End_Date.getTimeInMillis()-Base_Date.getTimeInMillis())/(1000 * 60 * 60 * 24);
	static public int trainData_start_year = 2001, trainData_end_year = 2010;
	static public int testData_start_year = 2001, testData_end_year = 2010;
	static public int totalDays = (int)(Data_end_day-Data_start_day+1) , totalSampleLocations = 5328;
	public int maxNonePCDays = 2, minPCDays = 7;
	public double lowPercentile = 0.2, PCPercentile=0.6, EPCPercentile=0.90;
	public double PCLowBound = 0.6, PCUpBound = 0.9;
	static public boolean RandomselectPC = true;
	static public String idFile[] = {"loc_Individuals.txt","loc_Iowa.txt","loc_All.txt"};
	public int PercentileUsed = 0; //0: Individuals  1: Iowa  2: All locations
	static public int start_month = 5, end_month = 9; // summer time = May to September
	public double support_percentile_start = 0.6, support_percentile_end = 1;
	public int support_start = 0, support_end = Integer.MAX_VALUE;
	static double[] supportranges = new double[]{0,0.2,0.4,0.6,0.8,1};
	static double[] supportthresholds = new double[]{0,0.2,0.4,0.6,0.8};
	public double confidence_start = 0.0, confidence_end = 1;
	static double[] confidencethresholds = new double[]{0,0.15,0.2,0.25,0.3};
	static public int baseclassifier =2; //0:SVM 1:LADTree 2:J48 3:NaiveBayes
	static public int crossValFolds =5;
	
	static public boolean loadlocfromfile = true; // Tell run function to load location file (By Jacky 2014-1-31)
	static public boolean RandomselectLoc = false; // Tell run function to select locations no in the range for comparison (By Jacky 2014-1-31)
	
	static public  String[] resultfiles = new String[]{"./results/Individuals.csv","./results/Iowa.csv","./results/All.csv"};
	
	static private Object inLock = new Object(), outLock = new Object(), fileLock = new Object();
	
	public <T> Void Run_maxNonePCDays(int start, int end, boolean append, Callable<T> func) throws Exception {
		BufferedWriter outresult = new BufferedWriter(new FileWriter(resultfiles[PercentileUsed],append));
		if (! append){
			ClassificationResults.writetitle(outresult);
		}
		for (int days=start;days <= end;days++) {
			maxNonePCDays = days;//
			callfunc(outresult, func);
		}
		outresult.close();
		
		return null;
	}
	
	public <T> Void Run_supportRange(int start, int end, boolean append, Callable<T> func) throws Exception {
		BufferedWriter outresult = new BufferedWriter(new FileWriter(resultfiles[PercentileUsed],append));
		if (! append){
			ClassificationResults.writetitle(outresult);
		}
		for (int s=start;s <= end;s++) {
			support_percentile_start = supportranges[s];
			support_percentile_end = supportranges[s+1];
			callfunc(outresult, func);
		}
		outresult.close();
		
		return null;
	}
	
	public <T> Void Run_supportThreshold(int start, int end, boolean append, Callable<T> func) throws Exception {
		BufferedWriter outresult = new BufferedWriter(new FileWriter(resultfiles[PercentileUsed],append));
		if (! append){
			ClassificationResults.writetitle(outresult);
		}
		for (int s=start;s <= end;s++) {
			support_percentile_start = supportthresholds[s];
			support_end = Integer.MAX_VALUE;
			callfunc(outresult, func);
		}
		outresult.close();
		
		return null;
	}
	
	public <T> Void Run_confThreshold(int start, int end, boolean append, Callable<T> func) throws Exception {
		BufferedWriter outresult = new BufferedWriter(new FileWriter(resultfiles[PercentileUsed],append));
		if (! append){
			ClassificationResults.writetitle(outresult);
		}
		for (int s=start;s <= end;s++) {
			confidence_start = confidencethresholds[s];
			confidence_end = Double.POSITIVE_INFINITY;
			callfunc(outresult, func);
		}
		outresult.close();
		
		return null;
	}
	
	public <T> Void Run_minPCDays(int start, int end, boolean append, Callable<T> func) throws Exception {
		BufferedWriter outresult = new BufferedWriter(new FileWriter(resultfiles[PercentileUsed],append));
		if (! append){
			synchronized (fileLock) {
				ClassificationResults.writetitle(outresult);
			}
		}
		for (int days=start;days <= end;days++) {
			minPCDays = days;//
			callfunc(outresult, func);
		}
		outresult.close();
		
		return null;
	}

	public <T> Void Run_EPCPercentile(boolean append, Callable<T> func) throws Exception {
		BufferedWriter outresult = new BufferedWriter(new FileWriter(resultfiles[PercentileUsed],append));
		if (! append){
			ClassificationResults.writetitle(outresult);
		}
		for (EPCPercentile=0.85;EPCPercentile < 1;EPCPercentile += 0.05) {
			PCUpBound = EPCPercentile;
			callfunc(outresult, func);
		}
		outresult.close();
		
		return null;
	}

	@SuppressWarnings("rawtypes")
	public <T> void callfunc(BufferedWriter outresult, Callable<T> func) throws Exception {
		ArrayList<ClassificationResults> result = new ArrayList<ClassificationResults>();
		if (func == null) {
			 result.add(run());
		} else {
			Object ret = func.call();
			if (ret instanceof ClassificationResults)
				result.add((ClassificationResults) ret);
			else if (ret instanceof ArrayList) {
				for (Object r:(ArrayList)ret) {
					if (r instanceof ClassificationResults)
						result.add((ClassificationResults) r);
				}
			}
		}
		for (ClassificationResults r:result) {
			synchronized (outLock) {
				r.printout();
			}
			synchronized (fileLock) {
				outresult.write(r.one_record(this));
				outresult.flush();
			}
		}
	}
	
	public ClassificationResults run() throws Exception {
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
		System.out.println("PCUpBound:"+PCUPBOUND);
		System.out.println("PCLowBound:"+PCLOWBOUND);
		
		System.out.println("Finding PWCs .... ");
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
		System.out.println("looking for the EPCs of Iowa .... ");
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
		}
		else {
			AllPCs = PWC.PWCRangeByAverage(pwclist, PCLOWBOUND,PCUPBOUND,"PC");
		}
		ArrayList<PWC> TestPCs = PWC.PWCRangeByYear(AllPCs, testData_start_year, testData_end_year);
		TestPCs = PWC.PWCRangeByMonth(TestPCs, start_month, end_month);
		ArrayList<PWC> TrainPCs = PWC.PWCRangeByYear(AllPCs, trainData_start_year, trainData_end_year);
		TrainPCs = PWC.PWCRangeByMonth(TrainPCs, start_month, end_month);
		if (RandomselectPC) {
			TrainPCs = PWC.RandomSelection(TrainPCs, trainEPCs.size(), 1);
			TestPCs = PWC.RandomSelection(TestPCs, trainEPCs.size(), 1);
		}
		System.out.println("Save TrainPCs data to "+ IowaPCFile +" ...");
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
		
		int back =5, backdays=6;

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
		// load the location support and confidence data from selected file

		/* ====== Marked by jacky 02-01-2014: this process is replace by calling function getLoclist() ======
		// find location support and confidence based on the selection of PercentileUsed and then save to a file
		double PW20p = 4.85, PW60p=19.87, PW90p=43.27; // from all locations 1980~2010
		double PW20pIA = 7.53, PW60pIA=18.63, PW90pIA=34.08; // from 2 locations of Iowa (3941, 3978) 1980~2010

		System.out.println("Finding location support and confidence and save to "+ idFile[PercentileUsed] +" ...");
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
		*/
		ArrayList<PWLocation> loclist;
		// get the location support and confidence data
		if (loadlocfromfile) {
			loclist = PWLocation.LoadLocData(idFile[PercentileUsed], "\\s+"); 
			}
		else {
			loclist = getLoclist(AllEPCs, back, backdays);
			PWLocation.StoreLocData(loclist, idFile[PercentileUsed]);
		}
		
		PWLocation.SortLocbySupport(loclist);//Support high to low
		// filter out the locations by support range
		int index =(int) ( (double)(loclist.size()-1) * (1-support_percentile_start));
		support_start =loclist.get(index).support;
		index =(int) ( (double)(loclist.size()-1) * (1-support_percentile_end));
		if (index<=0){ // use threshold
			support_end = 9999999;
		} else { // use range
			support_end =loclist.get(index).support;
		}
		ArrayList<PWLocation> locs= PWLocation.LOCRangeBySupport(loclist, support_start, support_end );
		// filter out the locations by confidence range
		locs=PWLocation.LOCRangeByConfidence(locs,confidence_start,confidence_end);
		// store the filtered location result into file 
		String idUsed2 = "locUsed2_"+support_start+".txt";
		PWLocation.StoreLocData(locs, idUsed2);
		
		/* creat weka arff file using location file 
		PWC.createWekaFile(features, featureFiles, delimit2, TrainPCs, backdays, back, idFile[PercentileUsed], delimit2,trainFile);
		PWC.createWekaFile(features, featureFiles, delimit2, TestPCs, backdays, back, idFile[PercentileUsed],delimit2, testFile);
		*/
	
		// creat weka file using loc ArrayList
		String features[] = DataLoader.features;
    	String featureFiles[] = DataLoader.featureFiles;
		String delimit2 = "\\s+";
		String trainFile = "./EPC_arff/train"+trainData_start_year+"_"+trainData_end_year+".arff";
		String testFile = "./EPC_arff/test"+testData_start_year+"_"+testData_end_year+".arff";
		if (RandomselectLoc) {
			String trainRFile = "./EPC_arff/trainR"+trainData_start_year+"_"+trainData_end_year+".arff";
			String testRFile = "./EPC_arff/testR"+testData_start_year+"_"+testData_end_year+".arff";
			ArrayList<PWLocation> locsR = new ArrayList<PWLocation>(loclist);
			for (PWLocation loc: locs){
				locsR.remove(loc);
			}
			System.out.println("Not in range locations:"+locsR.size());
			locsR = PWLocation.RandomSelection(locsR, locs.size(), 1);
			System.out.println("Creating trainingR set~");
			PWC.createWekaFile(features, featureFiles, delimit2, TrainPCs, backdays, back, locsR,trainRFile);
			System.out.println("Creating testR set~");
			PWC.createWekaFile(features, featureFiles, delimit2, TestPCs, backdays, back, locsR, testRFile);
			if (crossValFolds >1) {
				RunWeka.runFolds(RunWeka.getBaseClassifier(baseclassifier),null,crossValFolds,trainRFile);
			} else {
				RunWeka.run(RunWeka.getBaseClassifier(baseclassifier),null,trainRFile, testRFile);
			}
		}
		System.out.println("In range locations:"+locs.size());
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
	
	/**
	 * Store all raw data in memory to avoid disk I/O during experimenting
	 * The memory consumption is ~2gb
	 * run it in 64 bit mode and increase JVM heap size to satisfy it
	 * works for me with Java 64 bit in win7 64 bit
	 */
	public static double[] IowaPWs = null;	// Precipitated Water
	public static double[][][] data = null;	// all data
	
	public static double percentile_step = 0.05;	// an value in (0, 1]
	public static double[] percentilesIowa = null;	// Iowa precipitated water percentiles
	public static double[][] percentiles0 = null;	// individual
	public static double[] percentiles1 = null;	// Iowa 2 locations
	public static double[] percentiles2 = null;	// all locations
	
	static private void readData() throws NumberFormatException, IOException {
		String delimit = "\\s+";
		
		// load Iowa Precipitated water data
		if (IowaPWs == null) IowaPWs = new double[totalDays];
    	BufferedReader reader = new BufferedReader(new FileReader(IowaPrecipFile));
		String line="";
		int day=0, i=0;
		while ((line = reader.readLine()) != null) {
			if ( (day >= Data_start_day) && (day <= Data_end_day)) {
				String[] values = line.split(delimit);
				IowaPWs[i++] = Double.parseDouble(values[1]);	//skip values[0] which is empty
			}
			day++;
		}
		reader.close();

		/** load percentiles data for Iowa precipitated water, Iowa precipitable water (2 locations), all precipitable water
		 *  when can not read from file, calculate it from raw PW data and create the file
		 */
		readPercentiles();
		// load percentiles data for precipitable water per location
		readIndividualPercentiles();
		
		// load raw data
		String featureFiles[] = DataLoader.featureFiles;
		if (data == null) data = new double[featureFiles.length][][];
		for (int f=0;f<featureFiles.length;f++) {
			data[f] = DataLoader.loadingData(featureFiles[f], delimit, totalSampleLocations, (int)Data_start_day, (int)Data_end_day);
		}

	}
	
	static private void readIndividualPercentiles() throws NumberFormatException, IOException {
		if (percentiles0 != null) return;
		String delimit = "\\s+";
		String featureFiles[] = DataLoader.featureFiles;
		double[][] pwData = DataLoader.loadingData(featureFiles[DataLoader.PW], delimit, totalSampleLocations, (int)Data_start_day, (int)Data_end_day);
		if (percentile_step > 1.0) percentile_step = 1.0;
		if (percentile_step <= 0.0) percentile_step = 0.01;
		int pNo = (int)(1.0/percentile_step)+1;
		String percentileFile0 = "./data/text/percentile0_"+String.format("%.2f", percentile_step)+".txt";
		percentiles0 = DataLoader.readIndividualPercentiles(percentileFile0, totalSampleLocations, pNo);
		if (percentiles0 == null) {
			percentiles0 = new double[totalSampleLocations][];
			for (int i=0;i<totalSampleLocations;i++)
				percentiles0[i] = StdStats.percentilesInLine(pwData[i], percentile_step);
			DataLoader.writeIndividualPercentiles(percentileFile0, percentiles0);
		}
	}
	
	static private void readPercentiles() throws NumberFormatException, IOException {
		if (percentiles1 != null && percentiles2 != null)
			return;
		if (percentile_step > 1.0) percentile_step = 1.0;
		if (percentile_step <= 0.0) percentile_step = 0.01;
		int pNo = (int)(1.0/percentile_step)+1;
		String percentileFileIowa = "./data/text/percentileIowa_"+String.format("%.2f", percentile_step)+".txt";
		percentilesIowa = DataLoader.readPercentiles(percentileFileIowa, pNo);
		if (percentilesIowa == null) {
			percentilesIowa = StdStats.percentiles(IowaPWs, percentile_step);
			DataLoader.writePercentiles(percentileFileIowa, percentilesIowa);
		}
		
		String percentileFile1 = "./data/text/percentile1_"+String.format("%.2f", percentile_step)+".txt";
		String percentileFile2 = "./data/text/percentile2_"+String.format("%.2f", percentile_step)+".txt";

		percentiles1 = DataLoader.readPercentiles(percentileFile1, pNo);
		percentiles2 = DataLoader.readPercentiles(percentileFile2, pNo);
		if (percentiles1 != null && percentiles2 != null)
			return;
		
		String delimit = "\\s+";
		String featureFiles[] = DataLoader.featureFiles;
		double[] pwData = new double[totalSampleLocations*totalDays];
		BufferedReader reader = new BufferedReader(new FileReader(featureFiles[DataLoader.PW]));
		String line="";
		int day=0, i=0;
		while ((line = reader.readLine()) != null) {
			if ( (day >= Data_start_day) && (day <= Data_end_day)) {
				String[] values = line.split(delimit);
				for (int j=0;j<totalSampleLocations;j++)
					pwData[i++] = Double.parseDouble(values[j+1]);	//skip values[0] which is empty
			}
			day++;
		}
		reader.close();
		
		if (percentiles1 == null) {
			double[] pwIowa = new double[2*totalDays];
			int[] IowaIds = {3941, 3978};
			for (i=0;i<totalDays;i++)
				for (int j=0;j<2;j++)
					pwIowa[j+i*2] = pwData[i*totalSampleLocations+(IowaIds[j])];

			percentiles1 = StdStats.percentilesInLine(pwIowa, percentile_step);
			DataLoader.writePercentiles(percentileFile1, percentiles1);
		}
		if (percentiles2 == null) {
			percentiles2 = StdStats.percentilesInLine(pwData, percentile_step);
			DataLoader.writePercentiles(percentileFile2, percentiles2);
		}
	}
	
	public ArrayList<PWLocation> getLoclist(ArrayList<PWC> AllEPCs, int back,int backdays) throws IOException {
		ArrayList<PWLocation> loclist = new ArrayList<PWLocation>();
		int lowpIndex = (int)(lowPercentile/percentile_step);
		int pcpIndex = (int)(PCPercentile/percentile_step);
		int epcpIndex = (int)(EPCPercentile/percentile_step);
		double lowp=4.85, pcp=19.87, epcp=43.27;	// default value is overwritten later
		if (PercentileUsed == 1) {
			lowp = percentiles1[lowpIndex];
			pcp = percentiles1[pcpIndex];
			epcp = percentiles1[epcpIndex];
		} else if (PercentileUsed == 2) {
			lowp = percentiles2[lowpIndex];
			pcp = percentiles2[pcpIndex];
			epcp = percentiles2[epcpIndex];
		}
		for (int loc=0;loc<flooding_prediction_Parallel.totalSampleLocations;loc++) {
			if (PercentileUsed == 0) {
				lowp = percentiles0[loc][lowpIndex];
				pcp = percentiles0[loc][pcpIndex];
				epcp = percentiles0[loc][epcpIndex];
			}
			ArrayList<PWC> PWEPClist = PWC.FindPWCs(flooding_prediction_Parallel.Start_Date,data[DataLoader.PW][loc]
					,lowp,pcp,maxNonePCDays,minPCDays); 
			PWEPClist = PWC.PWCRangeByAverage(PWEPClist, epcp, Double.MAX_VALUE,"EPC");
			PWC.SortPWCbyStartDate(PWEPClist);
			//System.out.println(loc+":="+PWEPClist.size());
			PWLocation plc = PWLocation.findLocSupport(loc,flooding_prediction_Parallel.totalDays, backdays, back,AllEPCs,PWEPClist);
			loclist.add(plc);
		}
		PWLocation.SortLocbySupport(loclist);
		return loclist;
	}
	
	public ArrayList<ClassificationResults> runInMemory() {
		//if (IowaPWs == null || data == null)
			//readData();
		StringBuffer log = new StringBuffer();
		double low = percentilesIowa[(int)(lowPercentile/percentile_step)];
		double PCThreshold = percentilesIowa[(int)(PCPercentile/percentile_step)];
		double EPCThreshold = percentilesIowa[(int)(EPCPercentile/percentile_step)];
		double PCUPBOUND = percentilesIowa[(int)(PCUpBound/percentile_step)];
		double PCLOWBOUND = percentilesIowa[(int)(PCLowBound/percentile_step)];
		log.append("\nlow:"+low+"\n");
		log.append("PCThreshold:"+PCThreshold+"\n");
		log.append("EPCThreshold:"+EPCThreshold+"\n");
		ArrayList<PWC> pwclist = PWC.FindPWCs(Start_Date,IowaPWs,low,PCThreshold,maxNonePCDays,minPCDays);
		log.append("PC found:"+pwclist.size()+"\n");

		// looking for the EPCs of Iowa
		ArrayList<PWC> AllEPCs = PWC.PWCRangeByAverage(pwclist, EPCThreshold,Double.MAX_VALUE,"EPC");
		//if (!validEPCs(AllEPCs, Start_Date, End_Date)) return null;
		ArrayList<PWC> testEPCs = null;
		if (crossValFolds <= 1) {
			testEPCs = PWC.PWCRangeByYear(AllEPCs, testData_start_year, testData_end_year);
			testEPCs = PWC.PWCRangeByMonth(testEPCs, start_month, end_month);
			//testEPCs = PWC.removeEmbedded(testEPCs);
		}
		ArrayList<PWC> trainEPCs = PWC.PWCRangeByYear(AllEPCs, trainData_start_year, trainData_end_year);
		trainEPCs = PWC.PWCRangeByMonth(trainEPCs, start_month, end_month);
		//trainEPCs = PWC.removeEmbedded(trainEPCs);
		log.append("Train EPC: "+trainEPCs.size());
		//AllEPCs = PWC.removeEmbedded(AllEPCs);
		log.append("\tTotal EPC: "+AllEPCs.size()+"\n");
		//PWC.StorePCData(trainEPCs,IowaEPCFile);
		
		ArrayList<PWC> AllPCs = null;
		// looking for the PCs of Iowa
		if (RandomselectPC)
			AllPCs = PWC.PWCRangeByAverage(pwclist, PCThreshold,EPCThreshold,"PC");
		else
			AllPCs = PWC.PWCRangeByAverage(pwclist, PCLOWBOUND,PCUPBOUND,"PC");
		ArrayList<PWC> TestPCs = null;
		if (crossValFolds <= 1) {
			TestPCs = PWC.PWCRangeByYear(AllPCs, testData_start_year, testData_end_year);
			TestPCs = PWC.PWCRangeByMonth(TestPCs, start_month, end_month);
		}
		ArrayList<PWC> TrainPCs = PWC.PWCRangeByYear(AllPCs, trainData_start_year, trainData_end_year);
		TrainPCs = PWC.PWCRangeByMonth(TrainPCs, start_month, end_month);
		TrainPCs = PWC.RandomSelection(TrainPCs, trainEPCs.size(), 1);
		//TrainPCs = PWC.removeEmbedded(TrainPCs);
		if (crossValFolds <= 1)
			TestPCs = PWC.RandomSelection(TestPCs, testEPCs.size(), 1);
		//PWC.StorePCData(TrainPCs,IowaPCFile);
		log.append("# of train EPC:"+trainEPCs.size()/*+"   # of test EPC:"+testEPCs.size()*/+"\n");
		log.append("# of train PC:"+TrainPCs.size()/*+"   # of test PC:"+TestPCs.size()*/+"\n");

		// combine PC and EPC into one list
		for (PWC epc:trainEPCs)
			TrainPCs.add(epc);
		if (crossValFolds <= 1)
			for (PWC epc:testEPCs)
				TestPCs.add(epc);

		int back =5, backdays=6;
		// remove the PCs' which fall out of the range
		while (TrainPCs.get(0).start_date <=(backdays+back)) {
			TrainPCs.remove(0);
		}
		while (AllEPCs.get(0).start_date <=(backdays+back)) {
			AllEPCs.remove(0);
		}
		log.append("Getting the location support and confidence data..."+"\n");

		System.out.println("Search for locations");
		ArrayList<PWLocation> loclist = null;
		String locfile = idFile[PercentileUsed]
				+String.format("-%.2f-%.2f", lowPercentile, EPCPercentile);
		try {
			synchronized (fileLock) {
				loclist = PWLocation.LoadLocData(locfile, "\\s+");
			}
		} catch (IOException e) {
			loclist = null;
			e.printStackTrace();
		}
		if (loclist == null) {
			try {
				loclist = getLoclist(AllEPCs, back, backdays);
			} catch (IOException e) {
				System.out.println("Call getLoclist failed with IOException.");
				e.printStackTrace();
				return null;
			}
			try {
				synchronized (fileLock) {
					PWLocation.StoreLocData(loclist, locfile);
				}
			} catch (IOException e) {
				System.out.println("Save loclist to file failed");
				e.printStackTrace();
			}
		}
		log.append("total number of locations:"+loclist.size()+"\n");
	
		// skip the one with too few instances that weka can't do cross-validation
		if (TrainPCs.size()<crossValFolds)
			return null;
		
		synchronized (outLock) {
			System.out.print(log);
		}
		/** 
		 * for any set of parameters, always test with all combinations of support and confidence, 
		 * those are valid if the number of resulting locs filtered falls in [1,500] 
		 */
		ArrayList<ClassificationResults> result = new ArrayList<ClassificationResults>();
		// filter out the locations by support range
		double support_percentile_step = 0.05;
		int prev_support_start = -1;
		for (support_percentile_start=0;support_percentile_start<1;support_percentile_start+=support_percentile_step) {
			int index = (int) ( (double)(loclist.size()-1) * (1-support_percentile_start));
			support_start =loclist.get(index).support;
			// skip already tested support_start
			if (prev_support_start==support_start)
				continue;
			else
				prev_support_start=support_start;
			index =(int) ( (double)(loclist.size()-1) * (1-support_percentile_end));
			if (index<=0){ // use threshold
				support_end = 9999999;
			} else { // use range
				support_end =loclist.get(index).support;
			}
			ArrayList<PWLocation> locsbysupport= PWLocation.LOCRangeBySupport(loclist, support_start, support_end );
			if (support_end == 9999999)
				support_end = loclist.get(0).support;	// output max_support instead of 9999999
			// filter out the locations by confidence range
			double confidence_step = 0.05;
			int prev_locs_number = -1;
			for (confidence_start=0;confidence_start<1;confidence_start+=confidence_step) {
				ArrayList<PWLocation> locs=PWLocation.LOCRangeByConfidence(locsbysupport,confidence_start,confidence_end);
				int locs_number = locs.size();
				// skip already tested locs_number
				if (prev_locs_number == locs_number)
					continue;
				else
					prev_locs_number = locs_number;
				// filter out invalid locs number, too many will cause out of memory error later
				int min_locs_number = 1, max_instance_x_Locs_number = 500000;
				if (locs_number<min_locs_number || TrainPCs.size()*locs_number>max_instance_x_Locs_number)
					continue;
				System.out.println("Locations used: "+locs_number);
				// store the filtered location result into file 
				//PWLocation.StoreLocData(locs, idUsed2);

				int[] ids = new int[locs_number];
				for (int i=0;i<locs_number;i++) ids[i] = locs.get(i).ID;

				try {
					if (crossValFolds >1) {
						result.add(RunWeka.runFoldsInMemory(RunWeka.getBaseClassifier(baseclassifier),null,crossValFolds,data,TrainPCs,ids,backdays,back));
					} else {
						result.add(RunWeka.runInMemory(RunWeka.getBaseClassifier(baseclassifier),null,data,TrainPCs,TestPCs,ids,backdays,back));
					}
				} catch (Exception e) {
					System.out.println("RunWeka failed with Exception.");
					e.printStackTrace();
				}
			}
		}
		return result;
	}

	private static int[][] floodingStartDates = {
		{1984, 6, 7}, /*{1987, 5, 26}, {1988, 7, 15},*/ {1990, 5, 18}, {1990, 7, 25}, 
		{1991, 6, 1}, {1992, 9, 14}, /*{1993, 3, 26}, {1993, 4, 13},*/ {1996, 5, 8},
		{1996, 6, 15}, {1998, 6, 13}, {1999, 5, 16}, /*{1999, 7, 2}, {2001, 4, 8}, {2002, 6, 3},*/ 
		{2004, 5, 19}, {2007, 5, 5}, {2007, 8, 17}, {2008, 5, 25}, {2010, 5, 11}, {2010, 6, 1}
	};
	/*
	private static int[][] floodingEndDates = {
		{1984, 6, 8}, {1987, 5, 26}, {1988, 7, 15}, {1990, 5, 18}, {1990, 7, 25}, 
		{1991, 6, 1}, {1992, 9, 14}, {1993, 3, 26}, {1993, 4, 13}, {1996, 5, 8},
		{1996, 6, 15}, {1998, 6, 13}, {1999, 5, 16}, {1999, 7, 2}, {2001, 4, 8}, {2002, 6, 3}, 
		{2004, 5, 19}, {2007, 5, 5}, {2007, 8, 17}, {2008, 5, 25}, {2010, 5, 11}, {2010, 6, 1}
	};*/
	private static Calendar floodingBaseDate = null;
	private static int[] floodingStartDays = null;
	
	/*
	 * validate EPC found by known flooding event at Iowa based on 
	 * http://homelandsecurity.iowa.gov/disasters/iowa_disaster_history.html
	 */
	public static boolean validEPCs(ArrayList<PWC> AllEPCs, Calendar startDate, Calendar endDate) {
		if (AllEPCs.isEmpty()) {
			System.out.println("validEPCs: no EPC found");
			return false;
		}
		if (floodingStartDays == null) {
			floodingStartDays = new int[floodingStartDates.length];
			floodingBaseDate = AllEPCs.get(0).BASE_DATE;
			for (int i=0;i<floodingStartDates.length;i++) {
				Calendar date = new GregorianCalendar(floodingStartDates[i][0], floodingStartDates[i][1]-1, floodingStartDates[i][2]);
				floodingStartDays[i] = (int) ((date.getTimeInMillis()-floodingBaseDate.getTimeInMillis())/(1000 * 60 * 60 * 24));
			}
		} else if (!floodingBaseDate.equals(AllEPCs.get(0).BASE_DATE)) {
			int shift = (int) ((AllEPCs.get(0).BASE_DATE.getTimeInMillis()-floodingBaseDate.getTimeInMillis())/(1000 * 60 * 60 * 24));
			floodingBaseDate = AllEPCs.get(0).BASE_DATE;
			for (int i=0;i<floodingStartDays.length;i++)
				floodingStartDays[i] -= shift;
		}
		int ValidAroundRange = 5;
		int startDay = (int) ((startDate.getTimeInMillis()-floodingBaseDate.getTimeInMillis())/(1000 * 60 * 60 * 24));
		int endDay = (int) ((endDate.getTimeInMillis()-floodingBaseDate.getTimeInMillis())/(1000 * 60 * 60 * 24));
		int fsdi = 0;
		while (floodingStartDays[fsdi] < startDay)
			fsdi++;
		int index = 0;
		for (int i=fsdi;i<floodingStartDays.length;i++) {
			int start = floodingStartDays[i];
			if (start > endDay)
				break;
			boolean covered = true;
			do {
				PWC epc = AllEPCs.get(index);
				if (epc.start_date <= start && epc.end_date >= start-ValidAroundRange)
					break;
				if (epc.start_date > start) {
					covered = false;
					break;
				}
				index++;
			} while (index < AllEPCs.size());
			if (index == AllEPCs.size())
				covered = false;
			if (!covered) {
				System.out.println("No EPC covers flooding event started on "
						+floodingStartDates[i][0]+"-"+floodingStartDates[i][1]+"-"+floodingStartDates[i][2]);
				return false;
			}
		}
		return true;
	}
	
	private void getLocation(int locNo) throws Exception {
		if (IowaPWs == null || data == null)
			readData();

		double low = percentilesIowa[(int)(lowPercentile/percentile_step)];
		double PCThreshold = percentilesIowa[(int)(PCPercentile/percentile_step)];
		double EPCThreshold = percentilesIowa[(int)(EPCPercentile/percentile_step)];
		double PCUPBOUND = percentilesIowa[(int)(PCUpBound/percentile_step)];
		double PCLOWBOUND = percentilesIowa[(int)(PCLowBound/percentile_step)];
		System.out.println("low:"+low);
		System.out.println("PCThreshold:"+PCThreshold);
		System.out.println("EPCThreshold:"+EPCThreshold);
		ArrayList<PWC> pwclist = PWC.FindPWCs(Start_Date,IowaPWs,low,PCThreshold,maxNonePCDays,minPCDays);
		System.out.println("pwclist.size:"+pwclist.size());

		// looking for the EPCs of Iowa
		ArrayList<PWC> AllEPCs = PWC.PWCRangeByAverage(pwclist, EPCThreshold,Double.MAX_VALUE,"EPC");
		ArrayList<PWC> testEPCs = PWC.PWCRangeByYear(AllEPCs, testData_start_year, testData_end_year);
		testEPCs = PWC.PWCRangeByMonth(testEPCs, start_month, end_month);
		ArrayList<PWC> trainEPCs = PWC.PWCRangeByYear(AllEPCs, trainData_start_year, trainData_end_year);
		trainEPCs = PWC.PWCRangeByMonth(trainEPCs, start_month, end_month);
		//PWC.StorePCData(trainEPCs,IowaEPCFile);

		
		ArrayList<PWC> AllPCs = null;
		// looking for the PCs of Iowa
		if (RandomselectPC) {
			AllPCs = PWC.PWCRangeByAverage(pwclist, PCThreshold,EPCThreshold,"PC");
		}
		else {
			AllPCs = PWC.PWCRangeByAverage(pwclist, PCLOWBOUND,PCUPBOUND,"PC");
		}
		ArrayList<PWC> TestPCs = PWC.PWCRangeByYear(AllPCs, testData_start_year, testData_end_year);
		TestPCs = PWC.PWCRangeByMonth(TestPCs, start_month, end_month);
		ArrayList<PWC> TrainPCs = PWC.PWCRangeByYear(AllPCs, trainData_start_year, trainData_end_year);
		TrainPCs = PWC.PWCRangeByMonth(TrainPCs, start_month, end_month);
		if (RandomselectPC) {
			TrainPCs = PWC.RandomSelection(TrainPCs, trainEPCs.size(), 1);
			TestPCs = PWC.RandomSelection(TestPCs, trainEPCs.size(), 1);
		}
		//PWC.StorePCData(TrainPCs,IowaPCFile);
		System.out.println("# of train PC:"+TrainPCs.size()+"   # of test PC:"+TestPCs.size());
		System.out.println("# of train EPC:"+trainEPCs.size()+"   # of test EPC:"+testEPCs.size());
		// combine PC and EPC into one list
		for (PWC epc:trainEPCs) {
			TrainPCs.add(epc);
		}
		for (PWC epc:testEPCs) {
			TestPCs.add(epc);
		}

		PWC.SortPWCbyStartDate(TrainPCs);
		PWC.SortPWCbyStartDate(TestPCs);
		PWC.SortPWCbyStartDate(AllEPCs);

		int back =5, backdays=6;
		// remove the PCs' which fall out of the range
		while (TrainPCs.get(0).start_date <=(backdays+back)) {
			TrainPCs.remove(0);
		}
		while (AllEPCs.get(0).start_date <=(backdays+back)) {
			AllEPCs.remove(0);
		}

		// skip the one with too few instances that weka can't do cross-validation
		if (TrainPCs.size()<crossValFolds) return;
		
		// get the location support and confidence data
		ArrayList<PWLocation> loclist = getLoclist(AllEPCs, back, backdays);
		// filter out the locations by support range
		double support_percentile_step = 0.05;
		int prev_support_start = -1;
		for (support_percentile_start=0;support_percentile_start<1;support_percentile_start+=support_percentile_step) {
			int index = (int) ( (double)(loclist.size()-1) * (1-support_percentile_start));
			support_start =loclist.get(index).support;
			// skip already tested support_start
			if (prev_support_start==support_start)
				continue;
			else
				prev_support_start=support_start;
			index =(int) ( (double)(loclist.size()-1) * (1-support_percentile_end));
			if (index<=0){ // use threshold
				support_end = 9999999;
			} else { // use range
				support_end =loclist.get(index).support;
			}
			ArrayList<PWLocation> locsbysupport= PWLocation.LOCRangeBySupport(loclist, support_start, support_end );
			ArrayList<PWLocation> locs=PWLocation.LOCRangeByConfidence(locsbysupport,confidence_start,confidence_end);
			if (locNo == locs.size()) {
				PWLocation.StoreLocData(locs, "./loc_Iowa-"+maxNonePCDays+"-"+minPCDays+".txt");
				int[] ids = new int[locNo];
				for (int i=0;i<locNo;i++) ids[i] = locs.get(i).ID;
				int numofattr = data.length*locNo*backdays+1;
				FastVector attributes = new FastVector(numofattr);
				int attrIndex = 0;
				for (int f=0;f<data.length;f++)
					for (int i=0;i<locNo;i++) 
						for (int di=0;di<backdays;di++) {
							attributes.addElement(new Attribute(DataLoader.features[f]+"_"+ids[i]+"_"+(back+backdays-1-di), attrIndex++));
						}
				FastVector classValues = new FastVector(2);
				classValues.addElement("EPC");
				classValues.addElement("PC");
				Attribute classAttr = new Attribute("class", classValues, attrIndex);
				attributes.addElement(classAttr);
				
				int numoftrain = TrainPCs.size();
				Instances trainset = new Instances("train_set", attributes, numoftrain);
				for (int e=0;e<numoftrain;e++) {
					double[] instValues = new double[numofattr];
					int attrID = 0;
					PWC pc = TrainPCs.get(e);
					for (int f=0;f<data.length;f++)
						for (int i=0;i<locNo;i++)
							for (int di=0;di<backdays;di++) {
								instValues[attrID++] = data[f][ids[i]][pc.start_date-back-backdays+1+di];
							}
					instValues[attrID] = classAttr.indexOfValue(pc.classlabel);
					double weight = 1.0;
					Instance inst = new Instance(weight, instValues);
					inst.setDataset(trainset);
					trainset.add(inst);
				}
				
				trainset.setClassIndex(trainset.attribute("class").index());
				double[] truth = new double[numoftrain];
				for (int i=0;i<numoftrain;i++) {
					truth[i]=trainset.instance(i).classValue();
				}
				Classifier classifier = RunWeka.getBaseClassifier(baseclassifier);
				classifier.buildClassifier(trainset);
				if (classifier instanceof J48) {
					String model = ((J48)classifier).prefix();
					//System.out.println(model);
					System.out.println("J48 selected locations: ");
					String[] values = model.split("_");
					int[] locIds = new int[values.length/2];
					for (int i=0;i<locIds.length;i++)
						locIds[i] = Integer.parseInt(values[1+i*2]);
					Arrays.sort(locIds);
					int previous = -1;
					for (int e:locIds) {
						if (e==previous) continue;
						System.out.println(e);
						previous = e;
					}
				} else
					System.out.println(classifier);
				break;
			}
		}
	}

	private static int[] getRandomLocations(int[] parentLocs, int number) {
		if (parentLocs == null) return null;
		int size = parentLocs.length;
		if (number<=0 || number>size) return null;
		int[] copyLocs = new int[size];
		for (int i=0;i<size;i++) copyLocs[i] = parentLocs[i];
    	Random rn = new Random();
		int[] ret = new int[number];
		for (int i=0;i<number;i++) {
			int index = rn.nextInt(size-i);
			ret[i] = copyLocs[index];
			copyLocs[index] = copyLocs[size-i-1];
		}
		Arrays.sort(ret);
		return ret;
	}
	
	public void runRandomLocations(int supportStart) throws Exception {
		if (IowaPWs == null || data == null)
			readData();

		double low = percentilesIowa[(int)(lowPercentile/percentile_step)];
		double PCThreshold = percentilesIowa[(int)(PCPercentile/percentile_step)];
		double EPCThreshold = percentilesIowa[(int)(EPCPercentile/percentile_step)];
		double PCUPBOUND = percentilesIowa[(int)(PCUpBound/percentile_step)];
		double PCLOWBOUND = percentilesIowa[(int)(PCLowBound/percentile_step)];
		System.out.println("low:"+low);
		System.out.println("PCThreshold:"+PCThreshold);
		System.out.println("EPCThreshold:"+EPCThreshold);
		ArrayList<PWC> pwclist = PWC.FindPWCs(Start_Date,IowaPWs,low,PCThreshold,maxNonePCDays,minPCDays);
		System.out.println("pwclist.size:"+pwclist.size());

		// looking for the EPCs of Iowa
		ArrayList<PWC> AllEPCs = PWC.PWCRangeByAverage(pwclist, EPCThreshold,Double.MAX_VALUE,"EPC");
		ArrayList<PWC> testEPCs = PWC.PWCRangeByYear(AllEPCs, testData_start_year, testData_end_year);
		testEPCs = PWC.PWCRangeByMonth(testEPCs, start_month, end_month);
		ArrayList<PWC> trainEPCs = PWC.PWCRangeByYear(AllEPCs, trainData_start_year, trainData_end_year);
		trainEPCs = PWC.PWCRangeByMonth(trainEPCs, start_month, end_month);
		//PWC.StorePCData(trainEPCs,IowaEPCFile);

		
		ArrayList<PWC> AllPCs = null;
		// looking for the PCs of Iowa
		if (RandomselectPC) {
			AllPCs = PWC.PWCRangeByAverage(pwclist, PCThreshold,EPCThreshold,"PC");
		}
		else {
			AllPCs = PWC.PWCRangeByAverage(pwclist, PCLOWBOUND,PCUPBOUND,"PC");
		}
		ArrayList<PWC> TestPCs = PWC.PWCRangeByYear(AllPCs, testData_start_year, testData_end_year);
		TestPCs = PWC.PWCRangeByMonth(TestPCs, start_month, end_month);
		ArrayList<PWC> TrainPCs = PWC.PWCRangeByYear(AllPCs, trainData_start_year, trainData_end_year);
		TrainPCs = PWC.PWCRangeByMonth(TrainPCs, start_month, end_month);
		if (RandomselectPC) {
			TrainPCs = PWC.RandomSelection(TrainPCs, trainEPCs.size(), 1);
			TestPCs = PWC.RandomSelection(TestPCs, trainEPCs.size(), 1);
		}
		//PWC.StorePCData(TrainPCs,IowaPCFile);
		System.out.println("# of train PC:"+TrainPCs.size()+"   # of test PC:"+TestPCs.size());
		System.out.println("# of train EPC:"+trainEPCs.size()+"   # of test EPC:"+testEPCs.size());
		// combine PC and EPC into one list
		for (PWC epc:trainEPCs) {
			TrainPCs.add(epc);
		}
		for (PWC epc:testEPCs) {
			TestPCs.add(epc);
		}

		PWC.SortPWCbyStartDate(TrainPCs);
		PWC.SortPWCbyStartDate(TestPCs);
		PWC.SortPWCbyStartDate(AllEPCs);

		int back =5, backdays=6;
		// remove the PCs' which fall out of the range
		while (TrainPCs.get(0).start_date <=(backdays+back)) {
			TrainPCs.remove(0);
		}
		while (AllEPCs.get(0).start_date <=(backdays+back)) {
			AllEPCs.remove(0);
		}

		// skip the one with too few instances that weka can't do cross-validation
		if (TrainPCs.size()<crossValFolds) return;
		
		// get the location support and confidence data
		ArrayList<PWLocation> loclist;
		// get the location support and confidence data
		if (loadlocfromfile) {
			loclist = PWLocation.LoadLocData(idFile[PercentileUsed], "\\s+"); 
			}
		else {
			loclist = getLoclist(AllEPCs, back, backdays);
			PWLocation.StoreLocData(loclist, idFile[PercentileUsed]);
		}
		// filter out the locations by support range
		double support_percentile_step = 0.05;
		int prev_support_start = -1;
		for (support_percentile_start=0;support_percentile_start<1;support_percentile_start+=support_percentile_step) {
			int index = (int) ( (double)(loclist.size()-1) * (1-support_percentile_start));
			support_start =loclist.get(index).support;
			if (support_start != supportStart) continue;
			// skip already tested support_start
			if (prev_support_start==support_start)
				continue;
			else
				prev_support_start=support_start;
			index =(int) ( (double)(loclist.size()-1) * (1-support_percentile_end));
			if (index<=0){ // use threshold
				support_end = 9999999;
			} else { // use range
				support_end =loclist.get(index).support;
			}
			ArrayList<PWLocation> locsbysupport= PWLocation.LOCRangeBySupport(loclist, support_start, support_end );
			if (support_end == 9999999)
				support_end = loclist.get(0).support;	// output max_support instead of 9999999
			ArrayList<PWLocation> locs=PWLocation.LOCRangeByConfidence(locsbysupport,confidence_start,confidence_end);
			int[] exclusive_ids = PWLocation.getIds(locs); Arrays.sort(exclusive_ids);
			int locNo = exclusive_ids.length;
			int[] inclusive_ids = new int[totalSampleLocations - locNo];
			int id_index = 0, id_value = 0;
			for (int eid:exclusive_ids) {
				for (int i=id_value;i<eid;i++) {
					inclusive_ids[id_index] = i;
					id_index++;
				}
				id_value = eid+1;
			}
			for (int i=id_value;i<totalSampleLocations;i++) {
				inclusive_ids[id_index] = i;
				id_index++;
			}
			ArrayList<ClassificationResults> result = new ArrayList<ClassificationResults>();
			for (int iter=0;iter<10;iter++) {
				int[] ids = getRandomLocations(inclusive_ids, locNo);
				if (crossValFolds >1) {
					result.add(RunWeka.runFoldsInMemory(RunWeka.getBaseClassifier(baseclassifier),null,crossValFolds,data,TrainPCs,ids,backdays,back));
				} else {
					result.add(RunWeka.runInMemory(RunWeka.getBaseClassifier(baseclassifier),null,data,TrainPCs,TestPCs,ids,backdays,back));
				}
			}
			BufferedWriter outresult = new BufferedWriter(new FileWriter("./random_"+maxNonePCDays+"-"+minPCDays+".csv",true));
			for (ClassificationResults r:result) {
				r.printout();
				outresult.write(r.one_record());
				outresult.flush();
			}
			outresult.close();
			break;
		}
	}
	
	private static void initFor(int year) {
		String featureFiles[] = new String[] {
			"./data/text/"+year+"-2010_Z1000.txt",
			"./data/text/"+year+"-2010_T850.txt",
			"./data/text/"+year+"-2010_PW.txt",
			"./data/text/"+year+"-2010_U300.txt",
			"./data/text/"+year+"-2010_U850.txt",
			"./data/text/"+year+"-2010_V300.txt",
			"./data/text/"+year+"-2010_V850.txt",
			"./data/text/"+year+"-2010_Z300.txt",
			"./data/text/"+year+"-2010_Z500.txt"
		};
		
		for (int i=0;i<featureFiles.length;i++)
			DataLoader.featureFiles[i] = featureFiles[i];
		
		Base_Date = new GregorianCalendar(year, 0, 1); // first date's ID starts with 0
		IowaPrecipFile = "./data/text/PRECIP2_"+year+"-2010.txt";
		Start_Date = new GregorianCalendar(year, 0, 1);
		//End_Date = new GregorianCalendar(2010, 11, 31);
		Data_start_day =(Start_Date.getTimeInMillis()-Base_Date.getTimeInMillis())/(1000 * 60 * 60 * 24);
		Data_end_day = (End_Date.getTimeInMillis()-Base_Date.getTimeInMillis())/(1000 * 60 * 60 * 24);
		trainData_start_year = year; //trainData_end_year = 2010;
		testData_start_year = year; //testData_end_year = 2010;
		totalDays = (int)(Data_end_day-Data_start_day+1);
		start_month = 3; end_month = 10;
	}
	
	/*
	 * for each percentile type, all combinations of 
	 * PC/EPC thresholds, maxNonePCDays, minPCDays as well as locations 
	 * selected by all combinations of support, confidence (in runInMemory)
	 * are tested.
	 */
	public static void testPercentileUsed(int percentileToUse) throws Exception {
		if (percentileToUse < 0 || percentileToUse > 2) {
			System.out.println("Set PercentileUsed to "+percentileToUse+" is not allowd.");
			return;
		}
		
		long startTime, stopTime;
		startTime = System.currentTimeMillis();
		initFor(1980);
		readData();
		stopTime = System.currentTimeMillis();
		System.out.println("Read data cost "+((stopTime-startTime)/1000)+"s\n");
		
		final double[] lowPercentiles = {0.35, 0.4};
		final double[] PCPercentiles = {0.5, 0.6};
		final int[] minPCDaysRange = {5, 10};
		
		int PercentileUsed = percentileToUse;
		int maxNonePCDays = 2; double EPCPercentile = 0.8, PCUpBound = EPCPercentile;
		for (int i=0;i<lowPercentiles.length;i++) {
			double lowPercentile = lowPercentiles[i];
			for (int j=0;j<PCPercentiles.length;j++) {
				double PCPercentile = PCPercentiles[j];
				double PCLowBound = PCPercentile;
				for (int PCDays=minPCDaysRange[0];PCDays<=minPCDaysRange[1];PCDays++) {
					flooding_prediction_Parallel fpp = new flooding_prediction_Parallel();
					fpp.PercentileUsed = PercentileUsed;
					fpp.maxNonePCDays = maxNonePCDays; fpp.minPCDays = PCDays;
					fpp.EPCPercentile = EPCPercentile; fpp.PCPercentile = PCPercentile; fpp.lowPercentile = lowPercentile;
					fpp.PCUpBound = PCUpBound; fpp.PCLowBound = PCLowBound;
					if ((i+j) == 0 && PCDays==minPCDaysRange[0])
						new Thread(fpp.new runMinPCDay(false)).start();
					else
						new Thread(fpp.new runMinPCDay(true)).start();
				}
			}
		}
	}
	
	public static void main(String[] args) throws Exception {
		int percentileToUse = 0;
		if (args.length > 0)
			percentileToUse = Integer.parseInt(args[0]);
		testPercentileUsed(percentileToUse);
	}
	
	private class runMinPCDay implements Runnable {
		boolean append;
		public runMinPCDay(boolean val) {
			append = val;
		}

		@Override
		public void run() {
			long startTime, stopTime;
			startTime = System.currentTimeMillis();
			try {
				Run_minPCDays(minPCDays, minPCDays, append, new Callable<ArrayList<ClassificationResults>>() {
					public ArrayList<ClassificationResults> call() throws Exception {
						return runInMemory(); }});
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			stopTime = System.currentTimeMillis();
			System.out.println(minPCDays+", "+lowPercentile+", "+PCPercentile+", "+EPCPercentile+": "+(stopTime-startTime)+"ms\n");
		}
		
	};
}
