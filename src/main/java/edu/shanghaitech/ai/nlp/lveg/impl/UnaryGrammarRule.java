package edu.shanghaitech.ai.nlp.lveg.impl;

import edu.shanghaitech.ai.nlp.lveg.model.GaussianMixture;
import edu.shanghaitech.ai.nlp.lveg.model.GrammarRule;


/**
 * @author Yanpeng Zhao
 *
 */
public class UnaryGrammarRule extends GrammarRule implements Comparable<Object> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 7796142278063439713L;
	/**
	 * the ID of the right-hand side nonterminal
	 */
	public int rhs;
	
	
	public UnaryGrammarRule() {
		// TODO
	}
	
	
	/**
	 * This constructor returns an instance without initializing the weight.
	 * 
	 * @param lhs  id of the tag on the left hand side
	 * @param rhs  id of the tag on the right hand side 
	 * 
	 */
	public UnaryGrammarRule(short lhs, int rhs) {
		this.lhs = lhs;
		this.rhs = rhs;
		this.type = LRURULE;
		
	}
	
	
	public UnaryGrammarRule(short lhs, int rhs, byte type) {
		this.lhs = lhs;
		this.rhs = rhs;
		this.type = type;
		initializeWeight(type);
	}
	
	
	public UnaryGrammarRule(short lhs, int rhs, byte type, GaussianMixture weight) {
		this.lhs = lhs;
		this.rhs = rhs;
		this.type = type;
		this.weight = weight;
	}
	
	
	private void initializeWeight(byte type) {
		weight = rndRuleWeight(type);
	}
	
	
	public GrammarRule copy() {
		UnaryGrammarRule rule = new UnaryGrammarRule(lhs, rhs);
		rule.weight = weight.copy(true);
		rule.type = type;
		return rule;
	}


	@Override
	public boolean isUnary() {
		return true;
	}
	
	
	@Override
	public int hashCode() {
		return (lhs << 18) ^ (rhs);
	}
	
	
	@Override
	public boolean equals(Object o) {
		if (this == o) { return true; }
		
		if (o instanceof UnaryGrammarRule) {
			UnaryGrammarRule rule = (UnaryGrammarRule) o;
			if (lhs == rule.lhs && rhs == rule.rhs && type == rule.type) {
				return true;
			}
		}
		return false;
	}


	@Override
	public int compareTo(Object o) {
		// TODO Auto-generated method stub
		UnaryGrammarRule rule = (UnaryGrammarRule) o;
		if (lhs < rule.lhs) { return -1; }
		if (lhs > rule.lhs) { return 1; }
		if (rhs < rule.rhs) { return -1; }
		if (rhs > rule.rhs) { return 1; }
		return 0;
	}
	
	
	@Override
	public String toString() {
		return "U-Rule [P: " + lhs +", UC: " + rhs + ", T: " + (short) type + "]";
	}

}
