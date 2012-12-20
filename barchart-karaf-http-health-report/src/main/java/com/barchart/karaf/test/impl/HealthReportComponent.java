/**
 * Copyright (C) 2011-2012 Barchart, Inc. <http://www.barchart.com/>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.barchart.karaf.test.impl;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.joda.time.DateTime;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.http.HttpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.barchart.karaf.test.api.HealthReport;

@SuppressWarnings("serial")
@Component(immediate = true, service = HealthReport.class)
public class HealthReportComponent extends HttpServlet implements HealthReport {

	private final Logger log = LoggerFactory.getLogger(getClass());

	static String entry(final String key, final String value) {
		return " \"" + key + "\"" + " : " + "\"" + value + "\" ";
	}

	@Override
	protected void doGet(final HttpServletRequest request,
			final HttpServletResponse response) throws ServletException,
			IOException {

		final String report = //
		"{ " + entry("timestamp", new DateTime().toString()) + " }";

		response.setContentType("application/json");

		response.getWriter().write(report);

	}

	@Activate
	protected void activate() throws Exception {

		log.info("activate : {}", PATH);

		httpService.registerServlet(PATH, this, null, null);

	}

	@Deactivate
	protected void deactivate() {

		httpService.unregister(PATH);

		log.info("deactivate : {}", PATH);

	}

	private HttpService httpService;

	@Reference
	protected void bind(final HttpService s) {
		httpService = s;
	}

	protected void unbind(final HttpService s) {
		httpService = null;
	}

}
