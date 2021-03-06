package ch.fhnw.tvver.pipeline;

import java.io.File;
import java.io.IOException;
import java.util.EnumSet;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import ch.fhnw.ether.audio.AudioUtilities.Window;
import ch.fhnw.ether.audio.fx.FFT;
import ch.fhnw.ether.audio.IAudioRenderTarget;
import ch.fhnw.ether.audio.fx.AutoGain;
import ch.fhnw.ether.audio.fx.BandsButterworth;
import ch.fhnw.ether.audio.fx.DCRemove;
import ch.fhnw.ether.media.RenderCommandException;
import ch.fhnw.ether.media.RenderProgram;
import ch.fhnw.tvver.AbstractPCM2MIDI;
import ch.fhnw.tvver.rendercommand.PitchDetect;
import ch.fhnw.tvver.rendercommand.PitchDetectToMidi;

public class AudioVisualizerPipeline extends AbstractPCM2MIDI {

	public AudioVisualizerPipeline(File track) throws UnsupportedAudioFileException, IOException, MidiUnavailableException, InvalidMidiDataException, RenderCommandException {
		super(track, EnumSet.of(Flags.REPORT, Flags.WAVE));
	}

	@Override
	protected void initializePipeline(RenderProgram<IAudioRenderTarget> program) {
		program.addLast(new DCRemove());
		program.addLast(new AutoGain());
		
		FFT fft = new FFT(20, Window.HANN);
		program.addLast(fft);
		
		program.addLast(new BandsButterworth(40, 8000, 40, 60, 1));
		
		PitchDetect pitchDetect = new PitchDetect(fft, 2);
		program.addLast(pitchDetect);
		
		program.addLast(new PitchDetectToMidi(this, pitchDetect));
	}
	
}
