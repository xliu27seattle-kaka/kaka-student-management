package com.kakastudio.sms.db;

import static org.assertj.core.api.Assertions.assertThat;

import com.kakastudio.sms.common.config.DemoDataConfig;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class DemoDataConfigTest {

  @Test
  void demoDataIsLoadedByDefault() {
    assertThat(DemoDataConfig.locationsFor(true))
        .containsExactly("classpath:db/migration", "classpath:db/demo");
  }

  @Test
  void demoDataCanBeSwitchedOff() {
    assertThat(DemoDataConfig.locationsFor(false)).containsExactly("classpath:db/migration");
  }

  @Test
  void demoAccountsUseTheDocumentedPasswordAndReservedAddresses() throws Exception {
    String sql =
        new ClassPathResource("db/demo/R__demo_accounts.sql")
            .getContentAsString(StandardCharsets.UTF_8);

    assertThat(sql).contains("amara.okafor@faculty.example").contains("lena.fischer@uni.example");
    Matcher hash = Pattern.compile("\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}").matcher(sql);
    assertThat(hash.find()).as("a BCrypt hash in R__demo_accounts.sql").isTrue();
    assertThat(new BCryptPasswordEncoder().matches("KakaDemo2026", hash.group())).isTrue();
  }
}
