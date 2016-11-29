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

public class BandPassFilterBankPipeline extends AbstractPCM2MIDI {
	public BandPassFilterBankPipeline(File track) throws UnsupportedAudioFileException, IOException, MidiUnavailableException, InvalidMidiDataException, RenderCommandException {
		super(track, EnumSet.of(Flags.REPORT));
	}
	
	@Override
	protected void initializePipeline(RenderProgram<IAudioRenderTarget> program) {
		program.addLast(new DCRemove());
		program.addLast(new AutoGainCopy());
		BandPassFilterBank bandPassFilterBank = new BandPassFilterBank(24, 101);
		program.addLast(bandPassFilterBank);
		program.addLast(new BandPassOnsetDetect(bandPassFilterBank, this, 11));
	}
}
