import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
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

public class flooding_prediction {
	static public int Start_Year = 1980, End_Year = 2010;
	static public int Start_Month = 3, End_Month = 10; // the months flooding happened
	static public Calendar Base_Date = new GregorianCalendar(Start_Year, 0, 1); // first date's ID starts with 0
	static public Calendar Start_Date = new GregorianCalendar(Start_Year, 0, 1);
	static public Calendar End_Date = new GregorianCalendar(End_Year, 11, 31);
	static public long Data_start_day =(Start_Date.getTimeInMillis()-Base_Date.getTimeInMillis())/(1000 * 60 * 60 * 24);
	static public long Data_end_day = (End_Date.getTimeInMillis()-Base_Date.getTimeInMillis())/(1000 * 60 * 60 * 24);
	static public int trainData_start_year = Start_Year, trainData_end_year = End_Year;
	static public int testData_start_year = Start_Year, testData_end_year = End_Year;
	static public String IowaPrecipFileAll = "./data/text/PRECIP2_1980-2010.txt";
	static public String IowaPrecipFileNoWinter = "./data/text/PRECIP2_1980-2010_3-10.txt";
	static public String PWFileNoWinter = "./data/text/1980-2010_PW_3-10.txt";
	static public String IowaPCFile = "IowaPCs.txt";
	static public String IowaEPCFile = "IowaEPCs.txt";
	//static public long Data_end_day = 23010-11688; //1980-1-1 to 2010-12-31
	//static int UsingData_start_day = 18993, UsingData_end_day = 23010; //2000-1-1 to 2010-12-31
	//static int UsingData_start_day = 0, UsingData_end_day = 23010;
	static public int totalDays = (int)(Data_end_day-Data_start_day+1) , totalSampleLocations = 5328;
	static public int maxNonePCDays = 2, minPCDays = 7, maxPCDays = 21;
	static public double lowPercentile = 0.4, PCPercentile=0.6, EPCPercentile=0.80;
	static public double PCLowBound = 0.6, PCUpBound = 0.9;
	static public boolean RandomselectPC = true;
	static public String idFile[] = {"loc_Individuals.txt","loc_Iowa.txt","loc_All.txt"};
	static public int PercentileUsed = 0; //0: Individuals  1: Iowa  2: All locations
	static public double support_percentile_start = 0.6, support_percentile_end = 1;
	static public int support_start = 0, support_end = Integer.MAX_VALUE;
	static double[] supportranges = new double[]{0,0.2,0.4,0.6,0.8,1};
	static double[] supportthresholds = new double[]{0,0.2,0.4,0.6,0.8};
	static public double confidence_start = 0.0, confidence_end = 1;
	static double[] confidencethresholds = new double[]{0,0.15,0.2,0.25,0.3};
	static public int baseclassifier =2; //0:SVM 1:LADTree 2:J48 3:NaiveBayes
	static public int crossValFolds =5;
	
	static public boolean loadlocfromfile = true; // Tell run function to load location file (By Jacky 2014-1-31)
	static public boolean RandomselectLoc = false; // Tell run function to select locations no in the range for comparison (By Jacky 2014-1-31)
	
	static public  String[] resultfiles = new String[]{"./results/Individuals.csv","./results/Iowa.csv","./results/All.csv"};
	
	/**
	 * Store all raw data in memory to avoid disk I/O during experimenting
	 * The memory consumption is ~2gb
	 * run it in 64 bit mode and increase JVM heap size to satisfy it
	 * works for me with Java 64 bit in win7 64 bit
	 */
	public static double[] IowaPWs = null;			// Precipitated Water
	public static double[][][] data = null;				// all data
	public static double[][] PWDataNoWinter = null;		// Precipitable Water for non winter days
	// percentile values are based on non winter days
	public static double percentile_step = 0.01;		// a value in (0, 1]
	public static double[][] percentilesIowa = null;	// Iowa precipitated water percentiles
	public static double[][][] percentiles0 = null;		// individual
	public static double[][] percentiles1 = null;		// Iowa 2 locations
	public static double[][] percentiles2 = null;		// all locations
	static String delimit = "\\s+";

	static private void loadData() throws NumberFormatException, IOException {
		if (IowaPWs == null) loadIowaPWs();	// Iowa precipitated water
		if (data == null) loadRawData();	// raw data of all 9 features for all locations
	}
	
	static private void loadIowaPWs() throws NumberFormatException, IOException  {
		IowaPWs = new double[totalDays];
		BufferedReader reader = new BufferedReader(new FileReader(IowaPrecipFileAll));
		String line="";
		int day=0, i=0;
		while ((line = reader.readLine()) != null) {
			if ( (day >= Data_start_day) && (day <= Data_end_day)) {
				String[] values = line.split(delimit);
				IowaPWs[i++] = Double.parseDouble(values[1]);	//skip empty values[0] 
			}
			day++;
		}
		reader.close();
	}
	
	static private void loadRawData() throws IOException {
		String featureFiles[] = DataLoader.featureFiles;
		if (data == null || data.length != featureFiles.length)
			data = new double[featureFiles.length][][];
		for (int f=0;f<featureFiles.length;f++) {
			if (data[f] == null)
				data[f] = DataLoader.loadingData(featureFiles[f], delimit, totalSampleLocations, (int)Data_start_day, (int)Data_end_day);
		}
	}
	
	static private void getPercentiles() throws NumberFormatException, IOException {
		if (percentilesIowa == null)
			getIowaPWPercentiles();	//percentiles data for Iowa precipitated water
		if (PercentileUsed != 0 && (percentiles1 == null || percentiles2 == null))
			getGlobalPercentiles();	//include the one from Iowa precipitable water (2 locations)
		if (PercentileUsed == 0 && percentiles0 == null)
			getLocalPercentiles();	//each location has its own percentile
	}
	
	static private void getIowaPWPercentiles() throws NumberFormatException, IOException {
		if (percentile_step > 1.0) percentile_step = 1.0;
		if (percentile_step <= 0.0) percentile_step = 0.01;
		int pNo = (int)(1.0/percentile_step)+1;
		if (percentilesIowa == null || percentilesIowa.length < maxPCDays)
			percentilesIowa = new double[maxPCDays][];
		double[] IowaPWsNoWinter = null;
		for (int pcday=0;pcday<maxPCDays;pcday++) {
			String percentileFileIowa = "./percentileIowa_"+String.format("%d-%d_%d_%.2f", trainData_start_year, trainData_end_year, pcday+1, percentile_step)+".txt";
			percentilesIowa[pcday] = DataLoader.readPercentiles(percentileFileIowa, pNo);
			if (percentilesIowa[pcday] != null) continue;
			if (IowaPWsNoWinter == null) {
				IowaPWsNoWinter = new double[DataLoader.getLineNumber(IowaPrecipFileNoWinter)];
				BufferedReader reader = new BufferedReader(new FileReader(IowaPrecipFileNoWinter));
				String line="";
				int i=0;
				while ((line = reader.readLine()) != null) {
					String[] values = line.split(delimit);
					IowaPWsNoWinter[i++] = Double.parseDouble(values[1]);	//skip values[0] which is empty
				}
				reader.close();
			}
			percentilesIowa[pcday] = StdStats.percentiles(IowaPWsNoWinter, percentile_step, pcday+1);
			DataLoader.writePercentiles(percentileFileIowa, percentilesIowa[pcday]);
		}
	}
	
