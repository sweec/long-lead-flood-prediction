import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;

/**
 * 
 */

/**
 * @author Jacky
 *
 */
public class PWC implements Comparable<PWC>{
	private static int maxPCSearchdays =94;
	public Calendar BASE_DATE;

	public int start_date;
	public int end_date;
	public double average; 
	public double[] values;
	
	public String classlable; // PC or EPC
	public String predictlable; // PC or EPC
	
	public PWC(Calendar Base_Date,int s_date,int e_date,double avg){
		start_date =s_date;
		end_date = e_date;
		average = avg;
		values = new double[e_date-s_date+1];
		BASE_DATE = Base_Date;
		//BASE_DATE.add(Calendar.DATE, flooding_prediction.UsingData_start_day);
	}
	
	public PWC(PWC pwc){
		start_date =pwc.start_date;
		end_date = pwc.end_date;
		average = pwc.average;
		values = Arrays.copyOf(pwc.values, pwc.values.length);
		if ( pwc.classlable != null){
			classlable = new String(pwc.classlable);
		}
		if (pwc.predictlable != null) {
			predictlable = new String(pwc.predictlable);
		}
		//BASE_DATE.add(Calendar.DATE, flooding_prediction.UsingData_start_day);
	}
	
	public int getlength(){
		return end_date-start_date+1;
	}
	
	public double getMaxValue(){
		return StdStats.max(values);
	}
	
	public double getMinValue(){
		return StdStats.min(values);
	}
	
	public double getStdDev(){
		return StdStats.stddev(values);
	}
	
	public double getMean(){
		return StdStats.mean(values);
	}
	
	public Calendar getStartDate(){
		Calendar cal= (Calendar)BASE_DATE.clone();
		cal.add(Calendar.DATE, start_date);
		return cal;
	}
	
	public int getStartYear(){
		Calendar cal= getStartDate();
		return cal.get(Calendar.YEAR);
	}
	
	public int getStartMonth(){
		Calendar cal= getStartDate();
		return cal.get(Calendar.MONTH)+1;
	}
	
	public String getStartDateString(){
		String ret="";
		Calendar cal = getStartDate();
		ret = cal.get(Calendar.YEAR)+"/"+ (cal.get(Calendar.MONTH)+1)+"/"+cal.get(Calendar.DATE);
		return ret;
	}

	
	public int compareTo(PWC anotherInstance) {
        return new Double(this.average).compareTo( new Double(anotherInstance.average));
    }
	
	public String toString(){
		return start_date+" "+this.getlength()+" "+ String.format("%f", average)
				+" "+ String.format("%.7f", this.getMaxValue())+" "+ String.format("%.7f", this.getMinValue())
				+" "+ String.format("%.7f", this.getStdDev())+" "+getStartDateString();
	}
	
