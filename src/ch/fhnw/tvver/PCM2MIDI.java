package ch.fhnw.tvver;

import java.io.File;
import java.io.IOException;
import java.util.EnumSet;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import ch.fhnw.ether.audio.FFT;
import ch.fhnw.ether.audio.IAudioRenderTarget;
import ch.fhnw.ether.audio.AudioUtilities.Window;
import ch.fhnw.ether.audio.fx.AutoGain;
import ch.fhnw.ether.audio.fx.DCRemove;
import ch.fhnw.ether.media.AbstractRenderCommand;
import ch.fhnw.ether.media.RenderCommandException;
import ch.fhnw.ether.media.RenderProgram;

public class PCM2MIDI extends AbstractPCM2MIDI {

	public PCM2MIDI(File track) throws UnsupportedAudioFileException, IOException, MidiUnavailableException, InvalidMidiDataException, RenderCommandException {
		super(track, EnumSet.of(Flags.REPORT, Flags.WAVE));
	}

	@Override
	protected void initializePipeline(RenderProgram<IAudioRenderTarget> program) {
		//program.addLast(new DCRemove());
		//program.addLast(new AutoGain());
		//program.addLast(new FFT(20, Window.HANN));
		program.addLast(new PCM2MIDIRenderCommand());
		//new BandsButterworth(40, 8000, 40, N_CUBES, 1);
		//new PitchDetect(fft, 2);
	}
	
	public class PCM2MIDIRenderCommand extends AbstractRenderCommand<IAudioRenderTarget> {
		public PCM2MIDIRenderCommand() {
			super();
		}
		@Override
		protected void run(IAudioRenderTarget target) throws RenderCommandException {
			
		}
		
	}

}
