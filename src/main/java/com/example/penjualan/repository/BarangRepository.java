package com.example.penjualan.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.penjualan.entity.Barang;

public interface BarangRepository extends JpaRepository<Barang, Long> {

	@Query("""
			SELECT b FROM Barang b
			WHERE LOWER(b.kodeBarang) LIKE LOWER(CONCAT('%', :q, '%'))
			   OR LOWER(b.namaBarang) LIKE LOWER(CONCAT('%', :q, '%'))
			""")
	Page<Barang> search(@Param("q") String q, Pageable pageable);

	boolean existsByKodeBarang(String kodeBarang);

	boolean existsByKodeBarangAndIdBarangNot(String kodeBarang, Long idBarang);
}
