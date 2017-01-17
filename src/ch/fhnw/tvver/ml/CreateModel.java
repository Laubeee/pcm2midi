package ch.fhnw.tvver.ml;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import weka.classifiers.trees.RandomForest;
import weka.core.Instances;
import weka.core.SerializationHelper;
import weka.core.converters.CSVLoader;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Standardize;

public class CreateModel {
	public static void main(String[] args) throws Exception {
		ZipFile zipFile = new ZipFile("D:/documents/FHNW/16HS/4_tvver/Temp.zip");
	    Enumeration<? extends ZipEntry> entries = zipFile.entries();
	    
	    CSVLoader loader = new CSVLoader();
	    loader.setNoHeaderRowPresent(true);
	    loader.setFieldSeparator(";");
	    Instances data = null;
	    int i = 0;
	    while(++i < 220 && entries.hasMoreElements()){
	        ZipEntry entry = entries.nextElement();
	        if(!entry.isDirectory() && entry.toString().startsWith("ml_data/")) {
	        	InputStream is = null;
	        	try {
					is = zipFile.getInputStream(entry);
					loader.setSource(is);
		        	if(data == null) {
		        		data = loader.getDataSet();
		        	} else {
		        		data.addAll(loader.getDataSet());
		        	}
				} catch (IOException e) {
					e.printStackTrace();
					System.err.println("i: " + i);
				} finally {
					if(is != null) {
						is.close();
					}
				}
	        }
	    }
	    
	    Standardize filter = new Standardize();
	    data.setClassIndex(10);
	    filter.setInputFormat(data);  // initializing the filter once with training set
	    data = Filter.useFilter(data, filter);  // configures the Filter based on train instances and returns filtered instances

	    long m = System.currentTimeMillis();
	    System.out.println(m);
	    
	    RandomForest f = new RandomForest();
	    f.buildClassifier(data);
	    
	    System.out.println(data.toSummaryString());
	    System.out.println((System.currentTimeMillis() - m)/60000);
	    
	    SerializationHelper.write("classifier.model", f);
	    SerializationHelper.write("standardize.filter", filter);
	}
}
