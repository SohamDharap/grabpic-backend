package com.grabpic.backend.exception;

public class FaceEmbeddingException extends RuntimeException {
    
    public FaceEmbeddingException(String message) {
        super(message);
    }
    
    public FaceEmbeddingException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public static class ServiceUnavailableException extends FaceEmbeddingException {
        public ServiceUnavailableException(String message) {
            super(message);
        }
        
        public ServiceUnavailableException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    
    public static class NoFaceDetectedException extends FaceEmbeddingException {
        public NoFaceDetectedException(String message) {
            super(message);
        }
    }
    
    public static class InvalidResponseException extends FaceEmbeddingException {
        public InvalidResponseException(String message) {
            super(message);
        }
        
        public InvalidResponseException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
