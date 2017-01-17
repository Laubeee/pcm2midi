package ch.fhnw.tvver.rendercommand;

import ch.fhnw.ether.audio.IAudioRenderTarget;
import ch.fhnw.ether.media.AbstractRenderCommand;
import ch.fhnw.ether.media.RenderCommandException;
import ch.fhnw.tvver.AbstractPCM2MIDI;

// TODO debug and tune parameters / doNoteOn = false sets
// TODO per-band thresholds
// TODO pause onset detection for a while after an onset
public class BandPassOnsetDetectADSR extends AbstractRenderCommand<IAudioRenderTarget> {
	private static final int N_FRAMES_ATTACK = 5;
	private static final int N_FRAMES_SUSTAIN = 5;
	private static final double MIN_DELTA_ATTACK = 0.0005;
	private static final double MAX_DELTA_SUSTAIN = 0.005;
	private static final double MIN_ENERGY = 0.015;
	
	// 330 / (BPM/60*(32/4))
	private static final int MIN_FRAMES_BETWEEN_NOTES = 11;
	
	private final BandPassFilterBank bandPassFilterBank;
	private final AbstractPCM2MIDI pipeline;
	// saves mean energy of previous frames
	private final double meanEnergyHistory[][];
	
	private long lastNoteOnFrame = -MIN_FRAMES_BETWEEN_NOTES;
	
	public BandPassOnsetDetectADSR(BandPassFilterBank bandPassFilterBank, AbstractPCM2MIDI pipeline) {
		this.bandPassFilterBank = bandPassFilterBank;
		this.pipeline = pipeline;
		int nFilters = bandPassFilterBank.highestNote - bandPassFilterBank.lowestNote + 1;
		meanEnergyHistory = new double[nFilters][N_FRAMES_ATTACK + N_FRAMES_SUSTAIN];
	}
	
	@Override
	protected void run(IAudioRenderTarget target) throws RenderCommandException {
		boolean globalDoNoteOn = false;
		double maxMean = 0;
		int pitchWithMaxMean = -1;
		for (int i = 0; i < bandPassFilterBank.filteredSamples.length; ++i) {
			double mean = getMean(i);
			boolean doNoteOn = true;
			if (doNoteOn) {
				for (int j = 0; j < N_FRAMES_ATTACK - 1; ++j) {
					if (meanEnergyHistory[i][j + 1] - meanEnergyHistory[i][j] < MIN_DELTA_ATTACK) {
						doNoteOn = false;
						break;
					}
				}
			}
			if (doNoteOn) {
				double sum = 0.0;
				for (int j = N_FRAMES_ATTACK + 1; j < meanEnergyHistory[i].length; ++j) {
					sum += (j == meanEnergyHistory[i].length - 1 ? mean : meanEnergyHistory[i][j + 1]) - meanEnergyHistory[i][j];
				}
				doNoteOn = sum < MAX_DELTA_SUSTAIN;
			}
			if (doNoteOn) {
				if (mean < MIN_ENERGY) {
					doNoteOn = false;
				}
				else {
					for (int j = N_FRAMES_ATTACK + 1; j < meanEnergyHistory[i].length; ++j) {
						if (meanEnergyHistory[i][j] < MIN_ENERGY) {
							doNoteOn = false;
							break;
						}
					}
				}
			}
			
			if (doNoteOn) {
				int pitch = bandPassFilterBank.lowestNote + i;
				System.out.println("local onset detection: " + pitch);
				globalDoNoteOn = true;
				if (mean > maxMean) {
					maxMean = mean;
					pitchWithMaxMean = pitch;
				}
			}
			
			for (int j = 0; j < meanEnergyHistory[i].length - 1; ++j) {
				meanEnergyHistory[i][j] = meanEnergyHistory[i][j + 1];
			}
			meanEnergyHistory[i][meanEnergyHistory[i].length - 1] = mean;
		}
		
		// Monophonic detection only
		if (globalDoNoteOn && target.getTotalElapsedFrames() - lastNoteOnFrame > MIN_FRAMES_BETWEEN_NOTES) {
			System.out.println("NOTE_ON: " + pitchWithMaxMean);
			pipeline.noteOn(pitchWithMaxMean, 64);
			lastNoteOnFrame = target.getTotalElapsedFrames();
		}
	}
	
	private double getMean(int i) {
		double sum = 0;
		for (int j = 0; j < bandPassFilterBank.filteredSamples[i].length; ++j) {
			sum += Math.abs(bandPassFilterBank.filteredSamples[i][j]);
		}
		return sum / bandPassFilterBank.filteredSamples[i].length;
	}
}
