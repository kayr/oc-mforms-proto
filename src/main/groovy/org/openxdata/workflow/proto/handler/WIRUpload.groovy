package org.openxdata.workflow.proto.handler

import groovy.xml.MarkupBuilder
import org.apache.log4j.Logger
import org.openxdata.mforms.model.FormData
import org.openxdata.mforms.model.QuestionData
import org.openxdata.mforms.model.RepeatQtnsDataList
import org.openxdata.mforms.model.ResponseHeader
import org.openxdata.mforms.server.DeserializationListenerAdapter
import org.openxdata.proto.WFSubmissionContext
import org.openxdata.proto.exception.ProtocolException
import org.openxdata.proto.model.WorkItem
import org.openxdata.workflow.mobile.model.MWorkItemData
import org.openxdata.workflow.mobile.model.MWorkItemDataList
import org.openxdata.workflow.mobile.model.WIRUploadResponse
import org.openxdata.workflow.mobile.model.WIRUploadResponseList
import org.openxdata.workflow.proto.handler.WIRUploadProcessor.FormDataXml

import java.text.SimpleDateFormat

import static org.openxdata.mforms.model.QuestionDef.*

public class WIRUpload extends DeserializationListenerAdapter implements RequestHandler {

    private final Logger log = Logger.getLogger(this.getClass().getName());
    WFSubmissionContext context;
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

        Vector<MWorkItemData> workItemDataList = workitems.getDataList();

        if (workItemDataList) {
            for (workitemData in workItemDataList) {
                processWorkItemData(workitemData);
            }
        }

