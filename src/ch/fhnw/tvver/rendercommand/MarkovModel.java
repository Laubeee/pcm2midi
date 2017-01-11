package ch.fhnw.tvver.rendercommand;

import java.io.Serializable;

public class MarkovModel implements Serializable {
	private static final int N_STATES = 4;
	
	public final int noteRangeFrom;
	public final int noteRangeTo;
	public final int historyRangeFrom;
	public final int historyRangeTo;
	
	public final int[] startCounts;
	public final int[][] transitionCounts;
	private final double[] startProbs;
	private final double[][] transitionProbs;
	
	public int nSamples;
	public int[] nTransitionsFrom;
	
	public MarkovModel(int noteRangeFrom, int noteRangeTo, int historyRangeFrom, int historyRangeTo) {
		this.noteRangeFrom = noteRangeFrom;
		this.noteRangeTo = noteRangeTo;
		
		assert historyRangeFrom > 0;
		this.historyRangeFrom = historyRangeFrom;
		this.historyRangeTo = historyRangeTo;
		
		startCounts = new int[N_STATES];
		transitionCounts = new int[N_STATES][N_STATES];
		startProbs = new double[N_STATES];
		transitionProbs = new double[N_STATES][N_STATES];
		
		nTransitionsFrom = new int[N_STATES];
	}
	
	public void train(double[] meanHistory) {
		int lastState = getState(meanHistory[historyRangeFrom - 1], meanHistory[historyRangeFrom]);
		++startCounts[lastState];
		++nSamples;
		for (int i = historyRangeFrom; i < historyRangeTo; ++i) {
			int currentState = getState(meanHistory[i], meanHistory[i + 1]);
			++transitionCounts[lastState][currentState];
			++nTransitionsFrom[lastState];
			lastState = currentState;
		}
	}
	
	public void calcProbs() {
		for (int i = 0; i < startCounts.length; ++i) {
			startProbs[i] = startCounts[i] / (double)(nSamples);
		}
		for (int i = 0; i < transitionCounts.length; ++i) {
			for (int j = 0; j < transitionCounts[i].length; ++j) {
				transitionProbs[i][j] = transitionCounts[i][j] / (double)(nTransitionsFrom[i]);
			}
		}
	}
	
	public double predict(double[] meanHistory) {
		int lastState = getState(meanHistory[historyRangeFrom - 1], meanHistory[historyRangeFrom]);
		double p = startProbs[lastState];
		for (int i = historyRangeFrom; i < historyRangeTo; ++i) {
			int currentState = getState(meanHistory[i], meanHistory[i + 1]);
			p *= transitionProbs[lastState][currentState];
			lastState = currentState;
		}
		
		return p;
	}
	
	private int getState(double meanFrom, double meanTo) {
		double changePercent = (meanTo / meanFrom - 1) * 100;
		if (changePercent < -10.0) {
			// <
			return 0;
		}
		else if (changePercent < 10.0) {
			// =
			return 1;
		}
		else if (changePercent < 500.0) {
			// >
			return 2;
		}
		else {
			// >>
			return 3;
		}
	}
	
	@Override
	public String toString() {
		return "nSamples=" + nSamples;
	}
}
