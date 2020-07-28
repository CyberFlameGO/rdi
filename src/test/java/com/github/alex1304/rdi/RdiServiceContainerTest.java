package com.github.alex1304.rdi;

import static com.github.alex1304.rdi.config.FactoryMethod.constructor;
import static com.github.alex1304.rdi.config.FactoryMethod.staticFactory;
import static com.github.alex1304.rdi.config.Injectable.ref;
import static com.github.alex1304.rdi.config.Injectable.value;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.github.alex1304.rdi.config.RdiConfig;
import com.github.alex1304.rdi.config.ServiceDescriptor;

import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;

class RdiServiceContainerTest {
	
	private static final ServiceReference<A> A = ServiceReference.of("A", A.class);
	private static final ServiceReference<B> B = ServiceReference.of("B", B.class);
	private static RdiConfig conf1, conf2, conf3, conf4, conf5, conf6,
			conf7, conf8, conf9, conf10, conf11, conf12;
	
	@BeforeAll
	static void setUpBeforeClass() {
		Hooks.onOperatorDebug();
		conf1 = RdiConfig.builder()
				.registerService(ServiceDescriptor.builder(A).build())
				.build();
		conf2 = RdiConfig.builder()
				.registerService(ServiceDescriptor.builder(A)
						.setFactoryMethod(constructor(ref(A)))
						.build())
				.build();
		conf3 = RdiConfig.builder()
				.registerService(ServiceDescriptor.builder(A)
						.setFactoryMethod(constructor(ref(B)))
						.build())
				.build();
		conf4 = RdiConfig.builder()
				.registerService(ServiceDescriptor.builder(A)
						.setFactoryMethod(constructor(ref(B)))
						.build())
				.registerService(ServiceDescriptor.builder(B).build())
				.build();
		conf5 = RdiConfig.builder()
				.registerService(ServiceDescriptor.builder(A)
						.setFactoryMethod(constructor(ref(B)))
						.build())
				.registerService(ServiceDescriptor.builder(B)
						.addSetterMethod("setA", ref(A))
						.build())
				.build();
		conf6 = RdiConfig.builder()
				.registerService(ServiceDescriptor.builder(A)
						.addSetterMethod("setB", ref(B))
						.build())
				.registerService(ServiceDescriptor.builder(B)
						.addSetterMethod("setA", ref(A))
						.build())
				.build();
		conf7 = RdiConfig.builder()
				.registerService(ServiceDescriptor.builder(A)
						.setSingleton(false)
						.addSetterMethod("setB", ref(B))
						.build())
				.registerService(ServiceDescriptor.builder(B)
						.addSetterMethod("setA", ref(A))
						.build())
				.build();
		conf8 = RdiConfig.builder()
				.registerService(ServiceDescriptor.builder(A)
						.setSingleton(false)
						.addSetterMethod("setB", ref(B))
						.build())
				.registerService(ServiceDescriptor.builder(B)
						.setSingleton(false)
						.addSetterMethod("setA", ref(A))
						.build())
				.build();
		conf9 = RdiConfig.builder()
				.registerService(ServiceDescriptor.builder(A)
						.setFactoryMethod(constructor(value(1304)))
						.build())
				.build();
		conf10 = RdiConfig.builder()
				.registerService(ServiceDescriptor.builder(A)
						.addSetterMethod("setValue", value(1304))
						.build())
				.build();
		conf11 = RdiConfig.builder()
				.registerService(ServiceDescriptor.builder(A)
						.setFactoryMethod(constructor(ref(B), value(1304)))
						.build())
				.registerService(ServiceDescriptor.builder(B).build())
				.build();
		conf12 = RdiConfig.builder()
				.registerService(ServiceDescriptor.builder(B)
						.setFactoryMethod(staticFactory("create", Mono.class,
								value("test", String.class),
								value(1304)))
						.build())
				.build();
	}
	
	@Test
	void testA_NoDeps() {
		assertDoesNotThrow(() -> {
			RdiServiceContainer cont = RdiServiceContainer.create(conf1);
			A a = cont.getService(A).block();
			assertNotNull(a);
		});
	}
	
	@Test
	void testA_ErrorDependsOnItself() {
		RdiException e = assertThrows(RdiException.class, () -> RdiServiceContainer.create(conf2));
		logExpectedException(Loggers.getLogger("testA_ErrorDependsOnItself"), e);
	}
	
	@Test
	void testA_ErrorMissingRefB() {
		RdiException e = assertThrows(RdiException.class, () -> RdiServiceContainer.create(conf3));
		logExpectedException(Loggers.getLogger("testA_ErrorMissingRefB"), e);
	}
	
