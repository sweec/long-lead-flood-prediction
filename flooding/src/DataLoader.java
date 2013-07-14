import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.Calendar;
import java.util.GregorianCalendar;

public class DataLoader {
	
	//private Calendar BASE_DATE = new GregorianCalendar(1948, 0, 1);
	
	public static int Z1000 = 0, T850 = 1, PW = 2, U300 =3, U850 = 4, V300 = 5, V850 = 6, Z300 = 7, Z500 = 8;
	
	public static String features[] = new String[] {"Z1000", "T850", "PW", "U300", "U850", "V300", "V850", "Z300", "Z500"};
	
	public static String DataFiles[] = new String[] {
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
	
	public static String featureFiles[] = new String[] {
		"./data/text/2010_Z1000.csv",
		"./data/text/2010_T850.csv",
		"./data/text/2010_PW.csv",
		"./data/text/2010_U300.csv",
		"./data/text/2010_U850.csv",
		"./data/text/2010_V300.csv",
		"./data/text/2010_V850.csv",
		"./data/text/2010_Z300.csv",
		"./data/text/2010_Z500.csv"
	};
	
	/*
	public static String featureFiles[] = new String[] {
		"./data/text/Z1000.csv",
		"./data/text/T850.csv",
		"./data/text/PW.csv",
		"./data/text/U300.csv",
		"./data/text/U850.csv",
		"./data/text/V300.csv",
		"./data/text/V850.csv",
		"./data/text/Z300.csv",
		"./data/text/Z500.csv"
	};
	*/
	public static String transFiles[] = new String[] {
		"./data/text/Z1000_t.csv",
		"./data/text/T850_t.csv",
		"./data/text/PW_t.csv",
		"./data/text/U300_t.csv",
		"./data/text/U850_t.csv",
		"./data/text/V300_t.csv",
		"./data/text/V850_t.csv",
		"./data/text/Z300_t.csv",
		"./data/text/Z500_t.csv"
	};

	public static void removeblanklines(String inFile, String outFile) throws IOException {
    	BufferedReader reader = new BufferedReader(new FileReader(inFile));
    	BufferedWriter out = new BufferedWriter(new FileWriter(outFile));
    	String line="";
    	int linenum = 0;
		while ((line = reader.readLine()) != null) {
			if ( ! line.isEmpty()) {
				if (linenum > 0) {
					out.write("\n"+line);
				} else {
					out.write(line);
				}
				linenum++;
			}
		}
		out.close();
		reader.close();
    }
	
    public static double[][] loadingData(String datafile, String delimit, int LocNum, int start, int end) throws IOException {
    	double[][] ret = new double[LocNum][end-start+1];
       	BufferedReader reader = new BufferedReader(new FileReader(datafile));
       	String line="";
    	int day = 0, i=0;
		while ((line = reader.readLine()) != null) {
			if ((day>= start)&&(day<= end)) {
				String[] values = line.split(delimit);
				for (int loc=1;loc<=LocNum;loc++){
					ret[loc-1][i] = Double.parseDouble(values[loc]);
				}
				i++;
			}
			day++;
		}
		reader.close();
    	return ret;
    }
    
    public static void saveArrayData(double[][] data, String delimit, String outFile) throws IOException {
    	BufferedWriter out = new BufferedWriter(new FileWriter(outFile));
    	for (int i=0;i<data.length;i++){
    		String line= "";
    		for (int j=0;j<data[0].length;j++){
        		line=line + delimit + data[i][j];
        	}
    		if (i> 0) {	out.write("\n"+line);} else {out.write(line);}
    	}
    	out.close();
    }
    
    public static void CopyData(String fromfile, String tofile, int startday, int enddata) throws IOException {
       	BufferedReader reader = new BufferedReader(new FileReader(fromfile));
		BufferedWriter out = new BufferedWriter(new FileWriter(tofile));

       	String line="";
    	int day = 0,count=0;
		while ((line = reader.readLine()) != null) { 
			if (day>=startday && day<=enddata){
				if (count>0) {
					out.write("\n"+line);
				} else {
					out.write(""+line);
				}
				count++;
			}
			day++;
		}
		reader.close();
		out.close();
    }
    
    
	static public void convertGeoData(String inputfileName,String fieldName, String outputfileName){
    	String line="";
		int days = 1;    	
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(outputfileName));
			out.write("id,day,"+fieldName+"\n");
			BufferedReader reader = new BufferedReader(new FileReader(inputfileName));
			while ((line = reader.readLine()) != null) {
				String[] values = line.split("  ");
				for ( int i=1;i<= values.length;i++){
					out.write(i+","+days+","+values[i-1]+"\n");
				}
				System.out.println("Day " + days);
				days++;
			}
			out.close();
			reader.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}	

