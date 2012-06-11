package org.openxdata.workflow.proto.handler;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import java.util.logging.Level;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.log4j.Logger;
import org.openxdata.mforms.server.XFormSerializer;
import org.openxdata.mforms.model.ResponseHeader;
import org.openxdata.mforms.model.StudyData;
import org.openxdata.mforms.model.StudyDataList;
import org.openxdata.mforms.model.UploadResponse;
import org.openxdata.mformsproto.UploadDataProcessor;
import org.openxdata.proto.WFSubmissionContext;
import org.openxdata.proto.exception.ProtocolException;
import org.openxdata.workflow.mobile.model.MWorkItemData;
import org.openxdata.workflow.mobile.model.MWorkItemDataList;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 *
 */
public class WIRUpload implements RequestHandler {

    private Logger log = Logger.getLogger(this.getClass().getName());
    private WFSubmissionContext context;
    private XFormSerializer serializer = new XFormSerializer();

    @Override
    public void handleRequest(WFSubmissionContext context) throws ProtocolException {
        this.context = context;
        DataInputStream inputStream = context.getInputStream();
        MWorkItemDataList workitems = new MWorkItemDataList();
        try {
            workitems.read(inputStream);
        } catch (Exception ex) {
            throw new ProtocolException("Failed to read the the workitemList", ex);


        }
        Vector dataList = workitems.getDataList();
        if (dataList != null) {
            for (int i = 0; i < dataList.size(); i++) {
                MWorkItemData mWirData = (MWorkItemData) dataList.elementAt(i);
                processWIRData(mWirData);
            }
        } else {
            writeUploadResponse(context.getOutputStream());
        }
    }

    private void processWIRData(MWorkItemData mWirData) throws ProtocolException {
        DataOutputStream out = context.getOutputStream();

        UploadDataProcessor uploadProcessor = new UploadDataProcessor();
        serializer.addDeserializationListener(uploadProcessor);

        StudyDataList allStudies = mWirData.getFormData();
        Map<Integer, String> xForms = context.getXForms();

        log.debug("upload for Workitem: [" + mWirData.getCaseId() + "] contains:  forms=" + allStudies.getStudies().size());

        List<String> deserialise = deserialise(allStudies, xForms);
	    for (String string : deserialise) {
		    context.setUploadResult(processXML(mWirData.getCaseId(), string));
	    }
        writeUploadResponse(out);

    }

    private void writeUploadResponse(DataOutputStream out) throws ProtocolException {
        UploadResponse reponse = new UploadResponse();

        try {
            out.write(ResponseHeader.STATUS_SUCCESS);
        } catch (IOException ex) {
            throw new ProtocolException("Failed to write the Response Header in Upload", ex);
        }

        try {
            reponse.write(out);
        } catch (IOException ex) {
            throw new ProtocolException("Failed writiting response message to the stream");
        }
    }

    private List<String> deserialise(StudyDataList data, Map<Integer, String> xformMap) {
        List<String> xmlForms = new ArrayList<String>();
        try {
            Vector<StudyData> studies = data.getStudies();
            Method deserialiseMethod = XFormSerializer.class.getDeclaredMethod("deSerialize", StudyData.class, List.class, Map.class);
            deserialiseMethod.setAccessible(true);

            for (StudyData studyData : studies) {
                try {
                    deserialiseMethod.invoke(serializer, studyData, xmlForms, xformMap);
                } catch (Exception x) {
                    log.error("Error while deserialising study data id: " + studyData.getId(), x);
                }
            }
        } catch (Exception ex) {
            log.error("Exception while processing upload", ex);
            throw new RuntimeException(ex);
        }
        return xmlForms;
    }
    
	String processXML(String workitemID, String xml) {
		try {
			String eventID = extractEventOID(workitemID);
			String newXML = updateXMLWithEventAttribute(eventID, xml);
			return newXML;
		} catch (Exception ex) {
			log.error("Error while update the workitem XML", ex);
		}
		return xml;
	}
    
    
	String extractEventOID(String workItemID) {
		String result = "UNKNOWN";
		if (workItemID == null) {
			return result;
		}
		int begin = workItemID.indexOf('&');
		if (begin == -1) {
			return result;
		}
		workItemID = workItemID.trim();
		
		try {
			result = workItemID.substring(begin + 1);
			if(result.isEmpty())
				result = "UNKNOWN";
		} catch (Exception e) {
			return result;
		}
		return result;
	}
	
	private String updateXMLWithEventAttribute(String eventOID, String xml) throws ParserConfigurationException, SAXException, IOException, TransformerConfigurationException, TransformerException {
		Document doc = getDocument(xml);
		Node firstChild = doc.getFirstChild();
		NamedNodeMap attributes = firstChild.getAttributes();
		Attr studyEventAttr = doc.createAttribute("StudyEventOID");
		studyEventAttr.setValue(eventOID);
		attributes.setNamedItem(studyEventAttr);
		String resultXML = fromDocToString(doc);
		return resultXML;
	}
	
	private String fromDocToString(Document doc) throws TransformerConfigurationException, TransformerException{
		Transformer transformer = getTranformer();
		
		StreamResult result = new StreamResult(new StringWriter());
		DOMSource source = new DOMSource(doc);
		transformer.transform(source, result);
		String xml = result.getWriter().toString();
		return xml;
	}
	
	private Transformer mTranformer;

	private Transformer getTranformer() throws IllegalArgumentException, TransformerConfigurationException, TransformerFactoryConfigurationError {
		if (mTranformer == null) {
			mTranformer = TransformerFactory.newInstance().newTransformer();
			mTranformer.setOutputProperty(OutputKeys.INDENT, "yes");
		}
		return mTranformer;
	}
	
	private DocumentBuilder mDocBuilder;

	private Document getDocument(String xml) throws SAXException, IOException, ParserConfigurationException {
		if (mDocBuilder == null) {
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			mDocBuilder = docFactory.newDocumentBuilder();

		}
		Document doc = mDocBuilder.parse(new ByteArrayInputStream(xml.getBytes()));
		return doc;
	}
	
	
}
