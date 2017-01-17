package ch.fhnw.tvver.ml;

import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import weka.classifiers.trees.RandomForest;
import weka.core.Instances;
import weka.core.SerializationHelper;
import weka.core.converters.CSVLoader;

public class Test {
	public static void main(String[] args) throws Exception {
		ZipFile zipFile = new ZipFile("D:/documents/FHNW/16HS/4_tvver/Temp.zip");
	    Enumeration<? extends ZipEntry> entries = zipFile.entries();
	    
	    CSVLoader loader = new CSVLoader();
	    loader.setNoHeaderRowPresent(true);
	    loader.setFieldSeparator(";");
	    Instances data = null;
	    int i = 0;
	    while(++i < 200 && entries.hasMoreElements()){
	        ZipEntry entry = entries.nextElement();
	        if(!entry.isDirectory() && entry.toString().startsWith("ml_data/")) {
	        	InputStream is = zipFile.getInputStream(entry);
	        	loader.setSource(is);
	        	if(data == null) {
	        		data = loader.getDataSet();
	        	} else {
	        		data.addAll(loader.getDataSet());
	        	}
	        	is.close();
	        }
	    }
	    
	    data.setClassIndex(10);
	    
	    long m = System.currentTimeMillis();
	    System.out.println(m);
	    
	    RandomForest f = new RandomForest();
	    f.buildClassifier(data);
	    
	    System.out.println(data.toSummaryString());
	    System.out.println((System.currentTimeMillis() - m)/1000);
	    
	    SerializationHelper.write("classifier.model", f);
	    
	    /*double[] arr = data.get(300).toDoubleArray();
    	for(double d : arr) {
    		System.out.println(d);
    	}*/
	    
	    /*DataSource ds = new DataSource(is);
    	System.out.println(ds.getDataSet().toSummaryString());*/
	}
}
