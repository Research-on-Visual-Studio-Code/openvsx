/** ******************************************************************************
 * Copyright (c) 2022 Precies. Software and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.publish;

import com.google.common.base.Joiner;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.openvsx.ExtensionProcessor;
import org.eclipse.openvsx.ExtensionService;
import org.eclipse.openvsx.ExtensionValidator;
import org.eclipse.openvsx.UserService;
import org.eclipse.openvsx.adapter.VSCodeIdNewExtensionJobRequest;
import org.eclipse.openvsx.entities.*;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.util.ErrorResultException;
import org.eclipse.openvsx.util.NamingUtil;
import org.eclipse.openvsx.util.TempFile;
import org.jobrunr.scheduling.JobRequestScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;

@Component
public class PublishExtensionVersionHandler {

    protected final Logger logger = LoggerFactory.getLogger(PublishExtensionVersionHandler.class);

    private final PublishExtensionVersionService service;
    private final ExtensionVersionIntegrityService integrityService;
    private final EntityManager entityManager;
    private final RepositoryService repositories;
    private final JobRequestScheduler scheduler;
    private final UserService users;
    private final ExtensionValidator validator;
    private final ObservationRegistry  observations;

    public PublishExtensionVersionHandler(
            PublishExtensionVersionService service,
            ExtensionVersionIntegrityService integrityService,
            EntityManager entityManager,
            RepositoryService repositories,
            JobRequestScheduler scheduler,
            UserService users,
            ExtensionValidator validator,
            ObservationRegistry  observations
    ) {
        this.service = service;
        this.integrityService = integrityService;
        this.entityManager = entityManager;
        this.repositories = repositories;
        this.scheduler = scheduler;
        this.users = users;
        this.validator = validator;
        this.observations = observations;
    }

    @Transactional(rollbackOn = ErrorResultException.class)
    public ExtensionVersion createExtensionVersion(ExtensionProcessor processor, PersonalAccessToken token, LocalDateTime timestamp, boolean checkDependencies) {
        return Observation.createNotStarted("PublishExtensionVersionHandler#createExtensionVersion", observations).observe(() -> {
            // Extract extension metadata from its manifest
            var extVersion = createExtensionVersion(processor, token.getUser(), token, timestamp);
            var dependencies = processor.getExtensionDependencies();
            var bundledExtensions = processor.getBundledExtensions();
            if (checkDependencies) {
                var parsedDependencies = dependencies.stream()
                        .map(id -> parseExtensionId(id, "extensionDependencies"))
                        .toList();

                if(!parsedDependencies.isEmpty()) {
                    checkDependencies(parsedDependencies);
                }
                bundledExtensions.forEach(id -> parseExtensionId(id, "extensionPack"));
            }

            extVersion.setDependencies(dependencies);
            extVersion.setBundledExtensions(bundledExtensions);
            if(integrityService.isEnabled()) {
                extVersion.setSignatureKeyPair(repositories.findActiveKeyPair());
            }

            return extVersion;
        });
    }

    private ExtensionVersion createExtensionVersion(ExtensionProcessor processor, UserData user, PersonalAccessToken token, LocalDateTime timestamp) {
        return Observation.createNotStarted("PublishExtensionVersionHandler#createExtensionVersion", observations).observe(() -> {
            var namespaceName = processor.getNamespace();
            var namespace = repositories.findNamespace(namespaceName);
            if (namespace == null) {
                throw new ErrorResultException("Unknown publisher: " + namespaceName
                        + "\nUse the 'create-namespace' command to create a namespace corresponding to your publisher name.");
            }
            if (!users.hasPublishPermission(user, namespace)) {
                throw new ErrorResultException("Insufficient access rights for publisher: " + namespace.getName());
            }

            var extensionName = processor.getExtensionName();
            var nameIssue = validator.validateExtensionName(extensionName);
            if (nameIssue.isPresent()) {
                throw new ErrorResultException(nameIssue.get().toString());
            }

            var version = processor.getVersion();
            var versionIssue = validator.validateExtensionVersion(version);
            if (versionIssue.isPresent()) {
                throw new ErrorResultException(versionIssue.get().toString());
            }

            var extVersion = processor.getMetadata();
            if (extVersion.getDisplayName() != null && extVersion.getDisplayName().trim().isEmpty()) {
                extVersion.setDisplayName(null);
            }
            extVersion.setTimestamp(timestamp);
            extVersion.setPublishedWith(token);
            extVersion.setActive(false);

            var extension = repositories.findExtension(extensionName, namespace);
            if (extension == null) {
                extension = new Extension();
                extension.setActive(false);
                extension.setName(extensionName);
                extension.setNamespace(namespace);
                extension.setPublishedDate(extVersion.getTimestamp());

                entityManager.persist(extension);
            } else {
                var existingVersion = repositories.findVersion(extVersion.getVersion(), extVersion.getTargetPlatform(), extension);
                if (existingVersion != null) {
                    var extVersionId = NamingUtil.toLogFormat(namespaceName, extensionName, extVersion.getTargetPlatform(), extVersion.getVersion());
                    var message = "Extension " + extVersionId + " is already published";
                    message += existingVersion.isActive() ? "." : ", but currently isn't active and therefore not visible.";
                    throw new ErrorResultException(message);
                }
            }

            extension.setLastUpdatedDate(extVersion.getTimestamp());
            extension.getVersions().add(extVersion);
            extVersion.setExtension(extension);

            var metadataIssues = validator.validateMetadata(extVersion);
            if (!metadataIssues.isEmpty()) {
                if (metadataIssues.size() == 1) {
                    throw new ErrorResultException(metadataIssues.get(0).toString());
                }
                throw new ErrorResultException("Multiple issues were found in the extension metadata:\n"
                        + Joiner.on("\n").join(metadataIssues));
            }

            entityManager.persist(extVersion);
            return extVersion;
        });
    }

    private void checkDependencies(List<String[]> dependencies) {
        Observation.createNotStarted("PublishExtensionVersionHandler#checkDependencies", observations).observe(() -> {
            var unresolvedDependency = repositories.findFirstUnresolvedDependency(dependencies);
            if (unresolvedDependency != null) {
                throw new ErrorResultException("Cannot resolve dependency: " + unresolvedDependency);
            }
        });
    }

    private String[] parseExtensionId(String extensionId, String formatType) {
        var split = extensionId.split("\\.");
        if (split.length != 2 || split[0].isEmpty() || split[1].isEmpty()) {
            throw new ErrorResultException("Invalid '" + formatType + "' format. Expected: '${namespace}.${name}'");
        }

        return split;
    }

    @Async
    @Retryable
    public void publishAsync(FileResource download, TempFile extensionFile, ExtensionService extensionService) {
        var extVersion = download.getExtension();

        // Delete file resources in case publishAsync is retried
        service.deleteFileResources(extVersion);
        download.setId(0L);

        service.storeDownload(download, extensionFile);
        service.persistResource(download);
        try(var processor = new ExtensionProcessor(extensionFile, ObservationRegistry.NOOP)) {
            extVersion.setPotentiallyMalicious(processor.isPotentiallyMalicious());
            if (extVersion.isPotentiallyMalicious()) {
                logger.warn("Extension version is potentially malicious: {}", NamingUtil.toLogFormat(extVersion));
                return;
            }

            Consumer<FileResource> consumer = resource -> {
                service.storeResource(resource);
                service.persistResource(resource);
            };

            if(integrityService.isEnabled()) {
                var keyPair = extVersion.getSignatureKeyPair();
                if(keyPair != null) {
                    var signature = integrityService.generateSignature(download, extensionFile, keyPair);
                    consumer.accept(signature);
                } else {
                    // Can happen when GenerateKeyPairJobRequestHandler hasn't run yet and there is no active SignatureKeyPair.
                    // This extension version should be assigned a SignatureKeyPair and a signature FileResource should be created
                    // by the ExtensionVersionSignatureJobRequestHandler migration.
                    logger.warn("Integrity service is enabled, but {} did not have an active key pair", NamingUtil.toLogFormat(extVersion));
                }
            }

            processor.processEachResource(extVersion, consumer);
            processor.getFileResources(extVersion).forEach(consumer);
            consumer.accept(processor.generateSha256Checksum(extVersion));
        }

        // Update whether extension is active, the search index and evict cache
        service.activateExtension(extVersion, extensionService);
        try {
            extensionFile.close();
        } catch (IOException e) {
            logger.error("failed to delete temp file", e);
        }
    }

    public void mirror(FileResource download, TempFile extensionFile, String signatureName) {
        var extVersion = download.getExtension();
        service.mirrorResource(download);
        if(signatureName != null) {
            service.mirrorResource(getSignatureResource(signatureName, extVersion));
        }
        try(var processor = new ExtensionProcessor(extensionFile, ObservationRegistry.NOOP)) {
            processor.getFileResources(extVersion).forEach(service::mirrorResource);
            service.mirrorResource(processor.generateSha256Checksum(extVersion));
            // don't store file resources, they can be generated on the fly to avoid traversing entire zip file
        }
    }

    private FileResource getSignatureResource(String signatureName, ExtensionVersion extVersion) {
        var resource = new FileResource();
        resource.setExtension(extVersion);
        resource.setName(signatureName);
        resource.setType(FileResource.DOWNLOAD_SIG);
        return resource;
    }

    public void schedulePublicIdJob(FileResource download) {
        Observation.createNotStarted("PublishExtensionVersionHandler#schedulePublicIdJob", observations).observe(() -> {
            var extension = download.getExtension().getExtension();
            if (StringUtils.isEmpty(extension.getPublicId())) {
                var namespace = extension.getNamespace();
                scheduler.enqueue(new VSCodeIdNewExtensionJobRequest(namespace.getName(), extension.getName()));
            }
        });
    }
}
