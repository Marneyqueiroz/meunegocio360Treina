package br.com.meunegocio360.core.auth.application;

import br.com.meunegocio360.core.auth.infra.RefreshTokenJpaRepository;
import java.time.Clock;
import java.time.Duration;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class RefreshTokenCleanup {
  private final RefreshTokenJpaRepository repository;
  private final Clock clock;
  public RefreshTokenCleanup(RefreshTokenJpaRepository repository, Clock clock) { this.repository = repository; this.clock = clock; }
  @Scheduled(cron = "0 20 3 * * *", zone = "UTC")
  @Transactional
  public void removerAntigos() {
    var now = clock.instant();
    repository.removerAntigos(now, now.minus(Duration.ofDays(30)));
  }
}
