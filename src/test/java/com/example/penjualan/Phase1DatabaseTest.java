package com.example.penjualan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
class Phase1DatabaseTest {

	private static final List<String> TABLES = List.of(
			"pelanggan", "barang", "transaksi", "detail_transaksi", "users", "identitas_instansi");

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	void allSixTablesExist() {
		List<String> tables = jdbc.queryForList("""
				SELECT table_name FROM information_schema.tables
				WHERE table_schema = DATABASE() AND table_name IN
				('pelanggan', 'barang', 'transaksi', 'detail_transaksi', 'users', 'identitas_instansi')
				ORDER BY table_name
				""", String.class);
		assertThat(tables).containsExactly(
				"barang", "detail_transaksi", "identitas_instansi", "pelanggan", "transaksi", "users");
	}

	@Test
	void everyTableHasPrimaryKey() {
		for (String table : TABLES) {
			Integer count = jdbc.queryForObject("""
					SELECT COUNT(*) FROM information_schema.table_constraints
					WHERE table_schema = DATABASE() AND table_name = ? AND constraint_type = 'PRIMARY KEY'
					""", Integer.class, table);
			assertThat(count).as("primary key pada tabel %s", table).isEqualTo(1);
		}
	}

	@Test
	void foreignKeysUseExpectedDeleteRules() {
		record Rule(String name, String deleteRule) {
		}
		List<Rule> rules = jdbc.query("""
				SELECT constraint_name, delete_rule FROM information_schema.referential_constraints
				WHERE constraint_schema = DATABASE()
				""", (rs, row) -> new Rule(rs.getString(1), rs.getString(2)));
		assertThat(rules).contains(
				new Rule("fk_transaksi_pelanggan", "RESTRICT"),
				new Rule("fk_transaksi_user", "RESTRICT"),
				new Rule("fk_detail_transaksi", "CASCADE"),
				new Rule("fk_detail_barang", "RESTRICT"));
	}

	@Test
	void seedMasterDataLoaded() {
		assertThat(count("SELECT COUNT(*) FROM pelanggan")).isGreaterThanOrEqualTo(5);
		assertThat(count("SELECT COUNT(*) FROM barang")).isGreaterThanOrEqualTo(8);
		assertThat(count("SELECT COUNT(*) FROM identitas_instansi")).isEqualTo(1);
		assertThat(count("SELECT COUNT(*) FROM barang WHERE kode_barang = 'BRG-001'")).isEqualTo(1);
	}

	@Test
	@Transactional
	void deletingCustomerWithTransactionIsRestricted() {
		jdbc.update("INSERT INTO pelanggan (id_pelanggan, nama) VALUES (900001, 'Pelanggan Uji')");
		jdbc.update("""
				INSERT INTO transaksi (id_transaksi, id_pelanggan, id_user, total)
				VALUES (900001, 900001, (SELECT id_user FROM users WHERE username = 'admin'), 0)
				""");

		assertThatThrownBy(() -> jdbc.update("DELETE FROM pelanggan WHERE id_pelanggan = 900001"))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	@Transactional
	void deletingTransactionCascadesToDetail() {
		jdbc.update("INSERT INTO pelanggan (id_pelanggan, nama) VALUES (900002, 'Pelanggan Uji')");
		jdbc.update("""
				INSERT INTO transaksi (id_transaksi, id_pelanggan, id_user, total)
				VALUES (900002, 900002, (SELECT id_user FROM users WHERE username = 'admin'), 55000)
				""");
		jdbc.update("""
				INSERT INTO detail_transaksi (id_detail, id_transaksi, id_barang, jumlah, harga_satuan, subtotal)
				VALUES (900002, 900002, (SELECT id_barang FROM barang WHERE kode_barang = 'BRG-001'), 1, 55000, 55000)
				""");

		jdbc.update("DELETE FROM transaksi WHERE id_transaksi = 900002");

		assertThat(count("SELECT COUNT(*) FROM detail_transaksi WHERE id_transaksi = 900002")).isZero();
	}

	@Test
	@Transactional
	void deletingBarangUsedByDetailIsRestricted() {
		jdbc.update("""
				INSERT INTO barang (id_barang, kode_barang, nama_barang, satuan, harga, stok, stok_minimum)
				VALUES (900003, 'TST-900003', 'Barang Uji', 'pcs', 1000, 10, 1)
				""");
		jdbc.update("""
				INSERT INTO transaksi (id_transaksi, id_pelanggan, id_user, total)
				VALUES (900003, (SELECT id_pelanggan FROM pelanggan ORDER BY id_pelanggan LIMIT 1),
				        (SELECT id_user FROM users WHERE username = 'admin'), 1000)
				""");
		jdbc.update("""
				INSERT INTO detail_transaksi (id_detail, id_transaksi, id_barang, jumlah, harga_satuan, subtotal)
				VALUES (900003, 900003, 900003, 1, 1000, 1000)
				""");

		assertThatThrownBy(() -> jdbc.update("DELETE FROM barang WHERE id_barang = 900003"))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	private Integer count(String sql) {
		return jdbc.queryForObject(sql, Integer.class);
	}
}
