package com.golmok.core;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * core는 실행되지 않는 라이브러리라 부트 클래스가 없다.
 * @SpringBootTest가 띄울 컨텍스트를 위해 테스트 소스에만 둔다.
 */
@SpringBootApplication
public class CoreTestApplication {
}
