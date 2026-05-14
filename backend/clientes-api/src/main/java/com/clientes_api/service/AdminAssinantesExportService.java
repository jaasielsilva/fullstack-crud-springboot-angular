package com.clientes_api.service;

import com.clientes_api.dto.admin.AssinanteAdminDTO;
import com.clientes_api.repository.AdminAssinantesRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Exportação para Excel (pt-BR): UTF-8 com BOM, linha {@code sep=;} e campos separados por {@code ;}.
 */
@Service
public class AdminAssinantesExportService {

    private static final char SEP = ';';
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final AdminAssinantesRepository adminAssinantesRepository;

    public AdminAssinantesExportService(AdminAssinantesRepository adminAssinantesRepository) {
        this.adminAssinantesRepository = adminAssinantesRepository;
    }

    public byte[] exportarAssinantesCsvUtf8() {
        List<AssinanteAdminDTO> list = adminAssinantesRepository.buscarTodosAssinantes();
        StringBuilder sb = new StringBuilder();
        sb.append('\ufeff');
        sb.append("sep=").append(SEP).append('\n');
        sb.append(String.join(String.valueOf(SEP),
                "empresa_id",
                "nome_empresa",
                "documento",
                "email",
                "status_empresa",
                "plano_nome",
                "status_assinatura",
                "data_inicio",
                "data_vencimento",
                "dias_ate_vencimento_plano",
                "ultimo_pagamento_status",
                "valor_ultimo_pagamento",
                "ultimo_pagamento_data"
        ));
        sb.append('\n');
        for (AssinanteAdminDTO a : list) {
            sb.append(a.getEmpresaId()).append(SEP);
            sb.append(campo(a.getNomeEmpresa())).append(SEP);
            sb.append(campo(a.getDocumento())).append(SEP);
            sb.append(campo(a.getEmail())).append(SEP);
            sb.append(campo(a.getStatusEmpresa() != null ? a.getStatusEmpresa().name() : "")).append(SEP);
            sb.append(campo(a.getPlanoNome())).append(SEP);
            sb.append(campo(a.getStatusAssinatura() != null ? a.getStatusAssinatura().name() : "")).append(SEP);
            sb.append(campo(a.getDataInicio() != null ? ISO.format(a.getDataInicio()) : "")).append(SEP);
            sb.append(campo(a.getDataVencimento() != null ? ISO.format(a.getDataVencimento()) : "")).append(SEP);
            sb.append(a.getDiasAteVencimentoPlano() != null ? a.getDiasAteVencimentoPlano() : "").append(SEP);
            sb.append(campo(a.getUltimoPagamentoStatus() != null ? a.getUltimoPagamentoStatus().name() : "")).append(SEP);
            sb.append(a.getValorUltimoPagamento() != null ? a.getValorUltimoPagamento().toPlainString().replace('.', ',') : "").append(SEP);
            sb.append(campo(a.getUltimoPagamentoData() != null ? ISO.format(a.getUltimoPagamentoData()) : ""));
            sb.append('\n');
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    /** Campo texto: aspas se contiver separador, aspas ou quebra de linha (RFC-style para Excel). */
    private static String campo(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.replace("\r", " ").replace("\n", " ").trim();
        boolean precisaAspas = s.indexOf(SEP) >= 0 || s.indexOf('"') >= 0;
        if (precisaAspas) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
