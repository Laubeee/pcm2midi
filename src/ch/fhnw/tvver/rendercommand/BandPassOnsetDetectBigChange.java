package ch.fhnw.tvver.rendercommand;

import ch.fhnw.ether.audio.IAudioRenderTarget;
import ch.fhnw.ether.media.AbstractRenderCommand;
import ch.fhnw.ether.media.RenderCommandException;
import ch.fhnw.tvver.AbstractPCM2MIDI;

public class BandPassOnsetDetectBigChange extends AbstractRenderCommand<IAudioRenderTarget> {
	private final BandPassFilterBank bandPassFilterBank;
	private final AbstractPCM2MIDI pipeline;
	
	private double[] lastMean;
	
	public BandPassOnsetDetectBigChange(BandPassFilterBank bandPassFilterBank, AbstractPCM2MIDI pipeline) {
		this.bandPassFilterBank = bandPassFilterBank;
		this.pipeline = pipeline;
		int nFilters = bandPassFilterBank.highestNote - bandPassFilterBank.lowestNote + 1;
		this.lastMean = new double[nFilters];
	}

	@Override
	protected void run(IAudioRenderTarget target) throws RenderCommandException {
		for (int i = 0; i < bandPassFilterBank.filteredSamples.length; ++i) {
			double mean = getMean(i);
			// peak detection: > 500% increase to lastMean
			if (mean / lastMean[i] - 1 > 5) {
				int pitch = bandPassFilterBank.lowestNote + i;
				pipeline.noteOn(pitch, 64);
				System.out.println("NOTE_ON: " + pitch);
			}
			lastMean[i] = mean;
		}
		
//		double maxMean = 0.0;
//		int pitchWithMaxMean = -1;
//		for (int i = 0; i < bandPassFilterBank.filteredSamples.length; ++i) {
//			double mean = getMean(i);
//			// peak detection: > 500% increase to lastMean
//			if (mean / lastMean[i] - 1 > 5 && mean > maxMean) {
//				maxMean = mean;
//				pitchWithMaxMean = bandPassFilterBank.lowestNote + i;
//			}
//			lastMean[i] = mean;
//		}
//		
//		if (pitchWithMaxMean > -1) {
//			pipeline.noteOn(pitchWithMaxMean, 64);
//			System.out.println("NOTE_ON: " + pitchWithMaxMean);
//		}
	}
	
	private double getMean(int i) {
		double sum = 0;
		for (int j = 0; j < bandPassFilterBank.filteredSamples[i].length; ++j) {
			sum += Math.abs(bandPassFilterBank.filteredSamples[i][j]);
		}
		return sum / bandPassFilterBank.filteredSamples[i].length;
	}
}
