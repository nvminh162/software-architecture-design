package com.nvminh162.identity.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR), // 500
    INVALID_KEY(1001, "Key enum is invalid", HttpStatus.BAD_REQUEST), // 400
    INVALID_PASSWORD(1002, "Password is invalid, min là {min}", HttpStatus.BAD_REQUEST), // 400
    INVALID_USERNAME(1003, "Username is invalid, min là {min}", HttpStatus.BAD_REQUEST), // 400
    USER_NOT_FOUND(1004, "User not found", HttpStatus.NOT_FOUND), // 404
    USER_ALREADY_EXISTS(1005, "Username already exists", HttpStatus.BAD_REQUEST), // 400
    UNAUTHENTICATED(1006, "Unauthenticated", HttpStatus.UNAUTHORIZED), // 401
    UNAUTHORIZED(1007, "You have no permission to access this resource", HttpStatus.FORBIDDEN), // 403
    INVALID_DOB(1008, "Your age must be at least {min}", HttpStatus.BAD_REQUEST), // 400
    ;

    private int code;
    private String message;
    private HttpStatusCode statusCode;
}
