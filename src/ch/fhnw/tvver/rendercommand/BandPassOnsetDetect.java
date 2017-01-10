package ch.fhnw.tvver.rendercommand;

import ch.fhnw.ether.audio.IAudioRenderTarget;
import ch.fhnw.ether.media.AbstractRenderCommand;
import ch.fhnw.ether.media.RenderCommandException;
import ch.fhnw.tvver.AbstractPCM2MIDI;
import ch.fhnw.tvver.pipeline.BandPassFilterBankPipeline;

public class BandPassOnsetDetect extends AbstractRenderCommand<IAudioRenderTarget> {
	private static final double THRESHOLD_DELTA = 0.015;
	private static final double THRESHOLD_ENERGY = 0.02;
	private static final int N_FRAMES = 24; // number of total frames taken into account since attack of piano is almost instantly, there should be max 4 frames relevant?
	private static final int N_CALC_FRAMES = 8; // number of frames on each end that are used for calculation
	
	private final BandPassFilterBank bandPassFilterBank;
	private final AbstractPCM2MIDI pipeline;
	private final double meanEnergyHistory[][]; // saves mean energy of previous frames
	//private final int meanEnergyHistoryIdx[]; // saves current idx
	private int idx = 0;
	//private int f = 0;
	
	public BandPassOnsetDetect(BandPassFilterBank bandPassFilterBank, AbstractPCM2MIDI pipeline) {
		this.bandPassFilterBank = bandPassFilterBank;
		this.pipeline = pipeline;
		int nFilters = bandPassFilterBank.highestNote - bandPassFilterBank.lowestNote + 1;
		meanEnergyHistory = new double[nFilters][N_FRAMES];
		//meanEnergyHistoryIdx = new int[nFilters];
	}
	
	@Override
	protected void run(IAudioRenderTarget target) throws RenderCommandException {
		//++f; if(f > 33) { System.out.println(" ===== 33 FRAMES ====="); f = 0; }
		for (int i = 0; i < bandPassFilterBank.filteredSamples.length; ++i) {
			double sum = 0;
			for (int j = 0; j < bandPassFilterBank.filteredSamples[i].length; ++j) {
				sum += Math.abs(bandPassFilterBank.filteredSamples[i][j]);
			}
			double mean = sum / bandPassFilterBank.filteredSamples[i].length;
			
			meanEnergyHistory[i][idx] = mean;
			double eNow = 0, ePrev=0;
			for(int x=0; x < N_CALC_FRAMES;) {
				eNow += meanEnergyHistory[i][(idx + N_FRAMES - x) % N_FRAMES];
				ePrev += meanEnergyHistory[i][(idx + ++x) % N_FRAMES];
			}
			
			double delta = (eNow - ePrev)/N_CALC_FRAMES; // don't use abs -> detect only rise in energy, not a fall
			if (delta >= THRESHOLD_DELTA && mean >= THRESHOLD_ENERGY) {
				pipeline.noteOn(bandPassFilterBank.lowestNote + i, 64);
				for(int x = 0; x < N_FRAMES; ++x) meanEnergyHistory[i][x] = 1; // Math.max(meanEnergyHistory[i][x], meanEnergyHistory[i][idx]); // set all values to the current value, so no futher noteons are detected in the following nFrames-1 frames
				System.out.println("pitch=" + (bandPassFilterBank.lowestNote + i) + ", mean=" + mean + ", eNow:" + eNow + ", delta=" + delta +", time:" + Math.round(System.currentTimeMillis() - BandPassFilterBankPipeline.START_TIME));
			} else {
				//System.out.println("p=" + (bandPassFilterBank.lowestNote + i) + ", mean=" + mean + ", delta=" + delta +", time:" + Math.round(System.currentTimeMillis() - BandPassFilterBankPipeline.START_TIME));
			}
		}
		idx = (idx+1)%N_FRAMES;
	}
}
