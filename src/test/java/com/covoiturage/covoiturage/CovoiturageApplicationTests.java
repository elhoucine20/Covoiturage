package com.covoiturage.covoiturage;

import com.covoiturage.covoiturage.service.TrajetService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class CovoiturageApplicationTests {


	@MockitoBean
	private TrajetService trajetService;

	@Test
	void contextLoads() {
	}

}
