package com.example.SummerBuild;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

public class SummerBuildApplicationTests {
  @Test
  public void testMainWithMock() {
    try (MockedStatic<SpringApplication> springApplicationMock =
        mockStatic(SpringApplication.class)) {
      ConfigurableApplicationContext mockContext = mock(ConfigurableApplicationContext.class);
      springApplicationMock
          .when(() -> SpringApplication.run(SummerBuildApplication.class, new String[] {}))
          .thenReturn(mockContext);

      SummerBuildApplication.main(new String[] {});

      springApplicationMock.verify(
          () -> SpringApplication.run(SummerBuildApplication.class, new String[] {}));
    }
  }
}
