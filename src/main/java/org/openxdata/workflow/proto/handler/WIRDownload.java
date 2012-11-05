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
import org.openxdata.proto.model.OxdWorkitem;
import org.openxdata.proto.model.ParameterQuestionMap;
import org.openxdata.proto.model.WorkitemFormRef;
import org.openxdata.workflow.mobile.model.MQuestionMap;
import org.openxdata.workflow.mobile.model.MWorkItem;
import org.openxdata.workflow.mobile.model.WIRFormReference;

/**
 *
 */
public class WIRDownload implements RequestHandler {

	@Override
	public void handleRequest(WFSubmissionContext context) throws ProtocolException {
		List<OxdWorkitem> availableWorkitems = context.availableWorkitems();
		Vector<MWorkItem> workitems = new Vector<MWorkItem>();
		System.out.println("Using Classloader: " + getClass().getClassLoader().toString());
		for (OxdWorkitem workitem : availableWorkitems) {
			Vector<WIRFormReference> formRefs = new Vector<WIRFormReference>();
			List<WorkitemFormRef> frmRfrncObjcts = workitem.getWorkitemForms();
			for (WorkitemFormRef formRef : frmRfrncObjcts) {
				WIRFormReference wirFormRef = new WIRFormReference();
				wirFormRef.setStudyId((Integer) formRef.getStudyId());
				wirFormRef.setFormId((Integer) formRef.getFormVersionId());
				List<ParameterQuestionMap> preflled = formRef.getParamQuestionMap();
				Vector<MQuestionMap> questionMaps = new Vector<MQuestionMap>();
				for (ParameterQuestionMap strings : preflled) {
					MQuestionMap questionMap = new MQuestionMap();
					questionMap.setParameter(strings.getWorkitemParameter());
					questionMap.setQuestion(strings.getQuestionVariable());
					questionMap.setValue(strings.getValue());
					questionMap.setOutput(strings.isReadOnly());
					questionMaps.add(questionMap);
				}
				wirFormRef.setPrefilledQns(questionMaps);
				formRefs.add(wirFormRef);
			}
			MWorkItem wir = new MWorkItem();
			wir.setTaskName((String) workitem.getWorkitemName());
			wir.setCaseId((String) workitem.getWorkitemId());
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
