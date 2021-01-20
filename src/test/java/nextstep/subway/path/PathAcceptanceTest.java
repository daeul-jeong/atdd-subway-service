package nextstep.subway.path;

import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import nextstep.subway.AcceptanceTest;
import nextstep.subway.line.dto.LineRequest;
import nextstep.subway.line.dto.LineResponse;
import nextstep.subway.line.dto.SectionRequest;
import nextstep.subway.path.dto.PathResponse;
import nextstep.subway.station.StationAcceptanceTest;
import nextstep.subway.station.dto.StationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;


@DisplayName("지하철 경로 조회")
public class PathAcceptanceTest extends AcceptanceTest {
	private LineResponse 신분당선;
	private LineResponse 이호선;
	private LineResponse 삼호선;
	private StationResponse 강남역;
	private StationResponse 양재역;
	private StationResponse 교대역;
	private StationResponse 남부터미널역;
	private StationResponse 광교역;

	/**
	 * (4)
	 * 교대역    --- *2호선* ---   강남역
	 * |                        |
	 * *3호선* (5)             *신분당선*  (10)
	 * |                        |
	 * 남부터미널역  --- *3호선* ---   양재
	 * (3)
	 */
	@BeforeEach
	public void setUp() {
		super.setUp();

		강남역 = StationAcceptanceTest.지하철역_등록되어_있음("강남역").as(StationResponse.class);
		양재역 = StationAcceptanceTest.지하철역_등록되어_있음("양재역").as(StationResponse.class);
		교대역 = StationAcceptanceTest.지하철역_등록되어_있음("교대역").as(StationResponse.class);
		남부터미널역 = StationAcceptanceTest.지하철역_등록되어_있음("남부터미널역").as(StationResponse.class);
		광교역 = StationAcceptanceTest.지하철역_등록되어_있음("광교역").as(StationResponse.class);

		신분당선 = this.지하철_노선_등록되어_있음(new LineRequest("신분당선", "bg-red-600", 강남역.getId(), 양재역.getId(), 10, 100));
		이호선 = this.지하철_노선_등록되어_있음(new LineRequest("이호선", "bg-green-400", 교대역.getId(), 강남역.getId(), 10, 200));
		삼호선 = this.지하철_노선_등록되어_있음(new LineRequest("삼호선", "bg-yellow-600", 교대역.getId(), 양재역.getId(), 5, 300));


		SectionRequest sectionRequest = new SectionRequest(교대역.getId(), 남부터미널역.getId(), 3);
		지하철_노선에_지하철역_등록되어_있음(삼호선.getId(), sectionRequest);

		SectionRequest sectionRequest2 = new SectionRequest(양재역.getId(), 광교역.getId(), 55);
		지하철_노선에_지하철역_등록되어_있음(신분당선.getId(), sectionRequest2);
	}

	public static LineResponse 지하철_노선_등록되어_있음(LineRequest params) {
		return 지하철_노선_생성_요청(params);
	}

	public static LineResponse 지하철_노선_생성_요청(LineRequest params) {
		ExtractableResponse<Response> responseExtractableResponse = RestAssured
				.given().log().all()
				.contentType(MediaType.APPLICATION_JSON_VALUE)
				.body(params)
				.when().post("/lines")
				.then().log().all().
						extract();

		LineResponse lineResponse = responseExtractableResponse.jsonPath().getObject(".", LineResponse.class);
		return lineResponse;
	}

	public static ExtractableResponse<Response> 지하철_노선에_지하철역_등록되어_있음(Long lineId, SectionRequest sectionRequest) {
		return RestAssured
				.given().log().all()
				.contentType(MediaType.APPLICATION_JSON_VALUE)
				.body(sectionRequest)
				.when().post("/lines/{lineId}/sections", lineId)
				.then().log().all().
						extract();

	}

