package com.example.penjualan.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.penjualan.dto.TransaksiForm;
import com.example.penjualan.dto.TransaksiForm.ItemForm;
import com.example.penjualan.entity.Transaksi;
import com.example.penjualan.repository.BarangRepository;
import com.example.penjualan.repository.IdentitasInstansiRepository;
import com.example.penjualan.repository.PelangganRepository;
import com.example.penjualan.repository.TransaksiRepository;
import com.example.penjualan.service.TransaksiException;
import com.example.penjualan.service.TransaksiService;

@Controller
@RequestMapping("/transaksi")
public class TransaksiController {

	private static final int PAGE_SIZE = 10;

	private final TransaksiRepository transaksiRepository;

	private final PelangganRepository pelangganRepository;

	private final BarangRepository barangRepository;

	private final IdentitasInstansiRepository identitasInstansiRepository;

	private final TransaksiService transaksiService;

	public TransaksiController(TransaksiRepository transaksiRepository, PelangganRepository pelangganRepository,
			BarangRepository barangRepository, IdentitasInstansiRepository identitasInstansiRepository,
			TransaksiService transaksiService) {
		this.transaksiRepository = transaksiRepository;
		this.pelangganRepository = pelangganRepository;
		this.barangRepository = barangRepository;
		this.identitasInstansiRepository = identitasInstansiRepository;
		this.transaksiService = transaksiService;
	}

	@GetMapping
	public String list(@RequestParam(defaultValue = "0") int page, Model model) {
		Pageable pageable = PageRequest.of(Math.max(page, 0), PAGE_SIZE, Sort.unsorted());
		model.addAttribute("page", transaksiRepository.findAllByOrderByTanggalDescIdTransaksiDesc(pageable));
		model.addAttribute("activeMenu", "transaksi");
		return "transaksi/list";
	}

	@GetMapping("/new")
	public String createForm(Model model) {
		TransaksiForm form = new TransaksiForm();
		form.getItems().add(newItem());
		return formView(model, form);
	}

	@PostMapping
	public String create(@ModelAttribute("transaksiForm") TransaksiForm form, BindingResult result, Model model,
			RedirectAttributes flash, Authentication authentication) {
		if (result.hasErrors()) {
			boolean quantityError = result.getFieldErrors().stream()
					.anyMatch(error -> error.getField().contains("jumlah"));
			model.addAttribute("error",
					quantityError ? "Jumlah barang harus berupa angka." : "Data transaksi tidak valid.");
			return formView(model, form);
		}

		try {
			Transaksi transaksi = transaksiService.create(form, authentication.getName());
			flash.addFlashAttribute("success", "Transaksi berhasil disimpan.");
			return "redirect:/transaksi/" + transaksi.getIdTransaksi();
		}
		catch (TransaksiException ex) {
			model.addAttribute("error", ex.getMessage());
			return formView(model, form);
		}
	}

	@GetMapping("/{id}")
	public String detail(@PathVariable Long id, Model model) {
		Transaksi transaksi = transaksiRepository.findDetailById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		model.addAttribute("transaksi", transaksi);
		model.addAttribute("identitas", identitasInstansiRepository.findFirstByOrderByIdInstansiAsc().orElse(null));
		model.addAttribute("activeMenu", "transaksi");
		return "transaksi/detail";
	}

	private String formView(Model model, TransaksiForm form) {
		if (form.getItems() == null || form.getItems().isEmpty()) {
			form.getItems().add(newItem());
		}
		model.addAttribute("transaksiForm", form);
		model.addAttribute("pelangganList", pelangganRepository.findAll(Sort.by("nama").ascending()));
		model.addAttribute("barangList", barangRepository.findAll(Sort.by("kodeBarang").ascending()));
		model.addAttribute("activeMenu", "transaksi");
		return "transaksi/form";
	}

	private ItemForm newItem() {
		ItemForm item = new ItemForm();
		item.setJumlah(1);
		return item;
	}
}
