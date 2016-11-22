package ch.fhnw.tvver.rendercommand;

import ch.fhnw.ether.audio.IAudioRenderTarget;
import ch.fhnw.ether.media.AbstractRenderCommand;
import ch.fhnw.ether.media.RenderCommandException;
import ch.fhnw.tvver.AbstractPCM2MIDI;
import ch.fhnw.tvver.util.MidiUtil;

public class PitchDetectToMidi extends AbstractRenderCommand<IAudioRenderTarget> {
	private final AbstractPCM2MIDI pipeline;
	private final PitchDetect pitchDetect;
	
	private int lastNote = Integer.MIN_VALUE;
	
	public PitchDetectToMidi(AbstractPCM2MIDI pipeline, PitchDetect pitchDetect) {
		this.pipeline = pipeline;
		this.pitchDetect = pitchDetect;
	}
	
	@Override
	protected void run(IAudioRenderTarget target) throws RenderCommandException {
		float[] amplitude = pitchDetect.amplitude();
		float[] pitch = pitchDetect.pitch();
		if (pitch.length > 0) {
			// Very simple rules to avoid huge numbers of false positives.
			// Proper onset detection needed to make this work better.
			// Also, the pitch detection doesn't seem to work for the low notes
			// --> highest peak != the note we look for.
			if (MidiUtil.frequencyToMidi(pitch[0]) != lastNote && amplitude[0] > 10000000) {
				lastNote = MidiUtil.frequencyToMidi(pitch[0]);
				System.out.println(lastNote + " " + amplitude[0]);
				pipeline.noteOn(lastNote, 0);
			}
		}
	}
}
