package com.example.marketplace.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import com.example.marketplace.model.*;
import org.springframework.stereotype.Service;

import com.example.marketplace.repository.ProdutoRepository;

@Service
public class ServicoCarrinho {

    private final ProdutoRepository repositorioProdutos;

    public ServicoCarrinho(ProdutoRepository repositorioProdutos) {
        this.repositorioProdutos = repositorioProdutos;
    }

    public ResumoCarrinho construirResumo(List<SelecaoCarrinho> selecoes) {

        List<ItemCarrinho> itens = new ArrayList<>();

        // =========================
        // Monta os itens do carrinho
        // =========================
        for (SelecaoCarrinho selecao : selecoes) {
            Produto produto = repositorioProdutos.buscarPorId(selecao.getProdutoId())
                    .orElseThrow(
                            () -> new IllegalArgumentException("Produto não encontrado: " + selecao.getProdutoId()));

            itens.add(new ItemCarrinho(produto, selecao.getQuantidade()));
        }


        BigDecimal desconto = null;
        // =========================
        // Calcula subtotal
        // =========================
        BigDecimal subtotal = itens.stream()
                .map(ItemCarrinho::calcularSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);


        int quantidadeTotalItens = itens.stream()
                .mapToInt(ItemCarrinho::getQuantidade)
                .sum();

        BigDecimal percentualDesconto = BigDecimal.ZERO;
        if (quantidadeTotalItens >= 4) {
            percentualDesconto = new BigDecimal("10");
        } else if (quantidadeTotalItens == 3) {
            percentualDesconto = new BigDecimal("7");
        } else if (quantidadeTotalItens == 2) {
            percentualDesconto = new BigDecimal("5");
        }

        BigDecimal descontoCategoria = BigDecimal.ZERO;

        for (ItemCarrinho item : itens) {
            BigDecimal descontoPorItem = BigDecimal.ZERO;

            switch (item.getProduto().getCategoria()) {
                case CAPINHA:
                case FONE:
                    descontoPorItem = new BigDecimal("3");
                    break;
                case CARREGADOR:
                    descontoPorItem = new BigDecimal("5");
                    break;
                case PELICULA:
                case SUPORTE:
                    descontoPorItem = new BigDecimal("2");
                    break;
            }

            BigDecimal descontoTotalDesteProduto = descontoPorItem.multiply(new BigDecimal(item.getQuantidade()));
            descontoCategoria = descontoCategoria.add(descontoTotalDesteProduto);
        }


        percentualDesconto = percentualDesconto.add(descontoCategoria);

        if (percentualDesconto.compareTo(new BigDecimal("25")) > 0) {
            percentualDesconto = new BigDecimal("25");
        }



        BigDecimal valorDesconto = subtotal.multiply(percentualDesconto)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);


        BigDecimal total = subtotal.subtract(valorDesconto);


        return new ResumoCarrinho(itens, subtotal, percentualDesconto, valorDesconto, total);
    }
}
