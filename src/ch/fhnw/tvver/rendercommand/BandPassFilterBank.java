package ch.fhnw.tvver.rendercommand;

import ch.fhnw.ether.audio.ButterworthFilter;
import ch.fhnw.ether.audio.IAudioRenderTarget;
import ch.fhnw.ether.media.AbstractRenderCommand;
import ch.fhnw.ether.media.RenderCommandException;
import ch.fhnw.tvver.util.MidiUtil;

public class BandPassFilterBank extends AbstractRenderCommand<IAudioRenderTarget> {
	public final int lowestNote;
	public final int highestNote;
	public float[][] filteredSamples;
	
	private ButterworthFilter[] filterBank;
	
	public BandPassFilterBank() {
		lowestNote = 0;
		highestNote = 127;
	}
	public BandPassFilterBank(int lowestNote, int highestNote) {
		assert lowestNote > -1;
		assert highestNote < 128;
		this.lowestNote = lowestNote;
		this.highestNote = highestNote;
	}
	
	private void initializeFilterBank(float sampleRate) {
		filterBank = new ButterworthFilter[highestNote - lowestNote + 1];
		for (int d = lowestNote; d <= highestNote; ++d) {
			float fLast = (d == 0 ? 0 : MidiUtil.midiToFrequency(d - 1));
			float f = MidiUtil.midiToFrequency(d);
			float fNext = MidiUtil.midiToFrequency(d + 1);
			float low = (f + fLast)/2;
			float high = (fNext + f)/2;
			filterBank[d - lowestNote] = ButterworthFilter.getBandpassFilter(sampleRate, low, high);
		}
	}
	
	@Override
	protected void run(IAudioRenderTarget target) throws RenderCommandException {
		if (filterBank == null) {
			initializeFilterBank(target.getSampleRate());
		}
		float[] samples = target.getFrame().samples;
		filteredSamples = new float[filterBank.length][samples.length];
		for (int i = 0; i < filterBank.length; ++i) {
			for (int j = 0; j < samples.length; ++j) {
				filteredSamples[i][j] = filterBank[i].process(samples[j]);
			}
		}
	}
}
