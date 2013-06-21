package org.openxdata.workflow.proto.handler;

import org.apache.log4j.Logger;
import org.openxdata.mforms.model.FormData;
import org.openxdata.mforms.model.ResponseHeader;
import org.openxdata.mforms.server.DeserializationListenerAdapter;
import org.openxdata.proto.WFSubmissionContext;
import org.openxdata.proto.exception.ProtocolException;
import org.openxdata.workflow.mobile.model.MWorkItemData;
import org.openxdata.workflow.mobile.model.MWorkItemDataList;
import org.openxdata.workflow.mobile.model.WIRUploadResponse;
import org.openxdata.workflow.mobile.model.WIRUploadResponseList;
import org.openxdata.workflow.proto.handler.WIRUploadProcessor.FormStudy;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/**
 *
 */
public class WIRUpload extends DeserializationListenerAdapter implements RequestHandler {

    private final Logger log = Logger.getLogger(this.getClass().getName());
    private WFSubmissionContext context;
    private WIRUploadResponseList wirResponseList = new WIRUploadResponseList();

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

        Vector wirDataList = workitems.getDataList();
        if (wirDataList != null) {
            for (int i = 0; i < wirDataList.size(); i++) {
                MWorkItemData mWirData = (MWorkItemData) wirDataList.elementAt(i);
                processWIRData(mWirData);
            }
        }

        writeUploadResponse(context.getOutputStream());

    }

    private void processWIRData(MWorkItemData mWirData) {

        @SuppressWarnings("unchecked") Vector<FormData> formData = mWirData.getFormDataList();
        Map<Integer, String> xForms = context.getXForms();

        WIRUploadProcessor uploadProcessor = new WIRUploadProcessor(formData, xForms);

        log.debug("upload for Workitem: [" + mWirData.getCaseId() + "] contains:  forms=" + formData.size());


        List<FormStudy> formStudies = uploadProcessor.getFormStudies();

        for (FormStudy string : formStudies) {
            try {
                context.setUploadResult(addSubjectKeyToXML(mWirData.getCaseId(), string.getXml()));
            } catch (Exception e) {
                log.error("Error While saving Form Data: " + e);
                addToUploadResponse(mWirData, string, e);
            }
        }


    }

    private void addToUploadResponse(MWorkItemData wirData, FormStudy formStudy, Exception e) {
        WIRUploadResponse wirResponse = new WIRUploadResponse();
        wirResponse.setErrorMessage(e.getMessage());
        wirResponse.setFormDataId(formStudy.getFormData().getDataId());
        wirResponse.setFormDefId(formStudy.getFormData().getDefId());
        wirResponse.setWirRecId(wirData.getWirRecId());
        this.wirResponseList.addWIRUploadResponse(wirResponse);

    }

    private void writeUploadResponse(DataOutputStream out) throws ProtocolException {


        try {
            out.write(ResponseHeader.STATUS_SUCCESS);
        } catch (IOException ex) {
            throw new ProtocolException("Failed to write the Response Header in Upload", ex);
        }

        try {
            wirResponseList.write(out);
        } catch (IOException ex) {
            throw new ProtocolException("Failed writiting response message to the stream");
        }
    }

    String addSubjectKeyToXML(String workitemID, String xml) {
        try {
            String eventID = extractEventOID(workitemID);
            return updateXMLWithEventAttribute(eventID, xml);
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

        int begin = workItemID.trim().indexOf('&');
        if (begin > 0 && begin < workItemID.length() - 1) {
            result = workItemID.substring(begin + 1);
        }
        return result;
    }

    private String updateXMLWithEventAttribute(String eventOID, String xml) throws ParserConfigurationException, SAXException, IOException, TransformerException {
        Document doc = getDocument(xml);
        Node firstChild = doc.getFirstChild();
        NamedNodeMap attributes = firstChild.getAttributes();
        Attr studyEventAttr = doc.createAttribute("StudyEventOID");
        studyEventAttr.setValue(eventOID);
        attributes.setNamedItem(studyEventAttr);
        String resultXML = fromDocToString(doc);
        return resultXML;
    }

    private String fromDocToString(Document doc) throws TransformerException {
        Transformer transformer = getTransformer();

        StreamResult result = new StreamResult(new StringWriter());
        DOMSource source = new DOMSource(doc);
        transformer.transform(source, result);
        String xml = result.getWriter().toString();
        return xml;
    }

    private Transformer mTranformer;

    private Transformer getTransformer() throws IllegalArgumentException, TransformerConfigurationException, TransformerFactoryConfigurationError {
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
