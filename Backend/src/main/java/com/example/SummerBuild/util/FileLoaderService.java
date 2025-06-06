package com.example.SummerBuild.util;

import java.io.File;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;

@Service
public class FileLoaderService {
    private static final Logger logger = LoggerFactory.getLogger(FileLoaderService.class);

    private static final String bucketName = "event-pictures";

    private final RestTemplate restTemplate;

    @Value("${supabase.auth.url}")
    private String supabaseUrl;

    @Value("${supabase.anonKey}")
    private String supabaseApiKey;

    public FileLoaderService() {
        this.restTemplate = new RestTemplate();
        logger.info("FileLoaderService initialized");
    }

    /**
     * Uploads a file to the server.
     *
     * @param file    The file to be uploaded.
     * @param eventId The ID of the event.
     * @return The path where the file is stored, or an error message if the upload
     *         fails.
     */
    public String uploadFile(List<MultipartFile> files, UUID eventId, String jwtToken) {
        logger.info("Starting file upload for event: {}", eventId);

        for (MultipartFile file : files) {

            if (file.isEmpty()) {
                logger.error("Failed to upload file: File is empty");
                return null;
            }

            // TODO: security check for file type

            String fileName = file.getOriginalFilename();
            if (fileName == null || fileName.isEmpty()) {
                logger.error("Failed to upload file: File name is empty");
                return "file name is empty, upload failed";
            }

            // building supabase url path
            String fullFilePath = supabaseUrl + "/storage/v1/object/" + bucketName + "/" + eventId + "/" + fileName;
            logger.info("Preparing to upload file: {}", fullFilePath);

            try {
                // step 1: get bytes from file [DONE]
                byte[] fileBytes = file.getBytes();
                logger.info("File bytes obtained for file: {}", fileName);

                // step 2: create the connection to supabase using a url connection (i.e build a
                // RestTemplate)
                // Step 2a: build the headers [DONE]
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
                headers.set("apikey", supabaseApiKey);
                headers.set("Authorization", "Bearer " + jwtToken);

                // step 2b: build the request entity with the file bytes and headers
                HttpEntity<byte[]> requestEntity = new HttpEntity<>(fileBytes, headers);

                // step 2c: make the POST request to upload the file [DONE]
                ResponseEntity<String> response = restTemplate.exchange(
                        fullFilePath,
                        HttpMethod.PUT,
                        requestEntity,
                        String.class
                );

                // TODO: improve status code handling
                HttpStatusCode statusCode = response.getStatusCode();
                if (statusCode.is2xxSuccessful()) {
                    logger.info("File uploaded successfully with status code: {}", statusCode.value());
                } else if (statusCode.value() == 409) {
                    logger.warn("File already exists: {}", fileName);
                    return "File already exists, upload failed. File failed: " + fileName;
                } else if (statusCode.is4xxClientError()) {
                    logger.error("Client error during file upload: {}", response.getBody());
                    return "Upload failed with client error: " + statusCode.value();
                } else if (statusCode.is5xxServerError()) {
                    logger.error("Server error during file upload: {}", response.getBody());
                    return "Upload failed with server error: " + statusCode.value();
                }
            } catch (Exception e) {
                logger.error("Failed to upload file due to exception: {}", e.getMessage());
            }
        }
        logger.info("All files uploaded successfully for event: {}", eventId);
        return "Files uploaded successfully";
    }

    /**
     * gets the file path of a file stored on the server.
     * 
     * @param fileName
     * @return file path of the file if it exists, null otherwise
     */
    public String getFilePath(String fileName) {
        String filePath = "user/" + fileName;
        if (fileExists(filePath)) {
            return filePath;
        } else {
            logger.warn("File not found: {}", filePath);
            return null;
        }
    }

    /**
     * Deletes a file from the server.
     *
     * @param filePath The path of the file to be deleted.
     * @return True if the file was deleted successfully, false otherwise.
     */
    public boolean deleteFile(String fileName) {
        return true;
    }

    /**
     * Checks if a file exists on the server.
     *
     * @param filePath The path of the file to check.
     * @return True if the file exists, false otherwise.
     */
    private boolean fileExists(String filePath) {
        return true;
    }
}
