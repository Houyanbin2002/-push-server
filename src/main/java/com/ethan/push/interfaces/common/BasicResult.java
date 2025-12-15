package com.ethan.push.interfaces.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BasicResult<T> {
    private String status;
    private String msg;
    private T data;

    public static <T> BasicResult<T> success() {
        return new BasicResult<>("200", "success", null);
    }

    public static <T> BasicResult<T> success(T data) {
        return new BasicResult<>("200", "success", data);
    }

    public static <T> BasicResult<T> fail(String msg) {
        return new BasicResult<>("500", msg, null);
    }
}
