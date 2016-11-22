package ch.fhnw.tvver.rendercommand;

import ch.fhnw.ether.audio.IAudioRenderTarget;
import ch.fhnw.ether.media.AbstractRenderCommand;
import ch.fhnw.ether.media.RenderCommandException;

public class BandPassPitchDetect extends AbstractRenderCommand<IAudioRenderTarget> {
	private BandPassFilterBank bandPassFilterBank;
	
	public BandPassPitchDetect(BandPassFilterBank bandPassFilterBank) {
		this.bandPassFilterBank = bandPassFilterBank;
	}
	
	@Override
	protected void run(IAudioRenderTarget target) throws RenderCommandException {
		double maxMean = 0;
		int iMaxMean = -1;
		for (int i = 0; i < bandPassFilterBank.filteredSamples.length; ++i) {
			double sum = 0;
			for (int j = 0; j < bandPassFilterBank.filteredSamples[i].length; ++j) {
				// -1 to 1 --> 0 to 2
				sum += Math.abs(bandPassFilterBank.filteredSamples[i][j]);
			}
			double mean = sum / bandPassFilterBank.filteredSamples[i].length;
			if (mean > maxMean) {
				maxMean = mean;
				iMaxMean = i;
			}
		}
		System.out.println("pitch=" + (iMaxMean + bandPassFilterBank.lowestNote) + ", mean=" + maxMean);
	}
}
