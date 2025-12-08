package com.ssafy.fitmeet.global.util;

import com.ssafy.fitmeet.global.error.CustomException;
import com.ssafy.fitmeet.global.error.ErrorCode;

public class PasswordValidator {

    private static final String PASSWORD_PATTERN =
            "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-={}\\[\\]|:;\"'<>,.?/]).{8,20}$";

    public static void validate(String password) {
        if (password == null || !password.matches(PASSWORD_PATTERN)) {
            throw new CustomException(ErrorCode.AUTH_INVALID_PASSWORD);
        }
    }
}
