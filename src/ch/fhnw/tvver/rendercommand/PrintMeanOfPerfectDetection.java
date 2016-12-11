package ch.fhnw.tvver.rendercommand;

import ch.fhnw.ether.audio.IAudioRenderTarget;
import ch.fhnw.ether.media.AbstractRenderCommand;
import ch.fhnw.ether.media.RenderCommandException;

public class PrintMeanOfPerfectDetection extends AbstractRenderCommand<IAudioRenderTarget> {
	private final PerfectMIDIDetection truth;
	private final BandPassFilterBank bandPassFilterBank;
	
	public PrintMeanOfPerfectDetection(PerfectMIDIDetection truth, BandPassFilterBank bandPassFilterBank) {
		this.truth = truth;
		this.bandPassFilterBank = bandPassFilterBank;
	}
	
	@Override
	protected void run(IAudioRenderTarget target) throws RenderCommandException {
		if (truth.lastNote >= bandPassFilterBank.lowestNote && truth.lastNote <= bandPassFilterBank.highestNote) {
			System.out.println("Mean of " + truth.lastNote + " = " + getMean(truth.lastNote - bandPassFilterBank.lowestNote));
		}
	}
	
	private double getMean(int i) {
		double sum = 0;
		for (int j = 0; j < bandPassFilterBank.filteredSamples[i].length; ++j) {
			sum += Math.abs(bandPassFilterBank.filteredSamples[i][j]);
		}
		return sum / bandPassFilterBank.filteredSamples[i].length;
	}
}
