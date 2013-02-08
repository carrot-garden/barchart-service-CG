package com.barchart.karaf.test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class DropAndKillFeaturesTest extends TestAny {

	final Logger log = LoggerFactory.getLogger(getClass());

	final String FEATURE = "barchart-karaf-base-test01-feature";

	@Configuration
	public Option[] config0() {
		return super.config();
	}

	@Configuration
	public Option[] config1() {
		return new Option[] {};
	}

	@Test
	public void dropFeaturesInstalled() throws Exception {

		final Path home = Paths.get(System.getProperty("karaf.home"));
		final Path test = home.resolve("../../../src/test/resources");

		log.info("\n\t home : {}\n\t test : {}", home, test);

		final Path source = test.resolve("case-01/feature.xml");

		final Path target = home.resolve("conf/feature.xml");

		assertFeatureNotInstalled(FEATURE);

		log.info("\n\t drop");
		Files.copy(source, target);

		for (int k = 0; k < 100; k++) {
			if (isFeatureInstalled(FEATURE)) {
				break;
			}
			Thread.sleep(100);
		}

		assertFeatureInstalled(FEATURE);

		log.info("\n\t kill");
		Files.delete(target);

		for (int k = 0; k < 100; k++) {
			if (!isFeatureInstalled(FEATURE)) {
				break;
			}
			Thread.sleep(100);
		}

		assertFeatureNotInstalled(FEATURE);

	}

}
