package com.example.penjualan.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "barang")
public class Barang {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_barang")
	private Long idBarang;

	@NotBlank(message = "Kode barang wajib diisi.")
	@Size(max = 20, message = "Kode barang maksimal 20 karakter.")
	@Column(name = "kode_barang", nullable = false, unique = true, length = 20)
	private String kodeBarang;

	@NotBlank(message = "Nama barang wajib diisi.")
	@Size(max = 100, message = "Nama barang maksimal 100 karakter.")
	@Column(name = "nama_barang", nullable = false, length = 100)
	private String namaBarang;

	@NotBlank(message = "Satuan wajib diisi.")
	@Size(max = 20, message = "Satuan maksimal 20 karakter.")
	@Column(nullable = false, length = 20)
	private String satuan = "pcs";

	@NotNull(message = "Harga wajib diisi.")
	@DecimalMin(value = "0.0", message = "Harga tidak boleh negatif.")
	@Column(nullable = false, precision = 15, scale = 2)
	private BigDecimal harga;

	@NotNull(message = "Stok wajib diisi.")
	@Min(value = 0, message = "Stok tidak boleh negatif.")
	@Column(nullable = false)
	private Integer stok = 0;

	@NotNull(message = "Stok minimum wajib diisi.")
	@Min(value = 0, message = "Stok minimum tidak boleh negatif.")
	@Column(name = "stok_minimum", nullable = false)
	private Integer stokMinimum = 5;

	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	private LocalDateTime createdAt;

	public Long getIdBarang() {
		return idBarang;
	}

	public void setIdBarang(Long idBarang) {
		this.idBarang = idBarang;
	}

	public String getKodeBarang() {
		return kodeBarang;
	}

	public void setKodeBarang(String kodeBarang) {
		this.kodeBarang = kodeBarang;
	}

	public String getNamaBarang() {
		return namaBarang;
	}

	public void setNamaBarang(String namaBarang) {
		this.namaBarang = namaBarang;
	}

	public String getSatuan() {
		return satuan;
	}

	public void setSatuan(String satuan) {
		this.satuan = satuan;
	}

	public BigDecimal getHarga() {
		return harga;
	}

	public void setHarga(BigDecimal harga) {
		this.harga = harga;
	}

	public Integer getStok() {
		return stok;
	}

	public void setStok(Integer stok) {
		this.stok = stok;
	}

	public Integer getStokMinimum() {
		return stokMinimum;
	}

	public void setStokMinimum(Integer stokMinimum) {
		this.stokMinimum = stokMinimum;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}
}