        writeUploadResponse(context.getOutputStream());

    }

    private void processWorkItemData(MWorkItemData mWirData) {

        @SuppressWarnings("unchecked") Vector<FormData> formData = mWirData.getFormDataList();
        Map<Integer, String> xForms = context.getXForms();

        WIRUploadProcessor uploadProcessor = new WIRUploadProcessor(formData, xForms);

        log.debug("upload for Workitem: [${mWirData.getCaseId()}] contains:  forms=${formData.size()}");


        List<FormDataXml> formDataXmlList = uploadProcessor.getFormStudies();

        for (FormDataXml formDataXml : formDataXmlList) {
            try {
                processFormDataAndWorkItem(formDataXml, mWirData.getCaseId());
            } catch (Exception e) {
                Throwable t = e.cause ?: e
                log.error("Error While saving Form Data: ", t);
                addToUploadResponse(mWirData, formDataXml, t);
            }
        }
    }

    void processFormDataAndWorkItem(FormDataXml formDataXml, String caseId) {

        WorkItem workItem = context.getWorkitem(caseId)

        if (!workItem) {
            log.error "workitem for caseId[$caseId] not found.. discarding"
            return
        }

        Map<String, String> paramsQuestionMapping = context.getOutParamsQuestionMapping(formDataXml.getDefId(), caseId);

        FormData mobileFormData = formDataXml.formData
        String formVarName = mobileFormData.def.variableName


        log.debug "processing workitem [$workItem.workitemName - $caseId]"
        StringWriter writer = new StringWriter()
        def xml = new MarkupBuilder(writer)

        def formDataId = context.setUploadResult(formDataXml.getXml());
        def params = getParams(workItem)
        xml."${workItem.parameters.decomposition}" {
            paramsQuestionMapping.each { wirParameterName, qnVariableName ->

                QuestionData question = mobileFormData.getQuestion("/$formVarName/$qnVariableName");

                if (question == null) {

                    log.warn("ParamQuestion entry form [$formVarName] workItem [$workItem.workitemName-$workItem.workitemId] = [$wirParameterName - $qnVariableName] Has no corresponding question");

                } else {//To continue.. Wrong entry
                    // WorkItemQuestion qn = new WorkItemQuestion(paramQnEntry.getKey(), extractAnswer(question));
                    if (workItem.workitemId.contains(':')) {
                        def answer = params."$wirParameterName" ?: extractAnswer(question)
                        xml."${wirParameterName}"(answer)
                    } else {
                        xml."${wirParameterName}"(extractAnswer(question))
                    }
                }

            }
            if (!workItem.workitemId.contains(':'))
                xml.formDataID(formDataId)
        }
        context.submitWorkitem(caseId, writer.toString());
    }


    private String extractAnswer(QuestionData questionData) {
        def questionType = questionData.getDef().getType()

        if (questionData.getAnswer() == null && questionType != QTN_TYPE_NUMERIC && questionType != QTN_TYPE_DATE) {
            log.warn("Extracting answer from question: [$questionData] and Answer = [null]");
            return null;
        }

        String answer = null
        switch (questionType) {
            case QTN_TYPE_AUDIO:
            case QTN_TYPE_VIDEO:
            case QTN_TYPE_IMAGE:
                answer = encodeMultimedia(questionData)
                break
            case QTN_TYPE_BOOLEAN:
                answer = questionData.getTextAnswer().toString().equalsIgnoreCase("Yes").toString()
                break
            case QTN_TYPE_NUMERIC:
                answer = extractNumber(questionData)
                break
            case QTN_TYPE_DATE:
                answer = extractDate(questionData)
                break
            case QTN_TYPE_REPEAT:
                answer = extractRepeat(questionData)
                break
            default:
                answer = questionData.textAnswer
                break

        }
        log.trace("Extracting answer from question: [${questionData.getDef().getText()}] and Answer = [${answer.length() > 100 ? answer[0..100] : answer}]");
        return answer;
    }

    private String extractRepeat(QuestionData questionData) {
        StringWriter writer = new StringWriter();
        def xml = new MarkupBuilder(writer)
        xml.doubleQuotes = true
        RepeatQtnsDataList repeatDataLst = (RepeatQtnsDataList) questionData.getAnswer();
        Vector repeatQuestionsData = repeatDataLst.getRepeatQtnsData();
        xml.repeatValues {
            for (def repeatQuestionData : repeatQuestionsData) {
                xml.questions {
                    repeatQuestionData.each { QuestionData qn ->
                        xml."${qn.def.variableName}"(qn.valueAnswer)
                    }
                }
            }
        }
        writer.toString()
    }

    private String extractDate(QuestionData parentQuestion) {
        Object dateObject = parentQuestion.getAnswer();
        Date date = null;
        String finalAnswer
        if (dateObject instanceof Date)//Fix where in some cases answer is of type string
        {
            date = (Date) parentQuestion.getAnswer();
        }
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        if (date == null) {
            date = new Date(1);
            finalAnswer = df.format(date);
        } else {
            finalAnswer = df.format(date);
        }
        finalAnswer
    }

    private String extractNumber(QuestionData parentQuestion) {
        def number = parentQuestion.getTextAnswer();
        log.trace("Question [${parentQuestion.def.text}] type is Numeric.");
        try {
            Integer.parseInt(number);
        } catch (Exception x) {
            number = "0";
        }
        number
    }

    private String encodeMultimedia(QuestionData parentQuestion) {
        StringWriter writer = new StringWriter();
        def xml = new MarkupBuilder(writer)
        xml.doubleQuotes = true
        def answer = ((byte[]) parentQuestion.getAnswer()).encodeBase64().toString()

        xml.base64(answer)
        answer = writer.toString()
        log.debug("Encoding Multimedia for question: ${parentQuestion.getDef().getVariableName()} :$answer:");
        answer
    }

    private void addToUploadResponse(MWorkItemData wirData, FormDataXml formDataXml, Throwable e) {
        WIRUploadResponse wirResponse = new WIRUploadResponse();
        wirResponse.setErrorMessage(e.getMessage());
        wirResponse.setFormDataId(formDataXml.getFormData().getRecordId());
        wirResponse.setFormDefId(formDataXml.getFormData().getDefId());
        wirResponse.setWirRecId(wirData.getWirRecId());
        this.wirResponseList.addWIRUploadResponse(wirResponse);

    }

    Map getParams(WorkItem workItem) {
        String script = workItem.parameters."spec.settings"

        if (!workItem.workitemId.contains(':') || !script) return [:]

        def user = context.getLoggedInUser()

        log.trace("Reading default launch settings")
        Binding binding = new Binding(user: user[1])
        GroovyShell gs = new GroovyShell(binding)

        ConfigObject config = gs.evaluate(script)
        log.info("Default Launch Setting: $config")
        if (config.isEmpty())
            log.warn("No configuration found for ")
        return config
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
            throw new ProtocolException("Failed writing response message to the stream");
        }
    }

}
