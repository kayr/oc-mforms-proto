package org.openxdata.workflow.proto.handler

import org.gmock.GMockTestCase
import org.junit.Test
import org.openxdata.mforms.model.QuestionData
import org.openxdata.mforms.model.QuestionDef
import org.openxdata.proto.WFSubmissionContext
import org.openxdata.proto.model.WorkItem

/**
 * Created with IntelliJ IDEA.
 * User: kay
 * Date: 9/1/13
 * Time: 12:59 PM
 * To change this template use File | Settings | File Templates.
 */
class WIRUploadTest extends GMockTestCase {


    WIRUpload uploader = new WIRUpload()

    @Test
    void testHandleRequest() {

    }

    void testProcessFormDataAndWorkItem() {

    }

    @Test
    void testGetParams() {

        WFSubmissionContext context = mock(WFSubmissionContext)

        context.getLoggedInUser().returns([1, 'admin', 'dd', 'kdk'] as Object[])

        uploader.context = context


        def text = getClass().getResourceAsStream('/SpecConfig.groovy').text
        WorkItem wi = new WorkItem('AFP:21', 'Collect')
        wi.addParameter('spec.settings', text)

        play {
            def params = uploader.getParams(wi)
            assert params.size() == 2
        }
    }

    @Test
    void testGetParamsForNonRegisteredUser() {

        WFSubmissionContext context = mock(WFSubmissionContext)

        context.getLoggedInUser().returns([1, 'admin2', 'dd', 'kdk'] as Object[])

        uploader.context = context


        def text = getClass().getResourceAsStream('/SpecConfig.groovy').text
        WorkItem wi = new WorkItem('AFP:21', 'Collect')
        wi.addParameter('spec.settings', text)

        play {
            def params = uploader.getParams(wi)
            assert params.size() == 0
        }
    }

    @Test
    void testGetTextAnswerFromBoolean() {
        def qnData = new QuestionData(new QuestionDef((short) 1, 'Is Boolean', '', false, QuestionDef.QTN_TYPE_BOOLEAN, null, true, true, false, 'is_bool', new Object()))

        WIRUpload upload = new WIRUpload()
        assert upload.extractAnswer(qnData) == 'false'

        qnData.setAnswer(true)
        assert upload.extractAnswer(qnData) == 'true'
    }
}
