/*
 * Copyright (C) 2010 Toni Menzel
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

import static org.ops4j.pax.url.mvn.Parser.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.repository.internal.MavenRepositorySystemSession;
import org.apache.maven.repository.internal.MavenServiceLocator;
import org.ops4j.pax.url.maven.commons.MavenConfiguration;
import org.ops4j.pax.url.maven.commons.MavenRepositoryURL;
import org.ops4j.pax.url.mvn.internal.LogAdapter;
import org.ops4j.pax.url.mvn.internal.ManualWagonProvider;
import org.slf4j.LoggerFactory;
import org.sonatype.aether.RepositoryException;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.connector.wagon.WagonProvider;
import org.sonatype.aether.connector.wagon.WagonRepositoryConnectorFactory;
import org.sonatype.aether.impl.internal.SimpleLocalRepositoryManagerFactory;
import org.sonatype.aether.repository.Authentication;
import org.sonatype.aether.repository.LocalRepository;
import org.sonatype.aether.repository.MirrorSelector;
import org.sonatype.aether.repository.Proxy;
import org.sonatype.aether.repository.ProxySelector;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.resolution.ArtifactRequest;
import org.sonatype.aether.resolution.ArtifactResolutionException;
import org.sonatype.aether.resolution.VersionRangeRequest;
import org.sonatype.aether.resolution.VersionRangeResolutionException;
import org.sonatype.aether.resolution.VersionRangeResult;
import org.sonatype.aether.spi.connector.RepositoryConnectorFactory;
import org.sonatype.aether.spi.localrepo.LocalRepositoryManagerFactory;
import org.sonatype.aether.spi.log.Logger;
import org.sonatype.aether.util.artifact.DefaultArtifact;
import org.sonatype.aether.util.repository.DefaultMirrorSelector;
import org.sonatype.aether.util.repository.DefaultProxySelector;
import org.sonatype.aether.version.Version;

/**
 * Aether based, drop in replacement for mvn protocol
 */
public class AetherBasedResolver2 {

	private static final org.slf4j.Logger LOG = LoggerFactory
			.getLogger(AetherBasedResolver2.class);
	private static final String LATEST_VERSION_RANGE = "(0.0,]";
	private static final String REPO_TYPE = "default";

	final private RepositorySystem m_repoSystem;
	final private MavenConfiguration m_config;
	final private MirrorSelector m_mirrorSelector;
	final private ProxySelector m_proxySelector;

	/**
	 * Create a AetherBasedResolver
	 * 
	 * @param configuration
	 *            (must be not null)
	 * 
	 * @throws java.net.MalformedURLException
	 *             in case of url problems in configuration.
	 */
	public AetherBasedResolver2(final MavenConfiguration configuration)
			throws MalformedURLException {
		m_config = configuration;
		m_repoSystem = newRepositorySystem();
		m_proxySelector = selectProxies();
		m_mirrorSelector = selectMirrors();
	}

	private void assignProxyAndMirrors(final List<RemoteRepository> remoteRepos) {
		final Map<String, List<String>> map = new HashMap<String, List<String>>();
		final Map<String, RemoteRepository> naming = new HashMap<String, RemoteRepository>();

		for (final RemoteRepository r : remoteRepos) {
			naming.put(r.getId(), r);

			r.setProxy(m_proxySelector.getProxy(r));

			final RemoteRepository mirror = m_mirrorSelector.getMirror(r);
			if (mirror != null) {
				final String key = mirror.getId();
				naming.put(key, mirror);
				if (!map.containsKey(key)) {
					map.put(key, new ArrayList<String>());
				}
				final List<String> mirrored = map.get(key);
				mirrored.add(r.getId());
			}
		}

		for (final String mirrorId : map.keySet()) {
			final RemoteRepository mirror = naming.get(mirrorId);
			final List<RemoteRepository> mirroedRepos = new ArrayList<RemoteRepository>();

			for (final String rep : map.get(mirrorId)) {
				mirroedRepos.add(naming.get(rep));
			}
			mirror.setMirroredRepositories(mirroedRepos);
			remoteRepos.removeAll(mirroedRepos);
			remoteRepos.add(0, mirror);
		}

	}

	private List<MavenRepositoryURL> getRemoteRepositories(
			final MavenConfiguration configuration)
			throws MalformedURLException {
		final List<MavenRepositoryURL> list = new ArrayList<MavenRepositoryURL>();
		for (final MavenRepositoryURL s : configuration.getRepositories()) {
			list.add(s);
		}
		return list;
	}

