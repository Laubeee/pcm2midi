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
import ch.fhnw.tvver.rendercommand.BandPassOnsetDetectMarkovTrain;
import ch.fhnw.tvver.rendercommand.PerfectMIDIDetection;

public class MarkovModelTrainPipeline extends AbstractPCM2MIDI {
	private final String trackName;
	private BandPassOnsetDetectMarkovTrain markov;
	
	public MarkovModelTrainPipeline(File track) throws UnsupportedAudioFileException, IOException, MidiUnavailableException, InvalidMidiDataException, RenderCommandException {
		super(track, EnumSet.of(Flags.MAX_SPEED));
		trackName = track.getName();
	}
	
	@Override
	protected void initializePipeline(RenderProgram<IAudioRenderTarget> program) {
		PerfectMIDIDetection ppd = new PerfectMIDIDetection(getRefMidi());
		program.addLast(ppd);
		//program.addLast(new DCRemove());
		//program.addLast(new AutoGainCopy());
		BandPassFilterBank bandPassFilterBank = new BandPassFilterBank(24, 101);
		program.addLast(bandPassFilterBank);
		
		//program.addLast(new BandPassOnsetDetectADSR(bandPassFilterBank, this));
		//program.addLast(new BandPassOnsetDetectBigChange(bandPassFilterBank, this));
		
		markov = new BandPassOnsetDetectMarkovTrain(ppd, bandPassFilterBank);
		program.addLast(markov);
		
		//BandPassPitchDetect bppd = new BandPassPitchDetect(bandPassFilterBank);
		//program.addLast(bppd);
		
		//program.addLast(new LastNoteComparator(ppd, bppd));
		
		//PrintMeanOfPerfectDetection print = new PrintMeanOfPerfectDetection(ppd, bandPassFilterBank);
		//program.addLast(print);
	}
	
	@Override
	protected void shutdown() {
		markov.writeMarkovModelsToDisk(trackName);
	}
}
