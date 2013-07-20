import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.concurrent.Callable;

public class flooding_prediction {
	static public Calendar Base_Date = new GregorianCalendar(2001, 0, 1); // first date's ID starts with 0
	static public String IowaPrecipFile = "./data/text/PRECIP2_2001-2010.txt";
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
	static public boolean RandomselectPC = true;
	static public String idFile[] = {"loc_Individuals.txt","loc_Iowa.txt","loc_All.txt"};
	static public int PercentileUsed = 0; //0: Individuals  1: Iowa  2: All locations
	static public int start_month = 5, end_month = 9; // summer time = May to September
	static public double support_percentile_start = 0.6, support_percentile_end = 1;
	static public int support_start = 0, support_end = Integer.MAX_VALUE;
	static double[] supportranges = new double[]{0,0.2,0.4,0.6,0.8,1};
	static double[] supportthresholds = new double[]{0,0.2,0.4,0.6,0.8};
	static public double confidence_start = 0.0, confidence_end = 1;
	static double[] confidencethresholds = new double[]{0,0.15,0.2,0.25,0.3};
	static public int baseclassifier =2; //0:SVM 1:LADTree 2:J48 3:NaiveBayes
	static public int crossValFolds =5;

	static public  String[] resultfiles = new String[]{"./results/Individuals.csv","./results/Iowa.csv","./results/All.csv"};
	
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
/*
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
*/		
		
		// load the location support and confidence data from selected file
		ArrayList<PWLocation> loclist = PWLocation.LoadLocData(idFile[PercentileUsed], delimit2);
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
	
	public static ArrayList<PWLocation> getLoclist(ArrayList<PWC> AllEPCs, int back,int backdays) throws IOException {
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
		for (int loc=0;loc<flooding_prediction.totalSampleLocations;loc++) {
			if (PercentileUsed == 0) {
				lowp = percentiles0[loc][lowpIndex];
				pcp = percentiles0[loc][pcpIndex];
				epcp = percentiles0[loc][epcpIndex];
			}
			ArrayList<PWC> PWEPClist = PWC.FindPWCs(flooding_prediction.Start_Date,data[DataLoader.PW][loc]
					,lowp,pcp,maxNonePCDays,minPCDays); 
			PWEPClist = PWC.PWCRangeByAverage(PWEPClist, epcp, Double.MAX_VALUE,"EPC");
			PWC.SortPWCbyStartDate(PWEPClist);
			//System.out.println(loc+":="+PWEPClist.size());
			PWLocation plc = PWLocation.findLocSupport(loc,flooding_prediction.totalDays, backdays, back,AllEPCs,PWEPClist);
			loclist.add(plc);
		}
		PWLocation.SortLocbySupport(loclist);
		return loclist;
	}
	
	public static ArrayList<ClassificationResults> runInMemory() throws Exception {
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
		if (TrainPCs.size()<crossValFolds)
			return null;
		
		// get the location support and confidence data
		ArrayList<PWLocation> loclist = getLoclist(AllEPCs, back, backdays);
		
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
				int min_locs_number = 1, max_Locs_number = 850;
				if (locs_number<min_locs_number || locs_number>max_Locs_number)
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
	
	public static void testPercentileUsed2() throws Exception {
		PercentileUsed = 2;
		/*
		Run_minPCDays(5, 15, true, new Callable<ArrayList<ClassificationResults>>() {
			   public ArrayList<ClassificationResults> call() throws Exception {
			       return runInMemory(); }});
		*/
		
		Run_minPCDays(5, 15, true, new Callable<Void>() {
			   public Void call() throws Exception {
				   return Run_maxNonePCDays(1, 3, true, new Callable<ArrayList<ClassificationResults>>() {
					   public ArrayList<ClassificationResults> call() throws Exception {
					       return runInMemory(); }}); }});
					       
		/*
		minPCDays = 15;
		maxNonePCDays = 1;
		Run_EPCPercentile(true, new Callable<ArrayList<ClassificationResults>>() {
			   public ArrayList<ClassificationResults> call() throws Exception {
			       return runInMemory(); }});
		*/
	}
	
	public static void main(String[] args) throws Exception {
		testPercentileUsed2();
		//runInMemory();
		/*
		BufferedWriter outresult = new BufferedWriter(new FileWriter(resultfiles[PercentileUsed],false));
		ClassificationResults.writetitle(outresult);
		outresult.close();
		Run_supportRange(4,4,true,null);
		*/
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
