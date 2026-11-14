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

import com.example.penjualan.entity.Pelanggan;
import com.example.penjualan.repository.PelangganRepository;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/pelanggan")
public class PelangganController {

	private static final int PAGE_SIZE = 10;

	private final PelangganRepository pelangganRepository;

	public PelangganController(PelangganRepository pelangganRepository) {
		this.pelangganRepository = pelangganRepository;
	}

	@GetMapping
	public String list(@RequestParam(defaultValue = "") String q,
			@RequestParam(defaultValue = "0") int page, Model model) {
		Pageable pageable = PageRequest.of(Math.max(page, 0), PAGE_SIZE, Sort.by("nama").ascending());
		String keyword = q.trim();
		Page<Pelanggan> result = keyword.isEmpty()
				? pelangganRepository.findAll(pageable)
				: pelangganRepository.findByNamaContainingIgnoreCase(keyword, pageable);

		model.addAttribute("page", result);
		model.addAttribute("q", keyword);
		model.addAttribute("activeMenu", "pelanggan");
		return "pelanggan/list";
	}

	@GetMapping("/new")
	public String createForm(Model model) {
		model.addAttribute("pelanggan", new Pelanggan());
		return form(model, "create", "Tambah Pelanggan", "/pelanggan");
	}

	@PostMapping
	public String create(@Valid @ModelAttribute("pelanggan") Pelanggan pelanggan, BindingResult result,
			Model model, RedirectAttributes flash) {
		if (result.hasErrors()) {
			return form(model, "create", "Tambah Pelanggan", "/pelanggan");
		}
		pelanggan.setIdPelanggan(null);
		pelangganRepository.save(pelanggan);
		flash.addFlashAttribute("success", "Pelanggan berhasil ditambahkan.");
		return "redirect:/pelanggan";
	}

	@GetMapping("/{id}/edit")
	public String editForm(@PathVariable Long id, Model model) {
		Pelanggan pelanggan = pelangganRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		model.addAttribute("pelanggan", pelanggan);
		return form(model, "edit", "Edit Pelanggan", "/pelanggan/" + id);
	}

	@PostMapping("/{id}")
	public String update(@PathVariable Long id, @Valid @ModelAttribute("pelanggan") Pelanggan pelanggan,
			BindingResult result, Model model, RedirectAttributes flash) {
		if (!pelangganRepository.existsById(id)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}
		if (result.hasErrors()) {
			return form(model, "edit", "Edit Pelanggan", "/pelanggan/" + id);
		}
		pelanggan.setIdPelanggan(id);
		pelangganRepository.save(pelanggan);
		flash.addFlashAttribute("success", "Pelanggan berhasil diperbarui.");
		return "redirect:/pelanggan";
	}

	@PostMapping("/{id}/delete")
	public String delete(@PathVariable Long id, RedirectAttributes flash) {
		if (pelangganRepository.existsById(id)) {
			try {
				pelangganRepository.deleteById(id);
				flash.addFlashAttribute("success", "Pelanggan berhasil dihapus.");
			}
			catch (DataIntegrityViolationException ex) {
				flash.addFlashAttribute("error",
						"Pelanggan tidak dapat dihapus karena sudah dipakai pada transaksi.");
			}
		}
		return "redirect:/pelanggan";
	}

	private String form(Model model, String mode, String title, String formAction) {
		model.addAttribute("mode", mode);
		model.addAttribute("pageTitle", title);
		model.addAttribute("formAction", formAction);
		model.addAttribute("activeMenu", "pelanggan");
		return "pelanggan/form";
	}
}
