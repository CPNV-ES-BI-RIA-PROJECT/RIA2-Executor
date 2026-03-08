package com.executor;

import com.executor.config.DotenvInitializer;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
public class ExecutorApplication {

  public static void main(String[] args) {
    new SpringApplicationBuilder(ExecutorApplication.class)
        .initializers(new DotenvInitializer())
        .run(args);
  }
}
