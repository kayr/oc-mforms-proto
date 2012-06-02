/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openxdata.workflow.proto.handler;

import java.io.IOException;
import org.openxdata.proto.WFSubmissionContext;
import org.openxdata.proto.exception.ProtocolException;

/**
 * Handle all stream requests from the mobile client.The implemetation of this class
 * will have to know how to read the data and send the send the required reply;
 * @author kay
 */
public interface RequestHandler
{
    /**
     * Processes the request and and builds the necessary reply
     * @param user User who sent request
     * @param is Input stream from the requester
     * @param os Output stream for the requester.
     * @throws IOException
     */
    public void handleRequest(WFSubmissionContext context) throws ProtocolException;
}
