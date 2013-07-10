import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import weka.classifiers.Classifier;
import weka.core.Instances;
import weka.classifiers.Evaluation;

public class RunWeka {
	public static Classifier[] baseclassifiers = 
			new Classifier[]{new weka.classifiers.functions.LibSVM(),
							new weka.classifiers.trees.LADTree(), 
							new weka.classifiers.trees.J48(), 
							new weka.classifiers.bayes.NaiveBayes()};
	
	public static Classifier getBaseClassifier(int classifier_id) throws Exception {
		return Classifier.makeCopy(baseclassifiers[classifier_id]);
	}
	
	public static ClassificationResults run(Classifier classifier, String[] options, String trainfilepath,String testfilepath) throws Exception {
		Instances testset = new Instances(new BufferedReader(new FileReader(testfilepath)));
		int numoftest = testset.numInstances();
		testset.setClassIndex(testset.attribute("class").index());
		double[] results = new double[numoftest];
		double[] truth = new double[numoftest];
		for (int i=0;i<numoftest;i++) {
			truth[i]=testset.instance(i).classValue();
		}
		
		Instances trainset = new Instances(new BufferedReader(new FileReader(trainfilepath)));
		trainset.setClassIndex(trainset.attribute("class").index());
		classifier.setOptions(options);
		classifier.buildClassifier(trainset);
		Evaluation eval = new Evaluation(testset); 
		results = eval.evaluateModel(classifier, testset);

		System.out.println(eval.toSummaryString(true));
		System.out.println(eval.toClassDetailsString());
		
		System.out.println("TP:" +eval.numTruePositives(0)+" FP:" +eval.numFalsePositives(0)
				+" TN:" +eval.numTrueNegatives(0)+" FN:"+eval.numFalseNegatives(0));
		System.out.println("Accuracy = " +eval.pctCorrect());
		System.out.println("F1 = " +eval.fMeasure(0));
		System.out.println("Precision = " +eval.precision(0));
		System.out.println("Recall = " +eval.recall(0));
		
		int classid = testset.classAttribute().indexOfValue("EPC");
		return new ClassificationResults((int)eval.numTruePositives(classid)
				,(int)eval.numFalsePositives(classid)
				,(int)eval.numTrueNegatives(classid)
				,(int)eval.numFalseNegatives(classid), classifier); 
	}
	
	public static void runSingleDay(int from_day, int end_day, String trainfilepath,String testfilepath) throws Exception{
		int totaldays = end_day-from_day+1;
		Classifier[] classifiers = new Classifier[totaldays];
		Instances testset = new Instances(new BufferedReader(new FileReader(testfilepath+from_day+".arff")));
		int numoftest = testset.numInstances();
		testset.setClassIndex(testset.attribute("class").index());
		double[][] results = new double[totaldays+1][numoftest];
		double[] truth = new double[numoftest];
		for (int i=0;i<numoftest;i++) {
			truth[i]=testset.instance(i).classValue ();
		}
		
		for (int i=from_day,j=0;i<=end_day;i++,j++){
			Instances trainset = new Instances(new BufferedReader(new FileReader(trainfilepath+i+".arff")));
			testset = new Instances(new BufferedReader(new FileReader(testfilepath+i+".arff")));
			trainset.setClassIndex(trainset.attribute("class").index());
			testset.setClassIndex(testset.attribute("class").index());
			classifiers[j]=getBaseClassifier(flooding_prediction.baseclassifier); 
			classifiers[j].buildClassifier(trainset);
			Evaluation eval = new Evaluation(testset); 
			results[j] = eval.evaluateModel(classifiers[j], testset);
			//System.out.println(i);
		}
		double tp,fp,tn,fn;
		tp=fp=tn=fn=0;
		for (int i=0;i<numoftest;i++) {
			double sum=0;
			for (int j=0;j<totaldays;j++){
				sum+=results[j][i];
			}
			if ((sum<totaldays) && (truth[i]==0)){
				tp++;
			} else if ((sum<totaldays) && (truth[i]>0)){
				fp++;
			} else if ((sum>=totaldays) && (truth[i]>0)){
				tn++;
			} else {
				fn++;
			}
		}
		System.out.println("TP:" +(int)tp+" FP:" +(int)fp+" TN:" +(int)tn+" FN:"+(int)fn);
		System.out.println("Accuracy = " +((tp+tn)/(tp+fp+tn+fn)));
		System.out.println("F1 = " +((tp+tp)/(tp+fp+tp+fn)));
		System.out.println("Precision = " +((tp)/(tp+fp)));
		System.out.println("Recall = " +((tp)/(tp+fn)));
	}
	
	public static void main(String[] args) throws Exception {
		RunWeka.runSingleDay(5, 10, "./EPC_arff/Single/Train_","./EPC_arff/Single/Test_");
	}

}
