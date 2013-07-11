import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;


public class PWLocation implements Comparable<PWLocation>{
	public int ID;//start from 0
	public int support;
	public int totalEPCdays;
	public double confidence; 
	
	public PWLocation(int id,int support,int EPCdays,double conf){
		this.ID = id;
		this.support = support;
		this.totalEPCdays = EPCdays;
		this.confidence = conf;
	}
	
	public PWLocation(PWLocation loc){
		this.ID = loc.ID;
		this.support = loc.support;
		this.totalEPCdays = loc.totalEPCdays;
		this.confidence = loc.confidence;
	}
	
	public int compareTo(PWLocation anotherInstance) { // higt to low
        return new Double(anotherInstance.confidence).compareTo( new Double(this.confidence));
    }
	
	public String toString() {
		return this.ID+" "+this.support+" "+ this.totalEPCdays +" "+String.format("%.7f", this.confidence);
	}
	
	// support values are total overlapping days
	public static PWLocation findLocSupport(int id, int totaldays, int days, int back,ArrayList<PWC> pw1, ArrayList<PWC> pw2) 
	{
		double conf=0;
		int supp=0;
		int totalpw2=0;
		ArrayList<PWC> pw1all = new ArrayList<PWC>();
		ArrayList<PWC> pw2all = new ArrayList<PWC>();
		for (int i=0;i<pw1.size();i++){
			PWC epc =  pw1.get(i);
			int sday=Math.max(0, epc.start_date-back-days+1);
			int eday=Math.max(0, epc.start_date-back);
			if (i+1<pw1.size()) {
				int nextepcsday=Math.max(0,pw1.get(i+1).start_date-back-days+1);
				while ( nextepcsday <=eday && nextepcsday >=sday && i+1<pw1.size())  {
					epc = pw1.get(++i);
					eday=Math.max(0, epc.start_date-back);
					if (i+1<pw1.size()){
						nextepcsday = Math.max(0,pw1.get(i+1).start_date-back-days+1);
					}
				}
			}
			pw1all.add(new PWC(epc.BASE_DATE,sday,eday,Double.MAX_VALUE));
		}
		
		for (int i=0;i<pw2.size();i++){
			PWC epc =  pw2.get(i);
			int sday=Math.max(0, epc.start_date);
			int eday=Math.max(0, epc.end_date);
			if (i+1<pw2.size()) {
				int nextepcsday=pw2.get(i+1).start_date;
				while ( nextepcsday <=eday && nextepcsday >=sday && i+1<pw2.size())  {
					epc = pw2.get(++i);
					eday=epc.end_date;
					if (i+1<pw2.size()){
						nextepcsday = pw2.get(i+1).start_date;
					}
				}
			}
			pw2all.add(new PWC(epc.BASE_DATE,sday,eday,Double.MAX_VALUE));
		}

		for (PWC c2:pw2all){
			totalpw2 += c2.getlength();
			for (PWC c1:pw1all) {
				if (   (c2.start_date >= c1.start_date && c2.start_date <= c1.end_date) 
				    || (c1.start_date >= c2.start_date && c1.start_date <= c2.end_date)	)
				{
					supp+=Math.min(c1.end_date, c2.end_date)- Math.max(c1.start_date, c2.start_date) +1;
				}
			}
		}
		
		if (totalpw2>0) {conf= (double)supp / (double) totalpw2 ;}
		return new PWLocation(id,supp,totalpw2,conf);
	}
	
	//support values are the total number of EPWCs which start between 5~10 days before any EPC
	public static PWLocation findLocSupport2(int id, int totaldays, int days, int back, ArrayList<PWC> pw1, ArrayList<PWC> pw2) 
	{
		double conf=0;
		int supp=0;
		int totalpw2=0;
		ArrayList<PWC> pw1all = new ArrayList<PWC>();
		for (int i=0;i<pw1.size();i++){
			PWC epc =  pw1.get(i);
			int sday=Math.max(0, epc.start_date-back-days+1);
			int eday=Math.max(0, epc.start_date-back);
			if (i+1<pw1.size()) {
				int nextepcsday=Math.max(0,pw1.get(i+1).start_date-back-days+1);
				while ( nextepcsday <=eday && nextepcsday >=sday && i+1<pw1.size())  {
					epc = pw1.get(++i);
					eday=Math.max(0, epc.start_date-back);
					if (i+1<pw1.size()){
						nextepcsday = Math.max(0,pw1.get(i+1).start_date-back-days+1);
					}
				}
			}
			pw1all.add(new PWC(epc.BASE_DATE, sday,eday,Double.MAX_VALUE));
		}

		for (PWC c2:pw2){
			totalpw2 ++;
			for (PWC c1:pw1all) {
				if (   (c2.start_date >= c1.start_date && c2.start_date <= c1.end_date) )
				{
					supp++;
				}
			}
		}
		
		if (totalpw2>0) {conf= (double)supp / (double) totalpw2 ;}
		return new PWLocation(id,supp,totalpw2,conf);
	}

	
	public static void SortLocbyID(ArrayList<PWLocation> loclist){
    	Collections.sort(loclist, new Comparator<PWLocation>() { //low to high
    		public int compare(PWLocation o1, PWLocation o2){ 
    				return (o1.ID-o2.ID);
    			}  
    		}
    	);    	
    }
	
