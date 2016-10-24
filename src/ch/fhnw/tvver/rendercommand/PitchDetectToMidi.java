package ch.fhnw.tvver.rendercommand;

import javax.sound.midi.InvalidMidiDataException;

import ch.fhnw.ether.audio.IAudioRenderTarget;
import ch.fhnw.ether.media.AbstractRenderCommand;
import ch.fhnw.ether.media.RenderCommandException;
import ch.fhnw.tvver.AbstractPCM2MIDI;

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
			if (frequencyToMidi(pitch[0]) != lastNote && amplitude[0] > 10000000) {
				lastNote = frequencyToMidi(pitch[0]);
				System.out.println(lastNote + " " + amplitude[0]);
				try {
					pipeline.noteOn(lastNote, 0);
				} catch (InvalidMidiDataException e) {
					System.out.println("noteOn failed:");
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * https://en.wikipedia.org/wiki/MIDI_Tuning_Standard#Frequency_values
	 * @param f frequency
	 * @return midi note number
	 */
	private int frequencyToMidi(float f) {
		return (int)(Math.round(
			69 + 12*(Math.log(f/440) / Math.log(2))
		));
	}

}
