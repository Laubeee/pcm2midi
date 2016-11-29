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
		
		
/*
final float[] spec   = spectrum.power().clone(); // spectrum = FFT
 
AudioUtilities.multiplyHarmonics(spec, nHarmonics); // multiply base freq with harmonic multiple frq.
 
final BitSet peaks  = AudioUtilities.peaks(spec, 3, THRESHOLD);
this.peaks.clear();
 
for (int i = peaks.nextSetBit(0); i >= 0; i = peaks.nextSetBit(i+1))
this.peaks.add(new Vec2(spec[i], spectrum.idx2f(i)));
 
Collections.sort(this.peaks, (Vec2 v0, Vec2 v1)->v0.x < v1.x ? 1 : v0.x > v1.x ? -1 : 0);
 
float[] pitch = new float[this.peaks.size()];
for(int i = 0; i < pitch.length; i++)
pitch[i] = this.peaks.get(i).y;
this.pitch.set(pitch);
*/
	}

}