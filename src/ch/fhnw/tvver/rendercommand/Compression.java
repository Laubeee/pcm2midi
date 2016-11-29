package ch.fhnw.tvver.rendercommand;

import ch.fhnw.ether.audio.AudioFrame;
import ch.fhnw.ether.audio.AudioUtilities;
import ch.fhnw.ether.audio.IAudioRenderTarget;
import ch.fhnw.ether.media.AbstractRenderCommand;
import ch.fhnw.ether.media.Parameter;
import ch.fhnw.ether.media.RenderCommandException;
import ch.fhnw.ether.ui.IPlotable;
import ch.fhnw.util.color.RGB;

public class Compression extends AbstractRenderCommand<IAudioRenderTarget> implements IPlotable {
	public static final Parameter FACTOR  = new Parameter("loudness", "Loudness", 0, 10, 2);
	
	public Compression() {
		super(FACTOR);
	}
	
	@Override
	protected void run(IAudioRenderTarget target) throws RenderCommandException {
		final float factor = getVal(FACTOR);
		
		float[] samples = target.getFrame().samples;
		for (int i = 0; i < samples.length; i++) {
	        int sign = (samples[i] < 0) ? -1 : 1;
	        float abs = Math.abs(samples[i]);
	        abs = (float) (1.0 - Math.pow(1.0 - abs, factor));
	        samples[i] = abs * sign;
	    }
		
		target.getFrame().modified(); 
		
		clear(); // advance plot by 1 col
		bar(AudioUtilities.energy(samples), RGB.GRAY);
		//point(correction, 0, 15, RGB.RED);
	}

}
