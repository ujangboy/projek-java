package com.example.penjualan;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "admin", roles = "ADMIN")
class Phase5LaporanTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void laporanIndexAndPreviewsWork() throws Exception {
		String today = LocalDate.now().toString();
		mockMvc.perform(get("/laporan"))
				.andExpect(status().isOk())
				.andExpect(view().name("laporan/index"))
				.andExpect(content().string(containsString("Laporan Transaksi")));

		mockMvc.perform(get("/laporan/transaksi").param("from", today).param("to", today))
				.andExpect(status().isOk())
				.andExpect(view().name("laporan/transaksi"))
				.andExpect(content().string(containsString("Toko Sinar Jaya")));

		mockMvc.perform(get("/laporan/detail-penjualan").param("from", today).param("to", today))
				.andExpect(status().isOk())
				.andExpect(view().name("laporan/detail-penjualan"));

		mockMvc.perform(get("/laporan/penjualan-barang").param("from", today).param("to", today))
				.andExpect(status().isOk())
				.andExpect(view().name("laporan/penjualan-barang"));

		mockMvc.perform(get("/laporan/penjualan-pelanggan").param("from", today).param("to", today))
				.andExpect(status().isOk())
				.andExpect(view().name("laporan/penjualan-pelanggan"));
	}

	@Test
	void exportsReturnFiles() throws Exception {
		String today = LocalDate.now().toString();
		mockMvc.perform(get("/laporan/transaksi/excel").param("from", today).param("to", today))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));

		mockMvc.perform(get("/laporan/transaksi/pdf").param("from", today).param("to", today))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Type", "application/pdf"));

		mockMvc.perform(get("/laporan/detail-penjualan/pdf").param("from", today).param("to", today))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Type", "application/pdf"));

		mockMvc.perform(get("/laporan/penjualan-barang/excel").param("from", today).param("to", today))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));

		mockMvc.perform(get("/laporan/penjualan-pelanggan/pdf").param("from", today).param("to", today))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Type", "application/pdf"));
	}
}