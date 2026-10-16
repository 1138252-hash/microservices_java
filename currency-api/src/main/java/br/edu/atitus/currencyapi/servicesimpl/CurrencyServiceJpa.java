package br.edu.atitus.currencyapi.servicesimpl;

import br.edu.atitus.currencyapi.dtos.CurrencyResponse;
import br.edu.atitus.currencyapi.entities.CurrencyEntity;
import br.edu.atitus.currencyapi.repositories.CurrencyRepository;
import br.edu.atitus.currencyapi.services.CurrencyService;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.Locale;

@Service("currencyServiceJpa")
public class CurrencyServiceJpa implements CurrencyService {
    private static final Logger log = LoggerFactory.getLogger(CurrencyServiceJpa.class);
    private final CurrencyRepository repository;
    private final BcbPtaxService bcb;

    @Value("${server.port:8100}") private String serverPort;
    @Value("${bcb.ptax.date}") private String date;

    public CurrencyServiceJpa(CurrencyRepository repository, BcbPtaxService bcb) {
        this.repository = repository;
        this.bcb = bcb;
    }

    @Override
    public CurrencyResponse findBySourceCurrencyAndTargetCurrency(String sourceCurrency,
                                                                    String targetCurrency) {
        String source = sourceCurrency.toUpperCase(Locale.ROOT);
        String target = targetCurrency.toUpperCase(Locale.ROOT);
        String environment = "Currency API running in Port: " + serverPort;
        if (source.equals(target)) {
            return new CurrencyResponse(source, target, 1.0, environment + " - same currency");
        }
        try {
            // PTAX informa quantos BRL correspondem a uma unidade da moeda.
            // Para A -> B, taxa = (BRL por A) / (BRL por B).
            double sourceBrl = "BRL".equals(source) ? 1.0 : bcb.brlPerUnit(source, date);
            double targetBrl = "BRL".equals(target) ? 1.0 : bcb.brlPerUnit(target, date);
            double rate = sourceBrl / targetBrl;
            if (!Double.isFinite(rate) || rate <= 0) throw new IllegalStateException("Invalid PTAX rate");
            return new CurrencyResponse(source, target, rate, environment + " - BCB PTAX " + date);
        } catch (Exception ex) {
            log.warn("BCB indisponivel ou sem cotacao para {} -> {}: {}. Consultando banco local.",
                    source, target, ex.toString());
            CurrencyEntity local = repository.findBySourceCurrencyAndTargetCurrency(source, target)
                    .filter(c -> c.getConversionRate() != null && c.getConversionRate() > 0)
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Cotacao nao encontrada no BCB nem no banco local para " + source + " -> " + target));
            return new CurrencyResponse(source, target, local.getConversionRate(),
                    environment + " - fallback PostgreSQL");
        }
    }
}
