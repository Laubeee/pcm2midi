package ch.fhnw.tvver.rendercommand;

import ch.fhnw.ether.audio.IAudioRenderTarget;
import ch.fhnw.ether.media.AbstractRenderCommand;
import ch.fhnw.ether.media.RenderCommandException;
import ch.fhnw.tvver.AbstractPCM2MIDI;

public class BandPassOnsetDetect extends AbstractRenderCommand<IAudioRenderTarget> {
	private static final double THRESHOLD = 0.03;
	private static final int N_FRAMES = 12; // since attack of piano is almost instantly, there should be max 4 frames relevant?
	
	private final BandPassFilterBank bandPassFilterBank;
	private final AbstractPCM2MIDI pipeline;
	private final double meanEnergyHistory[][]; // saves mean energy of previous frames
	private final int meanEnergyHistoryIdx[]; // saves current idx
	private int f = 0;
	
	public BandPassOnsetDetect(BandPassFilterBank bandPassFilterBank, AbstractPCM2MIDI pipeline) {
		this.bandPassFilterBank = bandPassFilterBank;
		this.pipeline = pipeline;
		int nFilters = bandPassFilterBank.highestNote - bandPassFilterBank.lowestNote + 1;
		meanEnergyHistory = new double[nFilters][N_FRAMES];
		meanEnergyHistoryIdx = new int[nFilters];
	}
	
	@Override
	protected void run(IAudioRenderTarget target) throws RenderCommandException {
		++f; if(f > 100) { System.out.println(" ===== 100 FRAMES ====="); f = 0; }
		for (int i = 0; i < bandPassFilterBank.filteredSamples.length; ++i) {
			double sum = 0;
			for (int j = 0; j < bandPassFilterBank.filteredSamples[i].length; ++j) {
				sum += Math.abs(bandPassFilterBank.filteredSamples[i][j]);
			}
			int idx = meanEnergyHistoryIdx[i];
			int idx2 = (idx+1)%N_FRAMES;
			meanEnergyHistory[i][idx] = sum / bandPassFilterBank.filteredSamples[i].length;
			
			double delta = meanEnergyHistory[i][idx] - meanEnergyHistory[i][idx2]; // don't use abs! we don't want to have decays as onsets
			if (delta >= THRESHOLD) {
				pipeline.noteOn(bandPassFilterBank.lowestNote + i, 0);
				for(int x = 0; x < N_FRAMES; ++x) meanEnergyHistory[i][x] = meanEnergyHistory[i][idx]; // set all values to the current value, so no futher noteons are detected in the following nFrames-1 frames
				System.out.println("pitch=" + (bandPassFilterBank.lowestNote + i) + ", mean=" + meanEnergyHistory[i][idx] + ", delta=" + delta);
			}
		}
	}
}
