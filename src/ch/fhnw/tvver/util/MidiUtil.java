package ch.fhnw.tvver.util;

public class MidiUtil {
	/**
	 * https://en.wikipedia.org/wiki/MIDI_Tuning_Standard#Frequency_values
	 * @param f frequency
	 * @return midi note number
	 */
	public static int frequencyToMidi(float f) {
		return (int)(Math.round(
			69 + 12*(Math.log(f/440) / Math.log(2))
		));
	}
	
	public static float midiToFrequency(int d) {
		return (float)(Math.pow(2, (d - 69)/12.0)*440);
	}
}
