package ch.fhnw.tvver.rendercommand;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;

import ch.fhnw.ether.audio.IAudioRenderTarget;
import ch.fhnw.ether.media.AbstractRenderCommand;
import ch.fhnw.ether.media.RenderCommandException;

public class BandPassOnsetDetectMarkov extends AbstractRenderCommand<IAudioRenderTarget> {
	private final BandPassFilterBank bandPassFilterBank;
	
	private final double[][] meanHistory;
	private final MarkovModel[] markovModels;
	
	public BandPassOnsetDetectMarkov(BandPassFilterBank bandPassFilterBank) {
		this.bandPassFilterBank = bandPassFilterBank;
		
		int nFilters = bandPassFilterBank.highestNote - bandPassFilterBank.lowestNote + 1;
		meanHistory = new double[nFilters][28];
		markovModels = new MarkovModel[40];
		for (File f : new File("models").listFiles()) {
			String[] nameSplit = f.getName().split("\\.");
			int index = Integer.parseInt(nameSplit[nameSplit.length - 2]);
			
			FileInputStream fis = null;
			ObjectInputStream in = null;
			MarkovModel modelPart;
			try {
				fis = new FileInputStream(f);
				in = new ObjectInputStream(fis);
				modelPart = (MarkovModel)in.readObject();
				in.close();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			
			if (markovModels[index] == null) {
				markovModels[index] = modelPart;
			}
			else {
				MarkovModel modelCombined = markovModels[index];
				for (int i = 0; i < modelCombined.startCounts.length; ++i) {
					modelCombined.startCounts[i] += modelPart.startCounts[i];
				}
				for (int i = 0; i < modelCombined.transitionCounts.length; ++i) {
					for (int j = 0; j < modelCombined.transitionCounts[i].length; ++j) {
						modelCombined.transitionCounts[i][j] += modelPart.transitionCounts[i][j];
					}
				}
				modelCombined.nSamples += modelPart.nSamples;
				for (int i = 0; i < modelCombined.nTransitionsFrom.length; ++i) {
					modelCombined.nTransitionsFrom[i] += modelPart.nTransitionsFrom[i];
				}
			}
		}
		for (int i = 0; i < markovModels.length; ++i) {
			markovModels[i].calcProbs();
		}
		System.out.println("Markov models read");
	}
	
	@Override
	protected void run(IAudioRenderTarget target) throws RenderCommandException {
		double[] maxP = new double[markovModels.length];
		int[] maxNote = new int[markovModels.length];
		for (int i = 0; i < bandPassFilterBank.filteredSamples.length; ++i) {
			int note = i + bandPassFilterBank.lowestNote;
			int markovIndex = getMarkovModelForNote(note);
			MarkovModel model = markovModels[markovIndex];
			double p = model.predict(meanHistory[i]);
			if (p > maxP[markovIndex]) {
				maxP[markovIndex] = p;
				maxNote[markovIndex] = note;
			}
			
			for (int j = 0; j < meanHistory[i].length - 1; ++j) {
				meanHistory[i][j] = meanHistory[i][j + 1];
			}
			meanHistory[i][meanHistory[i].length - 1] = getMean(i);
		}
		System.out.print(target.getTotalElapsedFrames() + ": ");
		for (int i = 0; i < markovModels.length; ++i) {
			System.out.print("model=" + i + ", maxNote=" + maxNote[i] + ", p=" + maxP[i] + "; ");
		}
		System.out.println();
	}
	
	private int getMarkovModelForNote(int note) {
		for (int i = 0; i < markovModels.length; ++i) {
			if (note >= markovModels[i].noteRangeFrom && note <= markovModels[i].noteRangeTo) {
				return i;
			}
		}
		throw new IllegalArgumentException("No markov model found for note " + note);
	}
	
	private double getMean(int i) {
		double sum = 0;
		for (int j = 0; j < bandPassFilterBank.filteredSamples[i].length; ++j) {
			sum += Math.abs(bandPassFilterBank.filteredSamples[i][j]);
		}
		return sum / bandPassFilterBank.filteredSamples[i].length;
	}
}