    private static void findOnePWC(Calendar Base_Date,double[] daily_data,int start, int end, double lowThreshold
    		,double PCThreshold, int maxNonePCDays, int minPCDays ,ArrayList<PWC> pwclist ){
    	double max = daily_data[start]-1;
    	int max_day=start;
    	int left_index, right_index;
    	int lowCount = maxNonePCDays;
    	double sum = 0;
    	if ((end-start+1) >= minPCDays){ // there are enough days in the range to form a PWC
    		//Find the day which has the max value between given start day and end day.
    		for (int day=start;day<=end;day++){
    			if (daily_data[day]>max){
    				max = daily_data[day];
    				max_day=day;
    			}
    		}
    		//System.out.println(max_day);
    		//System.out.println("lowThreshold:"+lowThreshold);
    		if (max > lowThreshold) { // there are days in this range have higher value than low Threshold
    			// start from the max_day find the cluster from its left and right
    			left_index = max_day-1;
    			if (left_index < start) {left_index=start;} // check the left boundary    		
    			right_index = max_day+1;
    			if (right_index > end) {right_index=end;} // check the right boundary

    			// Extend to the left hand side until reach the boundary or lower than the Threshold  
    			lowCount = maxNonePCDays;
    			while ((left_index-1 >= start) && (lowCount>0) ){
    				left_index--;
    				if (daily_data[left_index]<=lowThreshold){
    					lowCount--;
    				} else {lowCount = maxNonePCDays;} // reset the count of the number lower than Threshold
    			}
    			//System.out.println("left:"+left_index);
    			// Extend to the right hand side until reach the boundary or lower than the Threshol
    			lowCount = maxNonePCDays;
    			while ((right_index+1 <= end) && (lowCount>0) ){
    				right_index++;
    				if (daily_data[right_index]<=lowThreshold){
    					lowCount--;
    				} else {lowCount = maxNonePCDays;} // reset the count of the number lower than Threshold
    			}
    			//System.out.println("right:"+right_index);
    			// find the first date with value higher than Threshold
    			while (daily_data[left_index]<=lowThreshold ){
    				left_index++;
    			}
    			// find the last date with value higher than Threshold
    			while (daily_data[right_index]<=lowThreshold ){
    				right_index--;
    			}
    			// check the average if the total days greater than the required mini days
    			int PWC_length = right_index-left_index+1;
    			if (PWC_length>=minPCDays) {
    				for (int i=left_index;i<=right_index;i++){
    					sum+=daily_data[i];
    				}
    				double average= sum/(double)PWC_length;
					PWC pwc = new PWC(Base_Date, left_index,right_index, average);
					int day=0;
					for (int i=left_index;i<=right_index;i++) {
						pwc.values[day++]=daily_data[i];
					}
					pwclist.add(pwc);
					//System.out.println(pwc);

    				/*
    				if (average > PCThreshold) { // one PWC found
    					PWC pwc = new PWC(left_index,right_index, average);
    					int day=0;
    					for (int i=left_index;i<=right_index;i++) {
    						pwc.values[day++]=daily_data[i];
    					}
    					pwclist.add(pwc);
    					//System.out.println(pwc);
    				}
    				*/ 				
    			}     				
    			// find the PWC in the range of the rest of left hand size
    			if (left_index <= start) {left_index=start+1;}
				findOnePWC(Base_Date, daily_data,start,left_index-1,lowThreshold
						,PCThreshold,maxNonePCDays,minPCDays,pwclist);
				// find the PWC in the range of the rest of right hand size
				if (right_index >= end) {right_index=end-1;}
				findOnePWC(Base_Date,daily_data,right_index+1,end,lowThreshold
						,PCThreshold,maxNonePCDays,minPCDays,pwclist);    				
    		} else { // there is no any single day in this range has the value higher than Threshold
    			//System.out.println("there is no any single day in this range has the value higher than Threshold");
    		}
    	} else { //there are not enough days in the range to form a PWC
    		//System.out.println("there are not enough days in the range to form a PWC");
    	}
    }    
    
    public static void SortPWCbyStartDate(ArrayList<PWC> pwclist){
    	Collections.sort(pwclist, new Comparator<PWC>() { 
    		public int compare(PWC o1, PWC o2){ 
    				return (o1.start_date-o2.start_date);
    			}  
    		}
    	);    	
    }
    
    public static void SortPWCbyAverage(ArrayList<PWC> pwclist){
    	Collections.sort(pwclist, new Comparator<PWC>() { 
    		public int compare(PWC o1, PWC o2){ 
    				return (Double.compare(o1.average,o2.average));
    			}  
    		}
    	);    	
    }
    
    public static void SortPWCbyLength(ArrayList<PWC> pwclist){
    	Collections.sort(pwclist, new Comparator<PWC>() { 
    		public int compare(PWC o1, PWC o2){ 
    				return (o1.getlength()-o2.getlength());
    			}  
    		}
    	);    	
    }
	
    public static ArrayList<PWC> AllPCsList(Calendar Base_Date, double[] daily_data,int sday, int minPCDays){
    	ArrayList<PWC> pcs= new ArrayList<PWC>();
    	for (int i=minPCDays-1;i<daily_data.length;i++) {
    		PWC p = new PWC(Base_Date, sday,sday+i, 0);
    		p.values=Arrays.copyOfRange(daily_data, 0, i);
    		p.average = p.getMean();
    		pcs.add(p);
    	}
    	
    	return pcs;
    }
    
