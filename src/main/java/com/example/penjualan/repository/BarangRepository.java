package com.example.penjualan.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

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

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT b FROM Barang b WHERE b.idBarang IN :ids ORDER BY b.idBarang")
	List<Barang> findAllForUpdate(@Param("ids") Collection<Long> ids);

	@Query("SELECT b FROM Barang b WHERE b.stok <= b.stokMinimum ORDER BY b.stok ASC, b.namaBarang ASC")
	List<Barang> findLowStock();
}
