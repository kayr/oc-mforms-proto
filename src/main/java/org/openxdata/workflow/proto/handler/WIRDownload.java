package org.openxdata.workflow.proto.handler;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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
		List<Map<String, Object>> availableWorkitems = context.availableWorkitems();
        Vector<MWorkItem> workitems = new Vector<MWorkItem>();
        System.out.println("Using Classloader: " + getClass().getClassLoader().toString());
        for (Map<String, Object> wirMap : availableWorkitems) {
            Vector<WIRFormReference> formRefs = new Vector<WIRFormReference>();
            List<Map<String, Object>> frmRfrncObjcts = (List<Map<String, Object>>) wirMap.get("formrefs");
            for (Map<String, Object> formRef : frmRfrncObjcts) {
                WIRFormReference wirFormRef = new WIRFormReference();
                wirFormRef.setStudyId((Integer) formRef.get("studyid"));
                wirFormRef.setFormId((Integer) formRef.get("formid"));
                List<String[]> preflled = (List<String[]>) formRef.get("prefills");
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
            wir.setTaskName((String) wirMap.get("name"));
            wir.setCaseId((String) wirMap.get("id"));
            wir.setFormReferences(formRefs);
            workitems.add(wir);
        }
	sortByLabel(workitems);
        DataOutputStream outputStream = context.getOutputStream();
        try {
            outputStream.writeByte(ResponseHeader.STATUS_SUCCESS);
            PersistentHelper.writeMedium(workitems, outputStream);
        } catch (IOException ex) {
            throw new ProtocolException("Failed to write the workitems to the stream", ex);
        }

    }

	private void sortByLabel(Vector<MWorkItem> workitems) {
		Collections.sort(workitems, new Comparator<MWorkItem>() {

			public int compare(MWorkItem o1, MWorkItem o2) {
				return o1.getDisplayName().compareToIgnoreCase(o2.getDisplayName());
			}
		});
	}
}