    public static PWC maxPC(Calendar Base_Date, double[] daily_data,int sday, int minPCDays){
    	PWC pwc=null;
    	double max=Double.NEGATIVE_INFINITY;
    	for (int i=minPCDays-1, days=0;(i<daily_data.length) && (days<=maxPCSearchdays);i++, days++) {
    		PWC p = new PWC(Base_Date,sday,sday+i, 0);
    		p.values=Arrays.copyOfRange(daily_data, 0, i);
    		p.average = p.getMean();
    		if (p.average > max) {
    			pwc = p;
    			max = p.average;
    		}
    	}
    	
    	return pwc;
    }
    
    
    public static ArrayList<PWC> FindPWCs(Calendar Base_date, double[] daily_data,double lowThreshold
    		,double PCThreshold, int maxNonePCDays, int minPCDays) {
    	
    	ArrayList<PWC> PWCs = new ArrayList<PWC>();
    	ArrayList<PWC> PCs = new ArrayList<PWC>();
    	findOnePWC(Base_date, daily_data,0,daily_data.length-1,lowThreshold,PCThreshold,maxNonePCDays,minPCDays,PWCs);
    	for (PWC pwc:PWCs) {
    		if (pwc.getlength() > minPCDays) { // find overlapping PC and EPC in PWC
    			for (int i=0; i<(pwc.getlength()-minPCDays);i++) {
    				PCs.add(maxPC(Base_date,Arrays.copyOfRange(pwc.values, i, pwc.getlength()-1),pwc.start_date+i ,minPCDays));
    			}
    		} else {
    			PCs.add(pwc);
    		}
    	}
    	
    	// sort the list by start_date
    	SortPWCbyStartDate(PCs);
    	return PCs;
    }
    
    public static ArrayList<PWC> PWCRangeByAverage(ArrayList<PWC> list, double low, double high, String lable){
    	ArrayList<PWC> PCs = new ArrayList<PWC>();
    	
    	for (PWC pwc:list){
    		if ((pwc.average >= low) && (pwc.average <= high)) {
    			PWC pc = new PWC(pwc);
    			pc.classlable= lable;
    			PCs.add(pc);
    		}
    	}
    	return PCs;
    }
    
    public static ArrayList<PWC> PWCRangeByDay(ArrayList<PWC> list, int sdate, int edate){
    	ArrayList<PWC> PCs = new ArrayList<PWC>();
    	
    	for (PWC pwc:list){
    		if ((pwc.start_date >= sdate) && (pwc.start_date <= edate)) {
    			PCs.add(new PWC(pwc));
    		}
    	}
    	return PCs;
    }
    
    public static ArrayList<PWC> PWCRangeByMonth(ArrayList<PWC> list, int smonth, int emonth){
    	ArrayList<PWC> PCs = new ArrayList<PWC>();
    	int startmonth;
    	for (PWC pwc:list){
    		startmonth=pwc.getStartMonth(); 
    		if ( (startmonth >= smonth) && (startmonth <= emonth)) {
    			PCs.add(new PWC(pwc));
    		}
    	}
    	return PCs;
    }
    
    public static ArrayList<PWC> PWCRangeByYear(ArrayList<PWC> list, int syear, int eyear){
    	ArrayList<PWC> PCs = new ArrayList<PWC>();
    	int startyear;
    	for (PWC pwc:list){
    		startyear=pwc.getStartYear(); 
    		if ( startyear >= syear && startyear <= eyear) {
    			PCs.add(new PWC(pwc));
    		}
    	}
    	return PCs;
    }
    
    public static void StorePCData(ArrayList<PWC> pwclist, String filename) throws IOException {
    	ArrayList<PWC> PCs =pwclist;
		BufferedWriter outPCData =new BufferedWriter(new FileWriter(filename));
		//System.out.println(PCs.size());
		if (PCs.size()>0) {
			outPCData.write(PCs.get(0)+"");
			for (int i=1; i<PCs.size(); i++){
				//System.out.println(p);
				outPCData.write("\n" + PCs.get(i));
			}
			outPCData.close();
		}
    }

