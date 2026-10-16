package br.edu.atitus.currencyapi.servicesimpl;

import br.edu.atitus.currencyapi.clients.BcbPtaxClient;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class BcbPtaxService {
    private final BcbPtaxClient client;

    public BcbPtaxService(BcbPtaxClient client) { this.client = client; }

    // A excecao para cotacao ausente nao e armazenada em cache.
    @Cacheable(cacheNames = "bcb-ptax", key = "#currency + '-' + #date")
    @Retry(name = "bcb")
    @CircuitBreaker(name = "bcb")
    public double brlPerUnit(String currency, String date) {
        BcbPtaxClient.PtaxResult result = "USD".equals(currency)
                ? client.dollarRate("'" + date + "'", "json")
                : client.currencyRate("'" + currency + "'", "'" + date + "'", "json");
        if (result == null || result.value() == null || result.value().isEmpty()
                || result.value().get(0).sell() == null
                || result.value().get(0).sell() <= 0) {
            throw new PtaxNotFoundException("BCB nao disponibilizou cotacao para " + currency);
        }
        return result.value().get(0).sell();
    }

    public static class PtaxNotFoundException extends RuntimeException {
        public PtaxNotFoundException(String message) { super(message); }
    }
}
