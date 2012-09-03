package com.rhinoforms;

import javax.servlet.http.HttpSession;

public class SessionHelper {

	public static void setFlow(FormFlow formFlow, HttpSession session) {
		session.setAttribute(getAttributeName(formFlow.getId()), formFlow);
	}

	public static FormFlow getFlow(int id, HttpSession session) {
		return (FormFlow) session.getAttribute(getAttributeName(id));
	}
	
	public static void removeFlow(FormFlow formFlow, HttpSession session) {
		session.removeAttribute(getAttributeName(formFlow.getId()));
	}
	
	private static String getAttributeName(int id) {
		return "FormFlow" + id;
	}
}
