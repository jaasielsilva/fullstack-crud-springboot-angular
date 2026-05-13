package com.clientes_api.repository;

import com.clientes_api.dto.admin.AdminDashboardMetricsDTO;
import com.clientes_api.dto.admin.AssinanteAdminDTO;
import com.clientes_api.model.enums.StatusAssinatura;
import com.clientes_api.model.enums.StatusEmpresa;
import com.clientes_api.model.enums.StatusPagamento;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

@Repository
public class AdminAssinantesRepositoryImpl implements AdminAssinantesRepository {

    private final JdbcTemplate jdbcTemplate;

    public AdminAssinantesRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<AssinanteAdminDTO> buscarTodosAssinantes() {
        String sql = """
            SELECT 
                t.id AS empresaId, 
                t.nome AS nomeEmpresa, 
                t.cnpj AS documento, 
                t.email, 
                t.status AS statusEmpresa,
                pl.nome AS planoNome,
                a.status AS statusAssinatura,
                a.data_inicio AS dataInicio,
                a.data_fim AS dataVencimento,
                p.status AS ultimoPagamentoStatus,
                p.valor AS valorUltimoPagamento,
                p.created_at AS ultimoPagamentoData
            FROM tenants t
            LEFT JOIN assinaturas a ON a.tenant_id = t.id AND a.id = (
                SELECT MAX(id) FROM assinaturas a2 WHERE a2.tenant_id = t.id
            )
            LEFT JOIN planos pl ON a.plano_id = pl.id
            LEFT JOIN pagamentos p ON p.assinatura_id = a.id AND p.id = (
                SELECT MAX(id) FROM pagamentos p2 WHERE p2.assinatura_id = a.id
            )
            WHERE t.id != 1
            ORDER BY t.created_at DESC
        """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            AssinanteAdminDTO dto = new AssinanteAdminDTO();
            dto.setEmpresaId(rs.getLong("empresaId"));
            dto.setNomeEmpresa(rs.getString("nomeEmpresa"));
            dto.setDocumento(rs.getString("documento"));
            dto.setEmail(rs.getString("email"));
            
            String statusEmpresa = rs.getString("statusEmpresa");
            if (statusEmpresa != null) {
                dto.setStatusEmpresa(StatusEmpresa.valueOf(statusEmpresa));
            }

            dto.setPlanoNome(rs.getString("planoNome"));
            
            String statusAssinatura = rs.getString("statusAssinatura");
            if (statusAssinatura != null) {
                dto.setStatusAssinatura(StatusAssinatura.valueOf(statusAssinatura));
            }

            Timestamp dataInicio = rs.getTimestamp("dataInicio");
            if (dataInicio != null) dto.setDataInicio(dataInicio.toLocalDateTime());

            Timestamp dataVencimento = rs.getTimestamp("dataVencimento");
            if (dataVencimento != null) dto.setDataVencimento(dataVencimento.toLocalDateTime());

            String statusPagamento = rs.getString("ultimoPagamentoStatus");
            if (statusPagamento != null) {
                dto.setUltimoPagamentoStatus(StatusPagamento.valueOf(statusPagamento));
            }

            dto.setValorUltimoPagamento(rs.getBigDecimal("valorUltimoPagamento"));

            Timestamp ultimoPgData = rs.getTimestamp("ultimoPagamentoData");
            if (ultimoPgData != null) dto.setUltimoPagamentoData(ultimoPgData.toLocalDateTime());

            return dto;
        });
    }

    @Override
    public AdminDashboardMetricsDTO obterMetricas() {
        AdminDashboardMetricsDTO metrics = new AdminDashboardMetricsDTO();

        // Total Assinantes Ativos (Empresas ATIVA)
        Long ativos = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM tenants WHERE status = 'ATIVA' AND id != 1", 
            Long.class
        );
        metrics.setTotalAssinantesAtivos(ativos != null ? ativos : 0L);

        // MRR Estimado (soma dos valores das assinaturas ativas - usando a tabela de planos ou pagamentos recentes aprovados)
        BigDecimal mrr = jdbcTemplate.queryForObject("""
            SELECT SUM(pl.valor) FROM tenants t
            JOIN assinaturas a ON a.tenant_id = t.id AND a.id = (
                SELECT MAX(id) FROM assinaturas a2 WHERE a2.tenant_id = t.id
            )
            JOIN planos pl ON a.plano_id = pl.id
            WHERE t.status = 'ATIVA' AND a.status = 'ACTIVE' AND t.id != 1
        """, BigDecimal.class);
        metrics.setMrrEstimado(mrr != null ? mrr : BigDecimal.ZERO);

        // Empresas em Trial
        Long trial = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM tenants WHERE status = 'TRIAL' AND id != 1", 
            Long.class
        );
        metrics.setEmpresasEmTrial(trial != null ? trial : 0L);

        // Pagamentos Pendentes (Status pagamento PENDING ou REJECTED para o ultimo de empresas ativas/bloqueadas)
        Long pendentes = jdbcTemplate.queryForObject("""
            SELECT COUNT(*) FROM tenants t
            JOIN assinaturas a ON a.tenant_id = t.id AND a.id = (
                SELECT MAX(id) FROM assinaturas a2 WHERE a2.tenant_id = t.id
            )
            JOIN pagamentos p ON p.assinatura_id = a.id AND p.id = (
                SELECT MAX(id) FROM pagamentos p2 WHERE p2.assinatura_id = a.id
            )
            WHERE p.status IN ('PENDING', 'REJECTED', 'UNKNOWN') AND t.id != 1
        """, Long.class);
        metrics.setPagamentosPendentes(pendentes != null ? pendentes : 0L);

        return metrics;
    }
}
