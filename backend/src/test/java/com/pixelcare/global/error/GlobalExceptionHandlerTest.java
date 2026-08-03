package com.pixelcare.global.error;

import com.pixelcare.global.common.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void 없는_주소는_404로_답한다() {
        // 없는 주소를 500으로 돌려주면 주소를 잘못 부른 것인지 서버가 고장 난 것인지 알 수 없다.
        NoResourceFoundException error =
                new NoResourceFoundException(org.springframework.http.HttpMethod.GET, "/api/v1/nope");

        ResponseEntity<ApiResponse<Void>> response = handler.handleNotFound(error);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getMessage()).contains("찾을 수 없습니다");
    }

    @Test
    void 허용하지_않는_메서드는_405로_답한다() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleMethodNotAllowed(new HttpRequestMethodNotSupportedException("DELETE"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).contains("DELETE");
    }

    @Test
    void 도메인_예외는_지정한_상태코드를_그대로_쓴다() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleApiException(
                new ApiException(HttpStatus.CONFLICT, "ALREADY_SIGNED", "이미 서명된 약정입니다."));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("이미 서명된 약정입니다.");
    }
}
