package com.clientes_api.config;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.TransactionDefinition;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

/**
 * Habilita o filtro Hibernate {@code tenantFilter} no início de cada transação JPA quando
 * {@link TenantContext} contém um tenant válido (&gt; 0). Rotas sem tenant (login público, jobs)
 * não aplicam o filtro.
 * <p>
 * O {@link TenantContext} precisa estar definido <strong>antes</strong> da transação iniciar
 * (ex.: filtro HTTP que roda antes do {@code @Transactional}), pois {@code doBegin} lê o tenant
 * no momento em que a transação é aberta.
 */
public class TenantAwareJpaTransactionManager extends JpaTransactionManager {

    public TenantAwareJpaTransactionManager(EntityManagerFactory emf) {
        super(emf);
    }

    @Override
    protected void doBegin(Object transaction, TransactionDefinition definition) {
        super.doBegin(transaction, definition);
        Long tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null || tenantId <= 0) {
            return;
        }
        EntityManager em = EntityManagerFactoryUtils.getTransactionalEntityManager(getEntityManagerFactory());
        Session session;
        if (em != null) {
            session = em.unwrap(Session.class);
        } else {
            session = getEntityManagerFactory().unwrap(SessionFactory.class).getCurrentSession();
        }
        session.enableFilter("tenantFilter").setParameter("tenantId", tenantId);
    }
}