	@Test
	void testAInjectsBViaFactory() {
		assertDoesNotThrow(() -> {
			RdiServiceContainer cont = RdiServiceContainer.create(conf4);
			A a = cont.getService(A).block();
			assertNotNull(a);
			assertNotNull(a.b);
		});
	}
	
	@Test
	void testAInjectsBViaFactoryAndBInjectsAViaSetter() {
		assertDoesNotThrow(() -> {
			RdiServiceContainer cont = RdiServiceContainer.create(conf5);
			A a = cont.getService(A).block();
			assertNotNull(a);
			assertNotNull(a.b);
			assertNotNull(a.b.a);
			assertTrue(a == a.b.a); // A is singleton so should be the same instance
		});
	}
	
	@Test
	void testAInjectsBViaSetterAndBInjectsAViaSetter() {
		assertDoesNotThrow(() -> {
			RdiServiceContainer cont = RdiServiceContainer.create(conf6);
			A a = cont.getService(A).block();
			assertNotNull(a);
			assertNotNull(a.b);
			assertNotNull(a.b.a);
			assertTrue(a == a.b.a); // A is singleton so should be the same instance
		});
	}
	
	@Test
	void testAInjectsBViaSetterAndBInjectsAViaSetter_ANotSingleton() {
		assertDoesNotThrow(() -> {
			RdiServiceContainer cont = RdiServiceContainer.create(conf7);
			A a = cont.getService(A).block();
			assertNotNull(a);
			assertNotNull(a.b);
			assertNotNull(a.b.a);
			assertFalse(a == a.b.a); // A is NOT singleton so should be DIFFERENT instances
			assertTrue(a.b == a.b.a.b); // B however is a singleton so both A instances should share the same B
		});
	}
	
	@Test
	void testAInjectsBViaSetterAndBInjectsAViaSetter_NoSingleton_ErrorCircularInstantiation() {
		RdiServiceContainer cont = assertDoesNotThrow(() -> RdiServiceContainer.create(conf8));
		RdiException e = assertThrows(RdiException.class, () -> cont.getService(A).block());
		logExpectedException(Loggers.getLogger("testAInjectsBViaSetterAndBInjectsAViaSetter_NoSingleton_ErrorCircularInstantiation"), e);
	}
	
	@Test
	void testAInjectsValueViaFactory() {
		assertDoesNotThrow(() -> {
			RdiServiceContainer cont = RdiServiceContainer.create(conf9);
			A a = cont.getService(A).block();
			assertNotNull(a);
			assertEquals(1304, a.value);
		});
	}
	
	@Test
	void testAInjectsValueViaSetter() {
		assertDoesNotThrow(() -> {
			RdiServiceContainer cont = RdiServiceContainer.create(conf10);
			A a = cont.getService(A).block();
			assertNotNull(a);
			assertEquals(1304, a.value);
		});
	}
	
	@Test
	void testAInjectsValueAndBViaSetter() {
		assertDoesNotThrow(() -> {
			RdiServiceContainer cont = RdiServiceContainer.create(conf11);
			A a = cont.getService(A).block();
			assertNotNull(a);
			assertEquals(1304, a.value);
			assertNotNull(a.b);
		});
	}
	
	@Test
	void testBInjectsAViaStaticFactory() {
		assertDoesNotThrow(() -> {
			RdiServiceContainer cont = RdiServiceContainer.create(conf12);
			B b = cont.getService(B).block();
			assertNotNull(b);
		});
	}
	
	private static void logExpectedException(Logger logger, RdiException e) {
		logger.info("RdiException thrown as expected with message: {}{}", e.getMessage(),
				e.getCause() != null ? ", caused by " + e.getCause() : "");
	}
	
	public static class A {
		
		private B b;
		private int value;
		
		public A() {
		}
		
		public A(A a) {
		}
		
		public A(B b) {
			this.b = b;
		}
		
		public A(int value) {
			this.value = value;
		}
		
		public A(B b, int value) {
			this.b = b;
			this.value = value;
		}
		
		public void setB(B b) {
			this.b = b;
		}
		
		public void setValue(int value) {
			this.value = value;
		}
	}
	
	public static class B {
		private A a;
		
		public B() {
		}
		
		public B(A a) {
			this.a = a;
		}
		
		public void setA(A a) {
			this.a = a;
		}
		
		public static Mono<B> create(String value1, int value2) {
			return Mono.fromCallable(() -> new B());
		}
	}
}


