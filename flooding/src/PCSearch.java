import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Random;


public class PCSearch {
	private static String delimit = "\\s+";
	private int[] ids = {3942, 3979};
	private int minPCDays = 7, maxNonePCDays = 2;
	private double lowThreshold = 0, PCThreshold = 0.027272727;	//0.29772727
	private String dataFile = null, outFile = "PC.dat";
	
	public PCSearch(String dataFile) {
		this.dataFile = dataFile;
	}

	public void setIds(int[] ids) {
		this.ids = ids;
	}
	
	public void setThresholds(double lowThreshold, double PCThreshold) {
		this.lowThreshold = lowThreshold;
		this.PCThreshold = PCThreshold;
	}
	
	public void setLowThreshold(double lowThreshold) {
		this.lowThreshold = lowThreshold;
	}
	
	public void setPCThreshold(double PCThreshold) {
		this.PCThreshold = PCThreshold;
	}
	
	public void setOutFile(String outFile) {
		this.outFile = outFile;
	}

	public void setMinPCDays(int minPCDays) {
		this.minPCDays = minPCDays;
	}
	
	public void setMaxNonePCDays(int maxNonePCDays) {
		this.maxNonePCDays = maxNonePCDays;
	}
	
	public void doSearch(boolean searchForIowa) throws IOException {
		if (!searchForIowa && ids == null) return;
		ArrayList<Integer> PCStartDays = new ArrayList<Integer>();
		ArrayList<Integer> PCDays = new ArrayList<Integer>();
		ArrayList<Double> PCAvgs = new ArrayList<Double>();
		
		ArrayList<Double> PWs = new ArrayList<Double>();
    	String line="";
		BufferedReader reader = new BufferedReader(new FileReader(dataFile));
		BufferedWriter out = new BufferedWriter(new FileWriter(outFile+".txt"));
		if (searchForIowa) { // dataFile contains only PW fro Iowa
			while ((line = reader.readLine()) != null) {
				String[] values = line.split(delimit);
				PWs.add(Double.parseDouble(values[1]));	//skip values[0] which is empty
			}
		} else { // dataFile contains PWs for all sample location
			for (int i= 0;i<ids.length;i++)
				out.write(ids[i]+" ");
			out.write("\r\n");
			while ((line = reader.readLine()) != null) {
				double value = 0;
				String[] values = line.split(delimit);
				for ( int i=0;i<ids.length;i++)
					value += Double.parseDouble(values[ids[i]]);	//skip values[0] which is empty
				value /= (double)ids.length;
				PWs.add(value);
			}
		}
		int totalDays = PWs.size();
		boolean[] validForPC = new boolean[totalDays];
		int lastValid = -1;
		for (int i=0;i<maxNonePCDays+1;i++) {
			if (PWs.get(i)>lowThreshold)
				lastValid = i;
		}
		if (lastValid>=0) {
			for (int i=0;i<=lastValid;i++)
				validForPC[i] = true;
		} else {
			for (int i=0;i<maxNonePCDays+1;i++)
				validForPC[i] = false;
		}
		for (int i=maxNonePCDays+1;i<totalDays;i++) {
			if (PWs.get(i)>lowThreshold) {
				validForPC[i] = true;
				if (lastValid>=0) {
					for (int j=lastValid+1;j<maxNonePCDays+1;j++)
						validForPC[i-maxNonePCDays-1+j] = true;
					lastValid = maxNonePCDays;
				}
			} else {
				if (lastValid<0)
					validForPC[i] = false;
				else {
					if (lastValid == 0)
						for (int j=1;j<maxNonePCDays+2;j++)
							validForPC[i-maxNonePCDays-1+j] = false;
					lastValid--;
				}
			}
			
		}
		if (lastValid>=0) {
			for (int j=lastValid+1;j<maxNonePCDays+2;j++)
				validForPC[totalDays-1-maxNonePCDays-1+j] = true;
		}
		
		boolean[] validForPCSum = new boolean[totalDays];
		for (int i=0;i<totalDays;i++)
			validForPCSum[i] = validForPC[i];
		
		boolean foundPC = false;
		int days = minPCDays-1;
		double PCSum0 = 0;
		for (int i=0;i<days;i++) {
			PCSum0 += PWs.get(i);
			if (!validForPC[i])
				validForPCSum[0] = false;
		}
		do {
			foundPC = false;
			PCSum0 += PWs.get(days);
			double PCSum = PCSum0;
			for (int i=0;i<totalDays-days;i++) {
				if (i>0)
					PCSum = PCSum - PWs.get(i-1) + PWs.get(i+days);
				else {
					if (!validForPC[days]) {
						validForPCSum[0] = false;
						continue;
					}
				}
				if (!validForPC[i+days])
					validForPCSum[i] = false;
				if (!validForPCSum[i]) continue;
				double PCAvg = PCSum/(double)(days+1);
				if (PCAvg > PCThreshold) {
					out.write((i+1)+" "+(days+1)+" "+PCAvg+"\r\n");
					foundPC = true;
					PCStartDays.add(i+1); PCDays.add(days+1); PCAvgs.add(PCAvg);
				}
			}
			days++;
		} while (foundPC && days<totalDays);
		reader.close();
		out.close();
		
		// remove short PCs that covered by longer ones and save another file
		ArrayList<Integer> StartDays = new ArrayList<Integer>();
		ArrayList<Integer> EndDays = new ArrayList<Integer>();
		ArrayList<Integer> predictStartDays = new ArrayList<Integer>();
		ArrayList<Integer> predictEndDays = new ArrayList<Integer>();
		out = new BufferedWriter(new FileWriter(outFile+".less.txt"));
		for (int i=PCStartDays.size()-1;i>=0;i--) {
			int startDay = PCStartDays.get(i); days = PCDays.get(i);
			if (insertRange(StartDays, EndDays, startDay, startDay+days-1)) {
				out.write(startDay+" "+days+" "+PCAvgs.get(i)+"\r\n");
			}
			insertRange(predictStartDays, predictEndDays, Math.max(startDay-10, 1), Math.max(startDay+days-1-12, 1));
		}
		out.close();
		System.out.println(getRangeDays(StartDays, EndDays));
		System.out.println(getRangeDays(predictStartDays, predictEndDays));
	}
	
