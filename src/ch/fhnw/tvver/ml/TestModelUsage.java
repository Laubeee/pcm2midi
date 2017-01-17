package ch.fhnw.tvver.ml;

import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import weka.classifiers.trees.RandomForest;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SerializationHelper;
import weka.core.converters.CSVLoader;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Standardize;

public class TestModelUsage {
	public static void main(String[] args) throws Exception {
		ZipFile zipFile = new ZipFile("D:/documents/FHNW/16HS/4_tvver/Temp.zip");
	    Enumeration<? extends ZipEntry> entries = zipFile.entries();
	    ZipEntry lastEntry = null;
	    int c = 0;
	    while(entries.hasMoreElements()){
	        ZipEntry entry = entries.nextElement();
	        if(!entry.isDirectory() && entry.toString().startsWith("ml_data/")) {
	        	lastEntry = entry;
	        	if(++c > 20) break;
	        }
	    }
	    
	    CSVLoader loader = new CSVLoader();
	    loader.setNoHeaderRowPresent(true);
	    loader.setFieldSeparator(";");
	    
	    InputStream is = zipFile.getInputStream(lastEntry);
    	loader.setSource(is);
    	Instances data = loader.getDataSet();
    	is.close();
    	data.setClassIndex(10);
    	
    	RandomForest forest = (RandomForest) SerializationHelper.read("classifier.model");
    	Standardize filter = (Standardize) SerializationHelper.read("standardize.filter");
    	data = Filter.useFilter(data, filter);
    	
    	int count = 0;
		for(int i=0; i<data.size(); ++i) {
			double d = forest.classifyInstance(data.get(i));
			double d2 = data.get(i).toDoubleArray()[10];
			if(d2 != 0) {
				++count;
			}
			if(d != d2) {
				System.out.println(i + ": predicted " + d + ", but was " + d2);
			}
		}
		System.out.println(count);
	}
}
