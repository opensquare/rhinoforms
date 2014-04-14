package com.rhinoforms.util;

import java.util.Stack;
import javax.xml.xpath.XPathFactory;

public class XPathFactoryPool {

	private Stack<XPathFactory> pool = new Stack<XPathFactory>();

	public XPathFactoryPool() {
	}

	public XPathFactory getInstance() {
		if (!pool.isEmpty()) {
			XPathFactory factory = pool.pop();
			// Thread safe check
			if (factory != null) {
				return factory;
			}
		}
		return XPathFactory.newInstance();
	}

	public void returnInstance(XPathFactory xPathFactory) {
		pool.push(xPathFactory);
	}

}
