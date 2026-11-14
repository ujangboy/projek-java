package com.example.penjualan.controller;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
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

import com.example.penjualan.entity.Barang;
import com.example.penjualan.repository.BarangRepository;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/barang")
public class BarangController {

	private static final int PAGE_SIZE = 10;

	private final BarangRepository barangRepository;

	public BarangController(BarangRepository barangRepository) {
		this.barangRepository = barangRepository;
	}

	@GetMapping
	public String list(@RequestParam(defaultValue = "") String q,
			@RequestParam(defaultValue = "0") int page, Model model) {
		Pageable pageable = PageRequest.of(Math.max(page, 0), PAGE_SIZE, Sort.by("kodeBarang").ascending());
		String keyword = q.trim();
		Page<Barang> result = keyword.isEmpty()
				? barangRepository.findAll(pageable)
				: barangRepository.search(keyword, pageable);

		model.addAttribute("page", result);
		model.addAttribute("q", keyword);
		model.addAttribute("activeMenu", "barang");
		return "barang/list";
	}

	@GetMapping("/new")
	public String createForm(Model model) {
		model.addAttribute("barang", new Barang());
		return form(model, "create", "Tambah Barang", "/barang");
	}

	@PostMapping
	public String create(@Valid @ModelAttribute("barang") Barang barang, BindingResult result,
			Model model, RedirectAttributes flash) {
		if (barangRepository.existsByKodeBarang(barang.getKodeBarang())) {
			result.rejectValue("kodeBarang", "duplicate", "Kode barang sudah digunakan.");
		}
		if (result.hasErrors()) {
			return form(model, "create", "Tambah Barang", "/barang");
		}
		barang.setIdBarang(null);
		barangRepository.save(barang);
		flash.addFlashAttribute("success", "Barang berhasil ditambahkan.");
		return "redirect:/barang";
	}

	@GetMapping("/{id}/edit")
	public String editForm(@PathVariable Long id, Model model) {
		Barang barang = barangRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		model.addAttribute("barang", barang);
		return form(model, "edit", "Edit Barang", "/barang/" + id);
	}

	@PostMapping("/{id}")
	public String update(@PathVariable Long id, @Valid @ModelAttribute("barang") Barang barang,
			BindingResult result, Model model, RedirectAttributes flash) {
		if (!barangRepository.existsById(id)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}
		if (barangRepository.existsByKodeBarangAndIdBarangNot(barang.getKodeBarang(), id)) {
			result.rejectValue("kodeBarang", "duplicate", "Kode barang sudah digunakan.");
		}
		if (result.hasErrors()) {
			return form(model, "edit", "Edit Barang", "/barang/" + id);
		}
		barang.setIdBarang(id);
		barangRepository.save(barang);
		flash.addFlashAttribute("success", "Barang berhasil diperbarui.");
		return "redirect:/barang";
	}

	@PostMapping("/{id}/delete")
	public String delete(@PathVariable Long id, RedirectAttributes flash) {
		if (barangRepository.existsById(id)) {
			try {
				barangRepository.deleteById(id);
				flash.addFlashAttribute("success", "Barang berhasil dihapus.");
			}
			catch (DataIntegrityViolationException ex) {
				flash.addFlashAttribute("error",
						"Barang tidak dapat dihapus karena sudah dipakai pada transaksi.");
			}
		}
		return "redirect:/barang";
	}

	private String form(Model model, String mode, String title, String formAction) {
		model.addAttribute("mode", mode);
		model.addAttribute("pageTitle", title);
		model.addAttribute("formAction", formAction);
		model.addAttribute("activeMenu", "barang");
		return "barang/form";
	}
}
