/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.dm2e.omnom.test;

import junit.framework.TestCase;
import eu.dm2e.ws.grafeo.GLiteral;
import eu.dm2e.ws.grafeo.Grafeo;

/**
 *
 * @author kb
 */
public class GrafeoTest extends TestCase {

	private Grafeo g;
	
	public GrafeoTest(String testName) {
		super(testName);
	}
	
	@Override
	protected void setUp() throws Exception {
		this.g = new Grafeo();
		super.setUp();
	}
	
	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}
	// TODO add test methods here. The name must begin with 'test'. For example:

	/**
	 *
	 */
	public void testHello() {
		GLiteral lit = this.g.literal("Foo");
		assertEquals(lit.getValue(), "Foo");
	}
}