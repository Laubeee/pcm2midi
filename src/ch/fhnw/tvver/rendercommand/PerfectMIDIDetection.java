package ch.fhnw.tvver.rendercommand;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;

import ch.fhnw.ether.audio.IAudioRenderTarget;
import ch.fhnw.ether.media.AbstractRenderCommand;
import ch.fhnw.ether.media.IScheduler;
import ch.fhnw.ether.media.RenderCommandException;
import ch.fhnw.tvver.pipeline.OverallOnsetFourierPitchPipeline;

public class PerfectMIDIDetection extends AbstractRenderCommand<IAudioRenderTarget> {
	private final List<List<MidiEvent>> midiRef = new ArrayList<>();
	private       int                   msTime;
	private SortedSet<MidiEvent> midiRefRaw;
	public int lastNote;

	public PerfectMIDIDetection(SortedSet<MidiEvent> midiRef) {
		midiRefRaw = midiRef;
	}

	@Override
	protected void init(IAudioRenderTarget target) throws RenderCommandException {
		super.init(target);
		midiRef.clear();
		for(MidiEvent e : midiRefRaw) {
			MidiMessage msg = e.getMessage();
			if(msg instanceof ShortMessage && 
					(msg.getMessage()[0] & 0xFF) != ShortMessage.NOTE_ON || 
					(msg.getMessage()[2] & 0xFF) == 0) continue;
			int msTime = (int) (e.getTick() / 1000L);
			while(midiRef.size() <= msTime)
				midiRef.add(null);
			List<MidiEvent> evts = midiRef.get(msTime);
			if(evts == null) {
				evts = new ArrayList<MidiEvent>();
				midiRef.set(msTime, evts);
			}
			evts.add(e);
		}
	}

	@Override
	protected void run(IAudioRenderTarget target) throws RenderCommandException {
		try {
			int msTimeLimit = (int) (target.getFrame().playOutTime * IScheduler.SEC2MS);
			for(;msTime <= msTimeLimit; msTime++) {
				if(msTime < midiRef.size()) {
					List<MidiEvent> evts = midiRef.get(msTime);
					if(evts != null) {
						for(MidiEvent e : evts) {
							byte[] msg = e.getMessage().getMessage();
							lastNote = msg[1];
							System.err.println("noteOn("+msg[1]+","+msg[2]+")"+", frame:" + target.getTotalElapsedFrames());
						}
					}
				}
			}
		} catch(Throwable t) {
			throw new RenderCommandException(t);
		}
	}
}