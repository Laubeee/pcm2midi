package ch.fhnw.tvver.rendercommand;

import ch.fhnw.ether.audio.IAudioRenderTarget;
import ch.fhnw.ether.media.AbstractRenderCommand;
import ch.fhnw.ether.media.RenderCommandException;
import ch.fhnw.tvver.pipeline.OverallOnsetFourierPitchPipeline;
import ch.fhnw.tvver.util.MidiUtil;

public class OverallOnsetFourierPitch extends AbstractRenderCommand<IAudioRenderTarget> {
	//                                                 24    25    26    27    28    29    30    31    32    33    34    35    36    37    38    39    40    41    42    43    44    45    46    47    48    49    50    51    52    53    54    55    56    57    58    59    60    61    62    63    64    65    66    67    68    69    70    71    72    73    74    75    76    77    78    79    80    81    82    83    84    85    86    87    88    89    90    91    92    93    94    95    96    97    98    99   100   101 
	//private static final double[] DELTA_THRESHOLD = { 0.05, 0.05, 0.05, 0.05, 0.05, 0.05, 0.05, 0.05, 0.05, 0.05, 0.05, 0.05, 0.05, 0.05, 0.05, 0.05, 0.04, 0.04, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03 };
	//private static final double[] MIN_NOTE_ENERGY = { 0.10, 0.10, 0.10, 0.10, 0.10, 0.10, 0.10, 0.10, 0.10, 0.10, 0.09, 0.09, 0.08, 0.09, 0.09, 0.08, 0.08, 0.08, 0.08, 0.07, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06 };
	
	private static final double DELTA_THRESHOLD = 0.035;
	private static final int N_FRAMES = 24; // number of total frames taken into account since attack of piano is almost instantly, there should be max 4 frames relevant?
	private static final int N_CALC_FRAMES = 8; // number of frames on each end that are used for calculation
	
	private final OverallOnsetFourierPitchPipeline pipeline;
	private final PitchDetect pitchDetect;
	
	private final double history[] = new double[N_FRAMES]; // saves mean energy of previous frames
	private int idx = 0;
	
	private int framesPerBlock = -1;
	
	private boolean onsetDetected = false;
	private long frameNrOnset;
	private int lastNote;
	private long frameNrLastNote;
	
	public OverallOnsetFourierPitch(OverallOnsetFourierPitchPipeline pipeline, PitchDetect pitchDetect) {
//		this.bandPassFilterBank = bandPassFilterBank;
//		int nFilters = bandPassFilterBank.highestNote - bandPassFilterBank.lowestNote + 1;
//		historyFB = new double[nFilters][N_FRAMES];
		
		this.pipeline = pipeline;
		this.pitchDetect = pitchDetect;
	}
	
	@Override
	protected void run(IAudioRenderTarget target) throws RenderCommandException {
		if (target.getTotalElapsedFrames() == 1) {
			System.out.println();
			System.out.println(pipeline.trackName);
		}
		if (framesPerBlock == -1) {
			framesPerBlock = pitchDetect.fft.fftSize2 / target.getFrame().samples.length;
			System.out.println("Block size in frames: " + framesPerBlock);
		}
		
		float[] samples = target.getFrame().samples;
		double mean = 0;
		for(int i=0; i < samples.length; ++i){
			mean += Math.abs(samples[i]);
		}
		mean /= samples.length;
		history[idx] = mean;
		
		double eNow = 0, ePrev = 0;
		for(int i=0; i < N_CALC_FRAMES;) {
			eNow += history[(idx + N_FRAMES - i) % N_FRAMES];
			ePrev += history[(idx + ++i) % N_FRAMES];
		}
		double delta = (eNow- ePrev) / N_CALC_FRAMES;
		
		float[] amplitudes = pitchDetect.amplitude();
		float[] pitches = pitchDetect.pitch();
		int note = -1;
		float amplitude = -1.0f;
		if (pitches.length > 0) {
			note = Math.min(127, MidiUtil.frequencyToMidi(pitches[0]));
			amplitude = amplitudes[0];
//			System.out.println("note: " + note + ", amplitude=" + amplitude);
//			System.out.println("lastBlockFrame=" + pitchDetect.fft.frameNrLastBlock);
		}
		if (delta > DELTA_THRESHOLD) { // new onset
			onsetDetected = true;
			frameNrOnset = target.getTotalElapsedFrames();
			for(int x = 0; x < N_FRAMES; ++x) history[x] = 1; // set whole history to 1 to prevent multiple detection
			System.out.println("ONSET, delta=" + delta + ", mean=" + mean + ", frame=" + target.getTotalElapsedFrames());
		}
		
		if (onsetDetected && frameNrOnset + framesPerBlock < pitchDetect.fft.frameNrLastBlock && note > -1) {
			if (note >= 40 && (note != lastNote || target.getTotalElapsedFrames() - frameNrLastNote > 150)) {
				System.out.println("NOTE_ON, note=" + note + ", amplitude=" + amplitude + ", frame=" + target.getTotalElapsedFrames());
				pipeline.noteOn(note, 64);
				lastNote = note;
				frameNrLastNote = target.getTotalElapsedFrames();
			}
			onsetDetected = false;
		}
		idx = (idx+1)%N_FRAMES;
	}
}