	private ProxySelector selectProxies() {
		final DefaultProxySelector proxySelector = new DefaultProxySelector();
		final Map<String, Map<String, String>> proxies = m_config
				.getProxySettings();
		for (final Map<String, String> proxy : proxies.values()) {
			// The fields are user, pass, host, port, nonProxyHosts, protocol.
			final String nonProxyHosts = proxy.get("nonProxyHosts");
			final Proxy proxyObj = new Proxy(proxy.get("protocol"),
					proxy.get("host"), toInt(proxy.get("port")),
					getAuthentication(proxy));
			proxySelector.add(proxyObj, nonProxyHosts);
		}
		return proxySelector;
	}

	private MirrorSelector selectMirrors() {
		// configure mirror
		final DefaultMirrorSelector selector = new DefaultMirrorSelector();
		final Map<String, Map<String, String>> mirrors = m_config.getMirrors();

		for (final String mirrorName : mirrors.keySet()) {
			final Map<String, String> mirror = mirrors.get(mirrorName);
			// The fields are id, url, mirrorOf, layout, mirrorOfLayouts.
			final String mirrorOf = mirror.get("mirrorOf");
			final String url = mirror.get("url");
			// type can be null in this implementation (1.11)
			selector.add(mirrorName, url, null, false, mirrorOf, "*");
		}
		return selector;
		/**
		 * Set<RemoteRepository> mirrorRepoList = new
		 * HashSet<RemoteRepository>(); for (RemoteRepository r : m_remoteRepos)
		 * { RemoteRepository mirrorRepo = mirrorSelector.getMirror(r); if
		 * (mirrorRepo != null) { mirrorRepoList.add(mirrorRepo); } } return
		 * mirrorRepoList;
		 **/
	}

	private List<RemoteRepository> selectRepositories(
			final List<MavenRepositoryURL> repos) {

		final List<RemoteRepository> list = new ArrayList<RemoteRepository>();

		for (final MavenRepositoryURL r : repos) {
			if (r.isMulti()) {
				addSubDirs(list, r.getFile());
			} else {
				addRepo(list, r);
			}
		}

		return list;
	}

	private void addSubDirs(final List<RemoteRepository> list,
			final File parentDir) {
		if (!parentDir.isDirectory()) {
			LOG.debug("Repository marked with @multi does not resolve to a directory: "
					+ parentDir);
			return;
		}
		for (final File repo : parentDir.listFiles()) {
			if (repo.isDirectory()) {
				try {
					final String repoURI = repo.toURI().toString() + "@id="
							+ repo.getName();
					LOG.debug("Adding repo from inside multi dir: " + repoURI);
					addRepo(list, new MavenRepositoryURL(repoURI));
				} catch (final MalformedURLException e) {
					LOG.error("Error resolving repo url of a multi repo "
							+ repo.toURI());
				}
			}
		}
	}

	private void addRepo(final List<RemoteRepository> list,
			final MavenRepositoryURL repoUrl) {

		final RemoteRepository repo = new RemoteRepository(repoUrl.getId(),
				REPO_TYPE, repoUrl.getURL().toExternalForm());

		// XXX option 2
		// final RepositoryPolicy policy = new RepositoryPolicy();
		// policy.setUpdatePolicy(RepositoryPolicy.UPDATE_POLICY_ALWAYS);
		// repo.setPolicy(true, policy);
		// System.out.println("repo=" + repo);

		list.add(repo);
	}

	public InputStream resolve(final String groupId, final String artifactId,
			final String classifier, final String extension,
			final String version) throws IOException {
		final List<RemoteRepository> remoteRepos = selectRepositories(getRemoteRepositories(m_config));
		assignProxyAndMirrors(remoteRepos);
		// version = mapLatestToRange( version );
		final RepositorySystemSession session = newSession();

		final Artifact artifact = new DefaultArtifact(groupId, artifactId,
				classifier, extension, version);
		final File resolved = resolve(session, remoteRepos, artifact);

		LOG.info("Resolved ({}) as {}", artifact.toString(),
				resolved.getAbsolutePath());
		return new FileInputStream(resolved);
	}

	public Artifact resolve1(final String groupId, final String artifactId,
			final String classifier, final String extension,
			final String version) throws IOException {

		final List<RemoteRepository> remoteRepos = selectRepositories(getRemoteRepositories(m_config));
		assignProxyAndMirrors(remoteRepos);

		// version = mapLatestToRange( version );

		final RepositorySystemSession session = newSession();

		final Artifact artifact = new DefaultArtifact(groupId, artifactId,
				classifier, extension, version);

		final File resolved = resolve(session, remoteRepos, artifact);

		LOG.info("Resolved ({}) as {}", artifact.toString(),
				resolved.getAbsolutePath());

		return artifact;

	}