    // create weka file from two lists: EPC list and PC list
    public static void createWekaFile2(String[] features, String[] featureFiles,String delimit,
    		ArrayList<PWC> PClist, ArrayList<PWC> EPClist, int days, int back
    		, String idFile, String outFile) throws IOException
    {
    	
		int idNum = DataLoader.getLineNumber(idFile);
		int PCNum = PClist.size(), EPCNum=EPClist.size();
		System.out.println("idNum: "+idNum+"; PCNum: "+PCNum+"; EPCNum: "+EPCNum);
		int[] ids = new int[idNum], PCStart = new int[PCNum], EPCStart = new int[EPCNum];
		DataLoader.getColumnData(idFile, 0,delimit, ids);
		for (int i=0;i<PCNum;i++){
			PCStart[i]=PClist.get(i).start_date;
		}
		for (int i=0;i<EPCNum;i++){
			EPCStart[i]=EPClist.get(i).start_date;
		}
		Arrays.sort(ids); Arrays.sort(EPCStart); Arrays.sort(PCStart);
		double[][][][] dataForEPC = new double[idNum][featureFiles.length][EPCNum][days];
		double[][][][] dataForPC = new double[idNum][featureFiles.length][PCNum][days];
		
		for (int f=0;f<featureFiles.length;f++) {
			BufferedReader reader = new BufferedReader(new FileReader(featureFiles[f]));
			String line="";
			int day = 0, EPCi = 0, PCi = 0;
			int EPCe = EPCStart[0]-back, EPCs =EPCe-days;
			int PCe = PCStart[0]-back, PCs =PCe-days+1;
			while ((line = reader.readLine()) != null) {
				String[] values = line.split(delimit);
				if (day>=EPCs && day<=EPCe) {
					for (int r=EPCi;r<Math.min(EPCi+days, EPCNum);r++) {
						int EPCsr = EPCStart[r]-(back+days-1);
						if (day<EPCsr) break;
						int index = day-EPCsr;
						int i=0;
						for (int id:ids) {
							dataForEPC[i][f][r][index] = Double.parseDouble(values[id]);	// skip 1st value
							i++;
						}
					}
				} else if (day>EPCe && EPCi<EPCNum) {
					EPCi++;
					if (EPCi<EPCNum) {
						EPCe = EPCStart[EPCi]-back; 
						EPCs =EPCe-days;
					}
				}
				
				if (day>=PCs && day<=PCe) {
					for (int r=PCi;r<Math.min(PCi+days, PCNum);r++) {
						int PCsr = PCStart[r]-(back+days-1);
						if (day<PCsr) break;
						int index = day-PCsr;
						int i=0;
						for (int id:ids) {
							dataForPC[i][f][r][index] = Double.parseDouble(values[id]);	// skip 1st value
							i++;
						}
					} 
				} else if (day>PCe && PCi<PCNum) {
					PCi++;
					if (PCi<PCNum) {
						PCe = PCStart[PCi]-back;
						PCs =PCe-days+1;
					}
				}
				if (EPCi>=EPCNum && PCi>=PCNum)
					break;
				day++;
			}
			reader.close();
		}
		
		BufferedWriter out = new BufferedWriter(new FileWriter(outFile));
		out.write("@relation EPC\n\n");
		for (int f=0;f<features.length;f++)
			for (int i=0;i<idNum;i++) 
				for (int di=0;di<days;di++) {
					out.write("@attribute "+features[f]+"_"+ids[i]+"_"+(back+days-1-di)+" numeric\n");
				}
		out.write("@attribute class {EPC, PC}\n\n@data\n");
		for (int e=0;e<EPCNum;e++) {
			for (int f=0;f<features.length;f++)
				for (int i=0;i<idNum;i++)
					for (int di=0;di<days;di++) {
						out.write(dataForEPC[i][f][e][di]+",");
					}
			out.write("EPC\n");
		}
		for (int e=0;e<PCNum;e++) {
			for (int f=0;f<features.length;f++)
				for (int i=0;i<idNum;i++)
					for (int di=0;di<days;di++) {
						out.write(dataForPC[i][f][e][di]+",");
					}
			out.write("PC\n");
		}
		out.close();
    }
    
    
    // Create weka file from one list consist of EPCs and PCs using location list 
    public static void createWekaFile0(String[] features, String[] featureFiles,String delimit,
    		ArrayList<PWC> PClist, int days, int back
    		, ArrayList<PWLocation> idlist, String outFile) throws IOException
    {    	
		int idNum = idlist.size();
		int PCNum = PClist.size();
		System.out.println("idNum: "+idNum+"; PCNum: "+PCNum+" to "+outFile);
		// extract only the ids from location data and then sort the ids
		int[] ids = PWLocation.getIds(idlist);
		Arrays.sort(ids);
		// array use to save the whole dataset 
		double[][][][] dataForPC = new double[idNum][featureFiles.length][PCNum][days];
		
		for (int f=0;f<featureFiles.length;f++) {
			// read from each feature file
			double[][] featuredata = DataLoader.loadingData(featureFiles[2],"\\s+",flooding_prediction.totalSampleLocations
					,(int)flooding_prediction.Data_start_day
					,(int)flooding_prediction.Data_end_day);
			for (int PCi=0;PCi<PClist.size();PCi++) {
				int PCe = PClist.get(PCi).start_date-back; // current EPS's end date
				int PCs =PCe-days+1; // current EPS's start date
				for (int day=PCs; day<=PCe; day++) { 
					int loc=0;
					for (int id:ids) {
						dataForPC[loc++][f][PCi][day-PCs] = featuredata[id][day];
					}
				}
			}
		}
		
		BufferedWriter out = new BufferedWriter(new FileWriter(outFile));
		out.write("@relation EPC\n\n");
		for (int f=0;f<features.length;f++)
			for (int i=0;i<idNum;i++) 
				for (int di=0;di<days;di++) {
					out.write("@attribute "+features[f]+"_"+ids[i]+"_"+(back+days-1-di)+" numeric\n");
				}
		out.write("@attribute class {EPC, PC}\n\n@data\n");
		for (int e=0;e<PCNum;e++) {
			for (int f=0;f<features.length;f++)
				for (int i=0;i<idNum;i++)
					for (int di=0;di<days;di++) {
						out.write(dataForPC[i][f][e][di]+",");
					}
			out.write(PClist.get(e).classlable+"\n");
		}
		out.close();
    }
    
