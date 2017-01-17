package ch.fhnw.tvver.rendercommand;

import java.util.ArrayList;

import ch.fhnw.ether.audio.IAudioRenderTarget;
import ch.fhnw.ether.media.AbstractRenderCommand;
import ch.fhnw.ether.media.RenderCommandException;
import ch.fhnw.tvver.AbstractPCM2MIDI;
import weka.classifiers.trees.RandomForest;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SerializationHelper;
import weka.core.converters.CSVLoader;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Standardize;

public class MLOnsetDetection extends AbstractRenderCommand<IAudioRenderTarget> {
	private static final int N_FRAMES = 10; // number of total frames taken into account since attack of piano is almost instantly, there should be max 4 frames relevant?
	
	private final BandPassFilterBank bandPassFilterBank;
	private final AbstractPCM2MIDI pipeline;
	private RandomForest forest;
	private Standardize filter;
	private final double meanEnergyHistory[][]; // saves mean energy of previous frames
	private int idx = 0;
	
	private final ArrayList<Attribute> wekaAttributes;
	private final Instances data;
	
	public MLOnsetDetection(BandPassFilterBank bandPassFilterBank, AbstractPCM2MIDI pipeline) {
		this.bandPassFilterBank = bandPassFilterBank;
		this.pipeline = pipeline;
		
    	try {
    		forest = (RandomForest) SerializationHelper.read("classifier.model");
			filter = (Standardize) SerializationHelper.read("standardize.filter");
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
    	
		int nFilters = bandPassFilterBank.highestNote - bandPassFilterBank.lowestNote + 1;
		meanEnergyHistory = new double[nFilters][N_FRAMES];
		
		wekaAttributes = new ArrayList<Attribute>() {{ for(int x=0; x<=N_FRAMES; ++x) { add(new Attribute("a"+x)); }}};
		data = new Instances("Data", wekaAttributes, 1);
		data.setClassIndex(N_FRAMES);
	}
	
	@Override
	protected void run(IAudioRenderTarget target) throws RenderCommandException {
		for (int i = 0; i < bandPassFilterBank.filteredSamples.length; ++i) {
			double sum = 0;
			for (int j = 0; j < bandPassFilterBank.filteredSamples[i].length; ++j) {
				sum += Math.abs(bandPassFilterBank.filteredSamples[i][j]);
			}
			meanEnergyHistory[i][idx] = sum / bandPassFilterBank.filteredSamples[i].length;;
			
			Instance instance = new DenseInstance(N_FRAMES+1);
			for(int j=0; j < N_FRAMES; ++j) {
				instance.setValue(j, meanEnergyHistory[i][(idx+j+1) % N_FRAMES]);
			}
			//instance.setValue(10, 0);
			data.add(instance);
			data.get(0).setClassMissing();
			
			double predicted = 0;
			try {
				/*boolean bla = filter.input(instance);
				instance = filter.output();*/
				Filter.useFilter(data, filter);
				predicted = forest.classifyInstance(data.get(0));
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			data.remove(0);
			
			if (predicted >= 0.5) {
				pipeline.noteOn(bandPassFilterBank.lowestNote + i, 0);
				//for(int x = 0; x < N_FRAMES; ++x) meanEnergyHistory[i][x] = 1; // Math.max(meanEnergyHistory[i][x], meanEnergyHistory[i][idx]); // set all values to the current value, so no futher noteons are detected in the following nFrames-1 frames
				System.out.println("pitch=" + (bandPassFilterBank.lowestNote + i) + ", mean=" + meanEnergyHistory[i][idx] + ", p=" + predicted);
			} else if(predicted > 0.01) {
				System.out.println("p=" + (bandPassFilterBank.lowestNote + i) + ", mean=" + meanEnergyHistory[i][idx] + ", p=" + predicted);
			}
			
			
		}
		idx = (idx+1)%N_FRAMES;
	}

}
