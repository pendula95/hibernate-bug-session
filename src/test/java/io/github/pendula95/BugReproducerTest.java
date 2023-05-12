package io.github.pendula95;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.rxjava3.core.Vertx;
import org.hibernate.reactive.mutiny.Mutiny;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.PostgreSQLContainer;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hibernate.cfg.AvailableSettings.*;

@ExtendWith(VertxExtension.class)
public class BugReproducerTest {

	private static final Logger logger = LoggerFactory.getLogger(BugReproducerTest.class);

	private static Mutiny.SessionFactory sessionFactory;

	@Rule
	public static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:15-alpine")
		.withUsername("postgres")
		.withPassword("password");

	@BeforeAll
	static void initDB(Vertx vertx, VertxTestContext testContext) {
		postgres.start();

		Map<String, Object> map = new HashMap<>();
		System.out.println(postgres.getJdbcUrl());
		map.put(JPA_JDBC_URL, postgres.getJdbcUrl());
		map.put(JPA_JDBC_USER, "postgres");
		map.put(JPA_JDBC_PASSWORD, "password");
		//map.put("hibernate.default_schema", "public");
		map.put("javax.persistence.sql-load-script-source", "META-INF/sql/create-test.sql");

		vertx.rxExecuteBlocking(handler -> {
			EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("postgresql-datasource-test", map);
			sessionFactory = entityManagerFactory.unwrap(Mutiny.SessionFactory.class);
			handler.complete(sessionFactory);
		}).ignoreElement().subscribe(testContext::completeNow, testContext::failNow);
	}

	@Test
	public void test1(VertxTestContext vertxTestContext, Vertx vertx) {
		Multi.createFrom().items("Lazar", "Petar", "Nikola", "Igor", "Jack")
			.onItem()
			.transformToUni(this::getTestEntityByName)
			.merge()
			.collect()
			.asList()
			.subscribe()
			.with(item -> {
				logger.info("Success {}", item.size());
				vertxTestContext.completeNow();
			}, error -> {
				logger.error("", error);
				vertxTestContext.failNow(error);
			});
	}

	public Uni<List<TestEntity>> getTestEntityByName(String name) {
		logger.info("Called for {}", name);
		CriteriaBuilder cb = sessionFactory.getCriteriaBuilder();
		CriteriaQuery<TestEntity> cq = cb.createQuery(TestEntity.class);
		Root<TestEntity> from = cq.from(TestEntity.class);


		cq.where(new Predicate[]{cb.equal(from.get(TestEntity_.NAME), name)});

		return sessionFactory.withSession(session -> {
			return session.createQuery(cq).getResultList().onItem().delayIt().by(Duration.ofMillis(100));
		});
	}
}
