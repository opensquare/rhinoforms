package com.rhinoforms;

import java.io.FileInputStream;
import java.net.URLDecoder;
import java.util.Map;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

public class RemoteSubmissionHelperTest {
	
	private RemoteSubmissionHelper remoteSubmissionHelper;
	private Document dataDocument;
	private DocumentHelper documentHelper;
	private TestConnectionFactory testConnectionFactory;
	private String dataDocumentString;
	
	@Before
	public void setup() throws Exception {
		documentHelper = new DocumentHelper();
		remoteSubmissionHelper = new RemoteSubmissionHelper(new TestResourceLoader());
		testConnectionFactory = new TestConnectionFactory();
		remoteSubmissionHelper.setConnectionFactory(testConnectionFactory);
		dataDocumentString = "<myData><something>a</something></myData>";
		dataDocument = TestUtil.createDocument(dataDocumentString);
	}
	
	@Test
	public void testSimplestHandleSubmission() throws Exception {
		Submission submission = new Submission("http://localhost/dummyURL");
		submission.getData().put("xml", "[dataDocument]");
		testConnectionFactory.setResultXmlString("<submissionResult>one</submissionResult>");
		
		remoteSubmissionHelper.handleSubmission(submission, dataDocument);
		
		String submittedData = new String(testConnectionFactory.getByteArrayOutputStream().toByteArray());
		Assert.assertEquals("xml=<myData><something>a</something></myData>", URLDecoder.decode(submittedData, "UTF-8"));
		String dataDocumentStringAfterSubmission = documentHelper.documentToString(dataDocument);
		Assert.assertEquals("<myData><something>a</something></myData>", dataDocumentStringAfterSubmission);
	}
	
	@Test
	public void testHandleSubmissionInsertResult() throws Exception {
		Submission submission = new Submission("http://localhost/dummyURL");
		submission.getData().put("xml", "[dataDocument]");
		submission.setResultInsertPoint("/myData");
		testConnectionFactory.setResultXmlString("<submissionResult>one</submissionResult>");
		
		remoteSubmissionHelper.handleSubmission(submission, dataDocument);
		
		String submittedData = new String(testConnectionFactory.getByteArrayOutputStream().toByteArray());
		Assert.assertEquals("xml=" + dataDocumentString, URLDecoder.decode(submittedData, "UTF-8"));
		String dataDocumentStringAfterSubmission = documentHelper.documentToString(dataDocument);
		Assert.assertEquals("<myData><something>a</something><submissionResult>one</submissionResult></myData>", dataDocumentStringAfterSubmission);
	}
	
	@Test
	public void testHandleSubmissionBadResult() {
		Submission submission = new Submission("http://localhost/dummyURL");
		submission.getData().put("xml", "[dataDocument]");
		submission.setResultInsertPoint("/myData");
		testConnectionFactory.setResultXmlString("<submissionResult>one</submissionResult>");
		testConnectionFactory.setTestResponseCode(500);
		testConnectionFactory.setTestResponseMessage("Something went very wrong.");
		
		try {
			remoteSubmissionHelper.handleSubmission(submission, dataDocument);
			Assert.fail("Should have thrown Exception");
		} catch (RemoteSubmissionHelperException e) {
			// Pass
			Assert.assertEquals("Bad response from target service. Status:500, message:Something went very wrong.", e.getMessage());
		}
	}
	
	@Test
	public void testHandleSubmissionPreTransform() throws Exception {
		Submission submission = new Submission("http://localhost/dummyURL");
		submission.setPreTransform("xslt/toServerFormat.xsl");
		submission.getData().put("xml", "[dataDocument]");
		submission.setResultInsertPoint("/myData");
		testConnectionFactory.setResultXmlString("<submissionResult>one</submissionResult>");
		
		remoteSubmissionHelper.handleSubmission(submission, dataDocument);
		
		String submittedData = new String(testConnectionFactory.getByteArrayOutputStream().toByteArray());
		Assert.assertEquals("xml=<serverData><abc>a</abc></serverData>", URLDecoder.decode(submittedData, "UTF-8"));
		String dataDocumentStringAfterSubmission = documentHelper.documentToString(dataDocument);
		Assert.assertEquals("<myData><something>a</something><submissionResult>one</submissionResult></myData>", dataDocumentStringAfterSubmission);
	}
	
	@Test
	public void testHandleSubmissionPostTransform() throws Exception {
		Submission submission = new Submission("http://localhost/dummyURL");
		submission.getData().put("myXml", "[dataDocument]");
		submission.setPostTransform("xslt/fromServerFormat.xsl");
		submission.setResultInsertPoint("/myData");
		testConnectionFactory.setResultXmlString("<serverData><wrapper><premium123>10.00</premium123></wrapper></serverData>");
		
		remoteSubmissionHelper.handleSubmission(submission, dataDocument);
		
		String submittedData = new String(testConnectionFactory.getByteArrayOutputStream().toByteArray());
		Assert.assertEquals("myXml=" + dataDocumentString, URLDecoder.decode(submittedData, "UTF-8"));
		String dataDocumentStringAfterSubmission = documentHelper.documentToString(dataDocument);
		Assert.assertEquals("<myData><something>a</something><submissionResult><premium>10.00</premium></submissionResult></myData>", dataDocumentStringAfterSubmission);
	}
	
}