	static public void convertGeoData(String inputfileName, int year,String fieldName, String outputfileName){
    	String line="";
		int days = 1;    	
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(outputfileName));
			out.write("id,year,day,"+fieldName+"\n");
			BufferedReader reader = new BufferedReader(new FileReader(inputfileName));
			while ((line = reader.readLine()) != null) {
				String[] values = line.split(",");
				for ( int i=1;i<= values.length;i++){
					out.write(i+","+year+","+days+","+values[i-1]+"\n");
				}
				days++;
			}
			out.close();
			reader.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}	
        
    static public void convertGeoData(String inputfileName, int year, int start_day, int totaldays,String fieldName, String outputfileName){
    	String line="";
		int days = 1;    	
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(outputfileName));
			out.write("id,year,day,"+fieldName+"\n");
			BufferedReader reader = new BufferedReader(new FileReader(inputfileName));
                while ((line = reader.readLine()) != null) {
				String[] values = line.split(",");
                                if ( (days>= start_day) && days < (start_day+totaldays)){
                                    for ( int i=1;i<= values.length;i++){
                                            out.write(i+","+year+","+days+","+values[i-1]+"\n");
                                    }
                                }
				days++;
			}
			out.close();
			reader.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
    
    static public void convertCSVtoArff(String inputfileName, String outputfileName) throws IOException {
		BufferedWriter outDataSet =new BufferedWriter(new FileWriter(outputfileName));
		BufferedReader reader = new BufferedReader(new FileReader(inputfileName));
		outDataSet.write("@relation PW \n");
		String line="";
		int numberoflocations;
		if ((line = reader.readLine()) != null) {
			String[] values = line.split(",");
			numberoflocations=values.length;
			// making the header of arff file
			for (int i=0; i< numberoflocations;i++) {
	    		outDataSet.write("\n@attribute PW_Loc_"+(i+1)+" NUMERIC ");
	    	}
			outDataSet.write ("\n@attribute EPC {Yes, No}\n");
			outDataSet.write ("\n@Data\n");
			// first instance
			for (int i=0; i< numberoflocations;i++) {
	    		outDataSet.write(values[i]+",");
	    	}
			outDataSet.write ("Yes");
		}
		// reading the rest of instances
		while ((line = reader.readLine()) != null) {
			outDataSet.write ("\n"); // start a new line for this instance
			String[] values = line.split(",");
			for (int i=0; i< values.length;i++) {
				outDataSet.write(values[i]+",");
			}
			outDataSet.write ("Yes");
		}
		
		reader.close();
		outDataSet.close();
    }
    
    public static int getLineNumber(String file) {
		int ret = -1;
		try {
			LineNumberReader  reader = new LineNumberReader(new FileReader(file));
			reader.skip(Long.MAX_VALUE);
			ret = reader.getLineNumber();
			reader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ret+1;
	}
    
    public static void getColumnData(String file, int column,String delimit, int[] data) {
		//String delimit = "\\s+";
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
    
    public static void getColumnData(String file, int column,String delimit, double[] data) {
		//String delimit = "\\s+";
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			String line="";
			int i = 0;
			while ((line = reader.readLine()) != null) {
				String[] values = line.split(delimit);
				data[i] = Double.parseDouble(values[column]);
				i++;
			}
			reader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static double[][] readIndividualPercentiles(String file, int locNo, int pNo) throws NumberFormatException, IOException {
		if (!new File(file).exists()) return null;
		int lineNo = getLineNumber(file);
		if (lineNo != locNo) {
			System.out.println("Line number: "+lineNo+" not expected, want "+locNo);
			return null;
		}
		
		double[][] p = new double[locNo][pNo];
    	BufferedReader reader = new BufferedReader(new FileReader(file));
    	String delimit="\\s+";
		String line="";
		int loc=0;
		while ((line = reader.readLine()) != null) {
			String[] values = line.split(delimit);
			if (values.length<pNo) break;
			for (int i=0;i<pNo;i++)
				p[loc][i] = Double.parseDouble(values[i]);
			loc++;
		}
		reader.close();
		if (loc<locNo) return null;
		return p;
	}
	
	public static void writeIndividualPercentiles(String file, double[][] p) throws NumberFormatException, IOException {
		if (p == null || p[0] == null) return;
		BufferedWriter writer = new BufferedWriter(new FileWriter(file));
    	int pNo = p[0].length;
    	for (int i=0;i<p.length;i++) {
 			for (int j=0;j<pNo-1;j++)
				writer.write(p[i][j]+" ");
 			writer.write(p[i][pNo-1]+"");
 	 		if (i<p.length-1)
 	 			writer.write("\r\n");
    	}
    	writer.close();
	}
	
	public static double[] readPercentiles(String file, int pNo) throws NumberFormatException, IOException {
		if (!new File(file).exists()) return null;
		int lineNo = getLineNumber(file);
		if (lineNo != pNo) {
			System.out.println("Line number: "+lineNo+" not expected, want "+pNo);
			return null;
		}
		
		double[] p = new double[pNo];
    	BufferedReader reader = new BufferedReader(new FileReader(file));
		String line="";
		int i=0;
		while ((line = reader.readLine()) != null) {
				p[i] = Double.parseDouble(line);
			i++;
		}
		reader.close();
		return p;
	}
	
	public static void writePercentiles(String file, double[] p) throws NumberFormatException, IOException {
		if (p == null) return;
		BufferedWriter writer = new BufferedWriter(new FileWriter(file));
    	for (int j=0;j<p.length-1;j++)
    		writer.write(p[j]+"\r\n");
    	writer.write(p[p.length-1]+"");
     	writer.close();
	}
	
    /**
	 * @param args
	 */
	public static void main(String[] args) throws IOException {
    	//convertGeoData("./data/data_Precipitation/PW.csv",2010,"Precipitable_water","rPW.csv");
        //convertGeoData("./data/data_Precipitation/PW.csv",2010,1,10,"Precipitable_water","rPW1_10.csv");
		//convertGeoData("../hw4/data/PW.txt","PW","../hw4/data/PW_all.csv");
		//convertCSVtoArff("./data/data_Precipitation/PW.csv","./data/PW.arff");
		
		
		for (int i=0;i<DataFiles.length;i++){
			CopyData(DataFiles[i],featureFiles[i],19359,23010);
		}
	
		CopyData("./data/text/PRECIP2_1948-2010.txt","./data/text/PRECIP2_2001-2010.txt",19359,23010);
		
		/*
		double[][] PWdata = DataLoader.loadingData(featureFiles[2],"\\s+",flooding_prediction.totalSampleLocations,flooding_prediction.totalDays);	
		saveArrayData(PWdata,"\\s",transFiles[2]);
		*/
	}

}
