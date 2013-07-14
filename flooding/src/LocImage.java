import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * 
 */

/**
 * @author Jacky
 *
 */
public class LocImage {
	static public int Width = 144, Height = 37;
	static public String ImageFilePath = "./Image/";
	static private String delimit = "\\s+";
	
	static private ArrayList<PPMFile> LoadPGMImage(double[][] Locdata, int start, int end, double[] LocdataMax) throws IOException{
		ArrayList<PPMFile> pgms= new ArrayList<PPMFile>();
		int LocNum = Width*Height;
	
		// Locdata[LocNum][totaldays]
		// Generate each day's pgm
		for (int day = 0;day<Locdata[0].length;day++){
			byte[][] pgm = new byte[Height][Width];
			for (int loc=0;loc <LocNum;loc++) {
				int h=loc % Height;
				int w=loc / Height; 		
				// Normalize the value to between 0 and 254
				double scalevalue = Locdata[loc][day]*254.0/LocdataMax[loc];
				pgm[h][w] = (byte) (scalevalue+1);
				//System.out.println(Locdata[loc][day]+"-"+scalevalue+" max:"+LocdataMax[loc]);
			}
			pgms.add(new PPMFile(pgm));
		}
		return pgms;
	}
	
	static private ArrayList<PPMFile> LoadPPMImage(double[][] Locdata, int start, int end,double[] LocdataMax) throws IOException{
		ArrayList<PPMFile> ppms= new ArrayList<PPMFile>();
		int LocNum = Width*Height;
		
		// Locdata[LocNum][totaldays]
		// Generate each day's ppm
		for (int day = 0;day<Locdata[0].length;day++){
			byte[][] ppm = new byte[Height][Width*3];
			int loc=0;
			for (int w = 0;w<Width*3;w+=3){
				for (int h = 0;h<Height;h++){
					// Normalize the value to between 0 and 254
					double scalevalue = Locdata[loc][day]*254.0/LocdataMax[loc];
					ppm[h][w] = (byte) scalevalue;
					ppm[h][w+1] = (byte)(255.0-ppm[h][w]);
					ppm[h][w+2] = 0;
					loc++;
				}	
			}
			ppms.add(new PPMFile(ppm));
		}
		return ppms;
	}
	
	public static void Save2PPMImage(int featureIndex,String deli, Calendar start_date, Calendar end_date, double[] locMax) throws IOException{
		long start = (start_date.getTimeInMillis()-flooding_prediction.Base_Date.getTimeInMillis())/(1000 * 60 * 60 * 24);
		long end = (end_date.getTimeInMillis()-flooding_prediction.Base_Date.getTimeInMillis())/(1000 * 60 * 60 * 24);
		int LocNum = Width*Height;

		double[][] Locdata = DataLoader.loadingData(DataLoader.featureFiles[featureIndex], deli, LocNum, (int)start, (int)end);
		Calendar sdate = (Calendar)start_date.clone();
		ArrayList<PPMFile> ppms = LoadPPMImage(Locdata,(int)start,(int)end,locMax);
		for (int i=0;i<ppms.size();i++){
			String fn = ImageFilePath+DataLoader.features[featureIndex]
					+sdate.get(Calendar.YEAR)+"_"+(sdate.get(Calendar.MONTH)+1)+"_"+sdate.get(Calendar.DAY_OF_MONTH)
					+".ppm";
			System.out.println("Writing "+fn+" ...");
			ppms.get(i).WriteImage(fn);
			sdate.add(Calendar.DATE, 1);
		}
	}
	
	public static void Save2PGMImage(int featureIndex,String deli, Calendar start_date, Calendar end_date, double[] locMax) throws IOException{
		long start = (start_date.getTimeInMillis()-flooding_prediction.Base_Date.getTimeInMillis())/(1000 * 60 * 60 * 24);
		long end = (end_date.getTimeInMillis()-flooding_prediction.Base_Date.getTimeInMillis())/(1000 * 60 * 60 * 24);
		int LocNum = Width*Height;
		double[][] Locdata = DataLoader.loadingData(DataLoader.featureFiles[featureIndex], deli, LocNum, (int)start, (int)end);
		Calendar sdate = (Calendar)start_date.clone();
		ArrayList<PPMFile> pgms = LoadPGMImage(Locdata,(int)start,(int)end,locMax);
		for (int i=0;i<pgms.size();i++){
			String fn = ImageFilePath+DataLoader.features[featureIndex]
					+sdate.get(Calendar.YEAR)+"_"+(sdate.get(Calendar.MONTH)+1)+"_"+sdate.get(Calendar.DAY_OF_MONTH)
					+".pgm";
			System.out.println("Writing "+fn+" ...");
			pgms.get(i).WriteImagePGM(fn);
			sdate.add(Calendar.DATE, 1);
		}
	}
	
