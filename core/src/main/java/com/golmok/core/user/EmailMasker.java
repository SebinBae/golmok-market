package com.golmok.core.user;

/**
 * 아이디 찾기 결과에서 이메일을 가린다.
 * 별표는 항상 4개다. 가려진 길이만큼 찍으면 로컬파트 길이가 새어나간다.
 */
final class EmailMasker {

    private static final int VISIBLE_LENGTH = 2;
    private static final String MASK = "****";

    private EmailMasker() {
    }

    static String mask(String email) {
        int at = email.indexOf('@');
        if (at < 0) {
            return MASK;
        }
        String localPart = email.substring(0, at);
        String visible = localPart.substring(0, Math.min(VISIBLE_LENGTH, localPart.length()));
        return visible + MASK + email.substring(at);
    }
}
