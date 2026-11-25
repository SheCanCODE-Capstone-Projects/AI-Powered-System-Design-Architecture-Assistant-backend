package org.example.aipowereddesigntbackend.ExceptionHandling;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}