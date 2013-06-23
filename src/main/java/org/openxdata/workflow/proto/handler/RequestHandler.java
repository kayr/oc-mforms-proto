/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openxdata.workflow.proto.handler;

import org.openxdata.proto.WFSubmissionContext;
import org.openxdata.proto.exception.ProtocolException;

import java.io.IOException;

/**
 * Handle all stream requests from the mobile client.The implementation of this class
 * will have to know how to read the data and send the send the required reply;
 *
 * @author kay
 */
public interface RequestHandler {

    /**
     * Processes the request and and builds the necessary reply
     *
     * @param context the workflow submission context
     * @throws ProtocolException if an error occurs
     */
    public void handleRequest(WFSubmissionContext context) throws ProtocolException;
}
