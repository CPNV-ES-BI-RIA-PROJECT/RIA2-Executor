package com.executor.controllers;

import com.executor.BucketService;
import com.executor.dto.DownloadScriptResponse;
import com.executor.services.LocalScriptStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.io.IOException;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/v1/objects")
public class BucketController {

  @SuppressFBWarnings(
      value = "EI_EXPOSE_REP2",
      justification =
          "BucketService is injected by Spring (bean); the controller does not expose it.")
  private final BucketService bucketService;
  private final LocalScriptStorageService localScriptStorageService;

  public BucketController(BucketService bucketService, LocalScriptStorageService localScriptStorageService) {
    this.bucketService = bucketService;
    this.localScriptStorageService = localScriptStorageService;
  }

  @Operation(
      summary = "List objects",
      description =
          "Lists objects under the remote prefix. Use recursive=true to include subfolders.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "List returned"),
    @ApiResponse(responseCode = "400", description = "Invalid remote path"),
    @ApiResponse(responseCode = "404", description = "Bucket/object not found"),
    @ApiResponse(responseCode = "500", description = "Internal error")
  })
  @GetMapping(params = "remote")
  public List<String> list(
      @RequestParam String remote, @RequestParam(defaultValue = "false") boolean recursive) {
    return bucketService.list(remote, recursive);
  }

  @Operation(summary = "Upload an object")
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "Object created"),
    @ApiResponse(responseCode = "400", description = "Invalid remote path"),
    @ApiResponse(responseCode = "404", description = "Bucket/object not found")
  })
  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public void upload(@RequestParam String remote, @RequestPart("file") MultipartFile file) {
    try {
      bucketService.upload(remote, file.getBytes());
    } catch (IOException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to read uploaded file", e);
    }
  }

  @Operation(
      summary = "Delete an object",
      description =
          "Deletes the object targeted by remote. If remote points to a prefix, use recursive=true.")
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "Object deleted"),
    @ApiResponse(responseCode = "400", description = "Invalid remote path"),
    @ApiResponse(responseCode = "404", description = "Bucket/object not found"),
    @ApiResponse(responseCode = "500", description = "Internal error")
  })
  @DeleteMapping(params = "remote")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(
      @RequestParam String remote, @RequestParam(defaultValue = "false") boolean recursive) {
    bucketService.delete(remote, recursive);
  }

  @Operation(
      summary = "Share an object",
      description =
          "Returns a presigned URL for the remote object, valid for expirationTime seconds.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Presigned URL returned"),
    @ApiResponse(responseCode = "400", description = "Invalid remote path"),
    @ApiResponse(responseCode = "404", description = "Bucket/object not found"),
    @ApiResponse(responseCode = "500", description = "Internal error")
  })
  @GetMapping(
      value = "/share",
      params = {"remote", "expirationTime"})
  public String share(@RequestParam String remote, @RequestParam int expirationTime) {
    return bucketService.share(remote, expirationTime);
  }

  @Operation(
      summary = "Download an object",
      description = "Downloads the remote object as binary (application/octet-stream).")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Binary content returned"),
    @ApiResponse(responseCode = "400", description = "Invalid remote path"),
    @ApiResponse(responseCode = "404", description = "Bucket/object not found"),
    @ApiResponse(responseCode = "500", description = "Internal error")
  })
  @GetMapping(value = "/download", params = "remote")
  public ResponseEntity<DownloadScriptResponse> download(@RequestParam String remote) throws IOException {
    byte[] data = bucketService.download(remote);

    Path savedPath = localScriptStorageService.save(remote, data);

    DownloadScriptResponse response = new DownloadScriptResponse(
            remote,
            savedPath.getFileName().toString(),
            savedPath.toString()
    );

    return ResponseEntity.ok(response);
  }
}
