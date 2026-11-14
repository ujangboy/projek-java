package com.example.penjualan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.example.penjualan.dto.TransaksiForm;
import com.example.penjualan.entity.Transaksi;
import com.example.penjualan.service.TransaksiService;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "admin", roles = "ADMIN")
class Phase3TransaksiTest {

	private static final long CUSTOMER_ID = 930001L;
	private static final long GOODS_A_ID = 930001L;
	private static final long GOODS_B_ID = 930002L;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private TransaksiService transaksiService;

	@BeforeEach
	void setUp() {
		cleanUp();
		jdbc.update("INSERT INTO pelanggan (id_pelanggan, nama) VALUES (?, 'Pelanggan Uji')", CUSTOMER_ID);
		jdbc.update("""
				INSERT INTO barang (id_barang, kode_barang, nama_barang, satuan, harga, stok, stok_minimum)
				VALUES (?, 'TST-001', 'Barang Uji A', 'pcs', 1000.00, 10, 1)
				""", GOODS_A_ID);
		jdbc.update("""
				INSERT INTO barang (id_barang, kode_barang, nama_barang, satuan, harga, stok, stok_minimum)
				VALUES (?, 'TST-002', 'Barang Uji B', 'pcs', 2500.00, 5, 1)
				""", GOODS_B_ID);
	}

	@AfterEach
	void cleanUp() {
		jdbc.update("DELETE FROM transaksi WHERE id_pelanggan = ?", CUSTOMER_ID);
		jdbc.update("DELETE FROM barang WHERE id_barang IN (?, ?)", GOODS_A_ID, GOODS_B_ID);
		jdbc.update("DELETE FROM pelanggan WHERE id_pelanggan = ?", CUSTOMER_ID);
		jdbc.update("ALTER TABLE transaksi AUTO_INCREMENT = 1");
		jdbc.update("ALTER TABLE detail_transaksi AUTO_INCREMENT = 1");
		jdbc.update("ALTER TABLE pelanggan AUTO_INCREMENT = 6");
		jdbc.update("ALTER TABLE barang AUTO_INCREMENT = 9");
	}

	@Test
	void numberUsesDateAndPaddedId() {
		Transaksi transaksi = new Transaksi();
		transaksi.setIdTransaksi(7L);
		transaksi.setTanggal(LocalDateTime.of(2026, 11, 14, 10, 0));

		assertThat(transaksi.getNomorTransaksi()).isEqualTo("TRX-20261114-0007");
	}

	@Test
	void listShowsTransactionsNewestFirst() throws Exception {
		Long adminId = jdbc.queryForObject("SELECT id_user FROM users WHERE username = 'admin'", Long.class);
		jdbc.update("""
				INSERT INTO transaksi (id_transaksi, id_pelanggan, id_user, tanggal, total)
				VALUES (930010, ?, ?, '2026-11-13 10:00:00', 1000),
				       (930011, ?, ?, '2026-11-14 10:00:00', 2500)
				""", CUSTOMER_ID, adminId, CUSTOMER_ID, adminId);

		MvcResult result = mockMvc.perform(get("/transaksi"))
				.andExpect(status().isOk())
				.andExpect(view().name("transaksi/list"))
				.andExpect(content().string(containsString("Data Transaksi")))
				.andReturn();

		String html = result.getResponse().getContentAsString();
		assertThat(html.indexOf("TRX-20261114-930011")).isLessThan(html.indexOf("TRX-20261113-930010"));
	}

