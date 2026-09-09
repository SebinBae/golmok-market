package com.golmok.core.product;

/**
 * 테이블이 아니라 enum이다. 거의 바뀌지 않고, 테이블로 빼면 조회마다 조인이 붙는데 얻는 게 없다.
 */
public enum Category {
    DIGITAL,
    APPLIANCE,
    FURNITURE,
    LIVING,
    KITCHEN,
    CLOTHING,
    BEAUTY,
    SPORTS,
    HOBBY,
    BOOK,
    PET,
    PLANT,
    BABY,
    TICKET,
    ETC
}
