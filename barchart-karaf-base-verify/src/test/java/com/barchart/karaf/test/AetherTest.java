/*
 * Copyright (C) 2012 Andrei Pozolotin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.barchart.karaf.test;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class AetherTest {

	static final String MARKER_NAME = "build.marker";

	/**
	 * Project used to deploy snapshots.
	 */
	static final String GROUP = "com.barchart.test";
	static final String ARTIFACT = "barchart-karaf-base-test01";
	static final String VERSION = "1.0.1-SNAPSHOT";
	static final String TYPE = "jar";
	static final File PROJECT = new File("../.", ARTIFACT);
	static final File CLASSES = new File(PROJECT, "target/classes");
	static final File MARKER = new File(CLASSES, MARKER_NAME);

	static final String SERVER = "karaf-barchart-server";

	static final String REPO_URI = GROUP + "";

	static final File USER_HOME = new File(System.getProperty("user.home"));
	static final File USER_REPO = new File(CLASSES, MARKER_NAME);

	/**
	 * Command to deploy snapshots. Use alternative repository.
	 */
	static final String COMMAND = //
	"mvn deploy --define skipTests --define maven.repo.local=./test-repo";

	final Logger log = LoggerFactory.getLogger(getClass());

	public void mavenDeploy() throws Exception {
		AetherUtil.process(COMMAND, PROJECT);
	}

	@Test
	public void verifySnapshotUpdates() throws Exception {

		log.error("init");

		final long time0 = System.currentTimeMillis();

		final AetherBasedResolver2 resolver = new AetherBasedResolver2(
				AetherUtil.getUserConfig());

		final long time1;
		{
			mavenDeploy();

			final File file = //
			resolver.resolve2(GROUP, ARTIFACT, "", TYPE, VERSION);

			time1 = file.lastModified();
		}

		log.error("deploy 1");

		final long time2;
		{
			mavenDeploy();

			final File file = //
			resolver.resolve2(GROUP, ARTIFACT, "", TYPE, VERSION);

			time2 = file.lastModified();
		}

		log.error("deploy 2");

		assertTrue(time0 > 0);
		assertTrue(time1 > 0);
		assertTrue(time2 > 0);

		assertTrue(time1 > time0);
		assertTrue(time2 > time0);

		assertTrue(time2 > time1);

		log.error("done");

	}

}
