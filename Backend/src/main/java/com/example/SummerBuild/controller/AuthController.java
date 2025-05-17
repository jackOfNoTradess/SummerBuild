package com.example.SummerBuild.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

  @RequestMapping("/")
  public String index() {
    return "Welcome to SummerBuild";
  }
}