	private static boolean insertRange(ArrayList<Integer> StartDays, ArrayList<Integer> EndDays, int startDay, int endDay) {
		if (StartDays.size() == 0) {
			StartDays.add(startDay); EndDays.add(endDay);
			return true;
		}
		for (int i=0;i<StartDays.size();i++) {
			int s = StartDays.get(i), e = EndDays.get(i);
			if (endDay<s-1) {
				StartDays.add(i, startDay); EndDays.add(i, endDay);
				return true;
			}
			if (startDay<s && endDay>=s-1 && endDay<e) {
				StartDays.set(i, startDay);
				return true;
			}
			if (startDay>=s && endDay<=e) return false;
			if (startDay>=s && startDay<= e+1 && endDay>e) {
				int snext = Integer.MAX_VALUE;
				if (i+1<StartDays.size())
					snext = StartDays.get(i+1);
				if (endDay>=snext-1) {
					EndDays.set(i, EndDays.get(i+1));
					StartDays.remove(i+1); EndDays.remove(i+1);
					return true;
				} else {
					EndDays.set(i, endDay);
					return true;
				}
			}
			if (startDay>e+1) {
				int snext = Integer.MAX_VALUE;
				if (i+1<StartDays.size())
					snext = StartDays.get(i+1);
				if (startDay<snext && endDay>=snext-1) {
					StartDays.set(i+1, startDay);
					return true;
				} else if (endDay<snext-1 || snext == Integer.MAX_VALUE){
					StartDays.add(i+1, startDay); EndDays.add(i+1, endDay);
					return true;
				}
			}
		}
		return false;
	}
	