	public static void SortLocbySupport(ArrayList<PWLocation> loclist){
    	Collections.sort(loclist, new Comparator<PWLocation>() { // high to low
    		public int compare(PWLocation o1, PWLocation o2){ 
    				return (o2.support-o1.support);
    			}  
    		}
    	);    	
    }
	
	public static void SortLocbyConfidence(ArrayList<PWLocation> loclist){
    	Collections.sort(loclist); 	
    }
	
	public static ArrayList<PWLocation> LoadLocData(String filename, String delimit) throws IOException {
    	ArrayList<PWLocation> Locs = new ArrayList<PWLocation>();
		BufferedReader reader =new BufferedReader(new FileReader(filename));
		String line="";
		while ((line = reader.readLine()) != null) {   	
			String[] values = line.split(delimit);
			Locs.add(new PWLocation(Integer.parseInt(values[0]),Integer.parseInt(values[1]),Integer.parseInt(values[2]),Double.parseDouble(values[3])));
		}
		reader.close();
		return Locs;
    }
	
	public static int[] getIds(ArrayList<PWLocation> loclist){
		int[] ret=null;
		if (loclist.size()>0){
			ret = new int[loclist.size()];
			for (int i=0;i<loclist.size();i++) {
				ret[i]=loclist.get(i).ID;
			}
		}
		return ret;
	}
	
	public static void StoreLocData(ArrayList<PWLocation> loclist, String filename) throws IOException {
    	ArrayList<PWLocation> Locs =loclist;
		BufferedWriter outPCData =new BufferedWriter(new FileWriter(filename));
		//System.out.println(PCs.size());
		if (Locs.size()>0) {
			outPCData.write(Locs.get(0)+"");
			for (int i=1; i<Locs.size(); i++){
				//System.out.println(p);
				outPCData.write("\n" + Locs.get(i));
			}
			outPCData.close();
		}
    }
	
	public static void CreateLocFile(ArrayList<PWC> AllEPCs, String featureFilename, String delimit2, String outFile, int maxNonePCDays, int minPCDays, int back,int backdays
			) throws IOException {
		
		ArrayList<PWLocation> loclist = new ArrayList<PWLocation>();
		double[][] PWdata = DataLoader.loadingData(featureFilename, delimit2, 
				flooding_prediction.totalSampleLocations,
				(int)flooding_prediction.Data_start_day,
				(int)flooding_prediction.Data_end_day);
		double PW20p = StdStats.percentile(PWdata,flooding_prediction.lowPercentile); 
		double PW60p = StdStats.percentile(PWdata,flooding_prediction.PCPercentile);
		double PW90p = StdStats.percentile(PWdata,flooding_prediction.EPCPercentile);;
		for (int loc=0;loc<flooding_prediction.totalSampleLocations;loc++) {
			ArrayList<PWC> PWEPClist = PWC.FindPWCs(flooding_prediction.Start_Date,PWdata[loc]
					,PW20p,PW60p,maxNonePCDays,minPCDays); 
			PWEPClist = PWC.PWCRangeByAverage(PWEPClist, PW90p,Double.MAX_VALUE,"EPC");
			PWC.SortPWCbyStartDate(PWEPClist);
			//System.out.println(loc+":="+PWEPClist.size());
			PWLocation plc = findLocSupport(loc,flooding_prediction.totalDays, backdays, back,AllEPCs,PWEPClist);
			loclist.add(plc);
		}
		SortLocbySupport(loclist);
		StoreLocData(loclist, outFile);
		//System.out.println("loclist:="+loclist.size());
		//System.out.println(StdStats.percentile(PWdata, 0.9));
	}
	
