package com.alexastudillo.geographicreference.infrastructure.api.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Standard HTTP response envelope.
 *
 * @param <T> response data type
 */
@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private int status;
    private String code;
    private T data;
    private String nextCursor;
    private String prevCursor;
    private Integer totalElements;
    private Integer totalPages;
    private Integer numberOfElements;

    public ApiResponse() {
    }

    public ApiResponse(final int status, final String code, final T data) {
        this.status = status;
        this.code = code;
        this.data = data;
    }

    public ApiResponse<T> withNextCursor(final String nextCursor) {
        this.nextCursor = nextCursor;
        return this;
    }

    public ApiResponse<T> withPrevCursor(final String prevCursor) {
        this.prevCursor = prevCursor;
        return this;
    }

    public ApiResponse<T> withTotalElements(final Integer totalElements) {
        this.totalElements = totalElements;
        return this;
    }

    public ApiResponse<T> withTotalPages(final Integer totalPages) {
        this.totalPages = totalPages;
        return this;
    }

    public ApiResponse<T> withNumberOfElements(final Integer numberOfElements) {
        this.numberOfElements = numberOfElements;
        return this;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(final int status) {
        this.status = status;
    }

    public String getCode() {
        return code;
    }

    public void setCode(final String code) {
        this.code = code;
    }

    public T getData() {
        return data;
    }

    public void setData(final T data) {
        this.data = data;
    }

    public String getNextCursor() {
        return nextCursor;
    }

    public void setNextCursor(final String nextCursor) {
        this.nextCursor = nextCursor;
    }

    public String getPrevCursor() {
        return prevCursor;
    }

    public void setPrevCursor(final String prevCursor) {
        this.prevCursor = prevCursor;
    }

    public Integer getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(final Integer totalElements) {
        this.totalElements = totalElements;
    }

    public Integer getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(final Integer totalPages) {
        this.totalPages = totalPages;
    }

    public Integer getNumberOfElements() {
        return numberOfElements;
    }

    public void setNumberOfElements(final Integer numberOfElements) {
        this.numberOfElements = numberOfElements;
    }
}
