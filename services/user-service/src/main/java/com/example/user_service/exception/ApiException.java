package com.example.user_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

public abstract class ApiException extends ErrorResponseException {

    protected ApiException(HttpStatus status, String title, String detail) {
        super(status, problem(status, title, detail), null);
    }

    @Override
    public String getMessage() {
        return getBody().getDetail();
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