	static private void loadPWDataNoWinter() throws NumberFormatException, IOException {
		int days = DataLoader.getLineNumber(PWFileNoWinter);
		PWDataNoWinter = new double[totalSampleLocations][days];
		BufferedReader reader = new BufferedReader(new FileReader(PWFileNoWinter));
		String line="";
		int i=0;
		while ((line = reader.readLine()) != null) {
			String[] values = line.split(delimit);
			for (int j=0;j<totalSampleLocations;j++)
				PWDataNoWinter[j][i] = Double.parseDouble(values[j+1]);	//skip values[0] which is empty
			i++;
		}
		reader.close();
	}
	
	static private void getGlobalPercentiles() throws NumberFormatException, IOException {
		if (percentiles1 == null || percentiles1.length < maxPCDays)
			percentiles1 = new double[maxPCDays][];
		if (percentiles2 == null || percentiles2.length < maxPCDays)
			percentiles2 = new double[maxPCDays][];
		if (percentile_step > 1.0) percentile_step = 1.0;
		if (percentile_step <= 0.0) percentile_step = 0.01;
		int pNo = (int)(1.0/percentile_step)+1;
		double[][] pwIowa = null;
		for (int pcday=0;pcday<maxPCDays;pcday++) {
			String percentileFile1 = "./percentile1_"+String.format("%d-%d_%d_%.2f", trainData_start_year, trainData_end_year, pcday+1, percentile_step)+".txt";
			String percentileFile2 = "./percentile2_"+String.format("%d-%d_%d_%.2f", trainData_start_year, trainData_end_year, pcday+1, percentile_step)+".txt";

			percentiles1[pcday] = DataLoader.readPercentiles(percentileFile1, pNo);
			percentiles2[pcday] = DataLoader.readPercentiles(percentileFile2, pNo);
			if (percentiles1[pcday] != null && percentiles2[pcday] != null)
				continue;

			if (PWDataNoWinter == null)
				loadPWDataNoWinter();
			if (percentiles1[pcday] == null) {
				if (pwIowa == null) {
					int[] IowaIds = {3941, 3978};
					int days = PWDataNoWinter[0].length;
					pwIowa = new double[IowaIds.length][days];
					for (int j=0;j<IowaIds.length;j++)
						for (int i=0;i<days;i++)
							pwIowa[j][i] = PWDataNoWinter[IowaIds[j]][i];
				}
				percentiles1[pcday] = StdStats.percentiles(pwIowa, percentile_step, pcday+1);
				DataLoader.writePercentiles(percentileFile1, percentiles1[pcday]);
			}
			if (percentiles2[pcday] == null) {
				percentiles2[pcday] = StdStats.percentiles(PWDataNoWinter, percentile_step, pcday+1);
				DataLoader.writePercentiles(percentileFile2, percentiles2[pcday]);
			}
		}
	}

	static private void getLocalPercentiles() throws NumberFormatException, IOException {
		if (percentiles0 == null || percentiles0.length < maxPCDays)
			percentiles0 = new double[totalSampleLocations][maxPCDays][];
		if (percentile_step > 1.0) percentile_step = 1.0;
		if (percentile_step <= 0.0) percentile_step = 0.01;
		int pNo = (int)(1.0/percentile_step)+1;
		for (int pcday=0;pcday<maxPCDays;pcday++) {
			String percentileFile0 = "./percentile0_"+String.format("%d-%d-%d_%.2f", trainData_start_year, trainData_end_year, pcday+1, percentile_step)+".txt";
			double[][] percentiles = DataLoader.readIndividualPercentiles(percentileFile0, totalSampleLocations, pNo);
			if (percentiles != null) {
				for (int i=0;i<totalSampleLocations;i++) {
					percentiles0[i][pcday] = new double[pNo];
					for (int j=0;j<pNo;j++)
						percentiles0[i][pcday][j] = percentiles[i][j];
				}
				continue;
			}
			if (PWDataNoWinter == null)
				loadPWDataNoWinter();
			for (int i=0;i<totalSampleLocations;i++)
				percentiles0[i][pcday] = StdStats.percentiles(PWDataNoWinter[i], percentile_step, pcday+1);
			percentiles = new double[totalSampleLocations][pNo];
			for (int i=0;i<totalSampleLocations;i++)
				for (int j=0;j<pNo;j++)
					percentiles[i][j] = percentiles0[i][pcday][j];
			DataLoader.writeIndividualPercentiles(percentileFile0, percentiles);
		}
	}
	
