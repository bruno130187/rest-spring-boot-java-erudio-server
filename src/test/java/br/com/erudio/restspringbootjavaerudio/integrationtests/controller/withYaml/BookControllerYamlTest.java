package br.com.erudio.restspringbootjavaerudio.integrationtests.controller.withYaml;

import br.com.erudio.restspringbootjavaerudio.configs.TestConfigs;
import br.com.erudio.restspringbootjavaerudio.data.vo.security.AccountCredentialsVO;
import br.com.erudio.restspringbootjavaerudio.data.vo.security.TokenVO;
import br.com.erudio.restspringbootjavaerudio.integrationtests.controller.withYaml.mapper.YMLMapper;
import br.com.erudio.restspringbootjavaerudio.integrationtests.testcontainers.AbstractIntegrationTest;
import br.com.erudio.restspringbootjavaerudio.integrationtests.vo.BookVO;
import br.com.erudio.restspringbootjavaerudio.integrationtests.vo.PersonVO;
import br.com.erudio.restspringbootjavaerudio.integrationtests.vo.pagedModels.PagedModelBook;
import br.com.erudio.restspringbootjavaerudio.integrationtests.vo.pagedModels.PagedModelPerson;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.EncoderConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.filter.log.LogDetail;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.GregorianCalendar;

import static io.restassured.RestAssured.given;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestMethodOrder(OrderAnnotation.class)
@TestPropertySource("classpath:testcontainers.properties")
public class BookControllerYamlTest extends AbstractIntegrationTest{
	private static RequestSpecification requestSpecification;
	private static YMLMapper ymlMapper;
	private static BookVO bookVO;
	private final String user = "leandro";
	private final String pass = "123456";
	@BeforeAll
	public static void setup() {
		ymlMapper = new YMLMapper();
		bookVO = new BookVO();
	}
	@Test
	@Order(0)
	public void testAuthorization() throws JsonMappingException, JsonProcessingException {

		AccountCredentialsVO accountCredentialsVO = new AccountCredentialsVO(user, pass);

		var accessToken = given()
				.config(
						RestAssuredConfig.config()
								.encoderConfig(EncoderConfig.encoderConfig()
										.encodeContentTypeAs(TestConfigs.CONTENT_TYPE_YML,
												ContentType.TEXT)))
				.basePath("/auth/signin")
					.port(TestConfigs.SERVER_PORT)
					.contentType(TestConfigs.CONTENT_TYPE_YML)
				.body(accountCredentialsVO, ymlMapper)
					.when()
					.post()
				.then()
					.statusCode(200 )
					.extract()
					.body().as(TokenVO.class, ymlMapper)
					.getAccessToken();

		requestSpecification = new RequestSpecBuilder()
				.addHeader(TestConfigs.HEADER_PARAM_ORIGIN, TestConfigs.ORIGIN_LOCALHOST)
				.addHeader(TestConfigs.HEADER_PARAM_AUTHORIZATION, "Bearer " + accessToken)
				.setBasePath("/api/book/v1")
				.setPort(TestConfigs.SERVER_PORT)
				.addFilter(new RequestLoggingFilter(LogDetail.ALL))
				.addFilter(new ResponseLoggingFilter(LogDetail.ALL))
				.build();

	}
	@Test
	@Order(1)
	public void testCreate() throws JsonMappingException, JsonProcessingException {
		mockBook();

		var persistedBookVO = given()
				.spec(requestSpecification)
				.config(
						RestAssuredConfig.config()
								.encoderConfig(EncoderConfig.encoderConfig()
										.encodeContentTypeAs(TestConfigs.CONTENT_TYPE_YML,
												ContentType.TEXT)))
				.contentType(TestConfigs.CONTENT_TYPE_YML)
				.accept(TestConfigs.CONTENT_TYPE_YML)
				.body(bookVO, ymlMapper)
				.when()
				.post()
				.then()
				.statusCode(200)
				.extract()
				.body()
				.as(BookVO.class, ymlMapper);

		bookVO = persistedBookVO;
		
		assertNotNull(persistedBookVO);
		
		assertNotNull(persistedBookVO.getId());
		assertNotNull(persistedBookVO.getAuthor());
		assertNotNull(persistedBookVO.getLaunchDate());
		assertNotNull(persistedBookVO.getPrice());
		assertNotNull(persistedBookVO.getTitle());
		
		assertTrue(persistedBookVO.getId() > 0);

		GregorianCalendar gc = new GregorianCalendar(2023, 01, 27);

		assertEquals("Author Book", persistedBookVO.getAuthor());
		assertEquals(gc.getTime(), persistedBookVO.getLaunchDate());
		assertEquals(BigDecimal.TEN, persistedBookVO.getPrice());
		assertEquals("Title Book", persistedBookVO.getTitle());
	}

