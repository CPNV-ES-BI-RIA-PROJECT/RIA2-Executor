package com.executor.bucketadapterexceptions;

public class BucketObjectNotFoundException extends RuntimeException {
  public BucketObjectNotFoundException(String message) {
    super(message);
  }
}
