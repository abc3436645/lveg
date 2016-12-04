package edu.shanghaitech.ai.nlp.optimization;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import edu.shanghaitech.ai.nlp.lveg.GaussianMixture;
import edu.shanghaitech.ai.nlp.lveg.GrammarRule;
import edu.shanghaitech.ai.nlp.lveg.UnaryGrammarRule;
import edu.shanghaitech.ai.nlp.util.Recorder;

/**
 * Naive. The dedicated implementation is used to enable the program to run asap.
 * 
 * @author Yanpeng Zhao
 *
 */
public class SGDForMoG extends Recorder {
	
	protected Random random;
	protected short nsample;
	
	protected Map<String, List<Double>> truths;
	protected Map<String, List<Double>> sample;
	protected Map<String, List<Double>> ggrads;
	protected List<Double> wgrads;
	protected double wgrad;
	
	protected static double lr;
	
	/**
	 * To avoid the excessive 'new' operations.
	 */
	public SGDForMoG() {
		this.sample = new HashMap<String, List<Double>>();
		sample.put(GrammarRule.Unit.P, new ArrayList<Double>());
		sample.put(GrammarRule.Unit.C, new ArrayList<Double>());
		sample.put(GrammarRule.Unit.UC, new ArrayList<Double>());
		sample.put(GrammarRule.Unit.LC, new ArrayList<Double>());
		sample.put(GrammarRule.Unit.RC, new ArrayList<Double>());
		this.truths = new HashMap<String, List<Double>>();
		truths.put(GrammarRule.Unit.P, new ArrayList<Double>());
		truths.put(GrammarRule.Unit.C, new ArrayList<Double>());
		truths.put(GrammarRule.Unit.UC, new ArrayList<Double>());
		truths.put(GrammarRule.Unit.LC, new ArrayList<Double>());
		truths.put(GrammarRule.Unit.RC, new ArrayList<Double>());
		this.wgrads = new ArrayList<Double>();
		this.ggrads = new HashMap<String, List<Double>>();
		ggrads.put(GrammarRule.Unit.P, new ArrayList<Double>());
		ggrads.put(GrammarRule.Unit.C, new ArrayList<Double>());
		ggrads.put(GrammarRule.Unit.UC, new ArrayList<Double>());
		ggrads.put(GrammarRule.Unit.LC, new ArrayList<Double>());
		sample.put(GrammarRule.Unit.RC, new ArrayList<Double>());
	}
	
	
	public SGDForMoG(Random random) {
		this();
		this.random = random;
		this.nsample = 3;
		SGDForMoG.lr = 0.02;
	}
	
	
	public SGDForMoG(Random random, short nsample, double lr) {
		this();
		this.random = random;
		this.nsample = nsample;
		SGDForMoG.lr = lr;
	}
	
	
	/**
	 * @param rule
	 * @param ioScoreWithT
	 * @param ioScoreWithS
	 * @param scoresOfSAndT
	 */
	public void optimize(
			GrammarRule rule, 
			List<Map<String, GaussianMixture>> ioScoreWithT,
			List<Map<String, GaussianMixture>> ioScoreWithS,
			List<Double> scoresOfSAndT) {
		int size = ioScoreWithT.size();
		if (size != ioScoreWithS.size()) {
			logger.error("Rule count with the tree is not equal to that with the sentence.\n");
			return;
		}
		GaussianMixture ruleW = rule.getWeight();
		int ncomponent = ruleW.getNcomponent(), idx = -1;
		Map<String, GaussianMixture> iosWithT, iosWithS;
		double scoreT, scoreS, dRuleW;
		
		for (int i = 0; i < size; i++) {
			iosWithT = ioScoreWithT.get(i);
			iosWithS = ioScoreWithS.get(i);
			idx = getIdx(iosWithT.keySet(), iosWithS.keySet());
			scoreT = scoresOfSAndT.get(idx * 2);
			scoreS = scoresOfSAndT.get(idx * 2 + 1);
			for (int icomponent = 0; icomponent < ncomponent; icomponent++) {
				for (short isample = 0; isample < nsample; isample++) {
					clearSample(); // to ensure the correct sample is in use
					if (rule.isUnary()) {
						UnaryGrammarRule urule = (UnaryGrammarRule) rule;
						switch (urule.getType()) {
						case GrammarRule.GENERAL: {
							sample(sample.get(GrammarRule.Unit.P), ruleW.getDim(icomponent, GrammarRule.Unit.P));
							sample(sample.get(GrammarRule.Unit.UC), ruleW.getDim(icomponent, GrammarRule.Unit.UC));
							break;
						}
						case GrammarRule.LHSPACE: {
							sample(sample.get(GrammarRule.Unit.P), ruleW.getDim(icomponent, GrammarRule.Unit.P));
							/**
							 * For the rule A->w, count(A->w) = o(A->w) * i(A->w) = o(A->w) * w(A->w).
							 * For the sub-type rule r of A->w, count(a->w) = o(a->w) * w(a->w). The derivative
							 * of the objective function w.r.t w(r) is (count(r | T_S) - count(r | S)) / w(r), 
							 * which contains the term 1 / w(r), thus we could eliminate w(r) when computing it.
							 */
							if (icomponent == 0 && isample == 0) {
								iosWithT.remove(GrammarRule.Unit.C);
								iosWithS.remove(GrammarRule.Unit.C);
							}
							break;
						}
						case GrammarRule.RHSPACE: {
							sample(sample.get(GrammarRule.Unit.C), ruleW.getDim(icomponent, GrammarRule.Unit.C));
							break;
						}
						default: {
							logger.error("Not a valid unary grammar rule.\n");
						}
						}
					} else {
						sample(sample.get(GrammarRule.Unit.P), ruleW.getDim(icomponent, GrammarRule.Unit.P));
						sample(sample.get(GrammarRule.Unit.LC), ruleW.getDim(icomponent, GrammarRule.Unit.LC));
						sample(sample.get(GrammarRule.Unit.RC), ruleW.getDim(icomponent, GrammarRule.Unit.UC));
					}
					ruleW.restoreSample(icomponent, sample, truths);
					dRuleW = derivateRuleWeight(scoreT, scoreS, iosWithT, iosWithS);
					ruleW.derivative(isample, icomponent, dRuleW, sample, ggrads, wgrads);
				}
				ruleW.update(icomponent, lr, ggrads, wgrads);
			}
		}
	}
	
	
	private double derivateRuleWeight(
			double scoreT, 
			double scoreS, 
			Map<String, GaussianMixture> ioScoreWithT,
			Map<String, GaussianMixture> ioScoreWithS) {
		double cntWithT = 1.0, cntWithS = 1.0, dRuleW, part;
		for (Map.Entry<String, GaussianMixture> iosWithT : ioScoreWithT.entrySet()) {
			part = iosWithT.getValue().evalInsideOutside(truths.get(iosWithT.getKey()));
			cntWithT *= part;
		}
		for (Map.Entry<String, GaussianMixture> iosWithS : ioScoreWithS.entrySet()) {
			part = iosWithS.getValue().evalInsideOutside(truths.get(iosWithS.getKey()));
			cntWithS *= part;
		}
		if (scoreT <= 0 || scoreS <= 0) {
			logger.error("Invalid tree score or sentence score.\n");
			return -0.0;
		}
		dRuleW = cntWithS / scoreS - cntWithT / scoreT;
		return dRuleW;
	}
	
	
	protected void sample(List<Double> slice, int dim) {
		slice.clear();
		for (int i = 0; i < dim; i++) {
			slice.add(random.nextGaussian());
		}
	}
	
	
	protected void clearSample() {
		for (Map.Entry<String, List<Double>> slice : sample.entrySet()) {
			slice.getValue().clear();
		}
		for (Map.Entry<String, List<Double>> truth : truths.entrySet()) {
			truth.getValue().clear();
		}
	}
	
	
	private int getIdx(Set<String> strsWithT, Set<String> strsWithS) {
		int idx = -1;
		for (String str : strsWithT) {
			if (isNumeric(str) && strsWithS.contains(str)) {
				idx = Integer.valueOf(str);
				break;
			}
		}
		if (idx < 0) { logger.error("Not found the score of the tree or the score of the sentence.\n"); }
		return idx;
	}
	
	
	public static boolean isNumeric(String str){
	  return str.matches("[-+]?\\d*\\.?\\d+");  //match a number with optional '-' and decimal.
	}
	
}