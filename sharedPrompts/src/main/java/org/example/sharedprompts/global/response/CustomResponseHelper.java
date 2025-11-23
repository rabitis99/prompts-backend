package org.example.sharedprompts.global.response;

import org.example.sharedprompts.global.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class CustomResponseHelper {

    private CustomResponseHelper() {

    }

    public static <T> ResponseEntity<CustomResponse<T>> ok(T data) {
        return ResponseEntity.ok(CustomResponse.ok(data));
    }

    public static <T> ResponseEntity<CustomResponse<T>> created(T data) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CustomResponse.created(data));
    }

    public static ResponseEntity<Void> noContent() {
        return ResponseEntity.noContent().build();
    }

    public static ResponseEntity<CustomResponse<Void>> fail(ApiException e) {
        return ResponseEntity
                .status(e.getErrorCode().getHttpStatus())
                .body(CustomResponse.fail(e));
    }
}
