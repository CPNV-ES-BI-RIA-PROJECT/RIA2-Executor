package com.executor.bucketadapterexceptions;

public class BucketOperationException extends RuntimeException {
  public BucketOperationException(String message, Throwable cause) {
    super(message, cause);
  }
}
