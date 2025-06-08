package com.example.SummerBuild.util;

import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileLoaderService {
  private static final Logger logger = LoggerFactory.getLogger(FileLoaderService.class);

  private static final String bucketName = "event-pictures";

  private final RestTemplate restTemplate;

  @Value("${supabase.auth.url}")
  private String supabaseUrl;

  @Value("${supabase.serviceKey}")
  private String supabaseApiKey;

  public FileLoaderService() {
    this.restTemplate = new RestTemplate();
    logger.info("FileLoaderService initialized");
  }

  public String uploadFile(List<MultipartFile> files, UUID eventUuid, UUID hostUuid) {
    logger.info("Starting file upload for event: {}", eventUuid);

    for (MultipartFile file : files) {
      if (file.isEmpty()) {
        logger.error("Failed to upload file: File is empty");
        return null;
      }

      String fileName = file.getOriginalFilename();
      if (fileName == null || fileName.isEmpty()) {
        logger.error("Failed to upload file: File name is empty");
        return "file name is empty, upload failed";
      }

      String fullFilePath = buildFileUrl(eventUuid.toString(), fileName);
      logger.info("Preparing to upload file: {}", fullFilePath);

      try {
        byte[] fileBytes = file.getBytes();
        logger.info("File bytes obtained for file: {}", fileName);

        HttpHeaders headers = buildHeaders();
        headers.set("x-upsert", "true");
        headers.set("cache-control", "3600");
        headers.set("metadata", String.format("{\"owner_id\":\"%s\"}", hostUuid.toString()));

        HttpEntity<byte[]> requestEntity = new HttpEntity<>(fileBytes, headers);

        ResponseEntity<String> response =
            restTemplate.exchange(fullFilePath, HttpMethod.POST, requestEntity, String.class);

        if (response.getStatusCode().is2xxSuccessful()) {
          logger.info(
              "File uploaded successfully with status code: {}", response.getStatusCode().value());
        } else {
          logger.warn("Unexpected status code: {}", response.getStatusCode().value());
          return "Upload failed with unexpected status: " + response.getStatusCode().value();
        }

      } catch (HttpClientErrorException | HttpServerErrorException e) {
        logger.error(
            "Error during file upload: {} - {}",
            e.getStatusCode().value(),
            e.getResponseBodyAsString());
        return "Upload failed: " + e.getStatusCode().value();
      } catch (Exception e) {
        logger.error("Failed to upload file due to exception: {}", e.getMessage());
        return "Upload failed due to unexpected error: " + e.getMessage();
      }
    }
    logger.info("All files uploaded successfully for event: {}", eventUuid);
    return "Files uploaded successfully";
  }

  public String getFilePath(String eventId, String fileName) {
    // Only returns a path if the file exists

    String filePath = buildFileUrl(eventId, fileName);
    if (!fileExists(filePath)) {
      return null;
    }
    return buildPublicFileUrl(eventId, fileName);
  }

  public boolean deleteFile(String eventId, String fileName) {
    String filePath = buildFileUrl(eventId, fileName);

    if (!fileExists(filePath)) {
      logger.warn("File not found for deletion: {}", fileName);
      return false;
    }

    try {
      HttpHeaders headers = buildHeaders();
      HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

      ResponseEntity<String> response =
          restTemplate.exchange(filePath, HttpMethod.DELETE, requestEntity, String.class);

      if (response.getStatusCode().is2xxSuccessful()) {
        logger.info("File deleted successfully: {}", fileName);
        return true;
      } else {
        logger.warn(
            "Failed to delete file: {} - Status code: {}",
            fileName,
            response.getStatusCode().value());
      }
    } catch (Exception e) {
      logger.error("Exception during file deletion: {}", e.getMessage());
    }

    return false;
  }

  private boolean fileExists(String filePath) {
    try {
      HttpHeaders headers = buildHeaders();
      HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

      ResponseEntity<byte[]> response =
          restTemplate.exchange(filePath, HttpMethod.GET, requestEntity, byte[].class);

      return response.getStatusCode().is2xxSuccessful();
    } catch (HttpClientErrorException e) {
      if (e.getStatusCode().value() == 404) {
        return false;
      }
      logger.error("Error checking file existence: {}", e.getMessage());
      return false;
    } catch (Exception e) {
      logger.error("Unexpected error checking file existence: {}", e.getMessage());
      return false;
    }
  }

  // helper methods
  private String buildFileUrl(String eventUuid, String fileName) {
    String path = bucketName + "/";
    if (eventUuid != null) {
      path += eventUuid + "/";
    }
    path += fileName;
    return supabaseUrl + "/storage/v1/object/" + path;
  }

  private HttpHeaders buildHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.set("Authorization", "Bearer " + supabaseApiKey);
    return headers;
  }

  private String buildPublicFileUrl(String eventUuid, String fileName) {
    String path = bucketName + "/";
    if (eventUuid != null) {
      path += eventUuid + "/";
    }
    path += fileName;
    return supabaseUrl + "/storage/v1/object/public/" + path;
  }
}
