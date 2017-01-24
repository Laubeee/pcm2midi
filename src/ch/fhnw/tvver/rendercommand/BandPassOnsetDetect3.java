package ch.fhnw.tvver.rendercommand;

import java.util.Comparator;
import java.util.PriorityQueue;

import ch.fhnw.ether.audio.IAudioRenderTarget;
import ch.fhnw.ether.media.AbstractRenderCommand;
import ch.fhnw.ether.media.RenderCommandException;
import ch.fhnw.tvver.AbstractPCM2MIDI;
import ch.fhnw.tvver.pipeline.BandPassFilterBankPipeline;
import ch.fhnw.util.Pair;

public class BandPassOnsetDetect3 extends AbstractRenderCommand<IAudioRenderTarget> {
	//                                                 24    25    26    27    28    29    30    31    32    33    34    35    36    37    38    39    40    41    42    43    44    45    46    47    48    49    50    51    52    53    54    55    56    57    58    59    60    61    62    63    64    65    66    67    68    69    70    71    72    73    74    75    76    77    78    79    80    81    82    83    84    85    86    87    88    89    90    91    92    93    94    95    96    97    98    99   100   101 
	//private static final double[] DELTA_THRESHOLD = { 0.05, 0.05, 0.05, 0.05, 0.05, 0.05, 0.05, 0.05, 0.05, 0.05, 0.05, 0.05, 0.05, 0.05, 0.05, 0.05, 0.04, 0.04, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03 };
	//private static final double[] MIN_NOTE_ENERGY = { 0.10, 0.10, 0.10, 0.10, 0.10, 0.10, 0.10, 0.10, 0.10, 0.10, 0.09, 0.09, 0.08, 0.09, 0.09, 0.08, 0.08, 0.08, 0.08, 0.07, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06 };
	
	private static final double DELTA_THRESHOLD = 0.035;
	private static final int N_FRAMES = 24; // number of total frames taken into account since attack of piano is almost instantly, there should be max 4 frames relevant?
	private static final int N_CALC_FRAMES = 8; // number of frames on each end that are used for calculation
	
	private final BandPassFilterBank bandPassFilterBank;
	private final AbstractPCM2MIDI pipeline;
	private final double history[] = new double[N_FRAMES]; // saves mean energy of previous frames
	private final double historyFB[][]; // saves mean energy of previous frames per filter band
	private int idx = 0;
	
	public BandPassOnsetDetect3(BandPassFilterBank bandPassFilterBank, AbstractPCM2MIDI pipeline) {
		this.bandPassFilterBank = bandPassFilterBank;
		this.pipeline = pipeline;
		int nFilters = bandPassFilterBank.highestNote - bandPassFilterBank.lowestNote + 1;
		historyFB = new double[nFilters][N_FRAMES];
	}
	
	
	@Override
	protected void run(IAudioRenderTarget target) throws RenderCommandException {
		for (int i = 0; i < bandPassFilterBank.filteredSamples.length; ++i) {
			double mean = 0;
			for (int j = 0; j < bandPassFilterBank.filteredSamples[i].length; ++j) {
				mean += Math.abs(bandPassFilterBank.filteredSamples[i][j]);
			}
			historyFB[i][idx] = mean / bandPassFilterBank.filteredSamples[i].length;
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
		
		
		if(delta > DELTA_THRESHOLD) { // new onset
			for(int x = 0; x < N_FRAMES; ++x) history[x] = 1; // set whole history to 1 to prevent multiple detection
			System.out.println("delta: " + delta + ", mean: " + mean);
			
			// find filterbanks with max energy
			PriorityQueue<Pair<Double, Integer>> pq = new PriorityQueue<>(new Comparator<Pair<Double,Integer>>() {
				@Override public int compare(Pair<Double, Integer> p1, Pair<Double, Integer> p2) {
					return p2.first.compareTo(p1.first);
				}
			});
			for (int i = 0; i < bandPassFilterBank.filteredSamples.length; ++i) {
				pq.add(new Pair<Double,Integer>(historyFB[i][idx],i));
			}
			
			
			Pair<Double,Integer> last = null;
			int pKey = 0;
			for(int i=1; i<6 && pq.size() > 0; ++i) {
				Pair<Double,Integer> p = pq.remove();
				
				double now = 0, prev=0;
				for(int x=0; x < N_CALC_FRAMES;) {
					now += historyFB[p.second][(idx + N_FRAMES - x) % N_FRAMES];
					prev += historyFB[p.second][(idx + ++x) % N_FRAMES];
				}
				double deltaFB = (now - prev)/N_CALC_FRAMES; // don't use abs -> detect only rise in energy, not a fall
				System.out.println(String.format("%1$d: %2$d   mean: %3$f   delta: %4$f", i, bandPassFilterBank.lowestNote + p.second, p.first, deltaFB));
				//System.out.println(i + ": " + (bandPassFilterBank.lowestNote + p.second) + ", mean: " + p.first + ", delta: " + deltaFB);
				
				if(deltaFB < 0.001) {
					--i;
					continue;
				}
				
				if(last == null) {
					last = p;
				} else {
					if(pKey > 0) {
						pKey += p.second;
						if(i%2 == 1) {
							last = new Pair<Double, Integer>(1.0, pKey/i);
							break;
						}
					} else if(last.first / p.first < 1.2) {
						pKey = last.second + p.second;
					} else {
						break;
					}
				}
			}
			if(last != null) {
				System.err.println((bandPassFilterBank.lowestNote + last.second) + ": " + last.first);
				pipeline.noteOn(bandPassFilterBank.lowestNote + last.second, 64);
			}
			
			//TODO: if insecure (not very obvious strongest note or middle / median note) -> continue (set note on as unhandeled and go straight to pitch detection)
		}
		idx = (idx+1)%N_FRAMES;
	}
}