	@Test
	@DisplayName("최단 경로 조회")
	public void findShortestPath() {
		//http://0.0.0.0:8081/paths/?source=1&target=4
		ExtractableResponse<Response> response = RestAssured
				.given().log().all()
				.accept(MediaType.APPLICATION_JSON_VALUE)
//				.when().get(String.format("/paths?source=%d&target=%d", 1, 4))
				.when().get(String.format("/paths?source=%d&target=%d", 3, 4))
				.then().log().all()
				.extract();

		PathResponse pathResponse = response.jsonPath().getObject(".", PathResponse.class);
		assertThat(pathResponse.getStations().size()).isEqualTo(3);
	}

	@Test
	@DisplayName("출발역과 도착역이 같은 경우")
	public void whenSameSourceTarget() {
		ExtractableResponse<Response> response = RestAssured
				.given().log().all()
				.accept(MediaType.APPLICATION_JSON_VALUE)
				.when().get(String.format("/paths?source=%d&target=%d", 1, 1))
				.then().log().all()
				.extract();

		assertThat(response.statusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
	}

	@Test
	@DisplayName("출발역과 도착역이 연결이 되어 있지 않은 경우")
	public void whenDisconnectSourceTarget() {

		StationResponse 광화문역 = StationAcceptanceTest.지하철역_등록되어_있음("광화문역").as(StationResponse.class);
		StationResponse 군자역 = StationAcceptanceTest.지하철역_등록되어_있음("군자역").as(StationResponse.class);

		LineResponse 오호선 = this.지하철_노선_등록되어_있음(new LineRequest("오호선", "bg-purple-600", 광화문역.getId(), 군자역.getId(), 10, 1500));

		ExtractableResponse<Response> response = RestAssured
				.given().log().all()
				.accept(MediaType.APPLICATION_JSON_VALUE)
				.when().get(String.format("/paths?source=%d&target=%d", 1, 광화문역.getId()))
				.then().log().all()
				.extract();

		assertThat(response.statusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
	}

	@Test
	@DisplayName("존재하지 않은 출발역이나 도착역을 조회 할 경우")
	public void notExistsStation() {
		ExtractableResponse<Response> response = RestAssured
				.given().log().all()
				.accept(MediaType.APPLICATION_JSON_VALUE)
				.when().get(String.format("/paths?source=%d&target=%d", 1, 100))
				.then().log().all()
				.extract();

		assertThat(response.statusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
	}

	@Test
	@DisplayName("거리별 요금 정책 - 기본운임")
	public void checkDefaultDistanceFare() {
		//http://0.0.0.0:8081/paths/?source=1&target=4
		ExtractableResponse<Response> response = RestAssured
				.given().log().all()
				.accept(MediaType.APPLICATION_JSON_VALUE)
				.when().get(String.format("/paths?source=%d&target=%d", 3, 4))
				.then().log().all()
				.extract();

		PathResponse pathResponse = response.jsonPath().getObject(".", PathResponse.class);
		assertThat(pathResponse.getFinalFare()).isEqualTo(1250);
	}

	@Test
	@DisplayName("거리별 요금 정책 - 10km초과∼50km까지(5km마다 100원)")
	public void checkGTE10KMLT50KmKDistanceFare() {
		//http://0.0.0.0:8081/paths/?source=1&target=4
		ExtractableResponse<Response> response = RestAssured
				.given().log().all()
				.accept(MediaType.APPLICATION_JSON_VALUE)
				.when().get(String.format("/paths?source=%d&target=%d", 1, 4))
				.then().log().all()
				.extract();

		PathResponse pathResponse = response.jsonPath().getObject(".", PathResponse.class);
		assertThat(pathResponse.getFinalFare()).isEqualTo(1550);
	}

	@Test
	@DisplayName("거리별 요금 정책 - 50km 초과 시 (8km마다 100원)")
	public void checkGT50KmKDistanceFare() {
		//http://0.0.0.0:8081/paths/?source=1&target=4
		ExtractableResponse<Response> response = RestAssured
				.given().log().all()
				.accept(MediaType.APPLICATION_JSON_VALUE)
				.when().get(String.format("/paths?source=%d&target=%d", 2, 5))
				.then().log().all()
				.extract();

		PathResponse pathResponse = response.jsonPath().getObject(".", PathResponse.class);
//		System.out.println(pathResponse.getFinalFare());
		assertThat(pathResponse.getFinalFare()).isEqualTo(1950);
	}

	
}