	@Test
	@Order(2)
	public void testUpdate() throws JsonMappingException, JsonProcessingException {
		bookVO.setAuthor("Author Book Updated 2");

		var persistedBookVO = given()
				.spec(requestSpecification)
				.config(
						RestAssuredConfig.config()
								.encoderConfig(EncoderConfig.encoderConfig()
										.encodeContentTypeAs(TestConfigs.CONTENT_TYPE_YML,
												ContentType.TEXT)))
				.contentType(TestConfigs.CONTENT_TYPE_YML)
				.accept(TestConfigs.CONTENT_TYPE_YML)
					.body(bookVO, ymlMapper)
					.when()
					.post()
				.then()
				.statusCode(200)
					.extract()
					.body()
					.as(BookVO.class, ymlMapper);

		bookVO = persistedBookVO;

		assertNotNull(persistedBookVO);

		assertNotNull(persistedBookVO.getId());
		assertNotNull(persistedBookVO.getAuthor());
		assertNotNull(persistedBookVO.getLaunchDate());
		assertNotNull(persistedBookVO.getPrice());
		assertNotNull(persistedBookVO.getTitle());

		assertEquals(persistedBookVO.getId(), bookVO.getId());

		assertTrue(persistedBookVO.getId() > 0);

		GregorianCalendar gc = new GregorianCalendar(2023, 01, 27);

		assertEquals("Author Book Updated 2", persistedBookVO.getAuthor());
		assertEquals(gc.getTime(), persistedBookVO.getLaunchDate());
		assertEquals(BigDecimal.TEN, persistedBookVO.getPrice());
		assertEquals("Title Book", persistedBookVO.getTitle());
	}

	@Test
	@Order(3)
	public void testFindById() throws JsonMappingException, JsonProcessingException {
		//mockBook();

		var persistedBookVO = given().spec(requestSpecification)
				.config(
						RestAssuredConfig.config()
								.encoderConfig(EncoderConfig.encoderConfig()
										.encodeContentTypeAs(TestConfigs.CONTENT_TYPE_YML,
												ContentType.TEXT)))
				.contentType(TestConfigs.CONTENT_TYPE_YML)
				.accept(TestConfigs.CONTENT_TYPE_YML)
				.pathParam("id", bookVO.getId())
				.when()
				.get("{id}")
				.then()
				.statusCode(200)
				.extract()
				.body()
				.as(BookVO.class, ymlMapper);

		bookVO = persistedBookVO;

		assertNotNull(persistedBookVO);

		assertNotNull(persistedBookVO.getId());
		assertNotNull(persistedBookVO.getId());
		assertNotNull(persistedBookVO.getAuthor());
		assertNotNull(persistedBookVO.getLaunchDate());
		assertNotNull(persistedBookVO.getPrice());
		assertNotNull(persistedBookVO.getTitle());
		assertTrue(persistedBookVO.getEnabled());

		assertEquals(bookVO.getId(), persistedBookVO.getId());

		assertTrue(persistedBookVO.getId() > 0);

		GregorianCalendar gc = new GregorianCalendar(2023, 01, 27);

		assertEquals("Author Book Updated 2", persistedBookVO.getAuthor());
		assertEquals(gc.getTime(), persistedBookVO.getLaunchDate());
		assertEquals(new BigDecimal(10).setScale(2), persistedBookVO.getPrice().setScale(2));
		assertEquals("Title Book", persistedBookVO.getTitle());
	}

