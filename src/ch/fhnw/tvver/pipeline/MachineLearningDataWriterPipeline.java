package ch.fhnw.tvver.pipeline;

import java.io.File;
import java.io.IOException;
import java.util.EnumSet;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import ch.fhnw.ether.audio.IAudioRenderTarget;
import ch.fhnw.ether.media.RenderCommandException;
import ch.fhnw.ether.media.RenderProgram;
import ch.fhnw.tvver.AbstractPCM2MIDI;
import ch.fhnw.tvver.rendercommand.BandPassFilterBank;
import ch.fhnw.tvver.rendercommand.MachineLearningDataWriter;
import ch.fhnw.tvver.rendercommand.PerfectMIDIDetection;

public class MachineLearningDataWriterPipeline extends AbstractPCM2MIDI {
	private final String filename;
	private MachineLearningDataWriter mldw;
	
	public MachineLearningDataWriterPipeline(File track) throws UnsupportedAudioFileException,
			IOException, MidiUnavailableException, InvalidMidiDataException, RenderCommandException {
		super(track, EnumSet.of(Flags.MAX_SPEED));
		
		filename = track.getName();
	}

	@Override
	protected void initializePipeline(RenderProgram<IAudioRenderTarget> program) {
		int lowestNote = 24;
		int highestNote = 101;
		
		PerfectMIDIDetection ppd = new PerfectMIDIDetection(getRefMidi());
		program.addLast(ppd);
		BandPassFilterBank bandPassFilterBank = new BandPassFilterBank(lowestNote, highestNote, 3);
		program.addLast(bandPassFilterBank);
		mldw = new MachineLearningDataWriter(ppd, bandPassFilterBank, 10, 8, 11, filename);
		program.addLast(mldw);
	}
	
	@Override
	protected void shutdown() {
		mldw.closeStreams();
	}
}
