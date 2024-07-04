package br.com.marcos.projeto.cursospringboot2.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/hello")
public class Controller {

    @GetMapping("/remedio")
    public String hello () {
        return "Hello World";
    }



}