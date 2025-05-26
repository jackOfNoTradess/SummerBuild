package com.example.SummerBuild;

import io.github.cdimascio.dotenv.Dotenv;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Config {

  @PostConstruct
  public void loadEnv() {
    Dotenv dotenv = Dotenv.load();
    System.setProperty("SUPABASE_URL", dotenv.get("SUPABASE_URL"));
    System.setProperty("SUPABASE_ANON_KEY", dotenv.get("SUPABASE_ANON_KEY"));
  }
}
