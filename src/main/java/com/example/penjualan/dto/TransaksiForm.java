package com.example.penjualan.dto;

import java.util.ArrayList;
import java.util.List;

public class TransaksiForm {

	private Long idPelanggan;

	private List<ItemForm> items = new ArrayList<>();

	public Long getIdPelanggan() {
		return idPelanggan;
	}

	public void setIdPelanggan(Long idPelanggan) {
		this.idPelanggan = idPelanggan;
	}

	public List<ItemForm> getItems() {
		return items;
	}

	public void setItems(List<ItemForm> items) {
		this.items = items;
	}

	public static class ItemForm {

		private Long idBarang;

		private Integer jumlah;

		public Long getIdBarang() {
			return idBarang;
		}

		public void setIdBarang(Long idBarang) {
			this.idBarang = idBarang;
		}

		public Integer getJumlah() {
			return jumlah;
		}

		public void setJumlah(Integer jumlah) {
			this.jumlah = jumlah;
		}
	}
}
