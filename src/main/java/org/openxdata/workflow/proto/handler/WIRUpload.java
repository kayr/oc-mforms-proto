package org.openxdata.workflow.proto.handler;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

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
        context.setUploadResult(deserialise);
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
}
