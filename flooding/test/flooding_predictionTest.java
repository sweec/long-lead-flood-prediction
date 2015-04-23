import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

import org.junit.Test;


public class flooding_predictionTest {
	private static int[][] floodingStartDates = {
		{1984, 6, 7}, {1987, 5, 26}, {1988, 7, 15}, {1990, 5, 18}, {1990, 7, 25}, 
		{1991, 6, 1}, {1992, 9, 14}, {1993, 3, 26}, {1993, 4, 13}, {1996, 5, 8},
		{1996, 6, 15}, {1998, 6, 13}, {1999, 5, 16}, {1999, 7, 2}, {2001, 4, 8}, {2002, 6, 3}, 
		{2004, 5, 19}, {2007, 5, 5}, {2007, 8, 17}, {2008, 5, 25}, {2010, 5, 11}, {2010, 6, 1}
	};

	private static int[][] floodingEndDates = {
		{1984, 6, 8}, {1987, 5, 31}, {1988, 7, 16}, {1990, 7, 7}, {1990, 8, 31}, 
		{1991, 6, 15}, {1992, 9, 15}, {1993, 4, 12}, {1993, 10, 1}, {1996, 5, 28},
		{1996, 6, 30}, {1998, 7, 15}, {1999, 5, 29}, {1999, 8, 10}, {2001, 5, 29}, {2002, 6, 25}, 
		{2004, 6, 24}, {2007, 5, 7}, {2007, 9, 5}, {2008, 8, 13}, {2010, 5, 12}, {2010, 8, 31}
	};

	@Test
	public void testEPC() {
		Calendar Base_Date = new GregorianCalendar(1980, 0, 1); // first date's ID starts with 0
		String IowaPrecipFile = "./data/text/PRECIP2_1980-2010.txt";
		Calendar Start_Date = new GregorianCalendar(1980, 0, 1);
		Calendar End_Date = new GregorianCalendar(2010, 11, 31);
		long Data_start_day =(Start_Date.getTimeInMillis()-Base_Date.getTimeInMillis())/(1000 * 60 * 60 * 24);
		long Data_end_day = (End_Date.getTimeInMillis()-Base_Date.getTimeInMillis())/(1000 * 60 * 60 * 24);
		int trainData_start_year = 1980, trainData_end_year = 2010;
		int totalDays = (int)(Data_end_day-Data_start_day+1);
		int start_month = 3, end_month = 10;

		// load Iowa Precipitated water data
		double[] IowaPWs = new double[totalDays];
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

		double percentile_step = 0.05;
		double[] percentilesIowa = null;
		try {
			percentilesIowa = StdStats.percentiles(IowaPWs, percentile_step);
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		double[] EPCPercentiles = {0.9, 0.85, 0.8};
		double[] lowPercentiles = {0.35, 0.4};
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
	
	private ArrayList<PWC> removeEmbedded(ArrayList<PWC> PCs) {
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
}
