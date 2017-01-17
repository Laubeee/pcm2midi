package ch.fhnw.tvver.rendercommand;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

import ch.fhnw.ether.audio.IAudioRenderTarget;
import ch.fhnw.ether.media.AbstractRenderCommand;
import ch.fhnw.ether.media.RenderCommandException;

public class MachineLearningDataWriter extends AbstractRenderCommand<IAudioRenderTarget> {
	private static final String SEPARATOR = ";";
	
	private final PerfectMIDIDetection truth;
	private final BandPassFilterBank bandPassFilterBank;
	private final int nFramesBack;
	private final int nFramesMarkAsOnset;
	private final int minFramesBetweenNotes;
	private final double[][] meanHistory;
	private final int[][] yHistory;
	private final FileWriter[] fileWriters;
	
	private int lastNote = -1;
	private long lastNoteFrameNr = 0;
	
	public MachineLearningDataWriter(PerfectMIDIDetection truth, BandPassFilterBank bandPassFilterBank, int nFramesBack, int nFramesMarkAsOnset, int minFramesBetweenNotes, String filename) {
		assert nFramesMarkAsOnset < minFramesBetweenNotes;
		
		this.truth = truth;
		this.bandPassFilterBank = bandPassFilterBank;
		this.nFramesBack = nFramesBack;
		this.nFramesMarkAsOnset = nFramesMarkAsOnset;
		this.minFramesBetweenNotes = minFramesBetweenNotes;
		
		int nFilters = bandPassFilterBank.highestNote - bandPassFilterBank.lowestNote + 1;
		meanHistory = new double[nFilters][nFramesBack];
		yHistory = new int[nFilters][nFramesBack];
		fileWriters = new FileWriter[nFilters];
		for (int i = 0; i < nFilters; ++i) {
			try {
				fileWriters[i] = new FileWriter("ml_data/" + filename + "." + (i + bandPassFilterBank.lowestNote) + ".csv");
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	@Override
	protected void run(IAudioRenderTarget target) throws RenderCommandException {
		if (lastNoteFrameNr < truth.lastNoteFrameNr) {
			assert truth.lastNoteFrameNr - lastNoteFrameNr > minFramesBetweenNotes;
			
			lastNote = truth.lastNote;
			lastNoteFrameNr = truth.lastNoteFrameNr;
		}
		for (int i = 0; i < bandPassFilterBank.filteredSamples.length; ++i) {
			for (int j = 0; j < meanHistory[i].length - 1; ++j) {
				meanHistory[i][j] = meanHistory[i][j + 1];
			}
			meanHistory[i][meanHistory[i].length - 1] = getMean(i);
			for (int j = 0; j < yHistory[i].length - 1; ++j) {
				yHistory[i][j] = yHistory[i][j + 1];
			}
			yHistory[i][yHistory[i].length - 1] = (
				i == lastNote - bandPassFilterBank.lowestNote &&
				target.getTotalElapsedFrames() - lastNoteFrameNr < nFramesMarkAsOnset
				? 1 : 0
			);
			if (target.getTotalElapsedFrames() >= nFramesBack) {
				try {
					fileWriters[i].write(
						Arrays.stream(meanHistory[i])
						    .mapToObj(Double::toString)
						    .collect(Collectors.joining(SEPARATOR))
						    
						+ SEPARATOR + Integer.toString(yHistory[i][0]) + "\r\n"
					);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}
	
	public void closeStreams() {
		for (FileWriter fw : fileWriters) {
			try {
				fw.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
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
