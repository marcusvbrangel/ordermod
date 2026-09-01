package com.market;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest(properties = "spring.docker.compose.enabled=false")
@Import(PostgresTestcontainersConfiguration.class)
class OrdermodApplicationTests {

	@Test
	void contextLoads() {
	}

}