 // Create weka file from one list consist of EPCs and PCs using location list 
    public static void createWekaFile(String[] features, String[] featureFiles,String delimit,
    		ArrayList<PWC> PClist, int days, int back
    		, ArrayList<PWLocation> idlist, String outFile) throws IOException
    {    	
		int idNum = idlist.size();
		int PCNum = PClist.size();
		System.out.println("idNum: "+idNum+"; PCNum: "+PCNum+" to "+outFile);
		// extract only the ids from location data and then sort the ids
		int[] ids = PWLocation.getIds(idlist);
		Arrays.sort(ids);
		// array use to save the whole dataset 
		double[][][][] dataForPC = new double[idNum][featureFiles.length][PCNum][days];
		
		for (int f=0;f<featureFiles.length;f++) {
			// read from each feature file
			BufferedReader reader = new BufferedReader(new FileReader(featureFiles[f]));
			String line=""; int day = 0; // index of the date
			int PCi = 0; //  index of current EPC
			int PCe = PClist.get(PCi).start_date-back; // current EPS's end date
			int PCs =PCe-days+1; // current EPS's start date
		//System.out.println("s:"+PCs+"  e:"+PCe+" start_date="+PClist.get(PCi).start_date );
			while ((line = reader.readLine()) != null) { // read in one day's data of each location
				
				for (int r=PCi;r<PCNum;r++) {
					PCe = PClist.get(r).start_date-back; // current EPS's end date
					PCs = PCe-days+1; // current EPS's start date

					if (day>=PCs && day<=PCe) { // if this day fulls into an EPS
						int index = day-PCs;
						int i=0;
						String[] values = line.split(delimit);
						for (int id:ids) {
							dataForPC[i][f][r][index] = Double.parseDouble(values[id]);	// skip 1st value
							i++;
						} 
					} else if (day>PCe) { // the day has passed this EPC
						PCi=r+1;
					}
					if (PCs>day) break;
				}
				if (PCi>=PCNum)// reachs last EPS then end while loop
					break;
				day++; // ready to read next day's data
			}
			reader.close();
		}
		
		BufferedWriter out = new BufferedWriter(new FileWriter(outFile));
		out.write("@relation EPC\n\n");
		for (int f=0;f<features.length;f++)
			for (int i=0;i<idNum;i++) 
				for (int di=0;di<days;di++) {
					out.write("@attribute "+features[f]+"_"+ids[i]+"_"+(back+days-1-di)+" numeric\n");
				}
		out.write("@attribute class {EPC, PC}\n\n@data\n");
		for (int e=0;e<PCNum;e++) {
			for (int f=0;f<features.length;f++)
				for (int i=0;i<idNum;i++)
					for (int di=0;di<days;di++) {
						out.write(dataForPC[i][f][e][di]+",");
					}
			out.write(PClist.get(e).classlable+"\n");
		}
		out.close();
    }
    
    // create weka file from one list using location file
    public static void createWekaFile(String[] features, String[] featureFiles,String delimit, 
    		ArrayList<PWC> PClist, int days, int back
    		, String idFile, String delimit2, String outFile) throws IOException
    {
    	PWC.createWekaFile(features,featureFiles,delimit, PClist,days,back,PWLocation.LoadLocData(idFile, delimit2),outFile);
    }
    
    public static void main(String[] args) {
    	
    }
}
