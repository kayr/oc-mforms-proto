package org.openxdata.workflow.proto.handler;

import org.kxml2.kdom.Document;
import org.openxdata.mforms.model.FormData;
import org.openxdata.mforms.server.FormNotFoundException;
import org.openxdata.mforms.server.XForm;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/**
 * @author kay
 */
public class WIRUploadProcessor {

    private List<FormDataXml> formStudies = new ArrayList<FormDataXml>();
    private Vector<FormData> dataList;
    private Map<Integer, String> xformMap;

    public WIRUploadProcessor(Vector<FormData> dataList, Map<Integer, String> xformMap) {
        this.dataList = dataList;
        this.xformMap = xformMap;

    }

    public List<FormDataXml> getFormStudies() {
        try {
            if (formStudies.isEmpty()) {
                deserialise();
            }
            return formStudies;
        } catch (Exception ex) {
            return null;
        }
    }

    private void deserialise() throws Exception {

        for (FormData formData : dataList) {
            String xml = deserializeFormToXML(formData, xformMap);
            formStudies.add(new FormDataXml(formData, xml));
        }

    }

    private String deserializeFormToXML(FormData formData, Map<Integer, String> xformMap) throws Exception {

        String xml = xformMap.get(formData.getDefId());

        if (xml == null) {
            throw new FormNotFoundException("Cannot find form with id = " + formData.getDefId());
        }

        Document doc = XForm.getDocument(new StringReader(xml));
        formData.setDef(XForm.getFormDef(doc));
        xml = XForm.updateXformModel(doc, formData);

        return xml;
    }

    public static class FormDataXml {

        private FormData formData;
        private String xml;

        public FormDataXml(FormData formData, String xml) {
            this.formData = formData;
            this.xml = xml;
        }

        public FormData getFormData() {
            return formData;
        }

        public void setFormData(FormData formData) {
            this.formData = formData;
        }

        public String getXml() {
            return xml;
        }

        public void setXml(String xml) {
            this.xml = xml;
        }

        public int getDefId(){
            return formData.getDefId();
        }
    }
}
