package ch.fhnw.tvver.rendercommand;

import ch.fhnw.ether.audio.ButterworthFilter;
import ch.fhnw.ether.audio.IAudioRenderTarget;
import ch.fhnw.ether.media.AbstractRenderCommand;
import ch.fhnw.ether.media.RenderCommandException;
import ch.fhnw.tvver.util.MidiUtil;

public class BandPassFilterBank extends AbstractRenderCommand<IAudioRenderTarget> {
	public final int lowestNote;
	public final int highestNote;
	private final int filterNarrowness;
	
	public float[][] filteredSamples;
	
	private ButterworthFilter[] filterBank;
	
	public BandPassFilterBank() {
		lowestNote = 0;
		highestNote = 127;
		filterNarrowness = 2;
	}
	public BandPassFilterBank(int lowestNote, int highestNote, int filterNarrowness) {
		assert lowestNote > -1;
		assert highestNote < 128;
		assert filterNarrowness >= 2;
		this.lowestNote = lowestNote;
		this.highestNote = highestNote;
		this.filterNarrowness = filterNarrowness;
	}
	
	private void initializeFilterBank(float sampleRate) {
		filterBank = new ButterworthFilter[highestNote - lowestNote + 1];
		for (int d = lowestNote; d <= highestNote; ++d) {
			float fLast = (d == 0 ? 0 : MidiUtil.midiToFrequency(d - 1));
			float f = MidiUtil.midiToFrequency(d);
			float fNext = MidiUtil.midiToFrequency(d + 1);
			// 0 _2_ 4 --> 1-3
//			float low = (f + fLast)/2;
//			float high = (fNext + f)/2;
			// filterNarrowness=3:
			// 0 _2_ 4 --> 1.333-2.666
			float low = f - (f - fLast)/filterNarrowness;
			float high = f + (fNext - f)/filterNarrowness;
			filterBank[d - lowestNote] = ButterworthFilter.getBandpassFilter(sampleRate, low, high);
			//System.out.println(d + ": " + low + "-" + high);
		}
	}
	
	@Override
	protected void run(IAudioRenderTarget target) throws RenderCommandException {
		if (filterBank == null) {
			initializeFilterBank(target.getSampleRate());
		}
		float[] samples = target.getFrame().samples;
		filteredSamples = new float[filterBank.length][samples.length];
		// Assumes only one channel when reading from the samples.
		assert target.getNumChannels() == 1;
		for (int i = 0; i < filterBank.length; ++i) {
			// Filter samples with bandpass filter and write to filteredSamples 2D-array.
			filterBank[i].process(samples, filteredSamples[i]);
		}
	}
}
