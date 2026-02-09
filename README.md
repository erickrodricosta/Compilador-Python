# Compilador Python

**Autor:** Erick Rodrigues da Costa  
**RGA:** 202211310011

Este projeto é um compilador didático para um subconjunto da linguagem Python (LALG), desenvolvido em Java como **trabalho final da disciplina de Projeto de Compiladores**.

O compilador foi implementado utilizando a técnica de **Análise Sintática Descendente Recursiva**, realizando as etapas de análise léxica, sintática, semântica, geração de código objeto para uma máquina hipotética de pilha e execução através de uma Máquina Virtual.

## Estrutura do Projeto

O código fonte está organizado nos seguintes pacotes:

* **`br.com.ufmt.lexico`**: Contém o `AnalisadorLexico`, `Token` e `TipoToken`. Responsável por quebrar o código fonte em tokens.
* **`br.com.ufmt.sintatico`**: Contém o `AnalisadorSintatico`. Implementa a lógica descendente recursiva para validar a gramática e orquestrar a compilação.
* **`br.com.ufmt.semantico`**: Contém a `TabelaSimbolos`. Gerencia variáveis, funções e escopos.
* **`br.com.ufmt.gerador`**: Contém o `GeradorCodigo`. Responsável por emitir as instruções da máquina de pilha.
* **`br.com.ufmt.vm`**: Contém a `MaquinaVirtual`. Executa o código objeto gerado.
* **`br.com.ufmt.Main`**: Classe principal que executa o fluxo completo.

## Funcionalidades Suportadas

* Tipos de dados: Inteiros e Reais (Double).
* Operações aritméticas: `+`, `-`, `*`, `/`.
* Operações relacionais: `==`, `!=`, `>`, `<`, `>=`, `<=`.
* Controle de fluxo: `if`, `else`, `while`.
* Funções: Definição com `def`, parâmetros e chamadas recursivas.
* Entrada/Saída: `print(variavel)` e `input()`.
* Escopo: Variáveis globais e locais.

## Como Executar

1.  Certifique-se de ter o **Java 21** (ou superior) instalado.
2.  Coloque seu código fonte no arquivo `codigo.txt` na raiz do projeto.
3.  Compile e execute a classe `br.com.ufmt.Main`.

O compilador irá gerar o arquivo `codigo_objeto.txt` e, em seguida, a Máquina Virtual irá executá-lo automaticamente.


