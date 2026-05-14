package com.clientes_api.service;

import com.clientes_api.dto.admin.AssinanteAdminDTO;
import com.clientes_api.repository.AdminAssinantesRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Exportação CSV (UTF-8 com BOM) para o painel super admin.
 */
@Service
public class AdminAssinantesExportService {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final AdminAssinantesRepository adminAssinantesRepository;

    public AdminAssinantesExportService(AdminAssinantesRepository adminAssinantesRepository) {
        this.adminAssinantesRepository = adminAssinantesRepository;
    }

    public byte[] exportarAssinantesCsvUtf8() {
        List<AssinanteAdminDTO> list = adminAssinantesRepository.buscarTodosAssinantes();
        StringBuilder sb = new StringBuilder();
        sb.append('\ufeff');
        sb.append("empresa_id,nome_empresa,documento,email,status_empresa,plano_nome,status_assinatura,data_inicio,data_vencimento,dias_ate_vencimento_plano,ultimo_pagamento_status,valor_ultimo_pagamento,ultimo_pagamento_data\n");
        for (AssinanteAdminDTO a : list) {
            sb.append(a.getEmpresaId()).append(',');
            sb.append(csv(a.getNomeEmpresa())).append(',');
            sb.append(csv(a.getDocumento())).append(',');
            sb.append(csv(a.getEmail())).append(',');
            sb.append(csv(a.getStatusEmpresa() != null ? a.getStatusEmpresa().name() : "")).append(',');
            sb.append(csv(a.getPlanoNome())).append(',');
            sb.append(csv(a.getStatusAssinatura() != null ? a.getStatusAssinatura().name() : "")).append(',');
            sb.append(csv(a.getDataInicio() != null ? ISO.format(a.getDataInicio()) : "")).append(',');
            sb.append(csv(a.getDataVencimento() != null ? ISO.format(a.getDataVencimento()) : "")).append(',');
            sb.append(a.getDiasAteVencimentoPlano() != null ? a.getDiasAteVencimentoPlano() : "").append(',');
            sb.append(csv(a.getUltimoPagamentoStatus() != null ? a.getUltimoPagamentoStatus().name() : "")).append(',');
            sb.append(a.getValorUltimoPagamento() != null ? a.getValorUltimoPagamento().toPlainString() : "").append(',');
            sb.append(csv(a.getUltimoPagamentoData() != null ? ISO.format(a.getUltimoPagamentoData()) : ""));
            sb.append('\n');
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String csv(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.replace("\r", " ").replace("\n", " ").trim();
        if (s.contains(",") || s.contains("\"")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
