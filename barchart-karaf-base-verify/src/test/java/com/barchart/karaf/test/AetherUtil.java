package com.barchart.karaf.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

import org.apache.maven.repository.internal.MavenRepositorySystemSession;
import org.apache.maven.repository.internal.MavenServiceLocator;
import org.ops4j.pax.url.maven.commons.MavenConfiguration;
import org.ops4j.pax.url.maven.commons.MavenConfigurationImpl;
import org.ops4j.pax.url.maven.commons.MavenConstants;
import org.ops4j.pax.url.maven.commons.MavenRepositoryURL;
import org.ops4j.pax.url.maven.commons.MavenSettings;
import org.ops4j.pax.url.maven.commons.MavenSettingsImpl;
import org.ops4j.pax.url.mvn.ServiceConstants;
import org.ops4j.pax.url.mvn.internal.LogAdapter;
import org.ops4j.pax.url.mvn.internal.ManualWagonProvider;
import org.ops4j.util.property.PropertiesPropertyResolver;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.connector.wagon.WagonProvider;
import org.sonatype.aether.connector.wagon.WagonRepositoryConnectorFactory;
import org.sonatype.aether.impl.internal.SimpleLocalRepositoryManagerFactory;
import org.sonatype.aether.repository.LocalRepository;
import org.sonatype.aether.repository.LocalRepositoryManager;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.spi.connector.RepositoryConnectorFactory;
import org.sonatype.aether.spi.localrepo.LocalRepositoryManagerFactory;
import org.sonatype.aether.spi.log.Logger;
import org.sonatype.aether.util.artifact.DefaultArtifact;

/**
 */
public class AetherUtil {

	// static Logger LOG = LoggerFactory.getLogger(AetherUtil.class);

	static final Pattern SPACES = Pattern.compile("\\s+");

	static MavenConfiguration getConfig(final File settingsFile)
			throws Exception {

		final Properties props = new Properties();

		props.setProperty( //
				ServiceConstants.PID
						+ MavenConstants.PROPERTY_CERTIFICATE_CHECK,//
				"false" //
		);

		props.setProperty( //
				ServiceConstants.PID + MavenConstants.PROPERTY_SETTINGS_FILE, //
				settingsFile.toURI().toASCIIString() //
		);

		final MavenConfigurationImpl config = new MavenConfigurationImpl(
				new PropertiesPropertyResolver(props), ServiceConstants.PID);

		final MavenSettings settings = new MavenSettingsImpl(settingsFile
				.toURI().toURL());

		config.setSettings(settings);

		return config;

	}

	static MavenConfiguration getTestConfig() throws Exception {
		return getConfig(getTestSettings());
	}

	static File getTestRepo() throws IOException {

		final File folder = new File("./target");
		folder.mkdir();

		final File file = File.createTempFile("test", ".repo", folder);
		file.delete();
		file.mkdirs();

		return file;

	}

	static File getTestSettings() throws IOException {
		return new File("./src/test/resources/case-04/settings.xml");
	}

	static MavenConfiguration getUserConfig() throws Exception {
		return getConfig(getUserSettings());
	}

	static File getUserSettings() throws IOException {
		return new File(System.getProperty("user.home"), ".m2/settings.xml");
	}

	/**
	 * Invoke external process and wait for completion.
	 */
	public static void process(final String command, final File directory)
			throws Exception {
		final ProcessBuilder builder = new ProcessBuilder(SPACES.split(command));
		builder.directory(directory);
		final Process process = builder.start();
		process.waitFor();
	}

	public static void addSubDirs(final List<RemoteRepository> list,
			final File parentDir) {
		if (!parentDir.isDirectory()) {
			return;
		}
		for (final File repo : parentDir.listFiles()) {
			if (repo.isDirectory()) {
				try {
					final String repoURI = repo.toURI().toString() + "@id="
							+ repo.getName();
					addRepo(list, new MavenRepositoryURL(repoURI));
				} catch (final MalformedURLException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static final String REPO_TYPE = "default";

	public static void addRepo(final List<RemoteRepository> list,
			final MavenRepositoryURL repoUrl) {
		list.add(new RemoteRepository(repoUrl.getId(), REPO_TYPE, repoUrl
				.getURL().toExternalForm()));
	}

}
