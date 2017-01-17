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
import ch.fhnw.tvver.rendercommand.BandPassFilterBank;
import ch.fhnw.tvver.rendercommand.Compression;
import ch.fhnw.tvver.rendercommand.MLOnsetDetection;
import ch.fhnw.tvver.rendercommand.PerfectMIDIDetection;

public class MachineLearningPipeline extends AbstractPCM2MIDI {
	public MachineLearningPipeline(File track) throws UnsupportedAudioFileException, IOException, MidiUnavailableException, InvalidMidiDataException, RenderCommandException {
		super(track, EnumSet.of(Flags.REPORT));
	}
	
	@Override
	protected void initializePipeline(RenderProgram<IAudioRenderTarget> program) {
		program.addLast(new PerfectMIDIDetection(getRefMidi()));
		program.addLast(new DCRemove());
		program.addLast(new Compression());
		BandPassFilterBank bandPassFilterBank = new BandPassFilterBank(24, 101, 3);
		program.addLast(bandPassFilterBank);
		program.addLast(new MLOnsetDetection(bandPassFilterBank, this));
	}
}
