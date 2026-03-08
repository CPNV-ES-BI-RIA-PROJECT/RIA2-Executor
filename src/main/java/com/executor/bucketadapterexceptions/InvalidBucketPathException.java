package com.executor.bucketadapterexceptions;

public class InvalidBucketPathException extends RuntimeException {
  public InvalidBucketPathException(String message) {
    super(message);
  }
}
