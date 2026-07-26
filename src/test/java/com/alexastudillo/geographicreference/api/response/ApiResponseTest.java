package com.alexastudillo.geographicreference.api.response;

import com.alexastudillo.geographicreference.api.error.ApiResponseCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    @Test
    void shouldCreateAndMutateResponseEnvelope() {
        final ApiResponse<String> response = new ApiResponse<>();
        response.setStatus(200);
        response.setCode("successful");
        response.setData("value");
        response.setNextCursor("next");
        response.setPrevCursor("previous");
        response.setTotalElements(10);
        response.setTotalPages(2);
        response.setNumberOfElements(5);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getCode()).isEqualTo("successful");
        assertThat(response.getData()).isEqualTo("value");
        assertThat(response.getNextCursor()).isEqualTo("next");
        assertThat(response.getPrevCursor()).isEqualTo("previous");
        assertThat(response.getTotalElements()).isEqualTo(10);
        assertThat(response.getTotalPages()).isEqualTo(2);
        assertThat(response.getNumberOfElements()).isEqualTo(5);
    }

    @Test
    void shouldSupportCompactAndCompleteConstructors() {
        final ApiResponse<String> compact = new ApiResponse<>(200, "successful", "data");
        final ApiResponse<String> complete = new ApiResponse<>(200, "successful", "data")
                .withNextCursor("next")
                .withPrevCursor("previous")
                .withTotalElements(20)
                .withTotalPages(4)
                .withNumberOfElements(5);

        assertThat(compact.getData()).isEqualTo("data");
        assertThat(compact.getNextCursor()).isNull();
        assertThat(complete.getStatus()).isEqualTo(200);
        assertThat(complete.getNextCursor()).isEqualTo("next");
        assertThat(complete.getPrevCursor()).isEqualTo("previous");
        assertThat(complete.getTotalElements()).isEqualTo(20);
        assertThat(complete.getTotalPages()).isEqualTo(4);
        assertThat(complete.getNumberOfElements()).isEqualTo(5);
    }

    @Test
    void responseManagerShouldCreateEveryEnvelopeVariant() {
        final ResponseManager manager = new ResponseManager();

        assertThat(manager.success("data").getData()).isEqualTo("data");
        assertThat(manager.success().getCode()).isEqualTo("successful");
        assertThat(manager.error(ApiResponseCode.BAD_REQUEST).getStatus()).isEqualTo(400);

        final ApiResponse<Integer> custom = manager.customResponse(ApiResponseCode.SUCCESSFUL, 42);
        assertThat(custom.getStatus()).isEqualTo(200);
        assertThat(custom.getCode()).isEqualTo("successful");
        assertThat(custom.getData()).isEqualTo(42);
    }
}
