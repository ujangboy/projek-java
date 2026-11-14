package com.example.penjualan.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "detail_transaksi")
public class DetailTransaksi {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_detail")
	private Long idDetail;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_transaksi", nullable = false)
	private Transaksi transaksi;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_barang", nullable = false)
	private Barang barang;

	@Column(nullable = false)
	private Integer jumlah;

	@Column(name = "harga_satuan", nullable = false, precision = 15, scale = 2)
	private BigDecimal hargaSatuan;

	@Column(nullable = false, precision = 15, scale = 2)
	private BigDecimal subtotal;

	public Long getIdDetail() {
		return idDetail;
	}

	public void setIdDetail(Long idDetail) {
		this.idDetail = idDetail;
	}

	public Transaksi getTransaksi() {
		return transaksi;
	}

	public void setTransaksi(Transaksi transaksi) {
		this.transaksi = transaksi;
	}

	public Barang getBarang() {
		return barang;
	}

	public void setBarang(Barang barang) {
		this.barang = barang;
	}

	public Integer getJumlah() {
		return jumlah;
	}

	public void setJumlah(Integer jumlah) {
		this.jumlah = jumlah;
	}

	public BigDecimal getHargaSatuan() {
		return hargaSatuan;
	}

	public void setHargaSatuan(BigDecimal hargaSatuan) {
		this.hargaSatuan = hargaSatuan;
	}

	public BigDecimal getSubtotal() {
		return subtotal;
	}

	public void setSubtotal(BigDecimal subtotal) {
		this.subtotal = subtotal;
	}
}