	@Test
	@Order(4)
	public void testFindAll() throws JsonMappingException, JsonProcessingException {

		var wrapperBookVO = given()
				.spec(requestSpecification)
				.config(
						RestAssuredConfig.config()
								.encoderConfig(EncoderConfig.encoderConfig()
										.encodeContentTypeAs(TestConfigs.CONTENT_TYPE_YML,
												ContentType.TEXT)))
				.contentType(TestConfigs.CONTENT_TYPE_YML)
				.accept(TestConfigs.CONTENT_TYPE_YML)
				.queryParams("page", 0, "size", 10, "direction", "asc")
				.when()
				.get()
				.then()
				.statusCode(200)
				.extract()
				.body()
				.as(PagedModelBook.class, ymlMapper)
		;

		var books = wrapperBookVO.getContent();
		BookVO foundBookOneVO = books.get(0);

		assertNotNull(foundBookOneVO);

		assertNotNull(foundBookOneVO.getId());
		assertNotNull(foundBookOneVO.getAuthor());
		assertNotNull(foundBookOneVO.getLaunchDate());
		assertNotNull(foundBookOneVO.getPrice());
		assertNotNull(foundBookOneVO.getTitle());

		assertTrue(foundBookOneVO.getId() > 0);

		GregorianCalendar gc = new GregorianCalendar(2017, 10, 29);

		assertEquals("Ralph Johnson, Erich Gamma, John Vlissides e Richard Helm", foundBookOneVO.getAuthor());
		assertEquals(gc.getTime(), foundBookOneVO.getLaunchDate());
		assertEquals(new BigDecimal(45).setScale(2), foundBookOneVO.getPrice().setScale(2));
		assertEquals("Design Patterns", foundBookOneVO.getTitle());
	}

	@Test
	@Order(5)
	public void testFindByTitle() throws JsonMappingException, JsonProcessingException {
		//mockBook();

		var wrapperPersonVO = given()
				.spec(requestSpecification)
				.config(
						RestAssuredConfig.config()
								.encoderConfig(EncoderConfig.encoderConfig()
										.encodeContentTypeAs(TestConfigs.CONTENT_TYPE_YML,
												ContentType.TEXT)))
				.contentType(TestConfigs.CONTENT_TYPE_YML)
				.accept(TestConfigs.CONTENT_TYPE_YML)
				.pathParam("title", "code")
				.queryParams("page", 0, "size", 10, "direction", "asc")
				.when()
				.get("findBooksByTitle/{title}")
				.then()
				.statusCode(200)
				.extract()
				.body()
				.as(PagedModelBook.class, ymlMapper);

		var books = wrapperPersonVO.getContent();
		BookVO foundBookOneVO = books.get(0);

		assertNotNull(foundBookOneVO);

		assertNotNull(foundBookOneVO.getId());
		assertNotNull(foundBookOneVO.getAuthor());
		assertNotNull(foundBookOneVO.getLaunchDate());
		assertNotNull(foundBookOneVO.getPrice());
		assertNotNull(foundBookOneVO.getTitle());
		assertTrue(foundBookOneVO.getEnabled());

		assertTrue(foundBookOneVO.getId() > 0);

		GregorianCalendar gc = new GregorianCalendar(2009, 00, 10);

		assertEquals("Robert C. Martin", foundBookOneVO.getAuthor());
		assertEquals(gc.getTime(), foundBookOneVO.getLaunchDate());
		assertEquals(new BigDecimal(77).setScale(2), foundBookOneVO.getPrice().setScale(2));
		assertEquals("Clean Code", foundBookOneVO.getTitle());
	}

	@Test
	@Order(6)
	public void testFindAllWithoutToken() throws JsonMappingException, JsonProcessingException {

		RequestSpecification requestSpecificationWithoutToken = new RequestSpecBuilder()
				.setBasePath("/api/book/v1")
				.setPort(TestConfigs.SERVER_PORT)
				.addFilter(new RequestLoggingFilter(LogDetail.ALL))
				.addFilter(new ResponseLoggingFilter(LogDetail.ALL))
				.build();

		given()
				.spec(requestSpecificationWithoutToken)
				.config(
						RestAssuredConfig.config()
								.encoderConfig(EncoderConfig.encoderConfig()
										.encodeContentTypeAs(TestConfigs.CONTENT_TYPE_YML,
												ContentType.TEXT)))
				.contentType(TestConfigs.CONTENT_TYPE_YML)
				.accept(TestConfigs.CONTENT_TYPE_YML)
				.when()
				.get()
				.then()
				.statusCode(401)
				;

	}

