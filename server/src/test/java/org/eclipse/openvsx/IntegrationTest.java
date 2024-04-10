/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import org.eclipse.openvsx.json.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class IntegrationTest {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    TestService testService;

    private String apiCall(String path) {
        return "http://localhost:" + port + path;
    }

    @Test
    public void testPublishExtension() throws Exception {
        testService.createUser();
        createNamespace();
        getNamespaceMetadata("/api/editorconfig");
        getNamespaceMetadata("/api/editorconfig/details");
        duplicateNamespaceLowercase();
        verifyToken();
        publishExtension();

        // Wait a bit until the publish extension background job has finished
        Thread.sleep(15000);

        getExtensionMetadata("/api/editorconfig/editorconfig");
        getExtensionMetadata("/api/editorconfig/editorconfig/0.16.6");
        getExtensionMetadata("/api/editorconfig/editorconfig/latest");
        getExtensionMetadata("/api/editorconfig/editorconfig/universal");
        getExtensionMetadata("/api/editorconfig/editorconfig/universal/0.16.6");
        getExtensionMetadata("/api/editorconfig/editorconfig/universal/latest");

        getVersionsMetadata("editorconfig", "editorconfig", null);
        getVersionsMetadata("editorconfig", "editorconfig", "universal");

        getVersionReferencesMetadata("/api/editorconfig/editorconfig/version-references");
        getVersionReferencesMetadata("/api/editorconfig/editorconfig/universal/version-references");

        getReviews();

        getFile("/api/editorconfig/editorconfig/latest/file/download");
        getFile("/api/editorconfig/editorconfig/universal/latest/file/download");
        getFile("/api/editorconfig/editorconfig/0.16.6/file/download");
        getFile("/api/editorconfig/editorconfig/universal/0.16.6/file/download");
        getFile("/api/editorconfig/editorconfig/latest/file/editorconfig.editorconfig-0.16.6.vsix");
        getFile("/api/editorconfig/editorconfig/universal/latest/file/editorconfig.editorconfig-0.16.6.vsix");
        getFile("/api/editorconfig/editorconfig/0.16.6/file/editorconfig.editorconfig-0.16.6.vsix");
        getFile("/api/editorconfig/editorconfig/universal/0.16.6/file/editorconfig.editorconfig-0.16.6.vsix");
        getFile("/vscode/asset/editorconfig/editorconfig/0.16.6/Microsoft.VisualStudio.Services.VSIXPackage");
        getFile("/vscode/asset/editorconfig/editorconfig/0.16.6/Microsoft.VisualStudio.Services.VSIXPackage?targetPlatform=universal");
        getFile("/vscode/unpkg/editorconfig/editorconfig/0.16.6");
        getFile("/vscode/unpkg/editorconfig/editorconfig/0.16.6/extension.vsixmanifest");

        getVscodeDownloadLink();

        // Wait a bit until the new entry has landed in the search index
        Thread.sleep(2000);
        searchExtension();
        publishDuplicateExtensionLowercase();
    }

    private void createNamespace() {
        var requestBody = new NamespaceJson();
        requestBody.name = "EditorConfig";
        var response = restTemplate.postForEntity(apiCall("/api/-/namespace/create?token={token}"), requestBody,
                ResultJson.class, "test_token");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().error).isNull();
        assertThat(response.getBody().success).isEqualTo("Created namespace " + requestBody.name);
    }

    private void duplicateNamespaceLowercase() {
        var requestBody = new NamespaceJson();
        requestBody.name = "editorconfig";
        var response = restTemplate.postForEntity(apiCall("/api/-/namespace/create?token={token}"), requestBody,
                ResultJson.class, "test_token");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().success).isNull();
        assertThat(response.getBody().error).isEqualTo("Namespace already exists: EditorConfig");
    }

    private void getNamespaceMetadata(String path) {
        var response = restTemplate.getForEntity(apiCall(path), JsonNode.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        var json = response.getBody();
        assertThat(json.get("error")).isNull();
        assertThat(json.get("name").asText()).isEqualTo("EditorConfig");
    }

    private void verifyToken() {
        var response = restTemplate.getForEntity(apiCall("/api/editorconfig/verify-pat?token=test_token"), ResultJson.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        var json = response.getBody();
        assertThat(json.error).isNull();
        assertThat(json.success).isEqualTo("Valid token");
    }

    private void publishExtension() throws IOException {
        try (var stream = getClass().getResourceAsStream("EditorConfig.EditorConfig-0.16.6.vsix")) {
            var bytes = stream.readAllBytes();
            var response = restTemplate.postForEntity(apiCall("/api/-/publish?token={token}"),
                    bytes, ExtensionJson.class, "test_token");
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody().error).isNull();
            assertThat(response.getBody().name).isEqualTo("EditorConfig");
        }
    }

    private void publishDuplicateExtensionLowercase() throws IOException {
        try (var stream = getClass().getResourceAsStream("editorconfig.editorconfig-0.16.6-2.vsix")) {
            var bytes = stream.readAllBytes();
            var response = restTemplate.postForEntity(apiCall("/api/-/publish?token={token}"),
                    bytes, ExtensionJson.class, "test_token");
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody().error).isEqualTo("Extension editorconfig.editorconfig 0.16.6 is already published.");
        }
    }

    private void getExtensionMetadata(String url) {
        var response = restTemplate.getForEntity(apiCall(url), ExtensionJson.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().description).isEqualTo("EditorConfig Support for Visual Studio Code");
    }

    private void getVersionsMetadata(String namespace, String extension, String target) {
        var path = "/api/" + namespace + "/" + extension;
        if(target != null) {
            path += "/" + target;
        }

        var response = restTemplate.getForEntity(apiCall(path + "/versions"), VersionsJson.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().versions.size()).isEqualTo(1);

        var version = "0.16.6";
        var versionPath = path + "/" + version;
        assertThat(response.getBody().versions.get(version)).isEqualTo(apiCall(versionPath));
    }

    private void getVersionReferencesMetadata(String path) {
        var response = restTemplate.getForEntity(apiCall(path), VersionReferencesJson.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().versions.size()).isEqualTo(1);
        assertThat(response.getBody().versions.get(0).version).isEqualTo("0.16.6");
    }

    private void getFile(String path) {
        var response = restTemplate.getForEntity(apiCall(path), byte[].class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotEmpty();
    }

    private void getReviews() {
        var response = restTemplate.getForEntity(apiCall("/api/editorconfig/editorconfig/reviews"), ReviewListJson.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().error).isNull();
        assertThat(response.getBody().reviews.size()).isEqualTo(0);
    }

    private void searchExtension() {
        var response = restTemplate.getForEntity(apiCall("/api/-/search?query=editorconfig"), SearchResultJson.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().extensions.size()).isEqualTo(1);
        assertThat(response.getBody().extensions.get(0).description)
                .isEqualTo("EditorConfig Support for Visual Studio Code");
    }

    public void getVscodeDownloadLink() throws URISyntaxException {
        var path = "/vscode/gallery/publishers/editorconfig/vsextensions/editorconfig/0.16.6/vspackage";
        var response = restTemplate.getForEntity(apiCall(path), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        var expectedPath = "/vscode/asset/editorconfig/editorconfig/0.16.6/Microsoft.VisualStudio.Services.VSIXPackage";
        assertThat(response.getHeaders().getLocation()).isEqualTo(new URI(apiCall(expectedPath)));
    }
}