	public static void CreateLocFile(ArrayList<PWC> AllEPCs, String featureFilename, String delimit2, String outFile, int maxNonePCDays, int minPCDays, int back,int backdays
			,double PW20p, double PW60p,double PW90p) throws IOException {
		
		ArrayList<PWLocation> loclist = new ArrayList<PWLocation>();
		double[][] PWdata = DataLoader.loadingData(featureFilename, delimit2, flooding_prediction.totalSampleLocations
				,(int) flooding_prediction.Data_start_day
				,(int) flooding_prediction.Data_end_day);
		for (int loc=0;loc<flooding_prediction.totalSampleLocations;loc++) {
			ArrayList<PWC> PWEPClist = PWC.FindPWCs(flooding_prediction.Start_Date,PWdata[loc],PW20p,PW60p,maxNonePCDays,minPCDays); 
			PWEPClist = PWC.PWCRangeByAverage(PWEPClist, PW90p,Double.MAX_VALUE,"EPC");
			PWC.SortPWCbyStartDate(PWEPClist);
			//System.out.println(loc+":="+PWEPClist.size());
			PWLocation plc = findLocSupport(loc,flooding_prediction.totalDays, backdays, back,AllEPCs,PWEPClist);
			loclist.add(plc);
		}
		SortLocbySupport(loclist);
		StoreLocData(loclist, outFile);
		//System.out.println("loclist:="+loclist.size());
		//System.out.println(StdStats.percentile(PWdata, 0.9));
	}
	
	public static void CreateLocFile2(ArrayList<PWC> AllEPCs, String featureFilename, String delimit2, String outFile, int maxNonePCDays, int minPCDays, int back,int backdays
			,double PW20p, double PW60p,double PW90p) throws IOException {

		ArrayList<PWLocation> loclist = new ArrayList<PWLocation>();
		double[][] PWdata = DataLoader.loadingData(featureFilename, delimit2, flooding_prediction.totalSampleLocations
				, (int)flooding_prediction.Data_start_day
				, (int)flooding_prediction.Data_end_day);
		for (int loc=0;loc<flooding_prediction.totalSampleLocations;loc++) {
			ArrayList<PWC> PWEPClist = PWC.FindPWCs(flooding_prediction.Start_Date, PWdata[loc],PW20p,PW60p,maxNonePCDays,minPCDays); 
			PWEPClist = PWC.PWCRangeByAverage(PWEPClist, PW90p,Double.MAX_VALUE,"EPC");
			PWC.SortPWCbyStartDate(PWEPClist);
			//System.out.println(loc+":="+PWEPClist.size());
			PWLocation plc = findLocSupport2(loc,flooding_prediction.totalDays, backdays, back,AllEPCs,PWEPClist);
			loclist.add(plc);
		}
		SortLocbySupport(loclist);
		StoreLocData(loclist, outFile);
		//System.out.println("loclist:="+loclist.size());
		//System.out.println(StdStats.percentile(PWdata, 0.9));
	}

	public static ArrayList<PWLocation> LOCRangeBySupport(ArrayList<PWLocation> list, int from, int to){
    	ArrayList<PWLocation> Locs = new ArrayList<PWLocation>();
    	
    	for (PWLocation loc:list){
    		if ((loc.support >= from) && (loc.support < to)) {
    			Locs.add(new PWLocation(loc));
    		}
    	}
    	return Locs;
    }
	
	public static ArrayList<PWLocation> LOCRangeByConfidence(ArrayList<PWLocation> list, double from, double to){
    	ArrayList<PWLocation> Locs = new ArrayList<PWLocation>();
    	
    	for (PWLocation loc:list){
    		if (loc.confidence >= from && loc.confidence < to) {
    			Locs.add(new PWLocation(loc));
    		}
    	}
    	return Locs;
    }
	
	public static ArrayList<PWLocation> RandomSelection(ArrayList<PWLocation> list, int num, int seed){
    	ArrayList<PWLocation> Locs = new ArrayList<PWLocation>(list);
    	
    	Random rn = new Random(seed);
    	while (Locs.size() > num) {
    		Locs.remove(rn.nextInt(Locs.size()));
    	}
    	return Locs;
    	
    }
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