	public static void Save2JPEGImage(int featureIndex,String deli, Calendar start_date, Calendar end_date, double[] locMax) throws IOException{
		long start = (start_date.getTimeInMillis()-flooding_prediction.Base_Date.getTimeInMillis())/(1000 * 60 * 60 * 24);
		long end = (end_date.getTimeInMillis()-flooding_prediction.Base_Date.getTimeInMillis())/(1000 * 60 * 60 * 24);
		int LocNum = Width*Height;
		double[][] Locdata = DataLoader.loadingData(DataLoader.featureFiles[featureIndex], deli, LocNum, (int)start, (int)end);
		Calendar sdate = (Calendar)start_date.clone();
		ArrayList<PPMFile> pgms = LoadPGMImage(Locdata,(int)start,(int)end,locMax);
	
		for (int i=0;i<pgms.size();i++){
			String fn = ImageFilePath+DataLoader.features[featureIndex]
					+sdate.get(Calendar.YEAR)+"_"+(sdate.get(Calendar.MONTH)+1)+"_"+sdate.get(Calendar.DAY_OF_MONTH)
					+".jpg";
			System.out.println("Writing "+fn+" ...");

			byte[][] myImage = pgms.get(i).GetBytes();
			BufferedImage im = new BufferedImage(Width,Height,BufferedImage.TYPE_3BYTE_BGR);
			WritableRaster raster = im.getRaster();
			int loc=0;
			for(int w=0;w<Width;w++)
			{
				for(int h=0;h<Height;h++)	
				{
					loc = w*Height+h; 
					int rgbArray = 0;
					if ((loc == 3941) || (loc == 3942) || (loc == 3978) || (loc == 3979) ){
						rgbArray = (byte) 255;
						rgbArray<<=8;
						rgbArray+= (byte) 255;
						rgbArray<<=8;
						rgbArray +=255;
					} else {
						//raster.setSample(w,h,0, myImage[h][w]);
						rgbArray = myImage[h][w];
						rgbArray<<=8;
						rgbArray+= 255-myImage[h][w];
						rgbArray<<=8;
						rgbArray +=(int) ((255-myImage[h][w]) * 0.8);
					}
					im.setRGB(w, h, rgbArray);
				}
			}
			javax.imageio.ImageIO.write(im, "jpg", new File(fn));
			sdate.add(Calendar.DATE, 1);
		}
	}
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		double[][] Locdata = DataLoader.loadingData(DataLoader.featureFiles[DataLoader.PW], delimit, Width*Height,0,3561);
		double[] locMax = new double[Width*Height];
		for (int loc=0;loc<locMax.length;loc++){
			locMax[loc]= StdStats.max(Locdata[loc]);
		}
		double AllMax = StdStats.max(locMax);
		double[] allmax =new double[Width*Height];
		for (int loc=0;loc<locMax.length;loc++){
			allmax[loc]= AllMax;
		}
		//Save2PGMImage(DataLoader.PW,delimit,new GregorianCalendar(2010, 0, 1),new GregorianCalendar(2010, 0, 1),allmax);
		//Save2PPMImage(DataLoader.PW,delimit,new GregorianCalendar(2010, 0, 1),new GregorianCalendar(2010, 0, 1),allmax);
		//Save2JPEGImage(DataLoader.PW,delimit,new GregorianCalendar(2010, 5, 10),new GregorianCalendar(2010, 6, 10),allmax);
		//Save2PGMImage(DataLoader.PW,delimit,new GregorianCalendar(2010, 0, 2),new GregorianCalendar(2010, 0, 2),locMax);
		//Save2PPMImage(DataLoader.PW,delimit,new GregorianCalendar(2010, 0, 2),new GregorianCalendar(2010, 0, 2),locMax);
		Save2JPEGImage(DataLoader.PW,delimit,new GregorianCalendar(2010, 5, 10),new GregorianCalendar(2010, 6, 30),locMax);
	}

}