	public static <T> Void Run_maxNonePCDays(int start, int end, boolean append, Callable<T> func) throws Exception {
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
	
	public static <T> Void Run_supportRange(int start, int end, boolean append, Callable<T> func) throws Exception {
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
	
	public static <T> Void Run_supportThreshold(int start, int end, boolean append, Callable<T> func) throws Exception {
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
	
	public static <T> Void Run_confThreshold(int start, int end, boolean append, Callable<T> func) throws Exception {
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
	
	public static <T> Void Run_minPCDays(int start, int end, boolean append, Callable<T> func) throws Exception {
		BufferedWriter outresult = new BufferedWriter(new FileWriter(resultfiles[PercentileUsed],append));
		if (! append){
			ClassificationResults.writetitle(outresult);
		}
		for (int days=start;days <= end;days++) {
			minPCDays = days;//
			callfunc(outresult, func);
		}
		outresult.close();
		
		return null;
	}

	public static <T> Void Run_EPCPercentile(boolean append, Callable<T> func) throws Exception {
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
	public static <T> void callfunc(BufferedWriter outresult, Callable<T> func) throws Exception {
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
			r.printout();
			outresult.write(r.one_record());
			outresult.flush();
		}
	}
	
	public static ClassificationResults run() throws Exception {
		if (IowaPWs == null) loadIowaPWs();
		if (percentilesIowa == null) getIowaPWPercentiles();
		// calculate the thresholds from Iowa's precipitation water data 
		double low = percentilesIowa[0][(int)(lowPercentile/percentile_step)];
		double PCThreshold = percentilesIowa[0][(int)(PCPercentile/percentile_step)];
		double EPCThreshold = percentilesIowa[0][(int)(EPCPercentile/percentile_step)];
		double PCUPBOUND = percentilesIowa[0][(int)(PCUpBound/percentile_step)];
		double PCLOWBOUND = percentilesIowa[0][(int)(PCLowBound/percentile_step)];
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
		testEPCs = PWC.PWCRangeByMonth(testEPCs, Start_Month, End_Month);
		ArrayList<PWC> trainEPCs = PWC.PWCRangeByYear(AllEPCs, trainData_start_year, trainData_end_year);
		trainEPCs = PWC.PWCRangeByMonth(trainEPCs, Start_Month, End_Month);
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
		TestPCs = PWC.PWCRangeByMonth(TestPCs, Start_Month, End_Month);
		ArrayList<PWC> TrainPCs = PWC.PWCRangeByYear(AllPCs, trainData_start_year, trainData_end_year);
		TrainPCs = PWC.PWCRangeByMonth(TrainPCs, Start_Month, End_Month);
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
	
	public static ArrayList<PWLocation> getLoclist(ArrayList<PWC> AllEPCs, int back,int backdays) throws IOException {
		if (PercentileUsed == 0 && percentiles0 == null)
			getLocalPercentiles();
		if (PercentileUsed != 0 && (percentiles1 == null || percentiles2 == null))
			getGlobalPercentiles();
		ArrayList<PWLocation> loclist = new ArrayList<PWLocation>();
		int lowpIndex = (int)(lowPercentile/percentile_step);
		int pcpIndex = (int)(PCPercentile/percentile_step);
		int epcpIndex = (int)(EPCPercentile/percentile_step);
		double lowp=4.85, pcp=19.87, epcp=43.27;	// default value is overwritten later
		int lastpcday = maxPCDays-1;
		if (PercentileUsed == 1) {
			lowp = percentiles1[lastpcday][lowpIndex]/maxPCDays;
			pcp = percentiles1[lastpcday][pcpIndex]/maxPCDays;
			epcp = percentiles1[lastpcday][epcpIndex]/maxPCDays;
		} else if (PercentileUsed == 2) {
			lowp = percentiles2[lastpcday][lowpIndex]/maxPCDays;
			pcp = percentiles2[lastpcday][pcpIndex]/maxPCDays;
			epcp = percentiles2[lastpcday][epcpIndex]/maxPCDays;
		}
		for (int loc=0;loc<flooding_prediction.totalSampleLocations;loc++) {
			if (PercentileUsed == 0) {
				lowp = percentiles0[lastpcday][loc][lowpIndex]/maxPCDays;
				pcp = percentiles0[lastpcday][loc][pcpIndex]/maxPCDays;
				epcp = percentiles0[lastpcday][loc][epcpIndex]/maxPCDays;
			}
			ArrayList<PWC> PWEPClist = PWC.FindPWCs(flooding_prediction.Start_Date,data[DataLoader.PW][loc]
					,lowp,pcp,maxNonePCDays,minPCDays); 
			PWEPClist = PWC.PWCRangeByAverage(PWEPClist, epcp, Double.MAX_VALUE,"EPC");
			PWC.SortPWCbyStartDate(PWEPClist);
			PWC.removeEmbedded(PWEPClist);
			//System.out.println(loc+":="+PWEPClist.size());
			PWLocation plc = PWLocation.findLocSupport(loc,flooding_prediction.totalDays, backdays, back,AllEPCs,PWEPClist);
			loclist.add(plc);
		}
		PWLocation.SortLocbySupport(loclist);
		return loclist;
	}
	
	private static double lastLowPercentile = -1, lastEPCPercentile = -1;
	public static ArrayList<ClassificationResults> runInMemory() throws Exception {
		loadData();
		getPercentiles();

		int lastpcday = maxPCDays-1;
		double low = percentilesIowa[lastpcday][(int)(lowPercentile/percentile_step)]/maxPCDays;
		double PCThreshold = percentilesIowa[lastpcday][(int)(PCPercentile/percentile_step)]/maxPCDays;
		double EPCThreshold = percentilesIowa[lastpcday][(int)(EPCPercentile/percentile_step)]/maxPCDays;
		double PCUPBOUND = percentilesIowa[lastpcday][(int)(PCUpBound/percentile_step)]/maxPCDays;
		double PCLOWBOUND = percentilesIowa[lastpcday][(int)(PCLowBound/percentile_step)]/maxPCDays;
		System.out.println("\nlow:"+low);
		System.out.println("PCThreshold:"+PCThreshold);
		System.out.println("EPCThreshold:"+EPCThreshold);
		ArrayList<PWC> pwclist = PWC.FindPWCs(Start_Date,IowaPWs,low,PCThreshold,maxNonePCDays,minPCDays);
		System.out.println("PC found:"+pwclist.size());

		// looking for the EPCs of Iowa
		ArrayList<PWC> AllEPCs = PWC.PWCRangeByAverage(pwclist, EPCThreshold,Double.MAX_VALUE,"EPC");
		ArrayList<PWC> testEPCs = PWC.PWCRangeByYear(AllEPCs, testData_start_year, testData_end_year);
		testEPCs = PWC.PWCRangeByMonth(testEPCs, Start_Month, End_Month);
		//testEPCs = PWC.removeEmbedded(testEPCs);
		ArrayList<PWC> trainEPCs = PWC.PWCRangeByYear(AllEPCs, trainData_start_year, trainData_end_year);
		trainEPCs = PWC.PWCRangeByMonth(trainEPCs, Start_Month, End_Month);
		//trainEPCs = PWC.removeEmbedded(trainEPCs);
		System.out.print("train EPC: "+trainEPCs.size());
		//PWC.StorePCData(trainEPCs,IowaEPCFile);
		//AllEPCs = PWC.removeEmbedded(AllEPCs);
		System.out.println("\tTotal EPC: "+AllEPCs.size());
		
		ArrayList<PWC> AllPCs = null;
		// looking for the PCs of Iowa
		if (RandomselectPC) {
			AllPCs = PWC.PWCRangeByAverage(pwclist, PCThreshold,EPCThreshold,"PC");
		}
		else {
			AllPCs = PWC.PWCRangeByAverage(pwclist, PCLOWBOUND,PCUPBOUND,"PC");
		}
		ArrayList<PWC> TestPCs = PWC.PWCRangeByYear(AllPCs, testData_start_year, testData_end_year);
		TestPCs = PWC.PWCRangeByMonth(TestPCs, Start_Month, End_Month);
		ArrayList<PWC> TrainPCs = PWC.PWCRangeByYear(AllPCs, trainData_start_year, trainData_end_year);
		TrainPCs = PWC.PWCRangeByMonth(TrainPCs, Start_Month, End_Month);
		//TrainPCs = PWC.removeEmbedded(TrainPCs);
		//if (RandomselectPC) {
			TrainPCs = PWC.RandomSelection(TrainPCs, trainEPCs.size(), 1);
			TestPCs = PWC.RandomSelection(TestPCs, testEPCs.size(), 1);
		//}
		//PWC.StorePCData(TrainPCs,IowaPCFile);
		System.out.println("# of train EPC:"+trainEPCs.size()+"   # of test EPC:"+testEPCs.size());
		System.out.println("# of train PC:"+TrainPCs.size()+"   # of test PC:"+TestPCs.size());

		// combine PC and EPC into one list
		for (PWC epc:trainEPCs) {
			TrainPCs.add(epc);
		}
		for (PWC epc:testEPCs) {
			TestPCs.add(epc);
		}

		//PWC.SortPWCbyStartDate(TrainPCs);
		//PWC.SortPWCbyStartDate(TestPCs);
		//PWC.SortPWCbyStartDate(AllEPCs);

		int back =5, backdays=6;
		// remove the PCs' which fall out of the range
		while (TrainPCs.get(0).start_date <=(backdays+back)) {
			TrainPCs.remove(0);
		}
		while (AllEPCs.get(0).start_date <=(backdays+back)) {
			AllEPCs.remove(0);
		}
		System.out.println("Getting the location support and confidence data...");

		ArrayList<PWLocation> loclist;
		// get the location support and confidence data
		if (loadlocfromfile && lastLowPercentile == lowPercentile
				&& lastEPCPercentile == EPCPercentile) {
			loclist = PWLocation.LoadLocData(idFile[PercentileUsed], "\\s+"); 
		} else {
			loclist = getLoclist(AllEPCs, back, backdays);
			PWLocation.StoreLocData(loclist, idFile[PercentileUsed]);
			lastLowPercentile = lowPercentile;
			lastEPCPercentile = EPCPercentile;
		}
		System.out.println("total number of locations:"+loclist.size());
	
		// skip the one with too few instances that weka can't do cross-validation
		if (TrainPCs.size()<crossValFolds)
			return null;
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

				// creat weka file using loc ArrayList
				/*
				String features[] = DataLoader.features;
				String featureFiles[] = DataLoader.featureFiles;
				String delimit2 = "\\s+";
				String trainFile = "./EPC_arff/train"+trainData_start_year+"_"+trainData_end_year+".arff";
				String testFile = "./EPC_arff/test"+testData_start_year+"_"+testData_end_year+".arff";

				System.out.println("Creating training set~");
				PWC.createWekaFile(features, featureFiles, delimit2, TrainPCs, backdays, back, locs,trainFile);
				System.out.println("Creating test set~");
				PWC.createWekaFile(features, featureFiles, delimit2, TestPCs, backdays, back, locs, testFile);
				*/ 
				if (crossValFolds >1) {
					result.add(RunWeka.runFoldsInMemory(RunWeka.getBaseClassifier(baseclassifier),null,crossValFolds,data,TrainPCs,ids,backdays,back));
				} else {
					result.add(RunWeka.runInMemory(RunWeka.getBaseClassifier(baseclassifier),null,data,TrainPCs,TestPCs,ids,backdays,back));
				}
			}
		}
		return result;
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

	private static int[] getInclusiveIds(int total, int[] exclusive_ids) {
		Arrays.sort(exclusive_ids);
		int locNo = exclusive_ids.length;
		int[] inclusive_ids = new int[total - locNo];
		int id_index = 0, id_value = 0;
		for (int eid:exclusive_ids) {
			for (int i=id_value;i<eid;i++) {
				inclusive_ids[id_index] = i;
				id_index++;
			}
			id_value = eid+1;
		}
		for (int i=id_value;i<total;i++) {
			inclusive_ids[id_index] = i;
			id_index++;
		}
		return inclusive_ids;
	}
	
	public static void runRandomLocations(int supportStart) throws Exception {
		loadData();
		getPercentiles();

		int lastpcday = maxPCDays-1;
		double low = percentilesIowa[lastpcday][(int)(lowPercentile/percentile_step)]/maxPCDays;
		double PCThreshold = percentilesIowa[lastpcday][(int)(PCPercentile/percentile_step)]/maxPCDays;
		double EPCThreshold = percentilesIowa[lastpcday][(int)(EPCPercentile/percentile_step)]/maxPCDays;
		double PCUPBOUND = percentilesIowa[lastpcday][(int)(PCUpBound/percentile_step)]/maxPCDays;
		double PCLOWBOUND = percentilesIowa[lastpcday][(int)(PCLowBound/percentile_step)]/maxPCDays;
		System.out.println("low:"+low);
		System.out.println("PCThreshold:"+PCThreshold);
		System.out.println("EPCThreshold:"+EPCThreshold);
		ArrayList<PWC> pwclist = PWC.FindPWCs(Start_Date,IowaPWs,low,PCThreshold,maxNonePCDays,minPCDays);
		System.out.println("pwclist.size:"+pwclist.size());

		// looking for the EPCs of Iowa
		ArrayList<PWC> AllEPCs = PWC.PWCRangeByAverage(pwclist, EPCThreshold,Double.MAX_VALUE,"EPC");
		ArrayList<PWC> testEPCs = PWC.PWCRangeByYear(AllEPCs, testData_start_year, testData_end_year);
		testEPCs = PWC.PWCRangeByMonth(testEPCs, Start_Month, End_Month);
		ArrayList<PWC> trainEPCs = PWC.PWCRangeByYear(AllEPCs, trainData_start_year, trainData_end_year);
		trainEPCs = PWC.PWCRangeByMonth(trainEPCs, Start_Month, End_Month);
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
		TestPCs = PWC.PWCRangeByMonth(TestPCs, Start_Month, End_Month);
		ArrayList<PWC> TrainPCs = PWC.PWCRangeByYear(AllPCs, trainData_start_year, trainData_end_year);
		TrainPCs = PWC.PWCRangeByMonth(TrainPCs, Start_Month, End_Month);
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
			int[] exclusive_ids = PWLocation.getIds(locs);
			int[] inclusive_ids = getInclusiveIds(totalSampleLocations, exclusive_ids);
			ArrayList<ClassificationResults> result = new ArrayList<ClassificationResults>();
			for (int iter=0;iter<10;iter++) {
				int[] ids = getRandomLocations(inclusive_ids, exclusive_ids.length);
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
	
	public static void testPerformance() throws Exception {
		PercentileUsed = 1;
		/*
		Run_minPCDays(5, 15, true, new Callable<ArrayList<ClassificationResults>>() {
			   public ArrayList<ClassificationResults> call() throws Exception {
			       return runInMemory(); }});
		*/
		/*
		PCPercentile = 0.5; EPCPercentile = 0.85;
		PCLowBound = PCPercentile; PCUpBound = EPCPercentile;
		Run_minPCDays(10, 12, true, new Callable<Void>() {
			   public Void call() throws Exception {
				   return Run_maxNonePCDays(2, 3, true, new Callable<ArrayList<ClassificationResults>>() {
					   public ArrayList<ClassificationResults> call() throws Exception {
					       return runInMemory(); }}); }});
					       
		PCPercentile = 0.6; EPCPercentile = 0.9;
		PCLowBound = PCPercentile; PCUpBound = EPCPercentile;
		Run_minPCDays(10, 12, true, new Callable<Void>() {
			   public Void call() throws Exception {
				   return Run_maxNonePCDays(2, 3, true, new Callable<ArrayList<ClassificationResults>>() {
					   public ArrayList<ClassificationResults> call() throws Exception {
					       return runInMemory(); }}); }});
					       */
		/*
		Run_EPCPercentile(true, new Callable<ArrayList<ClassificationResults>>() {
			   public ArrayList<ClassificationResults> call() throws Exception {
			       return runInMemory(); }});
			       */
		
		PCPercentile = 0.6; EPCPercentile = 0.9;
		PCLowBound = PCPercentile; PCUpBound = EPCPercentile;
		maxNonePCDays = 2; minPCDays = 13; confidence_start = 0.1; confidence_end = 1;
		runRandomLocations(7);
		maxNonePCDays = 2; minPCDays = 12; confidence_start = 0.05; confidence_end = 1;
		runRandomLocations(75);
		maxNonePCDays = 2; minPCDays = 11; confidence_start = 0; confidence_end = 1;
		runRandomLocations(46);
		
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
		
		PercentileUsed = percentileToUse;
		long startTime, stopTime;
		startTime = System.currentTimeMillis();
		setupTime(1980, 2010, 3, 10);
		stopTime = System.currentTimeMillis();
		System.out.println("Read data cost "+((stopTime-startTime)/1000)+"s\n");
		
		maxNonePCDays = 2; EPCPercentile = 0.8; PCUpBound = EPCPercentile;
		final double[] lowPercentiles = {/*0.35, */0.4};
		final double[] PCPercentiles = {/*0.5, */0.6};
		final int[] minPCDaysRange = {5, 10};
		
		for (int i=0;i<lowPercentiles.length;i++) {
			lowPercentile = lowPercentiles[i];
			for (int j=0;j<PCPercentiles.length;j++) {
				PCPercentile = PCPercentiles[j];
				PCLowBound = PCPercentile;
				System.out.println("Test with lowPercentile("+lowPercentile+"), PCPercentile("+PCPercentile+")");
				startTime = System.currentTimeMillis();
				boolean append = true;
				if ((i+j) == 0)
					append = false;
				Run_minPCDays(minPCDaysRange[0], minPCDaysRange[1], append, new Callable<ArrayList<ClassificationResults>>() {
					public ArrayList<ClassificationResults> call() throws Exception {
						return runInMemory(); }});
				stopTime = System.currentTimeMillis();
				System.out.println("Test with PCPercentile("+PCPercentile+"), EPCPercentile("+EPCPercentile+") costs "+(stopTime-startTime)+"ms\n");
			}
		}
	}

	private static void getLocations(int support, double confidence) throws NumberFormatException, IOException {
		setupTime(1980, 2010, 3, 10);
		loadIowaPWs();
		if (data == null)
			data = new double[DataLoader.featureFiles.length][][];
		if (data[DataLoader.PW] == null)
			data[DataLoader.PW] = DataLoader.loadingData(DataLoader.featureFiles[DataLoader.PW], delimit, totalSampleLocations, (int)Data_start_day, (int)Data_end_day);
		
		getPercentiles();

		double low = percentilesIowa[maxPCDays-1][(int)(lowPercentile/percentile_step)]/maxPCDays;
		double EPCThreshold = percentilesIowa[maxPCDays-1][(int)(EPCPercentile/percentile_step)]/maxPCDays;
		System.out.println("Threshold low bound: "+low+"; EPC bound: "+EPCThreshold);

		ArrayList<PWC> pwclist = PWC.FindPWCs(Start_Date,IowaPWs,low,EPCThreshold,maxNonePCDays,minPCDays);
		ArrayList<PWC> AllEPCs = PWC.PWCRangeByAverage(pwclist, EPCThreshold,Double.MAX_VALUE,"EPC");
		ArrayList<PWC> trainEPCs = PWC.PWCRangeByYear(AllEPCs, trainData_start_year, trainData_end_year);
		trainEPCs = PWC.PWCRangeByMonth(trainEPCs, Start_Month, End_Month);
		trainEPCs = PCSearch.removeEmbedded(trainEPCs);

		int back =5, backdays=6;
		// remove the PCs' which fall out of the range
		while (trainEPCs.get(0).start_date <=(backdays+back)) {
			trainEPCs.remove(0);
		}

		ArrayList<PWLocation> loclist = getLoclist(trainEPCs, back, backdays);
		ArrayList<PWLocation> locsbasic= PWLocation.LOCRangeBySupport(loclist, 1, 9999999 );
		ArrayList<PWLocation> locsbysupport= PWLocation.LOCRangeBySupport(locsbasic, support, 9999999 );
		ArrayList<PWLocation> locs=PWLocation.LOCRangeByConfidence(locsbysupport,confidence,1);
		String[] thresholdfiles = {"local", "Iowa", "global"};
		PWLocation.StoreLocData(locs, String.format("%s_%d_%.2f.txt", thresholdfiles[PercentileUsed], support, confidence));
		System.out.println(String.format("Iowa has %d EPCs when minPCDays=%d, lowPercentile=%.2f, EPCPercentile=%.2f.", trainEPCs.size(), minPCDays, lowPercentile, EPCPercentile));
		System.out.println("There are "+locsbasic.size()+" cooccurence locations when threshold value is taken from "+thresholdfiles[PercentileUsed]+".");
		System.out.println("Among above, "+locsbysupport.size()+" have support value > "+support+".");
		System.out.println("Among above, "+locs.size()+" have confidence value > "+confidence+".");
			
	}

	public static ArrayList<PWC> getIowaPCs(double lowRatio, double PCUpRatio, double PCLowRatio, String label) throws NumberFormatException, IOException {
		if (lowRatio < 0 || lowRatio > (PCLowRatio+0.01) || lowRatio > 1)
			return null;
		if (PCLowRatio > PCUpRatio || PCLowRatio > 1)
			return null;
		if (IowaPWs == null)
			loadIowaPWs();
		double low = percentilesIowa[maxPCDays-1][(int)(lowRatio/percentile_step)]/maxPCDays;
		double PCUPBOUND = Double.MAX_VALUE;
		if (PCUpRatio < 1)
			PCUPBOUND = percentilesIowa[maxPCDays-1][(int)(PCUpRatio/percentile_step)]/maxPCDays;
		double PCLOWBOUND = percentilesIowa[maxPCDays-1][(int)(PCLowRatio/percentile_step)]/maxPCDays;
		System.out.println("Searching "+label+"~");
		System.out.println("low: "+low+"; PCUPBOUND: "+PCUPBOUND+"; PCLOWBOUND: "+PCLOWBOUND);
		ArrayList<PWC> pwclist = PWC.FindPWCs(Start_Date,IowaPWs,low,PCLOWBOUND,maxNonePCDays,minPCDays);
		ArrayList<PWC> AllPCs = PWC.PWCRangeByAverage(pwclist, PCLOWBOUND,PCUPBOUND,label);
		ArrayList<PWC> PCs = PWC.PWCRangeByYear(AllPCs, trainData_start_year, trainData_end_year);
		PCs = PWC.PWCRangeByMonth(PCs, Start_Month, End_Month);
		System.out.println(label+" found: "+PCs.size());
		return PCs;
	}
	
	public static ArrayList<PWLocation> getLocations(ArrayList<PWC> EPCs, int backdays, int back) throws IOException {
		System.out.println("Getting the location support and confidence data...");

		ArrayList<PWLocation> loclist;
		// get the location support and confidence data
		if (loadlocfromfile && lastLowPercentile == lowPercentile
				&& lastEPCPercentile == EPCPercentile) {
			loclist = PWLocation.LoadLocData(idFile[PercentileUsed], "\\s+"); 
		} else {
			loclist = getLoclist(EPCs, back, backdays);
			PWLocation.StoreLocData(loclist, idFile[PercentileUsed]);
			lastLowPercentile = lowPercentile;
			lastEPCPercentile = EPCPercentile;
		}
		System.out.println("total number of locations:"+loclist.size());
		return loclist;
	}
	
	public static int getOverlappingDays(ArrayList<PWC> PCs1, ArrayList<PWC> PCs2) {
		int days = 0;
		int i = 0, j = 0;
		int s1 = PCs1.size(), s2 = PCs2.size();
		int start1 = 0, end1 = 0, start2 = 0, end2 = 0;
		
		boolean go1 = true, go2 = true;
		while (i<s1 && j<s2) {
			if (go1) {
				start1 = PCs1.get(i).start_date;
				end1 = PCs1.get(i).end_date;
			}
			if (go2) {
				start2 = PCs2.get(j).start_date;
				end2 = PCs2.get(j).end_date;
			}
			if (start1<=start2) {
				if (end1>=end2)
					days += end2-start2+1;
				else if (end1>=start2)
					days += end1-start2+1;
			} else {
				if (end1<=end2)
					days += end1-start1+1;
				else if (start1<=end2)
					days += end2-start1+1;
			}
			if (end1<end2) {
				go1 = true; go2 = false;
				i++;
			} else if (end1 == end2) {
				go1 = true; go2 = true;
				i++; j++;
			} else {
				go1 = false; go2 = true;
				j++;
			}
		}
		return days;
	}
	
	public static void createWekaFile(ArrayList<PWC> PCs, int[] ids, int backdays, int back, String[] labels, String filename) throws Exception {
		System.out.println("Remove PCs with few days ahead~");
		while (PCs.get(0).start_date <=(backdays+back)) {
			PCs.remove(0);
		}

		System.out.println("Creating weka file~");
		loadRawData();
		PWC.createWekaFile(DataLoader.features, data, "\\s+", PCs, backdays, back, ids, labels, "./EPC_arff/"+filename);
	}

	public static void createWekaFile(ArrayList<PWC> PCs, ArrayList<PWLocation> locs, int backdays, int back, String[] labels, String filename) throws Exception {
		createWekaFile(PCs, PWLocation.getIds(locs), backdays, back, labels, "./EPC_arff/"+filename);
	}

	private static void printPCInfo(ArrayList<PWC> PCs) {
		int minDays = Integer.MAX_VALUE, maxDays = 0, fullDays = 0;
		double minPW = Double.MAX_VALUE, maxPW = 0;
		for (PWC pwc:PCs) {
			int days = pwc.getlength();
			minDays = Math.min(minDays, days);
			maxDays = Math.max(maxDays, days);
			fullDays += days;
			double avg = pwc.average;
			minPW = Math.min(minPW, avg);
			maxPW = Math.max(maxPW, avg);
		}
		System.out.println("minDays: "+minDays+"; maxDays: "+maxDays+"; avgDays: "+fullDays/PCs.size());
		System.out.println("minPW: "+minPW+"; maxPW: "+maxPW);
	}
	
	private static double sumPWs[] = null;
	public static ArrayList<PWC> getPCs(double[] pwData, double[][] percentileData,
			double lowRatio, double PCUpRatio, double PCLowRatio, String label) throws NumberFormatException, IOException {
		if (lowRatio < 0 || lowRatio > 1)
			return null;
		if (PCLowRatio < 0 || PCLowRatio > PCUpRatio || PCLowRatio > 1)
			return null;
		if (pwData == null || percentileData == null)
			return null;
		int days = pwData.length;
		if (sumPWs == null || sumPWs.length < days)
			sumPWs = new double[days];
		sumPWs[0] = pwData[0];
		for (int i=1;i<days;i++)
			sumPWs[i] = sumPWs[i-1] + pwData[i];
		ArrayList<PWC> PCs = new ArrayList<PWC>();
		for (int i=0;i<=days-minPCDays;i++) {
			for (int j=Math.min(i+maxPCDays-1, days-1);j>=i+minPCDays-1;j--) {
				double low = percentileData[j-i][(int)(lowRatio/percentile_step)]/(j-i+1);
				if (pwData[i] <= low) continue;
				double PCUPBOUND = Double.MAX_VALUE;
				if (PCUpRatio < 1)
					PCUPBOUND = percentileData[j-i][(int)(PCUpRatio/percentile_step)];
				double PCLOWBOUND = percentileData[j-i][(int)(PCLowRatio/percentile_step)];
				double sum = sumPWs[j];
				if (i > 0)
					sum -= sumPWs[i-1];
				int length = j-i+1;
				double avg = sum/length;
				if (sum >= PCLOWBOUND && sum < PCUPBOUND) {
					PWC pc = new PWC(Base_Date, i, j, avg);
					pc.classlabel = label;
					for (int k=i;k<=j;k++)
						pc.values[k-i] = pwData[k];
					PCs.add(pc);
					break;
				}
			}
		}
		PCs = PWC.PWCRangeByYear(PCs, trainData_start_year, trainData_end_year);
		PCs = PWC.PWCRangeByMonth(PCs, Start_Month, End_Month);
		return PCs;
	}
	
	public static ArrayList<PWC> getIowaPCs_2(double lowRatio, double PCUpRatio, double PCLowRatio, String label) throws NumberFormatException, IOException {
		if (IowaPWs == null) loadIowaPWs();
		if (percentilesIowa == null) getIowaPWPercentiles();
		return getPCs(IowaPWs, percentilesIowa, lowRatio, PCUpRatio, PCLowRatio, label);
	}
	
	public static ArrayList<PWLocation> getLoclist_2(ArrayList<PWC> EPCs, int back,int backdays) throws IOException {
		if (PercentileUsed == 0 && percentiles0 == null)
			getLocalPercentiles();
		if (PercentileUsed != 0 && (percentiles1 == null || percentiles2 == null))
			getGlobalPercentiles();
		String[] featureFiles = DataLoader.featureFiles;
		int PW = DataLoader.PW;
		if (data == null)
			data = new double[DataLoader.features.length][][];
		if (data[PW] == null)
			data[PW] = DataLoader.loadingData(featureFiles[PW], delimit, totalSampleLocations, (int)Data_start_day, (int)Data_end_day);
		ArrayList<PWLocation> loclist = new ArrayList<PWLocation>();
		for (int loc=0;loc<flooding_prediction.totalSampleLocations;loc++) {
			ArrayList<PWC> PWCs = null;
			if (PercentileUsed == 1)
				PWCs = getPCs(data[PW][loc], percentiles1, lowPercentile, 2, EPCPercentile, "PWC"); 
			else if (PercentileUsed == 2)
				PWCs = getPCs(data[PW][loc], percentiles2, lowPercentile, 2, EPCPercentile, "PWC"); 
			else if (PercentileUsed == 0)
				PWCs = getPCs(data[PW][loc], percentiles0[loc], lowPercentile, 2, EPCPercentile, "PWC");
			if (PWCs != null && !PWCs.isEmpty()) {
				PWLocation plc = PWLocation.findLocSupport(loc, totalDays, backdays, back, PWC.mergeDate(EPCs), PWC.mergeDate(PWCs));
				loclist.add(plc);
			}
		}
		if (!loclist.isEmpty())
			PWLocation.SortLocbyConfidence(loclist);
		return loclist;
	}
	
	public static ArrayList<PWC> cleanPWCs(ArrayList<PWC> PWCs, int backdays, int back) {
		ArrayList<PWC> pwclist = new ArrayList<PWC>();
		int i = 0;
		int last_date = backdays+back+1-minPCDays;
		do {
			PWC pwc = PWCs.get(i);
			if (pwc.start_date >= last_date+minPCDays) {
				pwclist.add(new PWC(pwc));
				last_date = pwc.start_date;
			}
			i++;
		} while (i < PWCs.size());
		return pwclist;
	}
	
	public static ArrayList<PWC> preparePCs(ArrayList<PWC> PCs, ArrayList<PWC> EPCs, int backdays, int back) {
		ArrayList<PWC> pclist = new ArrayList<PWC>();
		int j = 0;
		int last_date = backdays+back+1-minPCDays;
		for (PWC pwc:PCs) {
			if (pwc.start_date < last_date+minPCDays) continue;
			while (j < EPCs.size() && EPCs.get(j).start_date < pwc.start_date) j++;
			if (j > 0 && EPCs.get(j-1).start_date > pwc.start_date-minPCDays)
				continue;
			if (j < EPCs.size() && EPCs.get(j).start_date < pwc.start_date+minPCDays)
				continue;
			pclist.add(new PWC(pwc));
			last_date = pwc.start_date;
		}
		return pclist;
	}
	
	public static ArrayList<PWC> prepareNonPCs(ArrayList<PWC> PCs, ArrayList<PWC> EPCs, int backdays, int back) {
		ArrayList<PWC> nonPCs = new ArrayList<PWC>();
		int i = 0, j = 0;
		int last_date = backdays+back+1-minPCDays;
		for (int k=backdays+back+1;k<=totalDays-minPCDays;k++) {
			if (k < last_date+minPCDays) continue;
			while (i < PCs.size() && PCs.get(i).start_date < k) i++;
			if (i > 0 && PCs.get(i-1).start_date > k-minPCDays)
				continue;
			if (i < PCs.size() && PCs.get(i).start_date < k+minPCDays)
				continue;
			while (j < EPCs.size() && EPCs.get(j).start_date < k) j++;
			if (j > 0 && EPCs.get(j-1).start_date > k-minPCDays)
				continue;
			if (j < EPCs.size() && EPCs.get(j).start_date < k+minPCDays)
				continue;
			PWC pwc = new PWC(PCs.get(0));
			//pwc.classlabel = "NONPC";
			pwc.start_date = k;
			nonPCs.add(pwc);
			last_date = pwc.start_date;
		}
		nonPCs = PWC.PWCRangeByMonth(nonPCs, Start_Month, End_Month);
		return nonPCs;
	}

	public static ArrayList<PWC> getNonEPCs(ArrayList<PWC> EPCs, int backdays, int back, String label) {
		ArrayList<PWC> nonEPCs = new ArrayList<PWC>();
		int j = 0;
		for (int k=backdays+back+1;k<=totalDays-minPCDays;k++) {
			while (j < EPCs.size() && EPCs.get(j).start_date < k) j++;
			if (j > 0 && EPCs.get(j-1).start_date > k-minPCDays)
				continue;
			if (j < EPCs.size() && EPCs.get(j).start_date < k+minPCDays)
				continue;
			PWC pwc = new PWC(EPCs.get(0));
			pwc.classlabel = label;
			pwc.start_date = k;
			nonEPCs.add(pwc);
		}
		nonEPCs = PWC.PWCRangeByMonth(nonEPCs, Start_Month, End_Month);
		return nonEPCs;
	}

	/*
	 * validate EPC found by known flooding event at Iowa based on 
	 * http://homelandsecurity.iowa.gov/disasters/iowa_disaster_history.html
	 */
	private static int[][] floodingStartDates = {
		{1984, 6, 7}, {1987, 5, 26}/*, {1988, 7, 15}*/, {1990, 5, 18}, {1990, 7, 25}, 
		{1991, 6, 1}, {1992, 9, 14}, {1993, 3, 26}, {1993, 4, 13}, {1996, 5, 8},
		{1996, 6, 15}, {1998, 6, 13}, {1999, 5, 16}, {1999, 7, 2}, {2001, 4, 8}, {2002, 6, 3}, 
		{2004, 5, 19}, {2007, 5, 5}, {2007, 8, 17}, {2008, 5, 25}, {2010, 5, 12}, {2010, 6, 1}
	};

	private static int[][] floodingEndDates = {
		{1984, 6, 8}, {1987, 5, 31}/*, {1988, 7, 16}*/, {1990, 7, 6}, {1990, 8, 31}, 
		{1991, 6, 15}, {1992, 9, 15}, {1993, 4, 12}, {1993, 10, 1}, {1996, 5, 28},
		{1996, 6, 30}, {1998, 7, 15}, {1999, 5, 29}, {1999, 8, 10}, {2001, 5, 29}, {2002, 6, 25}, 
		{2004, 6, 24}, {2007, 5, 7}, {2007, 9, 5}, {2008, 8, 13}, {2010, 5, 13}, {2010, 8, 31}
	};

	public static void floodingEventCoverage() throws NumberFormatException, IOException {
		int maxLagDays = 3;
		double[] EPCPercentiles = {/*0.8, 0.82, 0.84, 0.86, 0.88, */0.85, 0.9, 0.95};
		double[] lowPercentiles = {/*0.35, 0.36, 0.37, 0.38, 0.39, 0.40, 0.45, */0.5, 0.6, 0.7, 0.8, 0.85};
		int[] floodingStartDays = new int[floodingStartDates.length];
		int[] floodingEndDays = new int[floodingEndDates.length];
		for (int i=0;i<floodingStartDays.length;i++) {
			Calendar Date;
			Date = new GregorianCalendar(floodingStartDates[i][0], floodingStartDates[i][1]-1, floodingStartDates[i][2]);
			floodingStartDays[i] = (int) ((Date.getTimeInMillis()-Base_Date.getTimeInMillis())/(1000 * 60 * 60 * 24));
			Date = new GregorianCalendar(floodingEndDates[i][0], floodingEndDates[i][1]-1, floodingEndDates[i][2]);
			floodingEndDays[i] = (int) ((Date.getTimeInMillis()-Base_Date.getTimeInMillis())/(1000 * 60 * 60 * 24));
		}
		BufferedWriter out = new BufferedWriter(new FileWriter("flooding_coverage.txt"));
		out.write("EPCPercentile\tlowPercentile\tminPCDays\tnonPC\tpEPC\ttEPC\tpRate\tfloodingEventMissed\n");
		if (IowaPWs == null)
			loadIowaPWs();
		int backdays = 6, back = 5;
		int minPCDaysCopy = minPCDays;
		for (double EPCPerc:EPCPercentiles)
		for (minPCDays=5;minPCDays<22;minPCDays++) 
		for (double lowPerc:lowPercentiles){
			/*
			double low = percentilesIowa[maxPCDays-1][(int)(lowPercentile/percentile_step)]/maxPCDays;
			double PCThreshold = percentilesIowa[maxPCDays-1][(int)(0.6/percentile_step)]/maxPCDays;
			double EPCThreshold = percentilesIowa[maxPCDays-1][(int)(EPCPercentile/percentile_step)]/maxPCDays;
			ArrayList<PWC> pwclist = PWC.FindPWCs(Start_Date,IowaPWs,low,PCThreshold,maxNonePCDays,minPCDays);
			ArrayList<PWC> AllEPCs = PWC.PWCRangeByAverage(pwclist, EPCThreshold,Double.MAX_VALUE,"EPC");
			ArrayList<PWC> EPCs = PWC.PWCRangeByYear(AllEPCs, trainData_start_year, trainData_end_year);
			EPCs = PWC.PWCRangeByMonth(EPCs, Start_Month, End_Month);
			EPCs = PWC.removeEmbedded(EPCs);*/
			ArrayList<PWC> EPCs = getIowaPCs_2(lowPerc, 2, EPCPerc, "EPC");
			//ArrayList<PWC> PCs = getIowaPCs_2(lowPerc, EPCPerc, PCPercentile, "PC");
			//ArrayList<PWC> nonPCs = prepareNonPCs(PCs, EPCs, backdays, back);
			ArrayList<PWC> nonPCs = getNonEPCs(EPCs, backdays, back, "NONEPC");
			//PCs = preparePCs(PCs, EPCs, backdays, back);
			//EPCs = prepareEPCs(EPCs,backdays,back);
			//for (maxLagDays=3;maxLagDays<7;maxLagDays++) {
				boolean[] count = new boolean[EPCs.size()];
				int pEPC = 0;
				String missed = "";
				for (int i=0;i<floodingStartDays.length;i++) {
					int start = floodingStartDays[i], end = floodingEndDays[i];
					boolean cover = false;
					for (int index=0;index<EPCs.size();index++) {
						PWC epc = EPCs.get(index);
						if (epc.start_date <= end && epc.end_date >= start-maxLagDays) {
							cover = true;
							if (!count[index]) {
								pEPC++;
								count[index] = true;
							}
						}
						if (epc.start_date > end)
							break;
					}
					if (!cover) {
						if (missed == "")
							missed += floodingStartDates[i][0]+"-"+floodingStartDates[i][1]+"-"+floodingStartDates[i][2];
						else
							missed += ","+floodingStartDates[i][0]+"-"+floodingStartDates[i][1]+"-"+floodingStartDates[i][2];
					}
				}
				out.write(EPCPerc+"\t\t"+lowPerc+"\t\t"+minPCDays+"\t\t"
						+nonPCs.size()+"\t"
						+pEPC+"\t"+EPCs.size()+"\t"
						+String.format("%.4f", ((float)pEPC/EPCs.size()))
						+"\t"+missed+"\n");
			//}
		}
		out.close();
		minPCDays = minPCDaysCopy;
	}

	private static void setupTime(int sYear, int eYear, int sMonth, int eMonth) {
		String featureFiles[] = new String[] {
			"./data/text/"+sYear+"-"+eYear+"_Z1000.txt",
			"./data/text/"+sYear+"-"+eYear+"_T850.txt",
			"./data/text/"+sYear+"-"+eYear+"_PW.txt",
			"./data/text/"+sYear+"-"+eYear+"_U300.txt",
			"./data/text/"+sYear+"-"+eYear+"_U850.txt",
			"./data/text/"+sYear+"-"+eYear+"_V300.txt",
			"./data/text/"+sYear+"-"+eYear+"_V850.txt",
			"./data/text/"+sYear+"-"+eYear+"_Z300.txt",
			"./data/text/"+sYear+"-"+eYear+"_Z500.txt"
		};
		for (int i=0;i<featureFiles.length;i++)
			DataLoader.featureFiles[i] = featureFiles[i];
		IowaPrecipFileAll = "./data/text/PRECIP2_"+sYear+"-"+eYear+".txt";
		IowaPrecipFileNoWinter = "./data/text/PRECIP2_"+sYear+"-"+eYear+"_"+sMonth+"-"+eMonth+".txt";
		PWFileNoWinter = "./data/text/"+sYear+"-"+eYear+"_PW_"+sMonth+"-"+eMonth+".txt";

		Start_Year = sYear; End_Year = eYear;
		Start_Month = sMonth; End_Month = eMonth;
		Base_Date = new GregorianCalendar(sYear, 0, 1); // first date's ID starts with 0
		Start_Date = new GregorianCalendar(sYear, 0, 1);
		End_Date = new GregorianCalendar(eYear, 11, 31);
		Data_start_day =(Start_Date.getTimeInMillis()-Base_Date.getTimeInMillis())/(1000 * 60 * 60 * 24);
		Data_end_day = (End_Date.getTimeInMillis()-Base_Date.getTimeInMillis())/(1000 * 60 * 60 * 24);
		trainData_start_year = sYear; trainData_end_year = eYear;
		testData_start_year = sYear; testData_end_year = eYear;
		totalDays = (int)(Data_end_day-Data_start_day+1);
	}
	
	private static ArrayList<PWC> getPEPCs(ArrayList<PWC> EPCs) {
		int[] floodingStartDays = new int[floodingStartDates.length];
		int[] floodingEndDays = new int[floodingEndDates.length];
		for (int i=0;i<floodingStartDays.length;i++) {
			Calendar Date;
			Date = new GregorianCalendar(floodingStartDates[i][0], floodingStartDates[i][1]-1, floodingStartDates[i][2]);
			floodingStartDays[i] = (int) ((Date.getTimeInMillis()-Base_Date.getTimeInMillis())/(1000 * 60 * 60 * 24));
			Date = new GregorianCalendar(floodingEndDates[i][0], floodingEndDates[i][1]-1, floodingEndDates[i][2]);
			floodingEndDays[i] = (int) ((Date.getTimeInMillis()-Base_Date.getTimeInMillis())/(1000 * 60 * 60 * 24));
		}
		ArrayList<PWC> PEPCs = new ArrayList<PWC>();
		boolean[] count = new boolean[EPCs.size()];
		for (int i=0;i<floodingStartDays.length;i++) {
			int start = floodingStartDays[i], end = floodingEndDays[i];
			for (int index=0;index<EPCs.size();index++) {
				PWC epc = EPCs.get(index);
				if (epc.start_date <= end && epc.end_date >= start-3) {
					if (!count[index]) {
						PEPCs.add(new PWC(epc));
						count[index] = true;
					}
				}
				if (epc.start_date > end)
					break;
			}
		}
		return PEPCs;
	}
	
	public static void main(String[] args) throws Exception {
		maxNonePCDays = 2;
		minPCDays = 5;
		maxPCDays = 21;
		lowPercentile = 0.85;
		PCPercentile = 0.6;
		EPCPercentile = 0.85;
		PercentileUsed = 1;
		percentile_step = 0.01;
		int support = 100;
		//double confidence = 0.2;
		int clean = 1;
		int backdays = 6;
		int back = 5;
		if (args.length > 0)
			lowPercentile = Double.parseDouble(args[0]);
		if (args.length > 1)
			EPCPercentile = Double.parseDouble(args[1]);
		if (args.length > 2)
			minPCDays = Integer.parseInt(args[2]);
		if (args.length > 3)
			PercentileUsed = Integer.parseInt(args[3]);
		if (args.length > 4)
			support = Integer.parseInt(args[4]);
		if (args.length > 5)
			clean = Integer.parseInt(args[5]);

		setupTime(1980, 2010, 3, 10);
		//floodingEventCoverage();
		
		System.out.println("Searching EPC~");
		ArrayList<PWC> EPCs = getIowaPCs_2(lowPercentile, 2, EPCPercentile, "EPC");
		printPCInfo(EPCs);
		System.out.println("EPC: "+EPCs.size());
		
		System.out.println("Searching NONEPC~");
		ArrayList<PWC> NonEPCs = getNonEPCs(EPCs, backdays, back, "NONEPC");
		System.out.println("Non EPCs: "+NonEPCs.size());
		
		// get top locations based on support and confidence
		ArrayList<PWLocation> loclist = getLoclist_2(EPCs, backdays, back);
		if (loclist == null || loclist.isEmpty()) {
			System.out.println("No locations are found for current parameters.");
			return;
		}
		String[] thresholdfiles = {"local", "Iowa", "global"};
		PWLocation.StoreLocData(loclist, String.format("%s_%.2f_%.2f_%d.txt", thresholdfiles[PercentileUsed], lowPercentile, EPCPercentile, minPCDays));
		//ArrayList<PWLocation> locsbysupport= PWLocation.LOCRangeBySupport(loclist, support, 9999999 );
		//ArrayList<PWLocation> locs=PWLocation.LOCRangeByConfidence(locsbysupport,confidence,1);
		//PWLocation.StoreLocData(locs, String.format("%s_%d_%.2f.txt", thresholdfiles[PercentileUsed], support, confidence));
		int locnum = Math.min(500, loclist.size());
		int[] ids = new int[locnum];
		int i = 0;
		for (PWLocation loc:loclist) {
			if (loc.support >= support) {
				ids[i++] = loc.ID;
				if (i >= locnum) break;
			}
		}
		if (i < locnum) {
			locnum = i;
			int[] ids_2 = new int[locnum];
			for (int j=0;j<locnum;j++)
				ids_2[j] = ids[j];
			ids = ids_2;
		}

		if (clean > 0) {
			EPCs = cleanPWCs(EPCs, backdays, back);
			NonEPCs = cleanPWCs(NonEPCs, backdays, back);
			System.out.println("EPC after clean: "+EPCs.size());
			System.out.println("NonEPC after clean: "+NonEPCs.size());
		}
		
		ArrayList<PWC> AllPCs = new ArrayList<PWC>();
		for (PWC pwc:EPCs)
			AllPCs.add(pwc);
		//for (PWC pwc:trainPCs)
			//AllPCs.add(pwc);
		for (PWC pwc:NonEPCs)
			AllPCs.add(pwc);
		/*
		int[] ids=new int[totalSampleLocations];
		for (int i=0;i<totalSampleLocations;i++)
			ids[i] = i;*/
		String[] labels = new String[2];
		labels[0] = EPCs.get(0).classlabel;
		labels[1] = NonEPCs.get(0).classlabel;
		String wekafile = String.format("%s_%d_locs_%.2f_%.2f_%d", thresholdfiles[PercentileUsed], ids.length, lowPercentile, EPCPercentile, minPCDays);
		if (clean > 0)
			wekafile += "_cleaned.arff";
		else
			wekafile += ".arff";
		createWekaFile(AllPCs, ids, backdays, back, labels, wekafile);
		/*
		loadRawData();
		ClassificationResults r = RunWeka.runFoldsInMemory(RunWeka.getBaseClassifier(baseclassifier),null,crossValFolds,data,AllPCs,ids,backdays,back);
		BufferedWriter outresult = new BufferedWriter(new FileWriter("./All_"+EPCPercentile+"-"+minPCDays+".csv",true));
		r.printout();
		outresult.write(r.one_record());
		outresult.flush();
		outresult.close();
*/
		//getLocations(suppototalSampleLocationsrt, confidence);
		
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
