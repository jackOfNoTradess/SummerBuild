package com.example.SummerBuild.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

  Logger logger = LoggerFactory.getLogger(AuthController.class);

  @RequestMapping("/")
  public String index() {
    logger.info("Welcome to SummerBuild");
    return "Welcome to SummerBuild";
  }
}