	public static void getLocalDistributions(String dataFile, String outFile) {
		int totalDays = 7595, numLocation = 5328;
		double[][] PWs = new double[numLocation][totalDays];
		String line="";
		int days = 0;
		try {
			BufferedReader reader = new BufferedReader(new FileReader(dataFile));
			while ((line = reader.readLine()) != null) {
				String[] values = line.split(delimit);
				for (int i=0;i<numLocation;i++)
					PWs[i][days] = Double.parseDouble(values[i+1]);	//skip values[0] which is empty
				days++;
			}
			reader.close();

			BufferedWriter out = new BufferedWriter(new FileWriter(outFile));
			for (int i=0;i<numLocation;i++) {
				Arrays.sort(PWs[i]);
				for (int j=5;j<100;j+=5) {
					out.write(PWs[i][totalDays*j/100]+" ");
				}
				out.write("\r\n");
			}
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void getGlobalDistributions(String dataFile, String outFile) {
		int totalDays = 7595, numLocation = 5328;
		double[] PWs = new double[numLocation*totalDays];
		String line="";
		int index = 0;
		try {
			BufferedReader reader = new BufferedReader(new FileReader(dataFile));
			while ((line = reader.readLine()) != null) {
				String[] values = line.split(delimit);
				for (int i=0;i<numLocation;i++)
					PWs[index++] = Double.parseDouble(values[i+1]);	//skip values[0] which is empty
			}
			reader.close();

			BufferedWriter out = new BufferedWriter(new FileWriter(outFile));
			Arrays.sort(PWs);
			for (int j=5;j<100;j+=5) {
				out.write(PWs[(int)((double)(numLocation)*totalDays*j/100)]+" ");
			}
			out.write("\r\n");
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void getIowaPWDistribution(String dataFile, String outFile) {
		int totalDays = getLineNumber(dataFile);
		System.out.println(dataFile+" has "+totalDays+" days");
		double[] IowaPWs = new double[totalDays];
		
		String delimit = "\\s+";
		try {
			BufferedReader reader = new BufferedReader(new FileReader(dataFile));
			String line="";
			int days = 0;
			while ((line = reader.readLine()) != null) {
				String[] values = line.split(delimit);
				IowaPWs[days] = Double.parseDouble(values[1]);	//skip values[0] which is empty
				days++;
			}
			reader.close();
			BufferedWriter out = new BufferedWriter(new FileWriter(outFile));
			Arrays.sort(IowaPWs);
			for (int j=5;j<100;j+=5) {
				out.write(IowaPWs[totalDays*j/100]+" ");
			}
			out.write("\r\n");
			out.close();
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void testLocations() {
		//String labelPCFile = "C:\\hub\\project\\cs480\\IowaPC_Precip_20-90.txt";
		String distributionFile = "/home/dluo/Documents/CS480/flooding/PC/PWDistribution.txt";
		String dataFile = "/home/dluo/Documents/CS480/flooding/data/PW.txt";
		String outFile = "/home/dluo/Documents/CS480/flooding/PC/range-";
		String delimit = "\\s+";
		/*int[] nearByIds = { 3867, 3868, 3869, 3870,
							3904, 3905, 3906, 3907,
							3941, 3942, 3943, 3944,
							3978, 3979, 3980, 3981};*/
		int[] locations = {887, 1511, 2328, 2579, 2953, 3094};
		int totalDays = 23011; int lowThresholdIndex = 3, PCThresholdIndex = 17;
		int maxNonePCDays = 1, minPCDays = 7;
		double[][] PWs = new double[locations.length][totalDays];
		double [][] THs = new double[locations.length][2];
		String line="";
		try {
			BufferedReader reader = new BufferedReader(new FileReader(dataFile));
			int days = 0;
			while ((line = reader.readLine()) != null) {
				String[] values = line.split(delimit);
				for (int i=0;i<locations.length;i++)
					PWs[i][days] = Double.parseDouble(values[locations[i]+1]);	//skip values[0] which is empty
				days++;
			}
			reader.close();
			
			line="";
			reader = new BufferedReader(new FileReader(distributionFile));
			int i = 0, j = 0;
			while (i<locations.length && (line = reader.readLine()) != null) {
				if ((j+1)==locations[i]) {
					String[] values = line.split(delimit);
					THs[i][0] = Double.parseDouble(values[lowThresholdIndex]);
					THs[i][1] = Double.parseDouble(values[PCThresholdIndex]);
					i++;
				}
				j++;
			}
			reader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		for (int l=0;l<locations.length;l++) {
			ArrayList<Integer> PCStartDays = new ArrayList<Integer>();
			ArrayList<Integer> PCEndDays = new ArrayList<Integer>();
			ArrayList<Integer> PCRangeStart = new ArrayList<Integer>();
			ArrayList<Integer> PCRangeEnd = new ArrayList<Integer>();
			int id = locations[l];
			double lowThreshold = THs[l][0], PCThreshold = THs[l][1];
			getPCsAll(PWs[l], lowThreshold, PCThreshold, maxNonePCDays, minPCDays,
					PCStartDays, PCEndDays, null, false, false);
			getPCsRange(PCStartDays, PCEndDays, PCRangeStart, PCRangeEnd);
			System.out.println(id+" "+getRangeDays(PCRangeStart, PCRangeEnd));
			try {
				BufferedWriter out = new BufferedWriter(new FileWriter(outFile+id+".txt"));
				for (int i=0;i<PCRangeStart.size();i++) {
					out.write(PCRangeStart.get(i)+" "+PCRangeEnd.get(i)+"\r\n");
				}
				out.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
	}

	public static void getPCsAll(double[] PWs, double lowThreshold, double PCThreshold, int maxNonePCDays, int minPCDays,
								ArrayList<Integer> PCStartDays, ArrayList<Integer> PCDays, ArrayList<Double> PCAvgs,
								boolean returnMean, boolean returnRange) {
		if (PWs == null) return;
		int totalDays = PWs.length;
		boolean[] validForPC = new boolean[totalDays];
		boolean[] validForPCSum = new boolean[totalDays];
		int lastValid = -1;
		for (int i=0;i<maxNonePCDays+1;i++) {
			if (PWs[i]>lowThreshold)
				lastValid = i;
		}
		if (lastValid>=0) {
			for (int i=0;i<=lastValid;i++)
				validForPC[i] = true;
		} else {
			for (int i=0;i<maxNonePCDays+1;i++)
				validForPC[i] = false;
		}
		for (int i=maxNonePCDays+1;i<totalDays;i++) {
			if (PWs[i]>lowThreshold) {
				validForPC[i] = true;
				if (lastValid>=0) {
					for (int j=lastValid+1;j<maxNonePCDays+1;j++)
						validForPC[i-maxNonePCDays-1+j] = true;
					lastValid = maxNonePCDays;
				}
			} else {
				if (lastValid<0)
					validForPC[i] = false;
				else {
					if (lastValid == 0)
						for (int j=1;j<maxNonePCDays+2;j++)
							validForPC[i-maxNonePCDays-1+j] = false;
					lastValid--;
				}
			}
			
		}
		if (lastValid>=0) {
			for (int j=lastValid+1;j<maxNonePCDays+2;j++)
				validForPC[totalDays-1-maxNonePCDays-1+j] = true;
		}
		
		for (int i=0;i<totalDays;i++)
			validForPCSum[i] = validForPC[i];
		
		boolean foundPC = false;
		int days = minPCDays-1;
		double PCSum0 = 0;
		for (int i=0;i<days;i++) {
			PCSum0 += PWs[i];
			if (!validForPC[i])
				validForPCSum[0] = false;
		}
		do {
			foundPC = false;
			PCSum0 += PWs[days];
			double PCSum = PCSum0;
			for (int i=0;i<totalDays-days;i++) {
				if (i>0)
					PCSum = PCSum - PWs[i-1] + PWs[i+days];
				else {
					if (!validForPC[days]) {
						validForPCSum[0] = false;
						continue;
					}
				}
				if (!validForPC[i+days])
					validForPCSum[i] = false;
				if (!validForPCSum[i]) continue;
				double PCAvg = PCSum/(double)(days+1);
				if (PCAvg > PCThreshold) {
					foundPC = true;
					if (!returnRange) {
						PCStartDays.add(i+1); PCDays.add(days+1);
						if (returnMean) PCAvgs.add(PCAvg);
					} else {
						insertRange(PCStartDays, PCDays, i+1, i+1+days);
						if (getRangeDays(PCStartDays, PCDays)>15000) {
							foundPC = false;
							break;
						}
					}
				}
			}
			days++;
		} while (foundPC && days<totalDays);
	}
	
	public static void getPCsLess(ArrayList<Integer> allStartDays, ArrayList<Integer> allDays, ArrayList<Double> allAvgs,
			ArrayList<Integer> lessStartDays, ArrayList<Integer> lessDays, ArrayList<Double> lessAvgs) {
		// remove short PCs that covered by longer ones and save another file
		ArrayList<Integer> StartDays = new ArrayList<Integer>();
		ArrayList<Integer> EndDays = new ArrayList<Integer>();
		boolean returnMean = false;
		if (allAvgs != null && allAvgs.size() == allStartDays.size())
			returnMean = true;
		for (int i=allStartDays.size()-1;i>=0;i--) {
			int startDay = allStartDays.get(i), days = allDays.get(i);
			if (insertRange(StartDays, EndDays, startDay, startDay+days-1)) {
				lessStartDays.add(startDay); lessDays.add(days);
				if (returnMean)
					lessAvgs.add(allAvgs.get(i));
			}
		}
	}
	
	public static void getPCsRange(ArrayList<Integer> PCStartDays, ArrayList<Integer> PCDays,
			ArrayList<Integer> rangeStartDays, ArrayList<Integer> rangeEndDays) {
		for (int i=PCStartDays.size()-1;i>=0;i--) {
			int startDay = PCStartDays.get(i), days = PCDays.get(i);
			insertRange(rangeStartDays, rangeEndDays, startDay, startDay+days-1);
		}
	}
	
	/**
	 * calculate the overlapping days for two range sets
	 * each range set is sorted and has no overlapping within the set
	 * @param rangeStart1
	 * @param rangeEnd1
	 * @param rangeStart2
	 * @param rangeEnd2
	 * @return
	 */
	public static int getRangeShareDays(ArrayList<Integer> rangeStart1, ArrayList<Integer> rangeEnd1, 
			ArrayList<Integer> rangeStart2, ArrayList<Integer> rangeEnd2) {
		int days = 0;
		int i = 0, j = 0;
		int s1 = rangeStart1.size(), s2 = rangeStart2.size();
		int start1 = 0, end1 = 0, start2 = 0, end2 = 0;
		
		boolean go1 = true, go2 = true;
		while (i<s1 && j<s2) {
			if (go1) {
				start1 = rangeStart1.get(i);
				end1 = rangeEnd1.get(i);
			}
			if (go2) {
				start2 = rangeStart2.get(j);
				end2 = rangeEnd2.get(j);
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
		/*
		for (int i=0;i<s1;i++) {
			start1 = rangeStart1.get(i);
			end1 = rangeEnd1.get(i);
			for (int j=0;j<s2;j++) {
				start2 = rangeStart2.get(j);
				end2 = rangeEnd2.get(j);
				if (start1<=start2 && end2<=end1)
					days += (end2-start2+1);
				else if (start1<=start2 && end1>=start2 && end1<=end2)
					days += (end1-start2+1);
				else if (start2<=start1 && end2>=start1 && end2<=end1)
					days += (end2-start1+1);
				else if (start2<=start1 && end1<=end2)
					days += (end1-start1+1);
			}
		}*/
		return days;
	}
	
	public static int getRangeDays(ArrayList<Integer> startDays, ArrayList<Integer> endDays) {
		int days = 0;
		for (int i=0;i<startDays.size();i++) {
			days += endDays.get(i)-startDays.get(i) + 1;
		}
		return days;
	}
	
	public static void getPCCounts() throws IOException {
		int totalDays = 23011, totalSampleLocations = 5328;
		int predictStartDay = 10, predictEndDay = 12, maxNonePCDays = 2, minPCDays = 7;
		int lowThresholdIndex = 3, PCThresholdIndex = 17;
		String IowaPrecipFile = "./data/text/PRECIP2_1948-2010.txt";
		//String IowaEPCFile = "C:\\hub\\project\\cs480\\IowaPC_Precip_20-90.txt";
		String distributionFile = "PWDistribution.txt";
		//String dataFile = "C:\\hub\\project\\cs480\\data\\PW.txt";
		String dataFile = "PWTranspose";
		String outFile = "PCCountsSorted_90.txt";
		/*
		String base = "/home/dluo/Documents/CS480/flooding/";
		String IowaPrecipFile = base+"data/PRECIP2_1948-2010.txt";
		//String IowaEPCFile = "C:\\hub\\project\\cs480\\IowaPC_Precip_20-90.txt";
		String distributionFile = base+"PC/PWDistribution.txt";
		String dataFile = base+"data/PW.txt";
		String outFile = base+"PC/PCCountsSorted_90.txt";
		*/
		String delimit = "\\s+";

		/* read Iowa EPCs ranges and calculate predict ranges */
		ArrayList<Integer> IowaEPCStart = new ArrayList<Integer>();
		ArrayList<Integer> IowaEPCEnd = new ArrayList<Integer>();
		ArrayList<Integer> IowaEPCRangeStart = new ArrayList<Integer>();
		ArrayList<Integer> IowaEPCRangeEnd = new ArrayList<Integer>();
		double[] IowaPWs = new double[totalDays];
		
		BufferedReader reader = new BufferedReader(new FileReader(IowaPrecipFile));
		String line="";
		int days = 0;
		while ((line = reader.readLine()) != null) {
			String[] values = line.split(delimit);
			IowaPWs[days] = Double.parseDouble(values[1]);	//skip values[0] which is empty
			days++;
		}
		reader.close();
		getPCsAll(IowaPWs, 0, 0.29772727, maxNonePCDays, minPCDays, 
				IowaEPCStart, IowaEPCEnd, null, false, false);
		for (int i=IowaEPCStart.size()-1;i>=0;i--) {
			int startDay = IowaEPCStart.get(i), endDay = startDay + IowaEPCEnd.get(i) - 1;
			insertRange(IowaEPCRangeStart, IowaEPCRangeEnd, Math.max(startDay-predictStartDay, 1), Math.max(endDay-predictEndDay, 1));
		}
		/*
		BufferedReader reader = new BufferedReader(new FileReader(IowaEPCFile));
		String line="";
		while ((line = reader.readLine()) != null) {
			String[] values = line.split(delimit);
			int startDay = Integer.parseInt(values[0]), endDay = startDay + Integer.parseInt(values[1]) - 1;
			insertRange(IowaEPCRangeStart, IowaEPCRangeEnd, startDay-predictStartDay, endDay-predictEndDay);
		}
		reader.close();
		*/
		int steps = 2, stepLocations = totalSampleLocations/steps;
		int[] PCCounts = new int[totalSampleLocations];
		int[] PCDays = new int[totalSampleLocations];
		
		/* read threshold values for all sample locations */
		
		double[][] thresholds = new double[totalSampleLocations][2];
		line="";
		reader = new BufferedReader(new FileReader(distributionFile));
		int id = 0;
		while ((line = reader.readLine()) != null) {
			String[] values = line.split(delimit);
			thresholds[id][0] = Double.parseDouble(values[lowThresholdIndex]);
			thresholds[id][1] = Double.parseDouble(values[PCThresholdIndex]);
			id++;
		}
		reader.close();
		
		//double[][] PWs = new double[stepLocations][totalDays];
		double[] PWs = new double[totalDays];
		for (int s=0;s<steps;s++) {
			/* read PW values for each sample location */
			line="";
			/*
			reader = new BufferedReader(new FileReader(dataFile));
			days = 0;
			while ((line = reader.readLine()) != null) {
				String[] values = line.split(delimit);
				for (int i=0;i<stepLocations;i++)
					PWs[i][days] = Double.parseDouble(values[s*stepLocations+i+1]);	//skip values[0] which is empty
				days++;
			}
			reader.close();
			*/
			
			/* search for PCs for each sample location and count overlap days with Iowa EPC predict ranges */
			/*for (int l=0;l<stepLocations;l++) {
				ArrayList<Integer> PCStartDays = new ArrayList<Integer>();
				ArrayList<Integer> PCEndDays = new ArrayList<Integer>();
				ArrayList<Integer> PCRangeStart = new ArrayList<Integer>();
				ArrayList<Integer> PCRangeEnd = new ArrayList<Integer>();
				id = l + s*stepLocations;
				//double lowThreshold = thresholds[id][0], PCThreshold = thresholds[id][1];
				double lowThreshold = 4.85, PCThreshold = 43.3;	// 20.07/43.3
				getPCsAll(PWs[l], lowThreshold, PCThreshold, maxNonePCDays, minPCDays,
						PCStartDays, PCEndDays, null, false);
				getPCsRange(PCStartDays, PCEndDays, PCRangeStart, PCRangeEnd);
				PCCounts[id] = getRangeShareDays(IowaEPCRangeStart, IowaEPCRangeEnd, PCRangeStart, PCRangeEnd);
				PCDays[id] = getRangeDays(PCRangeStart, PCRangeEnd);
			}*/
			
			reader = new BufferedReader(new FileReader(dataFile+"-"+s+".txt"));
			int l = 0;
			while (l<stepLocations && (line = reader.readLine()) != null) {
				String[] values = line.split(delimit);
				for (int i=0;i<totalDays;i++)
					PWs[i] = Double.parseDouble(values[i]);
				ArrayList<Integer> PCRangeStart = new ArrayList<Integer>();
				ArrayList<Integer> PCRangeEnd = new ArrayList<Integer>();
				id = l + s*stepLocations;
				//double lowThreshold = thresholds[id][0], PCThreshold = thresholds[id][1];
				//double lowThreshold = 4.85, PCThreshold = 43.3;	// 20.07/43.3
				double lowThreshold = 4.85, PCThreshold = thresholds[id][1];
				getPCsAll(PWs, lowThreshold, PCThreshold, maxNonePCDays, minPCDays,
						PCRangeStart, PCRangeEnd, null, false, true);
				PCCounts[id] = getRangeShareDays(IowaEPCRangeStart, IowaEPCRangeEnd, PCRangeStart, PCRangeEnd);
				PCDays[id] = getRangeDays(PCRangeStart, PCRangeEnd);
				if (PCDays[id]>15000) PCCounts[id] = 0;
				l++;
			}
			reader.close();
		}
		/* debug purpose
		System.out.println(getRangeDays(IowaEPCRangeStart, IowaEPCRangeEnd));
		BufferedWriter out = new BufferedWriter(new FileWriter(outFile+".raw.txt"));
		for (int i=0;i<totalSampleLocations;i++) {
			out.write((i+1)+" "+PCCounts[i]+" "+PCDays[i]+"\r\n");
		}
		out.close(); */
		
		/* Sort PCCounts and output location IDs in descending order
		 * To make it simple, I shift counts by x10000 and put ID at the end
		 * This way, the sorted results contains the ID information
		 */
		for (int i=0;i<totalSampleLocations;i++) PCCounts[i] = PCCounts[i]*10000+(i+1);
		Arrays.sort(PCCounts);
		ArrayList<Integer> keys = new ArrayList<Integer>();
		ArrayList<Integer> values = new ArrayList<Integer>();
		int key = -1, value = 0;
		for (int i=totalSampleLocations-1;i>=0;i--) {
			int count = PCCounts[i]/10000;
			if (key == count) {
				value++;
			} else {
				if (value>0) {
					keys.add(key); values.add(value);
				}
				key = count; value = 1;
			}
		}
		if (value>0) { // add last entry
			keys.add(key); values.add(value);
		}
		
		BufferedWriter out = new BufferedWriter(new FileWriter(outFile));
		int keyStart = totalSampleLocations-1;
		for (int i=0;i<values.size();i++) {
			key = keys.get(i); value = values.get(i);
			int[] confidence = new int[value];
			for (int j=0;j<value;j++) {
				id = PCCounts[keyStart-j]-key*10000;
				confidence[j] = PCDays[id-1]*10000+id;
			}
			Arrays.sort(confidence);
			for (int j=0;j<value;j++) {
				int PCDay = confidence[j]/10000;
				id = confidence[j]-PCDay*10000;
				out.write(id+" "+key+" "+PCDay+String.format(" %.2f\r\n", ((double)key/(double)PCDay)));
			}
			keyStart -= value;
		}
		out.close();
	}
	
	public static void resavePWs() {
		int totalDays = 23011, totalSampleLocations = 5328;
		String dataFile = "C:\\hub\\project\\cs480\\data\\PW.txt";
		String outFile = "C:\\hub\\project\\cs480\\PWTranspose";
		String delimit = "\\s+";
		
		int steps = 2, stepLocations = totalSampleLocations/steps;
		double[][] PWs = new double[stepLocations][totalDays];
		String line="";
		BufferedReader reader;
		BufferedWriter out;
		for (int s=0;s<steps;s++) {
			/* read PW values for each sample location */
			try {
				reader = new BufferedReader(new FileReader(dataFile));
				int days = 0;
				while ((line = reader.readLine()) != null) {
					String[] values = line.split(delimit);
					for (int i=0;i<stepLocations;i++)
						PWs[i][days] = Double.parseDouble(values[s*stepLocations+i+1]);	//skip values[0] which is empty
					days++;
				}
				reader.close();
				out = new BufferedWriter(new FileWriter(outFile+"-"+s+".txt"));
				for (int i=0;i<stepLocations;i++) {
					for (int j=0;j<totalDays;j++) {
						out.write(PWs[i][j]+" ");
					}
					out.write("\r\n");
				}
				out.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public static void getIowaPCs() {
		double lowThreshold = 0;
		double[] ths = {0.027272727, 0.041818182, 0.062727273, 0.092727273, 0.1355, 0.19909091};
		//double PCThreshold = 0.092727273; // 0.027272727 / 0.29772727
		String outFile = "C:\\hub\\project\\cs480\\IowaPC_";
		String IowaPrecipFile = "C:\\hub\\project\\cs480\\data\\PRECIP2_1948-2010.txt";
		String delimit = "\\s+";
		
		int totalDays = 23011;
		double[] IowaPWs = new double[totalDays];
		
		try {
			BufferedReader reader = new BufferedReader(new FileReader(IowaPrecipFile));
			String line="";
			int days = 0;
			while ((line = reader.readLine()) != null) {
				String[] values = line.split(delimit);
				IowaPWs[days] = Double.parseDouble(values[1]);	//skip values[0] which is empty
				days++;
			}
			reader.close();
			for (int s=1;s<6;s++)
			for (int j=0;j<ths.length-s;j++) {
				ArrayList<Integer> PCStartDays = new ArrayList<Integer>(), PCEndDays = new ArrayList<Integer>();
				ArrayList<Integer> PCStartDaysS = new ArrayList<Integer>(), PCDaysS = new ArrayList<Integer>();
				ArrayList<Double> PCAvgs = new ArrayList<Double>(), PCAvgsS = new ArrayList<Double>();
				getPCsAll(IowaPWs, lowThreshold, ths[j]/*, ths[j+s]*/, 1, 7, PCStartDays, PCEndDays, PCAvgs, true, false);
				getPCsLess(PCStartDays, PCEndDays, PCAvgs, PCStartDaysS, PCDaysS, PCAvgsS);
				BufferedWriter out = new BufferedWriter(new FileWriter(outFile+(60+5*j)+"-"+(60+5*s+5*j)+".txt"));
				for (int i=0;i<PCStartDaysS.size();i++) {
					out.write(PCStartDaysS.get(i)+" "+PCDaysS.get(i)+" "+PCAvgs.get(i)+"\r\n");
				}
				out.close();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void cleanIowaPC() {
		String IowaEPCFile = "C:\\hub\\project\\cs480\\IowaPC_Precip_20-90.txt";
		String IowaPCFile = "C:\\hub\\project\\cs480\\IowaPC_";
		String outFile = "C:\\hub\\project\\cs480\\IowaPC_2_";
		String delimit = "\\s+";

		ArrayList<Integer> EPCStart = new ArrayList<Integer>(), EPCEnd = new ArrayList<Integer>();
		ArrayList<Integer> PCStart = new ArrayList<Integer>(), PCEnd = new ArrayList<Integer>();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(IowaEPCFile));
			String line="";
			while ((line = reader.readLine()) != null) {
				String[] values = line.split(delimit);
				int start = Integer.parseInt(values[0]);
				int end = start+Integer.parseInt(values[1])-1;
				insertRange(EPCStart, EPCEnd, start, end);
			}
			reader.close();
			for (int s=1;s<6;s++)
			for (int j=0;j<6-s;j++) {
				String ext = (60+5*j)+"-"+(60+5*s+5*j)+".txt";
				reader = new BufferedReader(new FileReader(IowaPCFile+ext));
				BufferedWriter out = new BufferedWriter(new FileWriter(outFile+ext));
				line = "";
				while ((line = reader.readLine()) != null) {
					String[] values = line.split(delimit);
					int start = Integer.parseInt(values[0]);
					int end = start+Integer.parseInt(values[1])-1;
					ArrayList<Integer> pcs = new ArrayList<Integer>(); pcs.add(start);
					ArrayList<Integer> pce = new ArrayList<Integer>(); pce.add(end);
					if (getRangeShareDays(EPCStart, EPCEnd, pcs, pce) == 0) {
						out.write(line+"\r\n");
						insertRange(PCStart, PCEnd, start, end);
					}
				}
				reader.close();
				out.close();
			}
			System.out.println("EPC and PC overlap days: "+getRangeShareDays(EPCStart, EPCEnd, PCStart, PCEnd));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void getRandomIds(int totalIds, int[] ids) {
		if (ids == null || ids.length>totalIds) return;
		int[] all = new int[totalIds];
		for (int i=0;i<totalIds;i++) all[i] = i+1;
		Random r = new Random();
		for (int i=0;i<ids.length;i++) {
			int index = r.nextInt(totalIds-i);
			ids[i] = all[index];
			all[index] = all[totalIds-1-i];
		}
	}
	
	public static void createWekaFile() {
		String[] features = {"Z1000", "T850", "PW", "U300", "U850", "V300", "V850", "Z300", "Z500"};
		String[] featureFiles = {
				"./data/text/Z1000.txt",
				"./data/text/T850.txt",
				"./data/text/PW.txt",
				"./data/text/U300.txt",
				"./data/text/U850.txt",
				"./data/text/V300.txt",
				"./data/text/V850.txt",
				"./data/text/Z300.txt",
				"./data/text/Z500.txt"
		};
		String delimit = "\\s+";
		String outFile = "./EPC_arff/EPC2";
		
		String idFile = "locations.txt";
		//String idFile = "C:\\hub\\project\\cs480\\PCCountsSorted_90_united.txt";
		//String EPCFile = "IowaEPC_Precip_20-90.txt";
		//String PCFile = "IowaPC_2_60-65.txt";
        String EPCFile = "IowaEPCs.txt";
		String PCFile = "IowaPCs.txt";
		int idNum = getLineNumber(idFile);
		int EPCNum = getLineNumber(EPCFile);
		int PCNum = getLineNumber(PCFile);
		System.out.println("idNum: "+getLineNumber(idFile)+"; EPCNum: "+getLineNumber(EPCFile)+"; PCNum: "+getLineNumber(PCFile));
		int[] ids = new int[idNum], EPCStart = new int[EPCNum], PCStart = new int[PCNum];
		//getRandomIds(5328, ids);
		getColumnData(idFile, 0, ids); getColumnData(EPCFile, 0, EPCStart); getColumnData(PCFile, 0, PCStart);
		Arrays.sort(ids); Arrays.sort(EPCStart); Arrays.sort(PCStart);
		
		double[][][] dataForEPC = new double[idNum][featureFiles.length][6*EPCNum];
		double[][][] dataForPC = new double[idNum][featureFiles.length][6*PCNum];
		try {
			for (int f=0;f<featureFiles.length;f++) {
				BufferedReader reader = new BufferedReader(new FileReader(featureFiles[f]));
				String line="";
				int day = 1, EPCi = 0, PCi = 0;
				int EPCs = EPCStart[0]-10, EPCe = EPCs+5;
				int PCs = PCStart[0]-10, PCe = PCs+5;
				while ((line = reader.readLine()) != null) {
					String[] values = line.split(delimit);
					if (day>=EPCs && day<=EPCe) {
						for (int r=EPCi;r<Math.min(EPCi+6, EPCNum);r++) {
							int EPCsr = EPCStart[r]-10;
							if (day<EPCsr) break;
							int index = r*6+(day-EPCsr);
							int i=0;
							for (int id:ids) {
								dataForEPC[i][f][index] = Double.parseDouble(values[id]);	// skip 1st value
								i++;
							}
						}
					} else if (day>EPCe && EPCi<EPCNum) {
						EPCi++;
						if (EPCi<EPCNum) {
							EPCs = EPCStart[EPCi]-10;
							EPCe = EPCs+5;
						}
					}
					
					if (day>=PCs && day<=PCe) {
						for (int r=PCi;r<Math.min(PCi+6, PCNum);r++) {
							int PCsr = PCStart[r]-10;
							if (day<PCsr) break;
							int index = r*6+(day-PCsr);
							int i=0;
							for (int id:ids) {
								dataForPC[i][f][index] = Double.parseDouble(values[id]);	// skip 1st value
								i++;
							}
						} 
					} else if (day>PCe && PCi<PCNum) {
						PCi++;
						if (PCi<PCNum) {
							PCs = PCStart[PCi]-10;
							PCe = PCs+5;
						}
					}
					if (EPCi>=EPCNum && PCi>=PCNum)
						break;
					day++;
				}
				reader.close();
			}
			/*
			for (int f=2;f<featureFiles.length;f++) {
				String ext = features[0]+"-"+features[1]+"-"+features[f];
				BufferedWriter out = new BufferedWriter(new FileWriter(outFile+"_"+ext+".arff"));
				out.write("@relation EPC_"+ext+"\r\n\r\n");
				for (int i=0;i<idNum;i++) 
					for (int fi=0;fi<3;fi++)
						for (int di=0;di<6;di++) {
							String feature = (fi<2)?features[fi]:features[f];
							out.write("@attribute "+feature+"_"+ids[i]+"_"+(10-di)+" numeric\r\n");
						}
				out.write("@attribute class {EPC, PC}\r\n\r\n@data\r\n");
				for (int e=0;e<EPCNum;e++) {
					for (int i=0;i<idNum;i++)
						for (int fi=0;fi<3;fi++)
							for (int di=0;di<6;di++) {
								out.write(dataForEPC[i][(fi<2)?fi:f][6*e+di]+",");
							}
					out.write("EPC\r\n");
				}
				for (int e=0;e<PCNum;e++) {
					for (int i=0;i<idNum;i++)
						for (int fi=0;fi<3;fi++)
							for (int di=0;di<6;di++) {
								int feature = (fi<2)?fi:f;
								out.write(dataForPC[i][feature][6*e+di]+",");
							}
					out.write("PC\r\n");
				}
				out.close();
			}
			*/
			String ext = "all-features";
			BufferedWriter out = new BufferedWriter(new FileWriter(outFile+"_"+ext+".arff"));
			out.write("@relation EPC_"+ext+"\r\n\r\n");
			for (int f=0;f<features.length;f++)
				for (int i=0;i<idNum;i++) 
					for (int di=0;di<6;di++) {
						out.write("@attribute "+features[f]+"_"+ids[i]+"_"+(10-di)+" numeric\r\n");
					}
			out.write("@attribute class {EPC, PC}\r\n\r\n@data\r\n");
			for (int e=0;e<EPCNum;e++) {
				for (int f=0;f<features.length;f++)
					for (int i=0;i<idNum;i++)
						for (int di=0;di<6;di++) {
							out.write(dataForEPC[i][f][6*e+di]+",");
						}
				out.write("EPC\r\n");
			}
			for (int e=0;e<PCNum;e++) {
				for (int f=0;f<features.length;f++)
					for (int i=0;i<idNum;i++)
						for (int di=0;di<6;di++) {
							out.write(dataForPC[i][f][6*e+di]+",");
						}
				out.write("PC\r\n");
			}
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	/**
	 * 
	 * @param file
	 * @param column 0 indexed
	 * @param data
	 */
	public static void getColumnData(String file, int column, int[] data) {
		String delimit = "\\s+";
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			String line="";
			int i = 0;
			while ((line = reader.readLine()) != null) {
				String[] values = line.split(delimit);
				data[i] = Integer.parseInt(values[column]);
				i++;
			}
			reader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static int getLineNumber(String file) {
		int ret = 0;
		try {
			LineNumberReader  reader = new LineNumberReader(new FileReader(file));
			reader.skip(Long.MAX_VALUE);
			ret = reader.getLineNumber();
			reader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ret;
	}
	
	public static void printHeader() {
		String[] featureFiles = {
				"C:\\hub\\project\\cs480\\data\\Z1000.txt",
				"C:\\hub\\project\\cs480\\data\\T850.txt",
				"C:\\hub\\project\\cs480\\data\\PW.txt",
				"C:\\hub\\project\\cs480\\data\\U300.txt",
				"C:\\hub\\project\\cs480\\data\\U850.txt",
				"C:\\hub\\project\\cs480\\data\\V300.txt",
				"C:\\hub\\project\\cs480\\data\\V850.txt",
				"C:\\hub\\project\\cs480\\data\\Z300.txt",
				"C:\\hub\\project\\cs480\\data\\Z500.txt"
		};
		String delimit = "\\s+";
		
		try {
			for (int i=0;i<featureFiles.length;i++) {
			BufferedReader reader = new BufferedReader(new FileReader(featureFiles[i]));
			String line="";
			while ((line = reader.readLine()) != null) {
				String[] values = line.split(delimit);
				System.out.println("v0: "+values[0]+"; v1: "+values[1]+"; v2: "+values[2]);
				break;
			}
			reader.close();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private static int[][] floodingStartDates = {
		{1984, 6, 7}, {1987, 5, 26}, {1988, 7, 15}, {1990, 5, 18}, {1990, 7, 25}, 
		{1991, 6, 1}, {1992, 9, 14}, {1993, 3, 26}, {1993, 4, 13}, {1996, 5, 8},
		{1996, 6, 15}, {1998, 6, 13}, {1999, 5, 16}, {1999, 7, 2}, {2001, 4, 8}, {2002, 6, 3}, 
		{2004, 5, 19}, {2007, 5, 5}, {2007, 8, 17}, {2008, 5, 25}, {2010, 5, 12}, {2010, 6, 1}
	};

	private static int[][] floodingEndDates = {
		{1984, 6, 8}, {1987, 5, 31}, {1988, 7, 16}, {1990, 7, 6}, {1990, 8, 31}, 
		{1991, 6, 15}, {1992, 9, 15}, {1993, 4, 12}, {1993, 10, 1}, {1996, 5, 28},
		{1996, 6, 30}, {1998, 7, 15}, {1999, 5, 29}, {1999, 8, 10}, {2001, 5, 29}, {2002, 6, 25}, 
		{2004, 6, 24}, {2007, 5, 7}, {2007, 9, 5}, {2008, 8, 13}, {2010, 5, 13}, {2010, 8, 31}
	};

	public static void floodingEventCoverage() {
		Calendar Base_Date = new GregorianCalendar(1980, 0, 1); // first date's ID starts with 0
		String IowaPrecipFile = "./data/text/PRECIP2_1980-2010.txt";
		Calendar Start_Date = new GregorianCalendar(1980, 0, 1);
		Calendar End_Date = new GregorianCalendar(2010, 11, 31);
		long Data_start_day =(Start_Date.getTimeInMillis()-Base_Date.getTimeInMillis())/(1000 * 60 * 60 * 24);
		long Data_end_day = (End_Date.getTimeInMillis()-Base_Date.getTimeInMillis())/(1000 * 60 * 60 * 24);
		int trainData_start_year = 1980, trainData_end_year = 2010;
		int start_month = 3, end_month = 10;

		// load Iowa Precipitated water data
		double[] IowaPWs = readIowaPWData(IowaPrecipFile, Data_start_day, Data_end_day);

		double percentile_step = 0.01;
		double[] percentilesIowa = null;
		try {
			percentilesIowa = StdStats.percentiles(IowaPWs, percentile_step);
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		double[] EPCPercentiles = {0.8, 0.81, 0.82, 0.83, 0.84};
		double[] lowPercentiles = {0.35, 0.36, 0.37, 0.38, 0.39};
		int maxNonePCDays = 2, minPCDays = 5;
		int[] floodingStartDays = new int[floodingStartDates.length];
		int[] floodingEndDays = new int[floodingEndDates.length];
		for (int i=0;i<floodingStartDays.length;i++) {
			Calendar Date;
			Date = new GregorianCalendar(floodingStartDates[i][0], floodingStartDates[i][1]-1, floodingStartDates[i][2]);
			floodingStartDays[i] = (int) ((Date.getTimeInMillis()-Base_Date.getTimeInMillis())/(1000 * 60 * 60 * 24));
			Date = new GregorianCalendar(floodingEndDates[i][0], floodingEndDates[i][1]-1, floodingEndDates[i][2]);
			floodingEndDays[i] = (int) ((Date.getTimeInMillis()-Base_Date.getTimeInMillis())/(1000 * 60 * 60 * 24));
		}
		System.out.println("EPCPercentile\tlowPercentile\tminPCDays\tpEPC\ttEPC\tpRate\tfloodingEventMissed");
		for (double EPCPercentile:EPCPercentiles)
			for (double lowPercentile:lowPercentiles) {
				double low = percentilesIowa[(int)(lowPercentile/percentile_step)];
				double PCThreshold = percentilesIowa[(int)(0.6/percentile_step)];
				double EPCThreshold = percentilesIowa[(int)(EPCPercentile/percentile_step)];
				for (minPCDays=5;minPCDays<15;minPCDays++) {
					ArrayList<PWC> pwclist = PWC.FindPWCs(Start_Date,IowaPWs,low,PCThreshold,maxNonePCDays,minPCDays);
					ArrayList<PWC> AllEPCs = PWC.PWCRangeByAverage(pwclist, EPCThreshold,Double.MAX_VALUE,"EPC");
					ArrayList<PWC> trainEPCs = PWC.PWCRangeByYear(AllEPCs, trainData_start_year, trainData_end_year);
					trainEPCs = PWC.PWCRangeByMonth(trainEPCs, start_month, end_month);
					trainEPCs = removeEmbedded(trainEPCs);
					int pEPC = 0, index = 0;
					PWC epc = null; String missed = "";
					for (int i=0;i<floodingStartDays.length;i++) {
						int start = floodingStartDays[i], end = floodingEndDays[i];
						while (index < trainEPCs.size()) {
							epc = trainEPCs.get(index);
							if (epc.end_date >= start - 3)
								break;
							index++;
						}
						int lastpEPC = pEPC;
						if (index < trainEPCs.size()) {
							while (epc.start_date <= end) {
								pEPC++;
								index++;
								if (index == trainEPCs.size())
									break;
								epc = trainEPCs.get(index);
							}
						}
						if (lastpEPC == pEPC) {
							if (missed == "")
								missed += floodingStartDates[i][0]+"-"+floodingStartDates[i][1]+"-"+floodingStartDates[i][2];
							else
								missed += ","+floodingStartDates[i][0]+"-"+floodingStartDates[i][1]+"-"+floodingStartDates[i][2];
						}
					}
					System.out.println(EPCPercentile+"("+String.format("%.2f", EPCThreshold)+")\t"
							+lowPercentile+"("+String.format("%.3f", low)+")\t"
							+minPCDays+"\t\t"+pEPC+"\t"+trainEPCs.size()+"\t"
							+String.format("%.2f", ((float)pEPC/trainEPCs.size()))
							+"\t"+missed);
				}
			}
		//System.out.println("Working Directory = " + System.getProperty("user.dir"));
	}
	
	// load Iowa Precipitated water data
	private static double[] readIowaPWData(String IowaPrecipFile, long Data_start_day, long Data_end_day) {
		double[] IowaPWs = new double[(int) (Data_end_day-Data_start_day+1)];
		try {
			BufferedReader reader = new BufferedReader(new FileReader(IowaPrecipFile));
			String line="";
			String delimit = "\\s+";
			int day=0, index=0;
			while ((line = reader.readLine()) != null) {
				if ( (day >= Data_start_day) && (day <= Data_end_day)) {
					String[] values = line.split(delimit);
					IowaPWs[index++] = Double.parseDouble(values[1]);	//skip values[0] which is empty
				}
				day++;
			}
			reader.close();
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return IowaPWs;
	}
	
	public static ArrayList<PWC> removeEmbedded(ArrayList<PWC> PCs) {
		ArrayList<PWC> list = new ArrayList<PWC>();
		int sDay = 0, eDay = Integer.MIN_VALUE;
		int index = 0;
		while (index < PCs.size()) {
			PWC pc = PCs.get(index);
			if (pc.end_date <= eDay) {
				index++;
				continue;
			}
			sDay = pc.start_date;
			eDay = pc.end_date;
			PWC maxpc = pc;
			index++;
			while (index < PCs.size()) {
				pc = PCs.get(index);
				if (pc.start_date > sDay)
					break;
				if (pc.end_date > eDay) {
					eDay = pc.end_date;
					maxpc = pc;
				}
				index++;
			}
			list.add(maxpc);
		}
		return list;
	}

	public static void testPCSearch() {
		Calendar Base_Date = new GregorianCalendar(1980, 0, 1); // first date's ID starts with 0
		String IowaPrecipFile = "./data/text/PRECIP2_1980-2010.txt";
		Calendar Start_Date = new GregorianCalendar(1980, 0, 1);
		Calendar End_Date = new GregorianCalendar(2010, 11, 31);
		long Data_start_day =(Start_Date.getTimeInMillis()-Base_Date.getTimeInMillis())/(1000 * 60 * 60 * 24);
		long Data_end_day = (End_Date.getTimeInMillis()-Base_Date.getTimeInMillis())/(1000 * 60 * 60 * 24);
		int trainData_start_year = 1980, trainData_end_year = 2010;
		int start_month = 3, end_month = 10;

		// load Iowa Precipitated water data
		double[] IowaPWs = readIowaPWData(IowaPrecipFile, Data_start_day, Data_end_day);

		double percentile_step = 0.05;
		double[] percentilesIowa = null;
		try {
			percentilesIowa = StdStats.percentiles(IowaPWs, percentile_step);
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		int maxNonePCDays = 2;
		double[] EPCPercentiles = {0.8, 0.85, 0.9};
		double[] PCPercentiles = {0.5, 0.55, 0.6};
		double[] lowPercentiles = {0.35, 0.4};
		for (int minPCDays=5;minPCDays<=15;minPCDays++)
			for (double lowPercentile:lowPercentiles)
				for (double PCPercentile:PCPercentiles)
					for (double EPCPercentile:EPCPercentiles) {
						double low = percentilesIowa[(int)(lowPercentile/percentile_step)];
						double PCThreshold = percentilesIowa[(int)(PCPercentile/percentile_step)];
						double EPCThreshold = percentilesIowa[(int)(EPCPercentile/percentile_step)];
						System.out.print(String.format("%d\t%.2f(%.2f)\t%.2f(%.2f)\t%.2f(%.2f)", minPCDays,
								lowPercentile, low, PCPercentile, PCThreshold, EPCPercentile, EPCThreshold));
						ArrayList<PWC> pwclist = PWC.FindPWCs(Start_Date,IowaPWs,low,PCThreshold,maxNonePCDays,minPCDays);
						ArrayList<PWC> AllEPCs = PWC.PWCRangeByAverage(pwclist, EPCThreshold,Double.MAX_VALUE,"EPC");
						ArrayList<PWC> AllPCs = PWC.PWCRangeByAverage(pwclist, PCThreshold,EPCThreshold,"PC");
						System.out.print("\t"+pwclist.size()+"\t"+AllEPCs.size()+"\t"+AllPCs.size());
						ArrayList<PWC> trainEPCs = PWC.PWCRangeByYear(AllEPCs, trainData_start_year, trainData_end_year);
						ArrayList<PWC> trainPCs = PWC.PWCRangeByYear(AllPCs, trainData_start_year, trainData_end_year);
						System.out.print("\t"+trainEPCs.size()+"\t"+trainPCs.size());
						trainEPCs = PWC.PWCRangeByMonth(trainEPCs, start_month, end_month);
						trainPCs = PWC.PWCRangeByMonth(trainPCs, start_month, end_month);
						System.out.print("\t"+trainEPCs.size()+"\t"+trainPCs.size());
						trainEPCs = removeEmbedded(trainEPCs);
						trainPCs = removeEmbedded(trainPCs);
						System.out.println("\t"+trainEPCs.size()+"\t"+trainPCs.size());
					}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		//testPCSearch();
		floodingEventCoverage();
		/*
		getIowaPWDistribution("./data/text/PRECIP2_1980-2010_3-10.txt",
				"./data/text/percentileIowa_0.05.txt");
		*/		
		
		//getGlobalDistributions("./data/text/1980-2010_PW_3-10.txt", "./data/text/percentile1_0.05.txt");
		
	}

}
