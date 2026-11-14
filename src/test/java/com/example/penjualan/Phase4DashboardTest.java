package com.example.penjualan;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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
class Phase4DashboardTest {

	private static final long CUSTOMER_ID = 940001L;
	private static final long LOW_STOCK_BARANG_ID = 940001L;
	private static final long TX_TODAY_ID = 940010L;
	private static final long TX_MONTH_ID = 940011L;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbc;

	@BeforeEach
	void setUp() {
		cleanUp();

		Long adminId = jdbc.queryForObject("SELECT id_user FROM users WHERE username = 'admin'", Long.class);

		jdbc.update("INSERT INTO pelanggan (id_pelanggan, nama) VALUES (?, 'Pelanggan Dashboard')", CUSTOMER_ID);
		jdbc.update("""
				INSERT INTO barang (id_barang, kode_barang, nama_barang, satuan, harga, stok, stok_minimum)
				VALUES (?, 'DASH-001', 'Barang Menipis', 'pcs', 1000.00, 2, 5)
				""", LOW_STOCK_BARANG_ID);

		LocalDate today = LocalDate.now();
		LocalDateTime todayTime = today.atTime(10, 0);
		LocalDateTime monthTime = today.withDayOfMonth(1).atTime(8, 0);

		jdbc.update("""
				INSERT INTO transaksi (id_transaksi, id_pelanggan, id_user, tanggal, total)
				VALUES (?, ?, ?, ?, 150000.00)
				""", TX_TODAY_ID, CUSTOMER_ID, adminId, java.sql.Timestamp.valueOf(todayTime));
		jdbc.update("""
				INSERT INTO transaksi (id_transaksi, id_pelanggan, id_user, tanggal, total)
				VALUES (?, ?, ?, ?, 50000.00)
				""", TX_MONTH_ID, CUSTOMER_ID, adminId, java.sql.Timestamp.valueOf(monthTime));
	}

	@AfterEach
	void cleanUp() {
		jdbc.update("DELETE FROM transaksi WHERE id_transaksi IN (?, ?)", TX_TODAY_ID, TX_MONTH_ID);
		jdbc.update("DELETE FROM barang WHERE id_barang = ?", LOW_STOCK_BARANG_ID);
		jdbc.update("DELETE FROM pelanggan WHERE id_pelanggan = ?", CUSTOMER_ID);
	}

	@Test
	void dashboardShowsMetricsRecentAndLowStock() throws Exception {
		LocalDate today = LocalDate.now();
		boolean sameMoment = today.getDayOfMonth() == 1; // today tx and month tx same day/time context possible but ids differ
		// month tx is 1st 08:00, today tx is today 10:00; if today is 1st they are same day.
		String omzetHariIni = today.getDayOfMonth() == 1 ? "200.000,00" : "150.000,00";
		String omzetBulanIni = "200.000,00";
		String omzetTotalPrefix = "Rp "; // at least includes our 200000 plus any seed? seeds have no transaksi
		String nomorToday = "TRX-" + today.format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + String.format("%04d", TX_TODAY_ID);

		mockMvc.perform(get("/dashboard"))
				.andExpect(status().isOk())
				.andExpect(view().name("dashboard/index"))
				.andExpect(content().string(containsString("Total Pelanggan")))
				.andExpect(content().string(containsString("Total Barang")))
				.andExpect(content().string(containsString("Total Transaksi")))
				.andExpect(content().string(containsString("Omzet Hari Ini")))
				.andExpect(content().string(containsString("Omzet Bulan Ini")))
				.andExpect(content().string(containsString("Omzet Total")))
				.andExpect(content().string(containsString("Rp " + omzetHariIni)))
				.andExpect(content().string(containsString("Rp " + omzetBulanIni)))
				.andExpect(content().string(containsString("Rp 200.000,00"))) // total (no other tx seeded)
				.andExpect(content().string(containsString("Transaksi Terbaru")))
				.andExpect(content().string(containsString(nomorToday)))
				.andExpect(content().string(containsString("Pelanggan Dashboard")))
				.andExpect(content().string(containsString("Stok Menipis")))
				.andExpect(content().string(containsString("DASH-001")))
				.andExpect(content().string(containsString("Barang Menipis")));
	}
}