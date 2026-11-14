package com.kakastudio.sms.db;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class SeedAdminPasswordTest {

  @Test
  void seededAdminHashMatchesTheDocumentedDemoPassword() throws Exception {
    String sql =
        new ClassPathResource("db/migration/V2__seed_admin.sql")
            .getContentAsString(StandardCharsets.UTF_8);
    Matcher matcher = Pattern.compile("\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}").matcher(sql);

    assertThat(matcher.find()).as("a BCrypt hash in V2__seed_admin.sql").isTrue();
    assertThat(new BCryptPasswordEncoder().matches("KakaDemo2026", matcher.group())).isTrue();
  }
}
