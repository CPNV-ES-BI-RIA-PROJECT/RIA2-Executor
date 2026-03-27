package com.executor.services;

import com.executor.config.S3PublicProperties;
import com.executor.dto.DownloadedFileDto;
import com.executor.dto.RemoteFileDto;
import jakarta.annotation.PostConstruct;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class S3PublicFileService {

    private final S3PublicProperties properties;
    private final RestClient restClient;

    public S3PublicFileService(S3PublicProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder().build();
    }

    @PostConstruct
    public void validateConfig() {
        if (!StringUtils.hasText(properties.getPublicBaseUrl())) {
            throw new IllegalStateException("app.s3.public-base-url is required");
        }
        if (!StringUtils.hasText(properties.getRemotePath())) {
            throw new IllegalStateException("app.s3.remote-path is required");
        }
    }

    public List<RemoteFileDto> list() {
        try {
            String prefix = normalizeRemotePath(properties.getRemotePath());
            String url = buildListUrl(prefix);

            byte[] xmlBytes = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(byte[].class);

            if (xmlBytes == null || xmlBytes.length == 0) {
                return List.of();
            }

            return parseS3ListResponse(xmlBytes, prefix);

        } catch (HttpClientErrorException e) {
            throw new ResponseStatusException(
                    e.getStatusCode(),
                    "Unable to list files from public S3 bucket: " + e.getResponseBodyAsString(),
                    e
            );
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to parse S3 public listing response",
                    e
            );
        }
    }

    public DownloadedFileDto download(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "fileName is required");
        }

        try {
            String prefix = normalizeRemotePath(properties.getRemotePath());
            String fullKey = prefix + fileName;
            String fileUrl = buildFileUrl(fullKey);

            String content = restClient.get()
                    .uri(fileUrl)
                    .retrieve()
                    .body(String.class);

            if (content == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File content is empty or not found");
            }

            return new DownloadedFileDto(fileName, content);

        } catch (HttpClientErrorException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found: " + fileName, e);
        } catch (HttpClientErrorException e) {
            throw new ResponseStatusException(
                    e.getStatusCode(),
                    "Unable to download file: " + e.getResponseBodyAsString(),
                    e
            );
        }
    }

    private List<RemoteFileDto> parseS3ListResponse(byte[] xmlBytes, String prefix) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);

        Document document = factory.newDocumentBuilder()
                .parse(new ByteArrayInputStream(xmlBytes));

        NodeList contentsNodes = document.getElementsByTagName("Contents");
        List<RemoteFileDto> files = new ArrayList<>();

        for (int i = 0; i < contentsNodes.getLength(); i++) {
            org.w3c.dom.Node node = contentsNodes.item(i);

            String key = null;
            long size = 0L;

            NodeList children = node.getChildNodes();
            for (int j = 0; j < children.getLength(); j++) {
                org.w3c.dom.Node child = children.item(j);

                if ("Key".equals(child.getNodeName())) {
                    key = child.getTextContent();
                } else if ("Size".equals(child.getNodeName())) {
                    String sizeValue = child.getTextContent();
                    if (StringUtils.hasText(sizeValue)) {
                        size = Long.parseLong(sizeValue);
                    }
                }
            }

            if (!StringUtils.hasText(key)) {
                continue;
            }

            if (key.endsWith("/")) {
                continue;
            }

            String fileName = extractFileName(key);

            if (!StringUtils.hasText(fileName)) {
                continue;
            }

            files.add(new RemoteFileDto(key, fileName, size));
        }

        return files;
    }

    private String buildListUrl(String prefix) {
        String encodedPrefix = URLEncoder.encode(prefix, StandardCharsets.UTF_8)
                .replace("+", "%20");

        return removeTrailingSlash(properties.getPublicBaseUrl())
                + "/?list-type=2&prefix="
                + encodedPrefix;
    }

    private String buildFileUrl(String key) {
        String[] segments = key.split("/");
        StringBuilder sb = new StringBuilder(removeTrailingSlash(properties.getPublicBaseUrl()));

        for (String segment : segments) {
            if (!segment.isEmpty()) {
                sb.append("/")
                        .append(URLEncoder.encode(segment, StandardCharsets.UTF_8).replace("+", "%20"));
            }
        }

        return sb.toString();
    }

    private String normalizeRemotePath(String remotePath) {
        String value = remotePath.trim();
        if (value.startsWith("/")) {
            value = value.substring(1);
        }
        if (!value.endsWith("/")) {
            value = value + "/";
        }
        return value;
    }

    private String removeTrailingSlash(String value) {
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }

    private String extractFileName(String key) {
        int index = key.lastIndexOf('/');
        if (index < 0) {
            return key;
        }
        return key.substring(index + 1);
    }
}
