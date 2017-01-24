package ch.fhnw.tvver.rendercommand;

import ch.fhnw.ether.audio.IAudioRenderTarget;
import ch.fhnw.ether.media.AbstractRenderCommand;
import ch.fhnw.ether.media.RenderCommandException;
import ch.fhnw.tvver.AbstractPCM2MIDI;

public class BandPassOnsetDetect3 extends AbstractRenderCommand<IAudioRenderTarget> {
	private static final double DELTA_THRESHOLD = 0.035;
	private static final double DELTA_THRESHOLD_FB = 0.0003; // threshold per filterbank
	private static final int N_FRAMES = 24; // number of total frames taken into account since attack of piano is almost instantly, there should be max 4 frames relevant?
	private static final int N_CALC_FRAMES = 8; // number of frames on each end that are used for calculation
	private static final int SKIP_FRAMES = 10; // number of frames to skip after onset to determine pitch
	
	private final BandPassFilterBank bandPassFilterBank;
	private final AbstractPCM2MIDI pipeline;
	private final double history[] = new double[N_FRAMES]; // saves mean energy of previous frames
	private final double historyFB[][]; // saves mean energy of previous frames per filter band
	private int idx = 0;
	
	private boolean pendingOnset = false;
	private int skipFrames =0;
	private long[] blockedNotes;
	
	public BandPassOnsetDetect3(BandPassFilterBank bandPassFilterBank, AbstractPCM2MIDI pipeline) {
		this.bandPassFilterBank = bandPassFilterBank;
		this.pipeline = pipeline;
		int nFilters = bandPassFilterBank.highestNote - bandPassFilterBank.lowestNote + 1;
		historyFB = new double[nFilters][N_FRAMES];
		blockedNotes = new long[nFilters];
		for(int i=0; i<nFilters; ++i) {
			blockedNotes[i] = -512;
		}
	}
	
	@Override
	protected void run(IAudioRenderTarget target) throws RenderCommandException {
		// write history per filterbank
		for (int i = 0; i < bandPassFilterBank.filteredSamples.length; ++i) {
			double mean = 0;
			for (int j = 0; j < bandPassFilterBank.filteredSamples[i].length; ++j) {
				mean += Math.abs(bandPassFilterBank.filteredSamples[i][j]);
			}
			historyFB[i][idx] = mean / bandPassFilterBank.filteredSamples[i].length;
		}
		
		if(skipFrames > 0) {
			--skipFrames;
			idx = (idx+1)%N_FRAMES;
			return;
		}
		
		// write overall mean history
		float[] samples = target.getFrame().samples;
		double mean = 0;
		for(int i=0; i < samples.length; ++i){
			mean += Math.abs(samples[i]);
		}
		mean /= samples.length;
		history[idx] = mean;
		
		if(!pendingOnset) {
			// get overall delta
			double eNow = 0, ePrev = 0;
			for(int i=0; i < N_CALC_FRAMES;) {
				eNow += history[(idx + N_FRAMES - i) % N_FRAMES];
				ePrev += history[(idx + ++i) % N_FRAMES];
			}
			double delta = (eNow- ePrev) / N_CALC_FRAMES;
			if(delta > DELTA_THRESHOLD) { // new onset
				System.out.println(String.format("ONSET, delta: %1$f, mean: %2$f, frame:%3$d",delta,mean,target.getTotalElapsedFrames()));
				pendingOnset = true;
				skipFrames = SKIP_FRAMES;
				for(int x = 0; x < N_FRAMES; ++x) history[x] = 1; // set whole history to 1 to prevent multiple detection
			}
		} else {
			// find filterbanks with max energy
			double max1=DELTA_THRESHOLD_FB, max2=DELTA_THRESHOLD_FB;
			int key1=-1, key2=-1;
			for (int i = 0; i < bandPassFilterBank.filteredSamples.length; ++i) {
				if(historyFB[i][idx] > max2) {
					double now=0,prev=0;
					for(int x=0; x < N_CALC_FRAMES;) {
						now += historyFB[i][(idx + N_FRAMES - x) % N_FRAMES];
						prev += historyFB[i][(idx + ++x) % N_FRAMES];
					}
					double d = (now - prev)/N_CALC_FRAMES;
					if(d > DELTA_THRESHOLD_FB) {
						if(historyFB[i][idx] > max1) {
							max2 = max1;
							key2 = key1;
							max1 = historyFB[i][idx];
							key1 = i;
						} else {
							max2 = historyFB[i][idx];
							key2 = i;
						}
					}
				}
			}
			
			if(key1 >= 0) {
				System.out.println(String.format("1. key: %1$d (mean: %2$f, delta: %3$f) frame:%4$d", bandPassFilterBank.lowestNote + key1,max1,target.getTotalElapsedFrames()));
				if(key2 >= 0) {
					System.out.println(String.format("2. key: %1$d (mean: %2$f, delta: %3$f) frame:%4$d", bandPassFilterBank.lowestNote + key2,max2,target.getTotalElapsedFrames()));
				} else {
					System.out.println("2. -");
				}
			} else {
				System.out.print("1. -\n2. - \n");
			}
			
			if(max1 / max2 > 1.2) {
				if(target.getTotalElapsedFrames() - blockedNotes[key1] > 150) {
					pipeline.noteOn(bandPassFilterBank.lowestNote + key1, 64);
					blockedNotes[key1] = target.getTotalElapsedFrames();
					System.err.println("send: " + (bandPassFilterBank.lowestNote + key1));
				} else {
					System.err.println("(suppressed): " + (bandPassFilterBank.lowestNote + key1));
				}
				pendingOnset = false;
			}
		}
		idx = (idx+1)%N_FRAMES;
	}
}
