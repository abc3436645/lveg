package edu.shanghaitech.ai.nlp.lveg;

import org.junit.Test;

public class LVeGTesterTest {

	@Test
	public void testLVeGLearner() {
		String[] args = {"param.in"};
		try {
			LVeGTester.main(args);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
