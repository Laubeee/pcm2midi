package ch.fhnw.tvver.rendercommand;

import ch.fhnw.ether.audio.IAudioRenderTarget;
import ch.fhnw.ether.media.AbstractRenderCommand;
import ch.fhnw.ether.media.RenderCommandException;
import ch.fhnw.tvver.AbstractPCM2MIDI;

public class BandPassOnsetDetect extends AbstractRenderCommand<IAudioRenderTarget> {
	private static final double THRESHOLD = 0.2;
	
	private final BandPassFilterBank bandPassFilterBank;
	private final AbstractPCM2MIDI pipeline;
	private final int skipFrames;
	private final int noteOnHistory[]; // saves noteOns of the previous frame
	
	public BandPassOnsetDetect(BandPassFilterBank bandPassFilterBank, AbstractPCM2MIDI pipeline, int skipFrames) {
		this.bandPassFilterBank = bandPassFilterBank;
		this.pipeline = pipeline;
		this.skipFrames = skipFrames; // e.g. 200bpm and 32th notes results in max 26.666 notes per second = 37.5 ms / note. For a frame of 3ms (~333 fps) this results in 12.5 frames. Skipping 12 frames means looking at every 13th!
		noteOnHistory = new int[bandPassFilterBank.highestNote - bandPassFilterBank.lowestNote + 1];
	}
	
	@Override
	protected void run(IAudioRenderTarget target) throws RenderCommandException {
		for (int i = 0; i < bandPassFilterBank.filteredSamples.length; ++i) {
			if(noteOnHistory[i] > 0) { // if noteOn was sent ignore this bank and update history
				noteOnHistory[i] = (noteOnHistory[i] + 1) % skipFrames; // modulo will reset (0) history after skipFrames frames
			} else {
				double sum = 0;
				for (int j = 0; j < bandPassFilterBank.filteredSamples[i].length; ++j) {
					sum += Math.abs(bandPassFilterBank.filteredSamples[i][j]);
				}
				double mean = sum / bandPassFilterBank.filteredSamples[i].length;
				if (mean >= THRESHOLD) {
					pipeline.noteOn(bandPassFilterBank.lowestNote + i, 0);
					noteOnHistory[i] = 1;
					System.out.println("pitch=" + (bandPassFilterBank.lowestNote + i) + ", mean=" + mean);
				}
			}
		}
	}
}
