package org.openxdata.workflow.proto.handler;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.openxdata.proto.WFSubmissionContext;

/**
 *
 * @author kay
 */
public class WIRUploadTest {

	public WIRUploadTest() {
	}

	@BeforeClass
	public static void setUpClass() throws Exception {
	}

	@AfterClass
	public static void tearDownClass() throws Exception {
	}

	@Test
	public void extractEventOID() throws Exception {
		WIRUpload instance = new WIRUpload();
		String result = instance.extractEventOID(null);
		assertEquals("UNKNOWN", result);

		result = instance.extractEventOID("BLAHEH");
		assertEquals("UNKNOWN", result);

		result = instance.extractEventOID("SHSHSH&SC3");
		assertEquals("SC3", result);

		result = instance.extractEventOID("SHSHSH&");
		assertEquals("UNKNOWN", result);
	}

	@Test
	public void processXML() throws Exception {
		WIRUpload instance = new WIRUpload();
		String xml = "<?xml version=\"1.0\" ?>"
			+ "<earth>"
			+ "    <country>us</country>"
			+ "</earth>";
		String expectedResult = "<?xmlversion=\"1.0\"encoding=\"UTF-8\"standalone=\"no\"?>"
			+ "<earth StudyEventOID=\"SC3\">"
			+ "    <country>us</country>"
			+ "</earth>";
		expectedResult = expectedResult.replaceAll("\\s+", "");

		String result = instance.addSubjectKeyToXML("S0053&SC3", xml);
		result = result.replaceAll("\\s+", "");
		assertEquals(expectedResult, result);

	}
}
