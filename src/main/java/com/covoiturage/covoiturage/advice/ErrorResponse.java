
package com.covoiturage.covoiturage.advice;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
public class ErrorResponse {

    private Date timestamp;
    private int statusCode;
    private String message;
    private String path;
}
