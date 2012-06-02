package org.openxdata.workflow.proto.handler;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Vector;
import org.openxdata.mforms.model.ResponseHeader;
import org.openxdata.mforms.persistent.PersistentHelper;
import org.openxdata.proto.WFSubmissionContext;
import org.openxdata.proto.exception.ProtocolException;
import org.openxdata.workflow.mobile.model.MQuestionMap;
import org.openxdata.workflow.mobile.model.MWorkItem;
import org.openxdata.workflow.mobile.model.WIRFormReference;

/**
 *
 */
public class WIRDownload implements RequestHandler {

    @Override
    public void handleRequest(WFSubmissionContext context) throws ProtocolException {

        List<Object[]> availableWorkitems = context.availableWorkitems();
        Vector<MWorkItem> workitems = new Vector<MWorkItem>();
        System.out.println("Using Classloader: " + getClass().getClassLoader().toString());
        for (Object[] objects : availableWorkitems) {
            Vector<WIRFormReference> formRefs = new Vector<WIRFormReference>();
            List<Object[]> frmRfrncObjcts = (List<Object[]>) objects[2];
            for (Object[] frmRfrncObj : frmRfrncObjcts) {
                WIRFormReference wirFormRef = new WIRFormReference();
                wirFormRef.setStudyId((Integer) frmRfrncObj[0]);
                wirFormRef.setFormId((Integer) frmRfrncObj[1]);
                List<String[]> preflled = (List<String[]>) frmRfrncObj[2];
                Vector<MQuestionMap> questionMaps = new Vector<MQuestionMap>();
                for (String[] strings : preflled) {
                    MQuestionMap questionMap = new MQuestionMap();
                    questionMap.setParameter(strings[0]);
                    questionMap.setQuestion(strings[1]);
                    questionMap.setValue(strings[2]);
                    questionMap.setOutput(Boolean.valueOf(strings[3]));
                    questionMaps.add(questionMap);
                }
                wirFormRef.setPrefilledQns(questionMaps);
                formRefs.add(wirFormRef);
            }
            MWorkItem wir = new MWorkItem();
            wir.setTaskName((String) objects[0]);
            wir.setCaseId((String) objects[1]);
            wir.setFormReferences(formRefs);
            workitems.add(wir);
        }
        DataOutputStream outputStream = context.getOutputStream();
        try {
            outputStream.writeByte(ResponseHeader.STATUS_SUCCESS);
            PersistentHelper.writeMedium(workitems, outputStream);
        } catch (IOException ex) {
            throw new ProtocolException("Failed to write the workitems to the stream", ex);
        }

    }
}
