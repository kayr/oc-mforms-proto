package org.openxdata.workflow.proto;

import org.apache.log4j.Logger;
import org.openxdata.mforms.model.ResponseHeader;
import org.openxdata.mformsproto.MFormsProtocolHandlerImpl;
import org.openxdata.proto.SubmissionContext;
import org.openxdata.proto.WFSubmissionContext;
import org.openxdata.proto.exception.ProtocolAccessDeniedException;
import org.openxdata.proto.exception.ProtocolException;
import org.openxdata.workflow.mobile.model.WFRequest;
import org.openxdata.workflow.proto.handler.ProcessorCreator;
import org.openxdata.workflow.proto.handler.RequestHandler;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;

/**
 *
 */
public class WfMFormsProtocolHandlerImpl extends MFormsProtocolHandlerImpl {

    private Logger log = Logger.getLogger(this.getClass().getName());

    @Override
    public void handleRequest(SubmissionContext ctx) throws ProtocolException {

        log.debug("WfMFormsProtocolHandlerImpl:Loaded From: " + getPath(getClass()));

        DataInputStream din = addMarkSupportToInputStream(ctx);
        final DataInputStream in = ctx.getInputStream();

        in.mark(1);//Mark original place mark place so that we can reset the stream for the super class

        byte action = -1;
        try {
            action = in.readByte();
        } catch (IOException ex) {
            throw new ProtocolException("Failed to read Action", ex);
        }

        if (action == WFRequest.ACTION_WORKFLOW) {
            String path = getPath(getClass());
            URL url = null;
            try {
                url = new URL(path);
            } catch (MalformedURLException ex) {
                throw new ProtocolException("Error while creating workitem handler");
            }
            String request = null;

            try {
                request = din.readUTF();
            } catch (IOException ex) {
                throw new ProtocolException("Error while Reading Workitem Command action");
            }


            // ProtocolClassLoader cl = new ProtocolClassLoader(new URL[]{url},Thread.currentThread().getContextClassLoader());
            ProcessorCreator handlerLoader = new ProcessorCreator();
            RequestHandler requestHandler = handlerLoader.buildRequestHandler(request);
            try {
                requestHandler.handleRequest((WFSubmissionContext) ctx);
            } catch (Exception e) {
                if (e instanceof ProtocolException) {
                    throw (ProtocolException) e;
                }
                if ("OpenXDataSecurityException".equals(e.getClass().getSimpleName())) {
                    // note: don't have access to this class, so can't match by "instanceof"
                    throw new ProtocolAccessDeniedException(e.getMessage());
                } else {
                    try {
                        ctx.getOutputStream().writeByte(ResponseHeader.STATUS_ERROR);
                    } catch (IOException e1) {
                        throw new ProtocolException("failed to write error response", e1);
                    }
                    throw new ProtocolException("error occure while relaying request: [" + request + "] to handler", e);
                }

            }
        } else {
            try {
                in.reset();//
                super.handleRequest(ctx);
            } catch (IOException ex) {
                throw new ProtocolException("Failed to Reset Input Stream", ex);
            }


        }


    }

    public DataInputStream addMarkSupportToInputStream(SubmissionContext ctx) throws ProtocolException {
        try {
            BufferedInputStream bis = new BufferedInputStream(ctx.getInputStream());
            DataInputStream din = new DataInputStream(bis);
            Field field = null;
            try {
                field = ctx.getClass().getDeclaredField("input");
            } catch (NoSuchFieldException noSuchFieldException) {
                field = ctx.getClass().getSuperclass().getDeclaredField("input");
            }
            field.setAccessible(true);
            field.set(ctx, din);
            return din;
        } catch (Exception ex) {
            throw new ProtocolException("Failed to Add MarkSupport to Submission Context:", ex);
        }
    }

    private String getPath(Class cls) {
        String cn = cls.getName();
        String rn = cn.replace('.', '/') + ".class";
        String path = getClass().getClassLoader().getResource(rn).getPath();
        int ix = path.indexOf("!");
        if (ix >= 0) {
            return path.substring(0, ix);
        } else {
            return path;
        }
    }
}
