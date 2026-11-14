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
class Phase2BarangTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbc;

	@AfterEach
	void cleanUp() {
		jdbc.update("DELETE FROM detail_transaksi WHERE id_transaksi IN (920004)");
		jdbc.update("DELETE FROM transaksi WHERE id_transaksi IN (920004)");
		jdbc.update("DELETE FROM barang WHERE id_barang IN (920003, 920004, 920010)");
		jdbc.update("DELETE FROM pelanggan WHERE id_pelanggan = 920004");
		jdbc.update("DELETE FROM barang WHERE kode_barang IN ('BRG-900', 'BRG-901', 'BRG-902')");
	}

	@Test
	void listShowsSeedGoods() throws Exception {
		mockMvc.perform(get("/barang"))
				.andExpect(status().isOk())
				.andExpect(view().name("barang/list"))
				.andExpect(content().string(containsString("Data Barang")))
				.andExpect(content().string(containsString("BRG-001")))
				.andExpect(content().string(containsString("Kertas HVS A4 80gsm")));
	}

	@Test
	void searchByCode() throws Exception {
		mockMvc.perform(get("/barang").param("q", "BRG-006"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Lem Kertas 20gr")))
				.andExpect(content().string(not(containsString("Kertas HVS A4 80gsm"))));
	}

	@Test
	void searchByName() throws Exception {
		mockMvc.perform(get("/barang").param("q", "Stapler"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Stapler Medium")))
				.andExpect(content().string(not(containsString("Pulpen Gel Hitam"))));
	}

	@Test
	void createGoodsInsertsRow() throws Exception {
		mockMvc.perform(post("/barang").with(csrf())
				.param("kodeBarang", "BRG-900")
				.param("namaBarang", "Barang Baru")
				.param("satuan", "pcs")
				.param("harga", "15000")
				.param("stok", "20")
				.param("stokMinimum", "5"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/barang"))
				.andExpect(flash().attribute("success", "Barang berhasil ditambahkan."));

		assertThat(count("SELECT COUNT(*) FROM barang WHERE kode_barang = 'BRG-900'")).isEqualTo(1);
	}

	@Test
	void duplicateCodeIsRejected() throws Exception {
		int before = count("SELECT COUNT(*) FROM barang");

		mockMvc.perform(post("/barang").with(csrf())
				.param("kodeBarang", "BRG-001")
				.param("namaBarang", "Barang Duplikat")
				.param("satuan", "pcs")
				.param("harga", "10000")
				.param("stok", "5")
				.param("stokMinimum", "1"))
				.andExpect(status().isOk())
				.andExpect(view().name("barang/form"))
				.andExpect(model().attributeHasFieldErrors("barang", "kodeBarang"))
				.andExpect(content().string(containsString("Kode barang sudah digunakan.")));

		assertThat(count("SELECT COUNT(*) FROM barang")).isEqualTo(before);
	}

	@Test
	void negativePriceIsRejected() throws Exception {
		mockMvc.perform(post("/barang").with(csrf())
				.param("kodeBarang", "BRG-901")
				.param("namaBarang", "Harga Negatif")
				.param("satuan", "pcs")
				.param("harga", "-1")
				.param("stok", "5")
				.param("stokMinimum", "1"))
				.andExpect(status().isOk())
				.andExpect(view().name("barang/form"))
				.andExpect(model().attributeHasFieldErrors("barang", "harga"))
				.andExpect(content().string(containsString("Harga tidak boleh negatif.")));

		assertThat(count("SELECT COUNT(*) FROM barang WHERE kode_barang = 'BRG-901'")).isZero();
	}

	@Test
	void nonNumericPriceIsRejected() throws Exception {
		mockMvc.perform(post("/barang").with(csrf())
				.param("kodeBarang", "BRG-902")
				.param("namaBarang", "Harga Bukan Angka")
				.param("satuan", "pcs")
				.param("harga", "abc")
				.param("stok", "5")
				.param("stokMinimum", "1"))
				.andExpect(status().isOk())
				.andExpect(view().name("barang/form"))
				.andExpect(model().attributeHasFieldErrors("barang", "harga"))
				.andExpect(content().string(containsString("Harga harus berupa angka.")));

		assertThat(count("SELECT COUNT(*) FROM barang WHERE kode_barang = 'BRG-902'")).isZero();
	}

	@Test
	void updateGoodsChangesRow() throws Exception {
		jdbc.update("""
				INSERT INTO barang (id_barang, kode_barang, nama_barang, satuan, harga, stok, stok_minimum)
				VALUES (920010, 'BRG-910', 'Barang Update', 'pcs', 10000, 5, 1)
				""");

		mockMvc.perform(post("/barang/920010").with(csrf())
				.param("kodeBarang", "BRG-910")
				.param("namaBarang", "Barang Update Baru")
				.param("satuan", "pcs")
				.param("harga", "12500")
				.param("stok", "6")
				.param("stokMinimum", "2"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/barang"))
				.andExpect(flash().attribute("success", "Barang berhasil diperbarui."));

		assertThat(count("SELECT COUNT(*) FROM barang WHERE id_barang = 920010"
				+ " AND nama_barang = 'Barang Update Baru' AND harga = 12500.00")).isEqualTo(1);
	}

	@Test
	void deleteUnusedGoodsRemovesRow() throws Exception {
		jdbc.update("""
				INSERT INTO barang (id_barang, kode_barang, nama_barang, satuan, harga, stok, stok_minimum)
				VALUES (920003, 'BRG-903', 'Barang Hapus', 'pcs', 1000, 1, 1)
				""");

		mockMvc.perform(post("/barang/920003/delete").with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/barang"))
				.andExpect(flash().attribute("success", "Barang berhasil dihapus."));

		assertThat(count("SELECT COUNT(*) FROM barang WHERE id_barang = 920003")).isZero();
	}

	@Test
	void deleteGoodsUsedByDetailIsRejected() throws Exception {
		jdbc.update("INSERT INTO pelanggan (id_pelanggan, nama) VALUES (920004, 'Pelanggan Uji')");
		jdbc.update("""
				INSERT INTO barang (id_barang, kode_barang, nama_barang, satuan, harga, stok, stok_minimum)
				VALUES (920004, 'BRG-904', 'Barang Terpakai', 'pcs', 1000, 1, 1)
				""");
		jdbc.update("""
				INSERT INTO transaksi (id_transaksi, id_pelanggan, id_user, total)
				VALUES (920004, 920004, (SELECT id_user FROM users WHERE username = 'admin'), 1000)
				""");
		jdbc.update("""
				INSERT INTO detail_transaksi (id_detail, id_transaksi, id_barang, jumlah, harga_satuan, subtotal)
				VALUES (920004, 920004, 920004, 1, 1000, 1000)
				""");

		mockMvc.perform(post("/barang/920004/delete").with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/barang"))
				.andExpect(flash().attribute("error",
						"Barang tidak dapat dihapus karena sudah dipakai pada transaksi."));

		assertThat(count("SELECT COUNT(*) FROM barang WHERE id_barang = 920004")).isEqualTo(1);
	}

	private int count(String sql) {
		return jdbc.queryForObject(sql, Integer.class);
	}
}
