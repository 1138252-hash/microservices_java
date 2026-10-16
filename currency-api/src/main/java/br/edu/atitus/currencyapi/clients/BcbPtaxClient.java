package br.edu.atitus.currencyapi.clients;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import java.util.List;

public interface BcbPtaxClient {
    record PtaxRate(@JsonProperty("cotacaoCompra") Double buy,
                    @JsonProperty("cotacaoVenda") Double sell) {}
    record PtaxResult(@JsonProperty("value") List<PtaxRate> value) {}

    @GetExchange("/CotacaoMoedaDia(moeda=@moeda,dataCotacao=@dataCotacao)")
    PtaxResult currencyRate(@RequestParam("@moeda") String currency,
                            @RequestParam("@dataCotacao") String date,
                            @RequestParam("$format") String format);

    @GetExchange("/CotacaoDolarDia(dataCotacao=@dataCotacao)")
    PtaxResult dollarRate(@RequestParam("@dataCotacao") String date,
                          @RequestParam("$format") String format);
}
