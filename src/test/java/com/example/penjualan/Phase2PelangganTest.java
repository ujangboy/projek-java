package com.example.penjualan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(roles = "ADMIN")
class Phase2PelangganTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbc;

	@AfterEach
	void cleanUp() {
		jdbc.update("DELETE FROM transaksi WHERE id_transaksi IN (920002)");
		jdbc.update("DELETE FROM pelanggan WHERE id_pelanggan IN (920001, 920002, 920010)");
		jdbc.update("DELETE FROM pelanggan WHERE nama = 'Pelanggan Baru'");
		jdbc.update("DELETE FROM pelanggan WHERE id_pelanggan BETWEEN 921001 AND 921006");
	}

	@Test
	void listShowsSeedCustomers() throws Exception {
		mockMvc.perform(get("/pelanggan"))
				.andExpect(status().isOk())
				.andExpect(view().name("pelanggan/list"))
				.andExpect(content().string(containsString("Data Pelanggan")))
				.andExpect(content().string(containsString("Budi Santoso")));
	}

	@Test
	void searchFiltersByName() throws Exception {
		mockMvc.perform(get("/pelanggan").param("q", "Siti"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Siti Aminah")))
				.andExpect(content().string(not(containsString("Budi Santoso"))));
	}

	@Test
	void paginationLimitsTenRows() throws Exception {
		for (int i = 1; i <= 6; i++) {
			jdbc.update("INSERT INTO pelanggan (id_pelanggan, nama) VALUES (?, ?)",
					921000 + i, String.format("Test Pelanggan %02d", i));
		}

		mockMvc.perform(get("/pelanggan").param("page", "1"))
				.andExpect(status().isOk())
				.andExpect(model().attribute("page", hasProperty("totalPages", is(2))))
				.andExpect(model().attribute("page", hasProperty("number", is(1))))
				.andExpect(content().string(containsString("Test Pelanggan 06")))
				.andExpect(content().string(not(containsString("Andi Wijaya"))));
	}

	@Test
	void createCustomerInsertsRow() throws Exception {
		mockMvc.perform(post("/pelanggan").with(csrf())
				.param("nama", "Pelanggan Baru")
				.param("alamat", "Jl. Test No. 1")
				.param("noTelepon", "081200000001")
				.param("email", "baru@example.com"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/pelanggan"))
				.andExpect(flash().attribute("success", "Pelanggan berhasil ditambahkan."));

		assertThat(count("SELECT COUNT(*) FROM pelanggan WHERE nama = 'Pelanggan Baru'")).isEqualTo(1);
	}

	@Test
	void blankNameIsRejected() throws Exception {
		int before = count("SELECT COUNT(*) FROM pelanggan");

		mockMvc.perform(post("/pelanggan").with(csrf()).param("nama", "  "))
				.andExpect(status().isOk())
				.andExpect(view().name("pelanggan/form"))
				.andExpect(model().attributeHasFieldErrors("pelanggan", "nama"))
				.andExpect(content().string(containsString("Nama pelanggan wajib diisi.")));

		assertThat(count("SELECT COUNT(*) FROM pelanggan")).isEqualTo(before);
	}

	@Test
	void updateCustomerChangesRow() throws Exception {
		jdbc.update("INSERT INTO pelanggan (id_pelanggan, nama) VALUES (920010, 'Pelanggan Update')");

		mockMvc.perform(post("/pelanggan/920010").with(csrf())
				.param("nama", "Pelanggan Update Baru")
				.param("alamat", "Jl. Baru No. 2")
				.param("noTelepon", "081200000002")
				.param("email", "update@example.com"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/pelanggan"))
				.andExpect(flash().attribute("success", "Pelanggan berhasil diperbarui."));

		assertThat(count("SELECT COUNT(*) FROM pelanggan WHERE id_pelanggan = 920010"
				+ " AND nama = 'Pelanggan Update Baru'")).isEqualTo(1);
	}

	@Test
	void deleteUnusedCustomerRemovesRow() throws Exception {
		jdbc.update("INSERT INTO pelanggan (id_pelanggan, nama) VALUES (920001, 'Pelanggan Hapus')");

		mockMvc.perform(post("/pelanggan/920001/delete").with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/pelanggan"))
				.andExpect(flash().attribute("success", "Pelanggan berhasil dihapus."));

		assertThat(count("SELECT COUNT(*) FROM pelanggan WHERE id_pelanggan = 920001")).isZero();
	}

	@Test
	void deleteCustomerUsedByTransactionIsRejected() throws Exception {
		jdbc.update("INSERT INTO pelanggan (id_pelanggan, nama) VALUES (920002, 'Pelanggan Terpakai')");
		jdbc.update("""
				INSERT INTO transaksi (id_transaksi, id_pelanggan, id_user, total)
				VALUES (920002, 920002, (SELECT id_user FROM users WHERE username = 'admin'), 0)
				""");

		mockMvc.perform(post("/pelanggan/920002/delete").with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/pelanggan"))
				.andExpect(flash().attribute("error",
						"Pelanggan tidak dapat dihapus karena sudah dipakai pada transaksi."));

		assertThat(count("SELECT COUNT(*) FROM pelanggan WHERE id_pelanggan = 920002")).isEqualTo(1);
	}

	private int count(String sql) {
		return jdbc.queryForObject(sql, Integer.class);
	}
}
