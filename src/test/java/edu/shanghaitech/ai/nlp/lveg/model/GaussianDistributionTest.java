package edu.shanghaitech.ai.nlp.lveg.model;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import edu.shanghaitech.ai.nlp.lveg.impl.DiagonalGaussianDistribution;
import edu.shanghaitech.ai.nlp.lveg.model.GaussianDistribution;

public class GaussianDistributionTest {
	
	@Test
	public void testGaussianDistribution() {
		GaussianDistribution gd = new DiagonalGaussianDistribution();
		System.out.println(gd);
	}
	
	
	@Test
	public void testInstanceEqual() {
		GaussianDistribution gd0 = new DiagonalGaussianDistribution();
		GaussianDistribution gd1 = new DiagonalGaussianDistribution();
		GaussianDistribution gd2 = new DiagonalGaussianDistribution((short) 5);
		
		assertFalse(gd0 == gd1);
		assertTrue(gd0.equals(gd1));
		
		gd0.getMus().add(2.0);
		gd0.getMus().add(3.0);
		gd0.getVars().add(4.0);
		
		gd1.getMus().add(2.0);
		gd1.getMus().add(3.0);
		gd1.getVars().add(4.0);
		
		assertTrue(gd0.equals(gd1));
		
		gd1.getMus().add(2.0);
		assertFalse(gd0.equals(gd1));
	}
	
	
	@Test
	public void testDoubleEqual() {
		List<Double> xx = new ArrayList<Double>();
		List<Double> yy = new ArrayList<Double>();
		
		xx.add(1.0);
		xx.add(2.0);
		
		yy.addAll(xx);
		
		assertTrue(xx.equals(yy));
		
		xx.set(0, 3.0);
		
		System.out.println(xx);
		System.out.println(yy);
		xx.clear();
		System.out.println(yy);
	}

	
	@Test
	public void testStringEqual() {
		String str0 = new String("nihaoa");
		String str1 = "nihaoa";
		String str2 = str1;
		String str3 = new String("nihaoa");
		
		assertFalse(str0 == str3);
		assertTrue(str0.equals(str3));
		
		assertTrue(str1 == str2);
		assertTrue(str0.equals(str1));
	}
}
