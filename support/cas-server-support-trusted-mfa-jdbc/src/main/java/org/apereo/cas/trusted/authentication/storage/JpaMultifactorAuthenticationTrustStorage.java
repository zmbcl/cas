package org.apereo.cas.trusted.authentication.storage;

import org.apereo.cas.trusted.authentication.api.MultifactorAuthenticationTrustRecord;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.beanutils.BeanUtils;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * This is {@link JpaMultifactorAuthenticationTrustStorage}.
 *
 * @author Misagh Moayyed
 * @since 5.0.0
 */
@EnableTransactionManagement(proxyTargetClass = true)
@Transactional(transactionManager = "transactionManagerMfaAuthnTrust")
@Slf4j
public class JpaMultifactorAuthenticationTrustStorage extends BaseMultifactorAuthenticationTrustStorage {
    private static final String TABLE_NAME = JpaMultifactorAuthenticationTrustRecord.class.getSimpleName();

    @PersistenceContext(unitName = "mfaTrustedAuthnEntityManagerFactory")
    private transient EntityManager entityManager;

    @Override
    public void expire(final LocalDateTime onOrBefore) {
        try {
            val count = this.entityManager.createQuery("DELETE FROM " + TABLE_NAME + " r WHERE r.recordDate <= :date")
                .setParameter("date", onOrBefore)
                .executeUpdate();
            LOGGER.info("Found and removed [{}] records", count);
        } catch (final NoResultException e) {
            LOGGER.debug("No trusted authentication records could be found");
        }
    }

    @Override
    public void expire(final String key) {
        try {
            val count = this.entityManager.createQuery("DELETE FROM " + TABLE_NAME + " r where r.recordKey = :key")
                .setParameter("key", key)
                .executeUpdate();
            LOGGER.info("Found and removed [{}] records", count);
        } catch (final NoResultException e) {
            LOGGER.info("No trusted authentication records could be found");
        }
    }

    @Override
    public Set<? extends MultifactorAuthenticationTrustRecord> get(final LocalDateTime onOrAfterDate) {
        try {
            val query = this.entityManager
                .createQuery("SELECT r FROM " + TABLE_NAME + " r WHERE r.recordDate >= :date", JpaMultifactorAuthenticationTrustRecord.class)
                .setParameter("date", onOrAfterDate);
            val results = query.getResultList();
            return new HashSet<>(results);
        } catch (final NoResultException e) {
            LOGGER.debug("No trusted authentication records could be found for [{}]", onOrAfterDate);
        }
        return new HashSet<>(0);
    }

    @Override
    public Set<? extends MultifactorAuthenticationTrustRecord> get(final String principal) {
        try {
            val query = this.entityManager
                .createQuery("SELECT r FROM " + TABLE_NAME + " r where r.principal = :principal", JpaMultifactorAuthenticationTrustRecord.class)
                .setParameter("principal", principal);
            val results = query.getResultList();
            return new HashSet<>(results);
        } catch (final NoResultException e) {
            LOGGER.debug("No trusted authentication records could be found for [{}]", principal);
        }
        return new HashSet<>(0);
    }

    @Override
    public MultifactorAuthenticationTrustRecord get(final long id) {
        try {
            val query = this.entityManager
                .createQuery("SELECT r FROM " + TABLE_NAME + " r WHERE r.id >= :id", JpaMultifactorAuthenticationTrustRecord.class)
                .setParameter("id", id)
                .setMaxResults(1);
            return query.getSingleResult();
        } catch (final NoResultException e) {
            LOGGER.debug("No trusted authentication records could be found for [{}]", id);
        }
        return null;
    }

    @SneakyThrows
    @Override
    public MultifactorAuthenticationTrustRecord setInternal(final MultifactorAuthenticationTrustRecord record) {
        val destination = new JpaMultifactorAuthenticationTrustRecord();
        BeanUtils.copyProperties(destination, record);
        LOGGER.trace("Persisting multifactor authentication trust record [{}]", destination);
        return this.entityManager.merge(destination);
    }
}
