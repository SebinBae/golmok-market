package com.golmok.core.product;

public enum ProductStatus {

    /** 판매중 */
    ON_SALE,

    /** 예약중. M1에는 이 상태로 가는 경로가 없다. 자리만 만들어둔다. */
    RESERVED,

    /** 거래완료 */
    SOLD
}
