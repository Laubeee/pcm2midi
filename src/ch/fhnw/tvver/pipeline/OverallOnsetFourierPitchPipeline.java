package ch.fhnw.tvver.pipeline;

import java.io.File;
import java.io.IOException;
import java.util.EnumSet;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import ch.fhnw.ether.audio.AudioUtilities.Window;
import ch.fhnw.ether.audio.IAudioRenderTarget;
import ch.fhnw.ether.audio.fx.DCRemove;
import ch.fhnw.ether.audio.fx.FFT;
import ch.fhnw.ether.media.RenderCommandException;
import ch.fhnw.ether.media.RenderProgram;
import ch.fhnw.tvver.AbstractPCM2MIDI;
import ch.fhnw.tvver.rendercommand.AutoGainCopy;
import ch.fhnw.tvver.rendercommand.OverallOnsetFourierPitch;
import ch.fhnw.tvver.rendercommand.PerfectMIDIDetection;
import ch.fhnw.tvver.rendercommand.PitchDetect;

public class OverallOnsetFourierPitchPipeline extends AbstractPCM2MIDI {
	public final String trackName;
	
	public OverallOnsetFourierPitchPipeline(File track) throws UnsupportedAudioFileException, IOException, MidiUnavailableException, InvalidMidiDataException, RenderCommandException {
		super(track, EnumSet.of(Flags.REPORT, Flags.MAX_SPEED));
		trackName = track.getName();
	}
	
	@Override
	protected void initializePipeline(RenderProgram<IAudioRenderTarget> program) {
		program.addLast(new PerfectMIDIDetection(getRefMidi()));
		program.addLast(new DCRemove());
		program.addLast(new AutoGainCopy());
		//program.addLast(new Compression());
		
		//BandPassFilterBank bandPassFilterBank = new BandPassFilterBank(24, 101);
		//program.addLast(bandPassFilterBank);
		//program.addLast(new BandPassOnsetDetect3(bandPassFilterBank, this));
		
		FFT fft = new FFT(20, Window.HANN);
		program.addLast(fft);
		
		PitchDetect pitchDetect = new PitchDetect(fft, 2);
		program.addLast(pitchDetect);
		
		program.addLast(new OverallOnsetFourierPitch(this, pitchDetect));
		
		//program.addLast(new OverallOnsetFourierPitchSimple(this));
	}
}
