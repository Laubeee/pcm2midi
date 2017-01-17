package ch.fhnw.tvver.pipeline;

import java.io.File;
import java.io.IOException;
import java.util.EnumSet;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import ch.fhnw.ether.audio.IAudioRenderTarget;
import ch.fhnw.ether.audio.fx.DCRemove;
import ch.fhnw.ether.media.RenderCommandException;
import ch.fhnw.ether.media.RenderProgram;
import ch.fhnw.tvver.AbstractPCM2MIDI;
import ch.fhnw.tvver.rendercommand.AutoGainCopy;
import ch.fhnw.tvver.rendercommand.BandPassFilterBank;
import ch.fhnw.tvver.rendercommand.BandPassOnsetDetect;
import ch.fhnw.tvver.rendercommand.BandPassPitchDetect;
import ch.fhnw.tvver.rendercommand.Compression;
import ch.fhnw.tvver.rendercommand.PerfectMIDIDetection;

public class BandPassFilterBankPipeline extends AbstractPCM2MIDI {
	public static long START_TIME;
	
	public BandPassFilterBankPipeline(File track) throws UnsupportedAudioFileException, IOException, MidiUnavailableException, InvalidMidiDataException, RenderCommandException {
		super(track, EnumSet.of(Flags.REPORT));
	}
	
	@Override
	protected void initializePipeline(RenderProgram<IAudioRenderTarget> program) {
		START_TIME = System.currentTimeMillis();
		program.addLast(new PerfectMIDIDetection(getRefMidi()));
		program.addLast(new DCRemove());
		//program.addLast(new AutoGainCopy());
		program.addLast(new Compression());
		BandPassFilterBank bandPassFilterBank = new BandPassFilterBank(24, 101, 3);
		program.addLast(bandPassFilterBank);
		program.addLast(new BandPassOnsetDetect(bandPassFilterBank, this));
	}
}
