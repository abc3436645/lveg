package edu.shanghaitech.ai.nlp.lveg.impl;

import java.util.ArrayList;
import java.util.List;

import edu.berkeley.nlp.syntax.Tree;
import edu.shanghaitech.ai.nlp.lveg.model.GaussianMixture;
import edu.shanghaitech.ai.nlp.lveg.model.LVeGLexicon;
import edu.shanghaitech.ai.nlp.lveg.model.Parser;
import edu.shanghaitech.ai.nlp.lveg.model.Inferencer.Chart;
import edu.shanghaitech.ai.nlp.lveg.model.LVeGGrammar;
import edu.shanghaitech.ai.nlp.syntax.State;
import edu.shanghaitech.ai.nlp.util.MethodUtil;

/**
 * @author Yanpeng Zhao
 *
 */
public class LVeGParser<I, O> extends Parser<I, O> {
	/**
	 * 
	 */
	private static final long serialVersionUID = -1363406979999225830L;
	private LVeGInferencer inferencer;
	
	
	private LVeGParser(LVeGParser<?, ?> parser) {
		this.inferencer = parser.inferencer;
		this.maxLenParsing = parser.maxLenParsing;
		this.chart = parser.reuse ? new Chart(maxLenParsing, false) : null;
		this.reuse = parser.reuse;
	}
	
	
	public LVeGParser(LVeGGrammar grammar, LVeGLexicon lexicon, short maxLenParsing, boolean reuse) {
		this.maxLenParsing = maxLenParsing;
		this.inferencer = new LVeGInferencer(grammar, lexicon);
		this.chart = reuse ? new Chart(maxLenParsing, false) : null;
		this.reuse = reuse;
	}
	
	
	@Override
	public synchronized Object call() throws Exception {
		if (task == null) { return null; }
		Tree<State> sample = (Tree<State>) task;
		List<Double> scores = new ArrayList<Double>(2);
		double scoreT = doInsideOutsideWithTree(sample); 
		double scoreS = doInsideOutside(sample); 
		scores.add(scoreT);
		scores.add(scoreS);
//		logger.trace("\no---id=" + Thread.currentThread().getId() + ", isample=" + isample + " " + 
//				MethodUtil.double2str(scores, 3, -1, false, true) + " comes...\n"); // DEBUG
		synchronized (inferencer) {
//			logger.trace("\ni---id=" + Thread.currentThread().getId() + ", isample=" + isample + " enters...\n"); // DEBUG
			inferencer.evalRuleCountWithTree(sample, (short) 0);
			inferencer.evalRuleCount(sample, chart, (short) 0);
			inferencer.evalGradients(scores);
//			logger.trace("\ni---id=" + Thread.currentThread().getId() + ", isample=" + isample + " " + 
//					MethodUtil.double2str(scores, 3, -1, false, true) + " leaves...\n"); // DEBUG
		}
		Meta<O> cache = new Meta(itask, scores);
		synchronized (caches) {
			caches.add(cache);
			caches.notifyAll();
		}
		task = null;
		return null;
	}


	@Override
	public LVeGParser<?, ?> newInstance() {
		return new LVeGParser<I, O>(this);
	}
	
	
	public double evalRuleCount(Tree<State> tree, short isample) {
		double scoreS = doInsideOutside(tree); 
//		logger.trace("\nInside scores with the sentence...\n\n"); // DEBUG
//		MethodUtil.debugChart(Chart.getChart(true), (short) 2); // DEBUG
//		logger.trace("\nOutside scores with the sentence...\n\n"); // DEBUG
//		MethodUtil.debugChart(Chart.getChart(false), (short) 2); // DEBUG
		
		inferencer.evalRuleCount(tree, chart, isample);
		
//		logger.trace("\nCheck rule count with the sentence...\n"); // DEBUG
//		MethodUtil.debugCount(inferencer.grammar, inferencer.lexicon, tree, chart); // DEBUG
//		logger.trace("\nEval count with the sentence over.\n"); // DEBUG
		return scoreS;
	}
	
	
	public double evalRuleCountWithTree(Tree<State> tree, short isample) {
//		logger.trace("\nInside/outside scores with the tree...\n\n"); // DEBUG
		double scoreT = doInsideOutsideWithTree(tree); 
//		MethodUtil.debugTree(tree, false, (short) 2); // DEBUG

		// compute the rule counts
		inferencer.evalRuleCountWithTree(tree, isample);
		
//		logger.trace("\nCheck rule count with the tree...\n"); // DEBUG
//		MethodUtil.debugCount(inferencer.grammar, inferencer.lexicon, tree); // DEBUG
//		logger.trace("\nEval count with the tree over.\n"); // DEBUG
		return scoreT;
	}
	
	
	/**
	 * @param tree the parse tree
	 * @return
	 */
	private double doInsideOutside(Tree<State> tree) {
		List<State> sentence = tree.getYield();
		int nword = sentence.size();
		if (reuse) {
			chart.clear(nword);
		} else {
			if (chart != null) { chart.clear(-1); }
			chart = new Chart(nword, false);
		}
//		logger.trace("\nInside score...\n"); // DEBUG
		LVeGInferencer.insideScore(chart, sentence, nword);
//		MethodUtil.debugChart(Chart.iGetChart(), (short) 2); // DEBUG

//		logger.trace("\nOutside score...\n"); // DEBUG
		LVeGInferencer.setRootOutsideScore(chart);
		LVeGInferencer.outsideScore(chart, sentence, nword);
//		MethodUtil.debugChart(Chart.oGetChart(), (short) 2); // DEBUG
		
		GaussianMixture score = chart.getInsideScore((short) 0, Chart.idx(0, 1));
		double scoreS = score.eval(null, true);
		
//		logger.trace("Sentence score in logarithm: " + scoreS + ", Margin: " + score.marginalize(false) + "\n"); // DEBUG
//		logger.trace("\nEval rule count with the sentence...\n"); // DEBUG
		
		if (Double.isInfinite(scoreS) || Double.isNaN(scoreS)) {
			System.err.println("Fatal Error: Sentence score is smaller than zero: " + scoreS);
			return -0.0;
		}
		return scoreS;
	}
	
	
	/**
	 * Compute the inside and outside scores for 
	 * every non-terminal in the given parse tree. 
	 * 
	 * @param tree the parse tree
	 */
	private double doInsideOutsideWithTree(Tree<State> tree) {
//		logger.trace("\nInside score with the tree...\n"); // DEBUG	
		LVeGInferencer.insideScoreWithTree(tree);
//		MethodUtil.debugTree(tree, false, (short) 2); // DEBUG
		
//		logger.trace("\nOutside score with the tree...\n"); // DEBUG
		LVeGInferencer.setRootOutsideScore(tree);
		LVeGInferencer.outsideScoreWithTree(tree);
//		MethodUtil.debugTree(tree, false, (short) 2); // DEBUG
		
		// the parse tree score, which should contain only weights of the components
		GaussianMixture score = tree.getLabel().getInsideScore();
		double scoreT = score.eval(null, true);
		
//		logger.trace("\nTree score: " + scoreT + "\n"); // DEBUG
//		logger.trace("\nEval rule count with the tree...\n"); // DEBUG
		
		if (Double.isInfinite(scoreT) || Double.isNaN(scoreT)) {
			System.err.println("Fatal Error: Tree score is smaller than zero: " + scoreT + "\n");
			return -0.0;
		}
		return scoreT;
	}
	
}