package ch.fhnw.tvver.rendercommand;

import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.PriorityQueue;
import java.util.Queue;

import ch.fhnw.ether.audio.IAudioRenderTarget;
import ch.fhnw.ether.media.AbstractRenderCommand;
import ch.fhnw.ether.media.RenderCommandException;

public class BandPassOnsetDetectMarkovTrain extends AbstractRenderCommand<IAudioRenderTarget> {
	private static class Onset implements Comparable<Onset> {
		public final int note;
		public final long frameNr;
		public final int markovModelIndex;
		
		public Onset(int note, long frameNr, int markovModelIndex) {
			this.note = note;
			this.frameNr = frameNr;
			this.markovModelIndex = markovModelIndex;
		}

		@Override
		public int compareTo(Onset o) {
			if (frameNr < o.frameNr) {
				return -1;
			}
			else if (frameNr > o.frameNr) {
				return 1;
			}
			else {
				if (note < o.note) {
					return -1;
				}
				else if (note > o.note) {
					return 1;
				}
				else {
					return 0;
				}
			}
		}
		
		@Override
		public boolean equals(Object o) {
			if (o == null || !(o instanceof Onset)) {
				return false;
			}
			Onset onset = (Onset)o;
			if (note == onset.note && frameNr == onset.frameNr) {
				return true;
			}
			else {
				return false;
			}
		}
	}
	
	private final PerfectMIDIDetection truth;
	private final BandPassFilterBank bandPassFilterBank;
	
	private final double[][] meanHistory;
	private final MarkovModel[] markovModels;
	private final Queue<Onset> queue;
	
	private long lastNoteFrameNr;
	
	public BandPassOnsetDetectMarkovTrain(PerfectMIDIDetection truth, BandPassFilterBank bandPassFilterBank) {
		this.truth = truth;
		this.bandPassFilterBank = bandPassFilterBank;
		
		int nFilters = bandPassFilterBank.highestNote - bandPassFilterBank.lowestNote + 1;
		meanHistory = new double[nFilters][28];
		markovModels = new MarkovModel[]{
			new MarkovModel(24, 39, 3, 27),
			new MarkovModel(40, 47, 3, 27),
			new MarkovModel(48, 84, 3, 12),
			new MarkovModel(85, 101, 2, 9),
		};
		queue = new PriorityQueue<>();
	}
	
	@Override
	protected void run(IAudioRenderTarget target) throws RenderCommandException {
		if (truth.lastNoteFrameNr > lastNoteFrameNr) {
			lastNoteFrameNr = truth.lastNoteFrameNr;
			queue.add(getOnsetForNote(truth.lastNote));
		}
		while (!queue.isEmpty() && queue.peek().frameNr == target.getTotalElapsedFrames()) {
			Onset onset = queue.remove();
			markovModels[onset.markovModelIndex].train(meanHistory[onset.note - bandPassFilterBank.lowestNote]);
		}
		for (int i = 0; i < bandPassFilterBank.filteredSamples.length; ++i) {
			for (int j = 0; j < meanHistory[i].length - 1; ++j) {
				meanHistory[i][j] = meanHistory[i][j + 1];
			}
			meanHistory[i][meanHistory[i].length - 1] = getMean(i);
		}
	}
	
	public void writeMarkovModelsToDisk(String trackName) {
		for (int i = 0; i < markovModels.length; ++i) {
			try {
				FileOutputStream fos = new FileOutputStream("models/" + trackName + "." + i + ".ser");
				ObjectOutputStream out = new ObjectOutputStream(fos);
				out.writeObject(markovModels[i]);
				out.close();
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	private Onset getOnsetForNote(int note) {
		for (int i = 0; i < markovModels.length; ++i) {
			if (note >= markovModels[i].noteRangeFrom && note <= markovModels[i].noteRangeTo) {
				return new Onset(truth.lastNote, lastNoteFrameNr + markovModels[i].historyRangeTo + 1, i);
			}
		}
		throw new IllegalArgumentException("No markov model found for note " + note);
	}
	
	private double getMean(int i) {
		double sum = 0;
		for (int j = 0; j < bandPassFilterBank.filteredSamples[i].length; ++j) {
			sum += Math.abs(bandPassFilterBank.filteredSamples[i][j]);
		}
		return sum / bandPassFilterBank.filteredSamples[i].length;
	}
}