	public File resolve2(final String groupId, final String artifactId,
			final String classifier, final String extension,
			final String version) throws IOException {

		final List<RemoteRepository> remoteRepos = selectRepositories(getRemoteRepositories(m_config));
		assignProxyAndMirrors(remoteRepos);

		// version = mapLatestToRange( version );

		final RepositorySystemSession session = newSession();

		final Artifact artifact = new DefaultArtifact(groupId, artifactId,
				classifier, extension, version);

		final File resolved = resolve(session, remoteRepos, artifact);

		LOG.info("Resolved ({}) as {}", artifact.toString(),
				resolved.getAbsolutePath());

		return resolved;

	}

	private File resolve(final RepositorySystemSession session,
			final List<RemoteRepository> remoteRepos, Artifact artifact)
			throws IOException {
		try {
			artifact = resolveLatestVersionRange(session, remoteRepos, artifact);
			return m_repoSystem
					.resolveArtifact(session,
							new ArtifactRequest(artifact, remoteRepos, null))
					.getArtifact().getFile();
		} catch (final ArtifactResolutionException e) {
			/**
			 * Do not add root exception to avoid NotSerializableException on
			 * DefaultArtifact. To avoid loosing information log the root cause.
			 * We can remove this again as soon as DefaultArtifact is
			 * serializeable. See http://team.ops4j.org/browse/PAXURL-206
			 */
			LOG.warn(
					"Error resolving artifact + artifact.toString() :"
							+ e.getMessage(), e);
			throw new IOException("Error resolving artifact "
					+ artifact.toString() + ": " + e.getMessage());
		} catch (final RepositoryException e) {
			throw new IOException("Error resolving artifact "
					+ artifact.toString(), e);
		}
	}

	/**
	 * Tries to resolve versions = LATEST using an open range version query. If
	 * it succeeds, version of artifact is set to the highest available version.
	 * 
	 * @param session
	 *            to be used.
	 * @param artifact
	 *            to be used
	 * 
	 * @return an artifact with version set properly (highest if available)
	 * 
	 * @throws org.sonatype.aether.resolution.VersionRangeResolutionException
	 *             in case of resolver errors.
	 */
	private Artifact resolveLatestVersionRange(
			final RepositorySystemSession session,
			final List<RemoteRepository> remoteRepos, Artifact artifact)
			throws VersionRangeResolutionException {
		if (artifact.getVersion().equals(VERSION_LATEST)) {
			artifact = artifact.setVersion(LATEST_VERSION_RANGE);

			final VersionRangeResult versionResult = m_repoSystem
					.resolveVersionRange(session, new VersionRangeRequest(
							artifact, remoteRepos, null));
			if (versionResult != null) {
				final Version v = versionResult.getHighestVersion();
				if (v != null) {

					artifact = artifact.setVersion(v.toString());
				} else {
					throw new VersionRangeResolutionException(versionResult,
							"Not highest version found for " + artifact);
				}
			}
		}
		return artifact;
	}

	private RepositorySystemSession newSession() {

		assert m_config != null : "local repository cannot be null";

		final File local = m_config.getLocalRepository().getFile();

		final MavenRepositorySystemSession session = new MavenRepositorySystemSession();

		// XXX option 1
		session.setUpdatePolicy("always");

		final LocalRepository localRepo = new LocalRepository(local);

		session.setLocalRepositoryManager(m_repoSystem
				.newLocalRepositoryManager(localRepo));

		session.setMirrorSelector(m_mirrorSelector);

		session.setProxySelector(m_proxySelector);

		return session;
	}

	private Authentication getAuthentication(final Map<String, String> proxy) {
		// user, pass
		if (proxy.containsKey("user")) {
			return new Authentication(proxy.get("user"), proxy.get("pass"));
		}
		return null;
	}

	private int toInt(final String intStr) {
		return Integer.parseInt(intStr);
	}

	private RepositorySystem newRepositorySystem() {
		final MavenServiceLocator locator = new MavenServiceLocator();

		locator.setServices(WagonProvider.class, new ManualWagonProvider(
				m_config.getTimeout()));
		locator.addService(RepositoryConnectorFactory.class,
				WagonRepositoryConnectorFactory.class);

		locator.setService(LocalRepositoryManagerFactory.class,
				SimpleLocalRepositoryManagerFactory.class);
		locator.setService(Logger.class, LogAdapter.class);

		return locator.getService(RepositorySystem.class);
	}
}
