import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.evaluation.NominalPrediction;
import weka.classifiers.rules.DecisionTable;
import weka.classifiers.rules.OneR;
import weka.classifiers.rules.PART;
import weka.classifiers.trees.DecisionStump;
import weka.classifiers.trees.J48;
import weka.classifiers.misc.VFI;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instances;
import weka.core.converters.ArffSaver;

public class WekaData {

	public static void saveInstances(Instances data, String outfile) throws IOException {
		 ArffSaver saver = new ArffSaver();
		 saver.setInstances(data);
		 saver.setFile(new File(outfile));
		 saver.writeBatch();
	}
	
	public static void addDropFeature(String arfFile, int features, int numLoc,int days,String[] targetF, String outFile)  throws IOException {
		Instances data = new Instances(new BufferedReader(new FileReader(arfFile)));
		Instances newData = new Instances(data);
		String[] newFeatureNames = new String[]{"drop","std","edge"};
		String[] locids = new String[numLoc];
		for (int loc=0, i=0; loc<numLoc;loc++,i=i+days) {
			String attName = newData.attribute(i).name();
			System.out.println(attName);
			String[] att = attName.split("_");//att[0] is feature att[1] is location id and att[2] is date
			locids[loc]=att[1];
		}
		
		for (int ins=0; ins<newData.numInstances();ins++){
			double[] daily= new double[days];
			System.out.println("instance:"+ins);
			for (int f=0;f<targetF.length;f++) {
				for (int loc=0; loc<numLoc;loc++) {
					for (int day=0;day <days;day++){
						daily[day]= newData.instance(ins).value(newData.attribute(targetF[f]+"_"+locids[loc]+"_"+(10-day)));
					}
					if (ins==0) {
						newData.insertAttributeAt(new Attribute(targetF[f]+"_"+locids[loc]+"_"+newFeatureNames[0]), newData.numAttributes()-1);
						newData.insertAttributeAt(new Attribute(targetF[f]+"_"+locids[loc]+"_"+newFeatureNames[1]), newData.numAttributes()-1);
						newData.insertAttributeAt(new Attribute(targetF[f]+"_"+locids[loc]+"_"+newFeatureNames[2]), newData.numAttributes()-1);
					}
					newData.instance(ins).setValue(newData.attribute(targetF[f]+"_"+locids[loc]+"_"+newFeatureNames[0]), ((daily[3]+daily[4]+daily[5])/3) - ((daily[0]+daily[1]+daily[2])/3));
					newData.instance(ins).setValue(newData.attribute(targetF[f]+"_"+locids[loc]+"_"+newFeatureNames[1]), StdStats.stddev(daily));
					newData.instance(ins).setValue(newData.attribute(targetF[f]+"_"+locids[loc]+"_"+newFeatureNames[2]), (daily[0]+daily[1])-(2*(daily[2]+daily[3]))+(daily[4]+daily[5]));
				}
			}
		}


		saveInstances(newData,outFile);
	}
	
	
	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		System.out.println("adding drop on training set...");
		addDropFeature("./EPC_arff/train1980_2004.arff",9,691,6,new String[]{"PW"} ,"./EPC_arff/train1980_2004_drop.arff");
		//System.out.println("adding drop on testing set...");
		//addDropFeature("./EPC_arff/test2005_2010.arff",9,691,6, new String[]{"PW"} ,"./EPC_arff/test2005_2010_drop.arff");
		//System.out.println("running Weka...");
		//RunWeka.run("./EPC_arff/train1980_2004_drop.arff", "./EPC_arff/test2005_20010_drop.arff");
		//addDropFeature("./EPC_arff/train2_1136_9999.arff",9,780,6,"./EPC_arff/train2_1136_9999_drop.arff");
		//addDropFeature("./EPC_arff/test2_1136_9999.arff",9,780,6,"./EPC_arff/test2_1136_9999_drop.arff");
	}

}
