package org.openxdata.workflow.proto.handler;

import org.apache.log4j.Logger;
import org.openxdata.mforms.model.ResponseHeader;
import org.openxdata.mforms.persistent.PersistentHelper;
import org.openxdata.proto.WFSubmissionContext;
import org.openxdata.proto.exception.ProtocolException;
import org.openxdata.proto.model.ParameterQuestionMap;
import org.openxdata.proto.model.WorkItem;
import org.openxdata.proto.model.WorkItemFormRef;
import org.openxdata.workflow.mobile.model.MQuestionMap;
import org.openxdata.workflow.mobile.model.MWorkItem;
import org.openxdata.workflow.mobile.model.WIRFormReference;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;

/**
 *
 */
public class WIRDownload implements RequestHandler {

    private final Logger log = Logger.getLogger(this.getClass().getName());

    @Override
    public void handleRequest(WFSubmissionContext context) throws ProtocolException {
        List<WorkItem> availableWorkitems = context.availableWorkitems();
        Vector<MWorkItem> workitems = new Vector<MWorkItem>();
        log.debug("Using ClassLoader: " + getClass().getClassLoader().toString());
        for (WorkItem workitem : availableWorkitems) {
            MWorkItem wir = toMobileWorkItem(workitem);
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

    private MWorkItem toMobileWorkItem(WorkItem workitem) {
        Vector<WIRFormReference> mobileFormRefs = getMobileFormReferences(workitem);
        MWorkItem wir = new MWorkItem();
        wir.setTaskName(workitem.getWorkitemName()+getTitleFromWiFormRef(mobileFormRefs));
        wir.setCaseId(workitem.getWorkitemId());
        wir.setFormReferences(mobileFormRefs);
        return wir;
    }

    String getTitleFromWiFormRef(Vector<WIRFormReference> workItemFormRefs) {
        if (workItemFormRefs == null)
            return "";

        StringBuilder builder = new StringBuilder();
        builder.append("{");
        for (WIRFormReference ref : workItemFormRefs) {

            builder.append(ref.getPrefilledString());
        }
        builder.append("}");
        return builder.toString();
    }

    private Vector<WIRFormReference> getMobileFormReferences(WorkItem workitem) {
        Vector<WIRFormReference> mobileFormRefs = new Vector<WIRFormReference>();
        List<WorkItemFormRef> workItemFormRefs = workitem.getWorkitemForms();
        for (WorkItemFormRef formRef : workItemFormRefs) {
            WIRFormReference wirFormRef = toMobileWiFormReference(formRef);
            mobileFormRefs.add(wirFormRef);
        }
        return mobileFormRefs;
    }

    private WIRFormReference toMobileWiFormReference(WorkItemFormRef formRef) {
        WIRFormReference wirFormRef = new WIRFormReference();
        wirFormRef.setStudyId(formRef.getStudyId());
        wirFormRef.setFormId(formRef.getFormVersionId());
        List<ParameterQuestionMap> prefills = formRef.getParamQuestionMap();
        Vector<MQuestionMap> questionMaps = new Vector<MQuestionMap>();
        for (ParameterQuestionMap strings : prefills) {
            MQuestionMap questionMap = toQuestionMap(strings);
            questionMaps.add(questionMap);
        }
        wirFormRef.setPrefilledQns(questionMaps);
        return wirFormRef;
    }

    private MQuestionMap toQuestionMap(ParameterQuestionMap paramQnMaps) {
        MQuestionMap questionMap = new MQuestionMap();
        questionMap.setParameter(paramQnMaps.getWorkitemParameter());
        questionMap.setQuestion(paramQnMaps.getQuestionVariable());
        questionMap.setValue(paramQnMaps.getValue());
        questionMap.setOutput(!paramQnMaps.isReadOnly());
        return questionMap;
    }

    private void sortByLabel(Vector<MWorkItem> workitems) {
        Collections.sort(workitems, new Comparator<MWorkItem>() {

            public int compare(MWorkItem o1, MWorkItem o2) {
                return o1.getDisplayName().compareToIgnoreCase(o2.getDisplayName());
            }
        });
    }
}
