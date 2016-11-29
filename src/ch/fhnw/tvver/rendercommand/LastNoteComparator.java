package ch.fhnw.tvver.rendercommand;

import ch.fhnw.ether.audio.IAudioRenderTarget;
import ch.fhnw.ether.media.AbstractRenderCommand;
import ch.fhnw.ether.media.RenderCommandException;

public class LastNoteComparator extends AbstractRenderCommand<IAudioRenderTarget> {
	private int equalsCount = 0;
	private int notEqualsCount = 0;
	private PerfectMIDIDetection ppd;
	private BandPassPitchDetect bppd;
	
	public LastNoteComparator(PerfectMIDIDetection ppd, BandPassPitchDetect bppd) {
		this.ppd = ppd;
		this.bppd = bppd;
	}
	
	@Override
	protected void run(IAudioRenderTarget target) throws RenderCommandException {
		if (ppd.lastNote == bppd.lastNote) {
			equalsCount += 1;
		}
		else {
			notEqualsCount += 1;
		}
		if (target.getTotalElapsedFrames() == 13696) {
			System.out.println("richtig=" + equalsCount + ", falsch=" + notEqualsCount);
		}
	}
}