	@Test
	@Order(7)
	public void testDisableBookById() throws JsonMappingException, JsonProcessingException {

		var persistedBookVO = given()
				.spec(requestSpecification)
				.config(
						RestAssuredConfig.config()
								.encoderConfig(EncoderConfig.encoderConfig()
										.encodeContentTypeAs(TestConfigs.CONTENT_TYPE_YML,
												ContentType.TEXT)))
				.contentType(TestConfigs.CONTENT_TYPE_YML)
				.accept(TestConfigs.CONTENT_TYPE_YML)
				.pathParam("id", bookVO.getId())
				.pathParam("enabled", 0)
				.when()
				.patch("{id}/{enabled}")
				.then()
				.statusCode(200)
				.extract()
				.body()
				.as(BookVO.class, ymlMapper);

		bookVO = persistedBookVO;

		assertNotNull(persistedBookVO);

		assertNotNull(persistedBookVO.getId());
		assertNotNull(persistedBookVO.getAuthor());
		assertNotNull(persistedBookVO.getLaunchDate());
		assertNotNull(persistedBookVO.getPrice());
		assertNotNull(persistedBookVO.getTitle());
		assertFalse(persistedBookVO.getEnabled());

		assertEquals(bookVO.getId(), persistedBookVO.getId());

		GregorianCalendar gc = new GregorianCalendar(2023, 01, 27);

		assertEquals("Author Book Updated 2", persistedBookVO.getAuthor());
		assertEquals(gc.getTime(), persistedBookVO.getLaunchDate());
		assertEquals(new BigDecimal(10).setScale(2), persistedBookVO.getPrice().setScale(2));
		assertEquals("Title Book", persistedBookVO.getTitle());
	}

	@Test
	@Order(8)
	public void testDelete() throws JsonMappingException, JsonProcessingException {
		//mockBook();

		given()
				.spec(requestSpecification)
				.config(
						RestAssuredConfig.config()
								.encoderConfig(EncoderConfig.encoderConfig()
										.encodeContentTypeAs(TestConfigs.CONTENT_TYPE_YML,
												ContentType.TEXT)))
				.contentType(TestConfigs.CONTENT_TYPE_YML)
				.accept(TestConfigs.CONTENT_TYPE_YML)
				.pathParam("id", bookVO.getId())
				.when()
				.delete("{id}")
				.then()
				.statusCode(204);

	}

	@Test
	@Order(9)
	public void testHATEOAS() throws JsonMappingException, JsonProcessingException {

		var content = given()
				.spec(requestSpecification)
				.config(
						RestAssuredConfig.config()
								.encoderConfig(EncoderConfig.encoderConfig()
										.encodeContentTypeAs(TestConfigs.CONTENT_TYPE_YML,
												ContentType.TEXT)))
				.contentType(TestConfigs.CONTENT_TYPE_YML)
				.accept(TestConfigs.CONTENT_TYPE_YML)
				.queryParams("page", 0, "size", 10, "direction", "asc")
				.when()
				.get()
				.then()
				.statusCode(200)
				.extract()
				.body()
				.asString();

		assertTrue(content.contains("- rel: \"first\"\n  href: \"http://localhost:8090/api/book/v1?direction=asc&page=0&size=10&sort=id,asc\""));
		assertTrue(content.contains("- rel: \"self\"\n  href: \"http://localhost:8090/api/book/v1?page=0&size=0&direction=asc\""));
		assertTrue(content.contains("- rel: \"next\"\n  href: \"http://localhost:8090/api/book/v1?direction=asc&page=1&size=10&sort=id,asc\""));
		assertTrue(content.contains("- rel: \"last\"\n  href: \"http://localhost:8090/api/book/v1?direction=asc&page=1&size=10&sort=id,asc\""));
		assertTrue(content.contains("page:\n  size: 10\n  totalElements: 14\n  totalPages: 2\n  number: 0"));

	}

	private void mockBook() {
		bookVO.setId(1L);
		bookVO.setAuthor("Author Book");
		GregorianCalendar gc = new GregorianCalendar(2023, 01, 27);
		bookVO.setLaunchDate(gc.getTime());
		bookVO.setPrice(BigDecimal.TEN);
		bookVO.setTitle("Title Book");
		bookVO.setEnabled(true);
	}

}
