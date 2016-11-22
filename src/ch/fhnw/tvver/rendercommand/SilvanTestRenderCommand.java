package ch.fhnw.tvver.rendercommand;

import ch.fhnw.ether.audio.fx.FFT;
import ch.fhnw.ether.audio.IAudioRenderTarget;
import ch.fhnw.ether.media.AbstractRenderCommand;
import ch.fhnw.ether.media.RenderCommandException;

public class SilvanTestRenderCommand extends AbstractRenderCommand<IAudioRenderTarget> {
	private FFT fft;

	public SilvanTestRenderCommand(FFT fft) {
		super();
		this.fft = fft;
	}

	/*
	 * @Override protected void init(IAudioRenderTarget target) throws
	 * RenderCommandException { super.init(target); // setup stuff... }
	 */

	@Override
	protected void run(IAudioRenderTarget target) throws RenderCommandException {
		float maxPower = 0;
		int maxIdx = 0;
		int idx = 0;
		for (float p : fft.power()) {
			++idx;
			if (p > maxPower) {
				maxPower = p;
				maxIdx = idx;
			}
		}
		System.out.println("power: " + maxPower + " idx: " + maxIdx);
		// AudioUtilities.peaks(fft.power(), 3, 0.2f);
	}

}