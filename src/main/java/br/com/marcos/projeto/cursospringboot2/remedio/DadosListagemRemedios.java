package br.com.marcos.projeto.cursospringboot2.remedio;

import java.time.LocalDate;

public record DataListagemRemedios(String nome, Via via, String lote, Laboratorio laboratorio, LocalDate validade) {

    public DataListagemRemedios (Remedio remedio) {
        this.nome = remedio.getNome();
        this.via = remedio.getVia();
        this.lote = remedio.getLote();
        this.laboratorio = remedio.getLaboratorio();
        this.validade = remedio.getValidade();
    }
}