	@Test
	void createSavesHeaderDetailsStockAndTotal() throws Exception {
		MvcResult result = mockMvc.perform(post("/transaksi").with(csrf())
				.param("idPelanggan", String.valueOf(CUSTOMER_ID))
				.param("items[0].idBarang", String.valueOf(GOODS_A_ID))
				.param("items[0].jumlah", "2")
				.param("items[1].idBarang", String.valueOf(GOODS_B_ID))
				.param("items[1].jumlah", "1"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrlPattern("/transaksi/*"))
				.andExpect(flash().attribute("success", "Transaksi berhasil disimpan."))
				.andReturn();

		Long id = idFromRedirect(result);
		assertThat(count("SELECT COUNT(*) FROM detail_transaksi WHERE id_transaksi = " + id)).isEqualTo(2);
		assertThat(amount("SELECT total FROM transaksi WHERE id_transaksi = " + id)).isEqualByComparingTo("4500.00");
		assertThat(count("SELECT stok FROM barang WHERE id_barang = " + GOODS_A_ID)).isEqualTo(8);
		assertThat(count("SELECT stok FROM barang WHERE id_barang = " + GOODS_B_ID)).isEqualTo(4);
	}

	@Test
	void duplicateRowsAreMerged() throws Exception {
		mockMvc.perform(post("/transaksi").with(csrf())
				.param("idPelanggan", String.valueOf(CUSTOMER_ID))
				.param("items[0].idBarang", String.valueOf(GOODS_A_ID))
				.param("items[0].jumlah", "1")
				.param("items[1].idBarang", String.valueOf(GOODS_A_ID))
				.param("items[1].jumlah", "2"))
				.andExpect(status().is3xxRedirection());

		Long id = jdbc.queryForObject(
				"SELECT id_transaksi FROM transaksi WHERE id_pelanggan = ? ORDER BY id_transaksi DESC LIMIT 1",
				Long.class, CUSTOMER_ID);
		assertThat(count("SELECT COUNT(*) FROM detail_transaksi WHERE id_transaksi = " + id)).isEqualTo(1);
		assertThat(count("SELECT jumlah FROM detail_transaksi WHERE id_transaksi = " + id)).isEqualTo(3);
		assertThat(amount("SELECT total FROM transaksi WHERE id_transaksi = " + id)).isEqualByComparingTo("3000.00");
		assertThat(count("SELECT stok FROM barang WHERE id_barang = " + GOODS_A_ID)).isEqualTo(7);
	}

	@Test
	void insufficientStockRollsBackEverything() throws Exception {
		mockMvc.perform(post("/transaksi").with(csrf())
				.param("idPelanggan", String.valueOf(CUSTOMER_ID))
				.param("items[0].idBarang", String.valueOf(GOODS_B_ID))
				.param("items[0].jumlah", "1")
				.param("items[1].idBarang", String.valueOf(GOODS_A_ID))
				.param("items[1].jumlah", "999"))
				.andExpect(status().isOk())
				.andExpect(view().name("transaksi/form"))
				.andExpect(model().attribute("error",
						"Stok barang 'Barang Uji A' tidak mencukupi. Tersedia: 10, diminta: 999."))
				.andExpect(content().string(containsString("tidak mencukupi")));

		assertThat(count("SELECT COUNT(*) FROM transaksi")).isZero();
		assertThat(count("SELECT COUNT(*) FROM detail_transaksi")).isZero();
		assertThat(count("SELECT stok FROM barang WHERE id_barang = " + GOODS_A_ID)).isEqualTo(10);
		assertThat(count("SELECT stok FROM barang WHERE id_barang = " + GOODS_B_ID)).isEqualTo(5);
	}

	@Test
	void missingCustomerIsRejected() throws Exception {
		mockMvc.perform(post("/transaksi").with(csrf())
				.param("items[0].idBarang", String.valueOf(GOODS_A_ID))
				.param("items[0].jumlah", "1"))
				.andExpect(status().isOk())
				.andExpect(view().name("transaksi/form"))
				.andExpect(model().attribute("error", "Pelanggan wajib dipilih."));

		assertThat(count("SELECT COUNT(*) FROM transaksi")).isZero();
	}

	@Test
	void unknownCustomerIsRejected() throws Exception {
		mockMvc.perform(post("/transaksi").with(csrf())
				.param("idPelanggan", "999999")
				.param("items[0].idBarang", String.valueOf(GOODS_A_ID))
				.param("items[0].jumlah", "1"))
				.andExpect(status().isOk())
				.andExpect(view().name("transaksi/form"))
				.andExpect(model().attribute("error", "Pelanggan tidak ditemukan."));

		assertThat(count("SELECT COUNT(*) FROM transaksi")).isZero();
	}

	@Test
	void emptyItemsAreRejected() throws Exception {
		mockMvc.perform(post("/transaksi").with(csrf())
				.param("idPelanggan", String.valueOf(CUSTOMER_ID)))
				.andExpect(status().isOk())
				.andExpect(view().name("transaksi/form"))
				.andExpect(model().attribute("error", "Minimal satu barang harus dipilih."));

		assertThat(count("SELECT COUNT(*) FROM transaksi")).isZero();
	}

	@Test
	void zeroQuantityIsRejected() throws Exception {
		mockMvc.perform(post("/transaksi").with(csrf())
				.param("idPelanggan", String.valueOf(CUSTOMER_ID))
				.param("items[0].idBarang", String.valueOf(GOODS_A_ID))
				.param("items[0].jumlah", "0"))
				.andExpect(status().isOk())
				.andExpect(view().name("transaksi/form"))
				.andExpect(model().attribute("error", "Jumlah barang minimal 1."));

		assertThat(count("SELECT COUNT(*) FROM transaksi")).isZero();
	}

	@Test
	void nonNumericQuantityIsRejected() throws Exception {
		mockMvc.perform(post("/transaksi").with(csrf())
				.param("idPelanggan", String.valueOf(CUSTOMER_ID))
				.param("items[0].idBarang", String.valueOf(GOODS_A_ID))
				.param("items[0].jumlah", "abc"))
				.andExpect(status().isOk())
				.andExpect(view().name("transaksi/form"))
				.andExpect(model().attribute("error", "Jumlah barang harus berupa angka."));

		assertThat(count("SELECT COUNT(*) FROM transaksi")).isZero();
	}

	@Test
	void receiptShowsInstitutionAndSnapshot() throws Exception {
		TransaksiForm form = new TransaksiForm();
		form.setIdPelanggan(CUSTOMER_ID);
		TransaksiForm.ItemForm item = new TransaksiForm.ItemForm();
		item.setIdBarang(GOODS_A_ID);
		item.setJumlah(2);
		form.setItems(List.of(item));
		Transaksi transaksi = transaksiService.create(form, "admin");

		jdbc.update("UPDATE barang SET harga = 9999.00 WHERE id_barang = ?", GOODS_A_ID);

		mockMvc.perform(get("/transaksi/" + transaksi.getIdTransaksi()))
				.andExpect(status().isOk())
				.andExpect(view().name("transaksi/detail"))
				.andExpect(content().string(containsString("Toko Sinar Jaya")))
				.andExpect(content().string(containsString("1.000,00")))
				.andExpect(content().string(containsString("2.000,00")))
				.andExpect(content().string(not(containsString("9.999,00"))))
				.andExpect(content().string(containsString("Cetak")));
	}

	@Test
	void receiptForMissingTransactionIs404() throws Exception {
		mockMvc.perform(get("/transaksi/999999"))
				.andExpect(status().isNotFound());
	}

	@Test
	void deleteRouteDoesNotExist() throws Exception {
		Long adminId = jdbc.queryForObject("SELECT id_user FROM users WHERE username = 'admin'", Long.class);
		jdbc.update("""
				INSERT INTO transaksi (id_transaksi, id_pelanggan, id_user, tanggal, total)
				VALUES (930012, ?, ?, CURRENT_TIMESTAMP, 1000)
				""", CUSTOMER_ID, adminId);

		mockMvc.perform(post("/transaksi/930012/delete").with(csrf()))
				.andExpect(status().isNotFound());

		assertThat(count("SELECT COUNT(*) FROM transaksi WHERE id_transaksi = 930012")).isEqualTo(1);
	}

	private Long idFromRedirect(MvcResult result) {
		String url = result.getResponse().getRedirectedUrl();
		return Long.valueOf(url.substring(url.lastIndexOf('/') + 1));
	}

	private int count(String sql) {
		return jdbc.queryForObject(sql, Integer.class);
	}

	private BigDecimal amount(String sql) {
		return jdbc.queryForObject(sql, BigDecimal.class);
	}
}
