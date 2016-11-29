package ch.fhnw.tvver.rendercommand;

import ch.fhnw.ether.audio.IAudioRenderTarget;
import ch.fhnw.ether.media.AbstractRenderCommand;
import ch.fhnw.ether.media.RenderCommandException;

public class BandPassPitchDetect extends AbstractRenderCommand<IAudioRenderTarget> {
	private BandPassFilterBank bandPassFilterBank;
	public int lastNote;
	
	public BandPassPitchDetect(BandPassFilterBank bandPassFilterBank) {
		this.bandPassFilterBank = bandPassFilterBank;
	}
	
	@Override
	protected void run(IAudioRenderTarget target) throws RenderCommandException {
		double maxMeanLast = 0;
		double maxMean = 0;
		double maxMeanNext = 0;
		int maxMeanBand = -1;
		for (int i = 0; i < bandPassFilterBank.filteredSamples.length; ++i) {
			double mean = getMean(i);
			if (mean > maxMean) {
				maxMeanLast = (i > 0 ? getMean(i - 1) : 0);
				maxMean = mean;
				maxMeanNext = (i < bandPassFilterBank.filteredSamples.length - 1 ? getMean(i + 1) : 0);
				maxMeanBand = i;
			}
		}
		int pitch = maxMeanBand + bandPassFilterBank.lowestNote;
		System.out.println(target.getTotalElapsedFrames());
		System.out.println("PD pitch=" + pitch + ", meanLast=" + maxMeanLast + ", mean=" + maxMean + ", meanNext=" + maxMeanNext);
		lastNote = pitch;
	}
	
	private double getMean(int i) {
		double sum = 0;
		for (int j = 0; j < bandPassFilterBank.filteredSamples[i].length; ++j) {
			sum += Math.abs(bandPassFilterBank.filteredSamples[i][j]);
		}
		return sum / bandPassFilterBank.filteredSamples[i].length;
	}
}
