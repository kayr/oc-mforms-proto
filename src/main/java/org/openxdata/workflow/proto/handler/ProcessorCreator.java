/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openxdata.workflow.proto.handler;

import org.apache.log4j.Logger;
import org.openxdata.proto.exception.ProtocolException;
import org.openxdata.proto.exception.ProtocolNotFoundException;

/**
 *
 * @author kay
 */
public class ProcessorCreator {

	private Logger log = Logger.getLogger(this.getClass().getName());
	private ClassLoader loader;

	public ProcessorCreator() {
	}

	public ProcessorCreator(ClassLoader loader) {
		this.loader = loader;
	}

	public RequestHandler buildRequestHandler(String type) throws ProtocolNotFoundException, ProtocolException {
		RequestHandler rh = null;
		try {
			rh = loadClassForQuery(type).newInstance();
		} catch (ClassNotFoundException ex) {
			throw new ProtocolNotFoundException("Protocol Handler Not Founder", ex);
		} catch (Exception ex) {
			throw new ProtocolException("Error While Creating Handler: " + ex);

		}
		return rh;
	}

	private Class<RequestHandler> loadClassForQuery(String handlerClassName) throws ClassNotFoundException {
		try {
			return (Class<RequestHandler>) Class.forName("org.openxdata.workflow.proto.handler." + handlerClassName);
		} catch (ClassNotFoundException classNotFoundException) {
		}
		try {
			return (Class<RequestHandler>) Class.forName(handlerClassName);
		} catch (ClassNotFoundException classNotFoundException) {
		}
		return (Class<RequestHandler>) Class.forName("org.openxdata.workflow.proto.handler." + handlerClassName + "Handler");
	}
}
