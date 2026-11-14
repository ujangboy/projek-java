package com.example.penjualan;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.LocalDate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "admin", roles = "ADMIN")
class Phase5LaporanTest {

	private static final long TRX_BASE = 985000L;
	private static final int BULK_TRX = 30;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbc;

	@BeforeEach
	void setUp() {
		cleanUp();
	}

	@AfterEach
	void cleanUp() {
		jdbc.update("DELETE FROM detail_transaksi WHERE id_transaksi BETWEEN ? AND ?", TRX_BASE + 1, TRX_BASE + BULK_TRX);
		jdbc.update("DELETE FROM transaksi WHERE id_transaksi BETWEEN ? AND ?", TRX_BASE + 1, TRX_BASE + BULK_TRX);
		jdbc.update("ALTER TABLE transaksi AUTO_INCREMENT = 1");
		jdbc.update("ALTER TABLE detail_transaksi AUTO_INCREMENT = 1");
	}

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

	@Test
	void reversedRangeShowsErrorAndRejectsExport() throws Exception {
		mockMvc.perform(get("/laporan/transaksi").param("from", "2026-11-20").param("to", "2026-11-10"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Tanggal awal tidak boleh melebihi tanggal akhir")))
				.andExpect(content().string(not(containsString("TRX-"))));

		mockMvc.perform(get("/laporan/transaksi/excel").param("from", "2026-11-20").param("to", "2026-11-10"))
				.andExpect(status().isBadRequest());
		mockMvc.perform(get("/laporan/transaksi/pdf").param("from", "2026-11-20").param("to", "2026-11-10"))
				.andExpect(status().isBadRequest());
		mockMvc.perform(get("/laporan/detail-penjualan/pdf").param("from", "2026-11-20").param("to", "2026-11-10"))
				.andExpect(status().isBadRequest());
		mockMvc.perform(get("/laporan/penjualan-barang/excel").param("from", "2026-11-20").param("to", "2026-11-10"))
				.andExpect(status().isBadRequest());
		mockMvc.perform(get("/laporan/penjualan-pelanggan/pdf").param("from", "2026-11-20").param("to", "2026-11-10"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void reportsShowRealRowsTotalsAndExportsForAllFilteredData() throws Exception {
		seedBulkTransaksi();
		String today = LocalDate.now().toString();

		mockMvc.perform(get("/laporan/transaksi").param("from", today).param("to", today))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("TRX-" + today.replace("-", "") + "-985001")))
				.andExpect(content().string(containsString("TRX-" + today.replace("-", "") + "-985030")))
				.andExpect(content().string(containsString("1.740.000,00")));

		mockMvc.perform(get("/laporan/detail-penjualan").param("from", today).param("to", today))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("TRX-" + today.replace("-", "") + "-985001")))
				.andExpect(content().string(containsString("TRX-" + today.replace("-", "") + "-985030")))
				.andExpect(content().string(containsString("60")))
				.andExpect(content().string(containsString("1.740.000,00")));

		mockMvc.perform(get("/laporan/penjualan-barang").param("from", today).param("to", today))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("BRG-001")))
				.andExpect(content().string(containsString("BRG-002")))
				.andExpect(content().string(containsString("30")));

		mockMvc.perform(get("/laporan/penjualan-pelanggan").param("from", today).param("to", today))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("30")))
				.andExpect(content().string(containsString("1.740.000,00")));

		mockMvc.perform(get("/laporan/transaksi/excel").param("from", today).param("to", today))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
		mockMvc.perform(get("/laporan/transaksi/pdf").param("from", today).param("to", today))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Type", "application/pdf"));
		mockMvc.perform(get("/laporan/detail-penjualan/excel").param("from", today).param("to", today))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
		mockMvc.perform(get("/laporan/detail-penjualan/pdf").param("from", today).param("to", today))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Type", "application/pdf"));
	}

	private void seedBulkTransaksi() {
		Long uid = jdbc.queryForObject("SELECT id_user FROM users WHERE username = 'admin' LIMIT 1", Long.class);
		Long pid = jdbc.queryForObject("SELECT id_pelanggan FROM pelanggan ORDER BY id_pelanggan LIMIT 1", Long.class);
		Long b1 = jdbc.queryForObject("SELECT id_barang FROM barang WHERE kode_barang = 'BRG-001' LIMIT 1", Long.class);
		Long b2 = jdbc.queryForObject("SELECT id_barang FROM barang WHERE kode_barang = 'BRG-002' LIMIT 1", Long.class);
		for (int i = 1; i <= BULK_TRX; i++) {
			long id = TRX_BASE + i;
			jdbc.update(
					"INSERT INTO transaksi (id_transaksi, id_pelanggan, id_user, tanggal, total) VALUES (?, ?, ?, CONCAT(CURDATE(), ' 08:00:00') + INTERVAL ? MINUTE, 58000.00)",
					id, pid, uid, i);
			jdbc.update(
					"INSERT INTO detail_transaksi (id_transaksi, id_barang, jumlah, harga_satuan, subtotal) VALUES (?, ?, 1, 55000.00, 55000.00)",
					id, b1);
			jdbc.update(
					"INSERT INTO detail_transaksi (id_transaksi, id_barang, jumlah, harga_satuan, subtotal) VALUES (?, ?, 1, 3000.00, 3000.00)",
					id, b2);
		}
	}
}