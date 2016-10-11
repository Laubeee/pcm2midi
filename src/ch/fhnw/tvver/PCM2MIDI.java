package ch.fhnw.tvver;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.ShortMessage;
import javax.sound.sampled.UnsupportedAudioFileException;

import ch.fhnw.ether.audio.AudioUtilities;
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
		FFT fft = new FFT(20, Window.HANN);
		program.addLast(new DCRemove());
		program.addLast(new AutoGain());
		program.addLast(fft);
		program.addLast(new PCM2MIDIRenderCommand(fft));
		//new BandsButterworth(40, 8000, 40, N_CUBES, 1);
		//new PitchDetect(fft, 2);
	}
	
	public class PCM2MIDIRenderCommand extends AbstractRenderCommand<IAudioRenderTarget> {
		private FFT fft;
		public PCM2MIDIRenderCommand(FFT fft) {
			super();
			this.fft = fft;
		}
		
		/*@Override
		protected void init(IAudioRenderTarget target) throws RenderCommandException {
			super.init(target);
			// setup stuff...
		}*/
		
		@Override
		protected void run(IAudioRenderTarget target) throws RenderCommandException {
			float maxPower = 0;
			int maxIdx = 0;
			int idx = 0;
			for(float p : fft.power()) {
				++idx;
				if(p > maxPower) {
					maxPower = p;
					maxIdx = idx;
				}
			}
			System.out.println("power: " + maxPower + " idx: " + maxIdx);
			//AudioUtilities.peaks(fft.power(), 3, 0.2f);
		}
		
	}

}
