package com.example.penjualan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class Phase0SmokeTest {

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private MockMvc mockMvc;

	@Test
	void usersTableExists() {
		Integer count = jdbc.queryForObject("""
				SELECT COUNT(*) FROM information_schema.tables
				WHERE table_schema = DATABASE() AND table_name = 'users'
				""", Integer.class);
		assertThat(count).isEqualTo(1);
	}

	@Test
	void adminSeedUsesBcrypt() {
		Map<String, Object> admin = jdbc.queryForMap(
				"SELECT username, password, role FROM users WHERE username = 'admin'");
		assertThat(admin.get("role")).isEqualTo("ADMIN");
		assertThat(new BCryptPasswordEncoder().matches("admin123", (String) admin.get("password"))).isTrue();
	}

	@Test
	void loginPageIsPublic() throws Exception {
		mockMvc.perform(get("/login"))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Masuk")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"_csrf\"")));
	}

	@Test
	void dashboardRequiresLogin() throws Exception {
		mockMvc.perform(get("/dashboard"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/login"));
	}

	@Test
	void seedAdminCanLogin() throws Exception {
		mockMvc.perform(formLogin("/login").user("admin").password("admin123"))
				.andExpect(authenticated())
				.andExpect(redirectedUrl("/dashboard"));
	}
}
